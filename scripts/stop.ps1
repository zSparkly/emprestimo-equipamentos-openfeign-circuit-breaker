$root = Split-Path $PSScriptRoot -Parent
$runtime = Join-Path $root '.runtime'
if (!(Test-Path $runtime)) { return }
foreach ($pidFile in Get-ChildItem $runtime -Filter '*.pid') {
    $proc = Get-Process -Id ([int](Get-Content $pidFile.FullName)) -ErrorAction SilentlyContinue
    if ($proc) {
        $expected = if ($pidFile.BaseName -eq 'mysql') { 'mysqld' } else { 'java' }
        if ($proc.ProcessName -eq $expected -and $proc.Path.StartsWith((Join-Path $root '.tools'),[System.StringComparison]::OrdinalIgnoreCase)) { Stop-Process -Id $proc.Id }
    }
    Remove-Item -LiteralPath $pidFile.FullName
}
Write-Host 'Processos locais do projeto encerrados.'
