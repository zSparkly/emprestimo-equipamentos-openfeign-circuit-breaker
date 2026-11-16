param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Net.Http
$results = [System.Collections.Generic.List[object]]::new()
function CallApi([string]$label,[string]$method,[string]$path,[object]$body,[int]$expected) {
    $params=@{Method=$method;Uri="$BaseUrl$path";ContentType='application/json';UseBasicParsing=$true}
    if ($PSVersionTable.PSVersion.Major -ge 7) { $params.SkipHttpErrorCheck=$true }
    if ($null -ne $body) { $params.Body=($body | ConvertTo-Json -Compress) }
    try { $r=Invoke-WebRequest @params }
    catch {
        if (!$_.Exception.Response) { throw }
        $response=$_.Exception.Response
        $reader=[System.IO.StreamReader]::new($response.GetResponseStream())
        try { $r=@{StatusCode=[int]$response.StatusCode;Content=$reader.ReadToEnd()} } finally { $reader.Dispose() }
    }
    if ([int]$r.StatusCode -ne $expected) { throw "$label : esperado $expected, recebido $($r.StatusCode): $($r.Content)" }
    $results.Add(@{cenario=$label;http=[int]$r.StatusCode;resultado='PASSOU'})
    Write-Host "PASSOU: $label ($expected)"
    if ($r.Content) { return ($r.Content | ConvertFrom-Json) }
}

. "$PSScriptRoot/env.ps1"
function Conferir($condicao, $mensagem) { if (!$condicao) { throw $mensagem } }
function Circuito { (Invoke-RestMethod 'http://127.0.0.1:8083/actuator/circuitbreakers').circuitBreakers.alterarCondicao }
$sufixo=[guid]::NewGuid().ToString('N')
$u=CallApi 'Preparar usuario' POST '/api/v1/usuarios' @{nome='Daniel';email="daniel-$sufixo@exemplo.com";matricula=$sufixo} 201
foreach ($condicao in @('BOM','AVARIADO')) {
  $e=CallApi "Cadastrar para $condicao" POST '/api/v1/equipamentos' @{nome='Notebook da sala';tipo='NOTEBOOK';patrimonio="$sufixo-$condicao"} 201
  Conferir ($e.condicao -eq 'BOM') 'Equipamento novo deve iniciar BOM'
  $l=CallApi 'Criar emprestimo' POST '/api/v1/emprestimos' @{usuarioId=$u.id;equipamentoId=$e.id} 201
  $d=CallApi "Devolver com $condicao" PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{condicao=$condicao} 200
  Conferir ($d.status -eq 'DEVOLVIDO' -and $d.dataDevolucao) 'Devolucao nao concluida'
  $atual=CallApi 'Conferir condicao e reserva' GET "/api/v1/equipamentos/$($e.id)" $null 200
  Conferir ($atual.condicao -eq $condicao -and $atual.disponivel) 'Condicao ou reserva incorreta'
  $repetida=CallApi 'Repetir devolucao com outra condicao' PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{condicao='BOM'} 200
  Conferir ($repetida.dataDevolucao -eq $d.dataDevolucao) 'Repeticao alterou data'
  $atual=CallApi 'Conferir repeticao sem sobrescrever condicao' GET "/api/v1/equipamentos/$($e.id)" $null 200
  Conferir ($atual.condicao -eq $condicao) 'Repeticao sobrescreveu condicao'
}
$null=CallApi 'CT03 equipamento inexistente' PUT "/api/v1/equipamentos/$([guid]::NewGuid())/condicao" @{condicao='BOM'} 404
$null=CallApi 'Condicao invalida' PUT "/api/v1/equipamentos/$($e.id)/condicao" @{condicao='RUIM'} 400
$null=CallApi 'Condicao obrigatoria na devolucao' PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{} 400
$pendentes=@()
1..3 | ForEach-Object {
  $e=CallApi 'Equipamento para indisponibilidade' POST '/api/v1/equipamentos' @{nome='Projetor';tipo='PROJETOR';patrimonio="$sufixo-falha-$_"} 201
  $l=CallApi 'Emprestimo para indisponibilidade' POST '/api/v1/emprestimos' @{usuarioId=$u.id;equipamentoId=$e.id} 201
  $pendentes+=@{emprestimo=$l.id;equipamento=$e.id}
}
# O processo parado pertence somente a este projeto e sera restaurado no finally.
Stop-Process -Id ([int](Get-Content '.runtime/msequipamento.pid')) -ErrorAction Stop
try {
  foreach ($item in $pendentes) {
    $null=CallApi 'CT04 falha remota' PUT "/api/v1/emprestimos/$($item.emprestimo)/devolucao" @{condicao='AVARIADO'} 503
    $ativo=CallApi 'Emprestimo continua ativo' GET "/api/v1/emprestimos/$($item.emprestimo)" $null 200
    Conferir ($ativo.status -eq 'ATIVO' -and !$ativo.dataDevolucao) 'Falha concluiu devolucao'
  }
  $estado=Circuito
  Conferir ($estado.state -eq 'OPEN') 'CT05 circuito nao abriu'
  $falhasAntes=$estado.failedCalls
  $null=CallApi 'CT05 chamada bloqueada' PUT "/api/v1/emprestimos/$($pendentes[0].emprestimo)/devolucao" @{condicao='BOM'} 503
  Conferir ((Circuito).failedCalls -eq $falhasAntes) 'Chamada bloqueada foi enviada'
} finally {
  $jar="$ProjectRoot/msequipamento/target/msequipamento-1.0.0.jar"
  Set-ServiceDatabasePassword 'msequipamento'
  $processo=Start-Process "$env:JAVA_HOME/bin/java.exe" -ArgumentList @('-Xms64m','-Xmx256m','-jar',"`"$jar`"") -WindowStyle Hidden -PassThru -RedirectStandardOutput '.runtime/logs/msequipamento.out.log' -RedirectStandardError '.runtime/logs/msequipamento.err.log'
  $processo.Id | Set-Content '.runtime/msequipamento.pid'
  $pronto=$false
  for ($i=0;$i -lt 90;$i++) {
    try { $h=Invoke-RestMethod 'http://127.0.0.1:8082/actuator/health'; if ($h.status -eq 'UP') {$pronto=$true;break} } catch {}
    Start-Sleep -Seconds 1
  }
  Conferir $pronto 'Equipamento nao reiniciou'
}
Start-Sleep -Seconds 31
$null=CallApi 'CT06 primeira chamada de recuperacao' PUT "/api/v1/emprestimos/$($pendentes[0].emprestimo)/devolucao" @{condicao='BOM'} 200
Conferir ((Circuito).state -eq 'HALF_OPEN') 'Nao passou por HALF_OPEN'
$null=CallApi 'CT06 segunda chamada de recuperacao' PUT "/api/v1/emprestimos/$($pendentes[1].emprestimo)/devolucao" @{condicao='AVARIADO'} 200
Conferir ((Circuito).state -eq 'CLOSED') 'Circuito nao fechou'
$null=CallApi 'Concluir ultimo emprestimo de teste' PUT "/api/v1/emprestimos/$($pendentes[2].emprestimo)/devolucao" @{condicao='BOM'} 200
$eventos=Invoke-RestMethod 'http://127.0.0.1:8083/actuator/circuitbreakerevents'
$eventos | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 'docs/eventos-circuito-etapa2.json'
@{executadoEm=(Get-Date -Format o);cenarios=$results;estadoFinal=(Circuito)} | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 'docs/resultado-etapa2.json'
Write-Host 'Etapa 2 aprovada: sucesso, fallback, OPEN, HALF_OPEN e CLOSED.'
