$mcpDir = 'c:\Users\Admin\Documents\Coolware v2\Coolware v2\MavenMCP-1.8.9-master'
$rootDir = 'c:\Users\Admin\Documents\Coolware v2\Coolware v2'
$targetJar = Join-Path $mcpDir 'target\MCP-1.8.9-jar-with-dependencies.jar'
$coolwareJar = Join-Path $mcpDir 'target\Coolware.jar'

if (Test-Path $targetJar) {
    Copy-Item -LiteralPath $targetJar -Destination $coolwareJar -Force
    Write-Host '[+] Copied to:' $coolwareJar

    $versionsDir = Join-Path $rootDir 'versions\Coolware'
    if (Test-Path $versionsDir) {
        Copy-Item -LiteralPath $targetJar -Destination (Join-Path $versionsDir 'Coolware.jar') -Force
        Write-Host '[+] Synced to:' (Join-Path $versionsDir 'Coolware.jar')
    } else {
        Write-Host '[!] versions\Coolware dir not found, skipping jar sync'
    }

    $coolwareJson = Join-Path $mcpDir 'Coolware.json'
    if (Test-Path $coolwareJson) {
        Copy-Item -LiteralPath $coolwareJson -Destination (Join-Path $mcpDir 'target\Coolware.json') -Force
        Write-Host '[+] Copied JSON to target'
        if (Test-Path $versionsDir) {
            Copy-Item -LiteralPath $coolwareJson -Destination (Join-Path $versionsDir 'Coolware.json') -Force
            Write-Host '[+] Synced JSON to versions\Coolware'
        }
    } else {
        Write-Host '[!] Coolware.json not found, skipping JSON sync'
    }
    Write-Host '[OK] Sync complete!'
} else {
    Write-Error "Built jar not found at: $targetJar"
    exit 1
}
