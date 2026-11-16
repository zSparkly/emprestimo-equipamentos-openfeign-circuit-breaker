param([switch]$SkipBuild)
. "$PSScriptRoot/env.ps1"
if (!$SkipBuild) {
    # No Windows, os arquivos JAR ficam bloqueados enquanto as aplicações estão abertas.
    & "$PSScriptRoot/stop.ps1"
    & "$PSScriptRoot/build.ps1"
}
$data = "$ProjectRoot/.runtime/mysql-data"
$mysqlPid = "$ProjectRoot/.runtime/mysql.pid"
if (!(Test-Path $data)) {
    & mysqld.exe --no-defaults --initialize-insecure "--basedir=$($mysql.FullName)" "--datadir=$data" --console
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao inicializar MySQL. Verifique o Visual C++ Redistributable x64.' }
}
$running = $false
if (Test-Path $mysqlPid) { $running = [bool](Get-Process -Id ([int](Get-Content $mysqlPid)) -ErrorAction SilentlyContinue) }
if (!$running) {
    $p = Start-Process "$($mysql.FullName)/bin/mysqld.exe" -ArgumentList @('--no-defaults',"--basedir=`"$($mysql.FullName)`"","--datadir=`"$data`"",'--port=3307','--bind-address=127.0.0.1','--mysqlx=0','--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput '.runtime/logs/mysql.out.log' -RedirectStandardError '.runtime/logs/mysql.err.log'
    $p.Id | Set-Content $mysqlPid
}
$ready = $false
for ($i=0; $i -lt 60; $i++) {
    & mysqladmin.exe --protocol=TCP -h 127.0.0.1 -P 3307 -u root ping 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready=$true; break }
    Start-Sleep -Seconds 1
}
if (!$ready) { throw 'MySQL nao iniciou; consulte .runtime/logs/mysql.err.log.' }
$passwordFile = "$ProjectRoot/.runtime/db-passwords.json"
if (!(Test-Path $passwordFile)) {
    $passwords = @{}
    foreach ($service in @('msusuario','msequipamento','msemprestimo')) {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $passwords[$service] = ([BitConverter]::ToString($bytes)).Replace('-','')
    }
    $passwords | ConvertTo-Json | Set-Content -Encoding utf8 $passwordFile
}
$passwords = Get-Content $passwordFile -Raw | ConvertFrom-Json
$sql = Get-Content "$PSScriptRoot/bancos.sql" -Raw
foreach ($service in @('msusuario','msequipamento','msemprestimo')) {
    $password = $passwords.$service
    if ($password -notmatch '^[A-F0-9]{64}$') { throw "Senha local invalida para $service" }
    $sql = $sql.Replace("{{SENHA_$($service.ToUpper())}}", $password)
}
$sql | & mysql.exe --protocol=TCP -h 127.0.0.1 -P 3307 -u root
if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar bancos.' }
$apps = [ordered]@{'discovery-server'=8761;'msusuario'=8081;'msequipamento'=8082;'msemprestimo'=8083;'api-gateway'=8080}
foreach ($name in $apps.Keys) {
    $pidFile = "$ProjectRoot/.runtime/$name.pid"
    $exists = $false
    if (Test-Path $pidFile) { $exists=[bool](Get-Process -Id ([int](Get-Content $pidFile)) -ErrorAction SilentlyContinue) }
    if (!$exists) {
        $jar = "$ProjectRoot/$name/target/$name-1.0.0.jar"
        Set-ServiceDatabasePassword $name
        $p = Start-Process "$env:JAVA_HOME/bin/java.exe" -ArgumentList @('-Xms64m','-Xmx256m','-jar',"`"$jar`"") -WindowStyle Hidden -PassThru -RedirectStandardOutput ".runtime/logs/$name.out.log" -RedirectStandardError ".runtime/logs/$name.err.log"
        $p.Id | Set-Content $pidFile
    }
    $healthy = $false
    for ($i=0; $i -lt 120; $i++) {
        try { $h=Invoke-RestMethod "http://127.0.0.1:$($apps[$name])/actuator/health"; if ($h.status -eq 'UP') { $healthy=$true; break } } catch {}
        Start-Sleep -Seconds 1
    }
    if (!$healthy) { throw "$name nao iniciou; consulte .runtime/logs." }
    Write-Host "$name pronto na porta $($apps[$name])"
}
Write-Host 'Aguardando propagacao do registro Eureka...'
Start-Sleep -Seconds 15
Write-Host 'Sistema pronto: http://localhost:8080 | Eureka: http://localhost:8761'
