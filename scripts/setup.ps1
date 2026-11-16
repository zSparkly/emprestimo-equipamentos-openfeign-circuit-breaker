$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
New-Item -ItemType Directory -Force .tools | Out-Null
$downloads = @(
    @{Pattern='jdk-*'; Url='https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'; File='jdk.zip'},
    @{Pattern='apache-maven-*'; Url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'; File='maven.zip'},
    @{Pattern='mysql-*-winx64'; Url='https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.13-winx64.zip'; File='mysql.zip'}
)
foreach ($d in $downloads) {
    if (!(Get-ChildItem .tools -Directory -Filter $d.Pattern)) {
        Write-Host "Baixando $($d.File)..."
        Invoke-WebRequest $d.Url -OutFile ".tools/$($d.File)"
        Expand-Archive ".tools/$($d.File)" .tools -Force
    }
}
. "$PSScriptRoot/env.ps1"
& java -version
Write-Host 'Ferramentas portateis prontas, sem alterar o PATH global.'
