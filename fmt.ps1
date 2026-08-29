param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $KtfmtArgs
)

$ErrorActionPreference = "Stop"

$ToolDir = Join-Path $PSScriptRoot ".tools"
$JarPath = Join-Path $ToolDir "ktfmt.jar"

if (-not (Test-Path $JarPath)) {
    New-Item -ItemType Directory -Path $ToolDir -Force | Out-Null

    $release = Invoke-RestMethod  -Uri "https://api.github.com/repos/facebook/ktfmt/releases/latest" 

    $asset = $release.assets |
        Where-Object { $_.name -like "*with-dependencies.jar" } |
        Select-Object -First 1

    if (-not $asset) {
        throw "Could not find a ktfmt *with-dependencies.jar asset in the latest release."
    }

    Write-Host "Downloading ktfmt $($release.tag_name)..."

    Invoke-WebRequest `
        -Uri $asset.browser_download_url `
        -OutFile $JarPath
}

& java -jar $JarPath @KtfmtArgs

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
