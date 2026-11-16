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
$suffix=[guid]::NewGuid().ToString('N')
$u=CallApi 'Cadastrar usuario' POST '/api/v1/usuarios' @{nome='Aluno Teste';email="aluno-$suffix@exemplo.com";matricula=$suffix} 201
$null=CallApi 'Consultar usuario' GET "/api/v1/usuarios/$($u.id)" $null 200
$null=CallApi 'Email duplicado' POST '/api/v1/usuarios' @{nome='Outro';email=$u.email;matricula="b$suffix"} 409
$null=CallApi 'Matricula duplicada' POST '/api/v1/usuarios' @{nome='Outro';email="outro-$suffix@exemplo.com";matricula=$suffix} 409
$null=CallApi 'Validacao de entrada' POST '/api/v1/usuarios' @{nome='';email='invalido';matricula=''} 400
$e=CallApi 'Cadastrar equipamento' POST '/api/v1/equipamentos' @{nome='Notebook';tipo='NOTEBOOK';patrimonio=$suffix} 201
$null=CallApi 'Consultar equipamento' GET "/api/v1/equipamentos/$($e.id)" $null 200
$null=CallApi 'Patrimonio duplicado' POST '/api/v1/equipamentos' @{nome='Projetor';tipo='PROJETOR';patrimonio=$suffix} 409
$null=CallApi 'Listar disponiveis' GET '/api/v1/equipamentos/disponiveis' $null 200
$missing=[guid]::NewGuid().ToString()
$null=CallApi 'Usuario inexistente' POST '/api/v1/emprestimos' @{usuarioId=$missing;equipamentoId=$e.id} 404
$null=CallApi 'Equipamento inexistente' POST '/api/v1/emprestimos' @{usuarioId=$u.id;equipamentoId=$missing} 404
$l=CallApi 'Solicitar emprestimo' POST '/api/v1/emprestimos' @{usuarioId=$u.id;equipamentoId=$e.id} 201
if ($l.status -ne 'ATIVO') { throw 'Status incorreto' }
$null=CallApi 'Consultar emprestimo' GET "/api/v1/emprestimos/$($l.id)" $null 200
$null=CallApi 'Consultar por usuario' GET "/api/v1/emprestimos/usuarios/$($u.id)" $null 200
$null=CallApi 'Equipamento ocupado' POST '/api/v1/emprestimos' @{usuarioId=$u.id;equipamentoId=$e.id} 409
$null=CallApi 'Reserva repetida idempotente' PUT "/api/v1/equipamentos/$($e.id)/reservas/$($l.id)" $null 200
$null=CallApi 'Outra reserva nao pode liberar' DELETE "/api/v1/equipamentos/$($e.id)/reservas/$missing" $null 409
$d=CallApi 'Registrar devolucao' PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{condicao='BOM'} 200
if ($d.status -ne 'DEVOLVIDO' -or !$d.dataDevolucao) { throw 'Devolucao incorreta' }
$d2=CallApi 'Repetir devolucao' PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{condicao='BOM'} 200
if ($d2.dataDevolucao -ne $d.dataDevolucao) { throw 'Devolucao nao idempotente' }
$null=CallApi 'Repetir liberacao' DELETE "/api/v1/equipamentos/$($e.id)/reservas/$($l.id)" $null 204
$null=CallApi 'UUID invalido' GET '/api/v1/usuarios/invalido' $null 400
$null=CallApi 'Emprestimo inexistente' GET "/api/v1/emprestimos/$missing" $null 404
# Duas chamadas simultaneas atravessam Gateway, OpenFeign e bloqueio no MySQL.
$raceBody=@{usuarioId=$u.id;equipamentoId=$e.id} | ConvertTo-Json -Compress
$client=[System.Net.Http.HttpClient]::new()
try {
    $content1=[System.Net.Http.StringContent]::new($raceBody,[System.Text.Encoding]::UTF8,'application/json')
    $content2=[System.Net.Http.StringContent]::new($raceBody,[System.Text.Encoding]::UTF8,'application/json')
    $t1=$client.PostAsync("$BaseUrl/api/v1/emprestimos",$content1)
    $t2=$client.PostAsync("$BaseUrl/api/v1/emprestimos",$content2)
    $r1=$t1.GetAwaiter().GetResult(); $r2=$t2.GetAwaiter().GetResult()
    $codes=@([int]$r1.StatusCode,[int]$r2.StatusCode) | Sort-Object
    if (($codes -join ',') -ne '201,409') { throw "Concorrencia falhou: $codes" }
    $results.Add(@{cenario='Dois emprestimos concorrentes: somente um vencedor';http='201,409';resultado='PASSOU'})
    $winner=if ([int]$r1.StatusCode -eq 201) {$r1} else {$r2}
    $loan=$winner.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    $null=CallApi 'Devolucao antiga nao libera novo emprestimo' PUT "/api/v1/emprestimos/$($l.id)/devolucao" @{condicao='BOM'} 200
    $eq=CallApi 'Nova reserva continua ocupada' GET "/api/v1/equipamentos/$($e.id)" $null 200
    if ($eq.disponivel) { throw 'Devolucao antiga liberou nova reserva!' }
    $null=CallApi 'Limpeza do emprestimo concorrente' PUT "/api/v1/emprestimos/$($loan.id)/devolucao" @{condicao='BOM'} 200
} finally { $client.Dispose() }
$root=Split-Path $PSScriptRoot -Parent
@{executadoEm=(Get-Date -Format o);total=$results.Count;cenarios=$results} | ConvertTo-Json -Depth 6 | Set-Content "$root/docs/resultado-testes.json" -Encoding utf8
Write-Host "$($results.Count) cenarios passaram. Relatorio em docs/resultado-testes.json."
