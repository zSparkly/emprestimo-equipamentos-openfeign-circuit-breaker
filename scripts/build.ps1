. "$PSScriptRoot/env.ps1"
& mvn.cmd -B -ntp verify
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar.' }
