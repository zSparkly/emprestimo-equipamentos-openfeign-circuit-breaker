$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $ProjectRoot
$jdk = Get-ChildItem "$ProjectRoot/.tools" -Directory -Filter 'jdk-*' | Select-Object -First 1
$maven = Get-ChildItem "$ProjectRoot/.tools" -Directory -Filter 'apache-maven-*' | Select-Object -First 1
$mysql = Get-ChildItem "$ProjectRoot/.tools" -Directory -Filter 'mysql-*-winx64' | Select-Object -First 1
if (!$jdk -or !$maven -or !$mysql) { throw 'Execute primeiro scripts/setup.ps1.' }
$env:JAVA_HOME = $jdk.FullName
$env:PATH = "$($jdk.FullName)/bin;$($maven.FullName)/bin;$($mysql.FullName)/bin;$env:PATH"
New-Item -ItemType Directory -Force "$ProjectRoot/.runtime/logs" | Out-Null

function Set-ServiceDatabasePassword([string]$ServiceName) {
    if ($ServiceName -in @('msusuario','msequipamento','msemprestimo')) {
        $passwords = Get-Content "$ProjectRoot/.runtime/db-passwords.json" -Raw | ConvertFrom-Json
        $env:DB_PASSWORD = $passwords.$ServiceName
        if (!$env:DB_PASSWORD) { throw "Senha local ausente para $ServiceName" }
    } else { Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue }
}
