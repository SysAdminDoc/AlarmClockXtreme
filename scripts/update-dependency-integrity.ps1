$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    & .\gradlew.bat resolveDependencyIntegrity `
        :app:lintVitalAnalyzePlayRelease `
        :app:lintVitalAnalyzeFdroidRelease `
        :wear:lintVitalAnalyzeRelease `
        :app:testPlayDebugUnitTest `
        :app:testFdroidDebugUnitTest `
        :wear:testDebugUnitTest `
        --write-locks `
        --write-verification-metadata sha256 `
        --no-daemon `
        --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle dependency-integrity refresh failed with exit code $LASTEXITCODE."
    }
    Write-Host "Dependency metadata refreshed. Review verification-metadata.xml and both lockfiles before committing."
} finally {
    Pop-Location
}
