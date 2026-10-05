<#
.SYNOPSIS
  เปิดเซิร์ฟ staging ของ Luma (ใช้ Paper ที่ setup-staging.ps1 ดาวน์โหลดไว้)
.EXAMPLE
  .\start-staging.cmd
  .\start-staging.cmd -MemoryGB 6
#>
[CmdletBinding()]
param(
    [string]$ServerDir = '',
    [ValidateRange(2, 64)][int]$MemoryGB = 4
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ServerRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($ServerDir)) { $ServerDir = Join-Path $ServerRoot 'runtime' }

$jarNameFile = Join-Path $ServerDir 'paper-jar.txt'
if (-not (Test-Path -LiteralPath $jarNameFile)) {
    Write-Host 'ยังไม่ได้ตั้งเซิร์ฟ — รัน setup-staging.cmd ก่อน' -ForegroundColor Red
    exit 1
}
$jar = (Get-Content -LiteralPath $jarNameFile -Raw).Trim()
$eula = Join-Path $ServerDir 'eula.txt'
if (-not (Test-Path -LiteralPath $eula) -or -not (Select-String -LiteralPath $eula -Pattern 'eula=true' -Quiet)) {
    Write-Host 'ยังไม่ได้ยอมรับ Minecraft EULA — อ่าน https://aka.ms/MinecraftEULA แล้วรัน setup-staging.cmd -AcceptEula' -ForegroundColor Yellow
    exit 1
}

# แฟล็ก JVM ชุดที่ PaperMC แนะนำ (Aikar's flags)
$mem = "${MemoryGB}G"
$flags = @(
    "-Xms$mem", "-Xmx$mem",
    '-XX:+AlwaysPreTouch', '-XX:+DisableExplicitGC', '-XX:+ParallelRefProcEnabled', '-XX:+PerfDisableSharedMem',
    '-XX:+UnlockExperimentalVMOptions', '-XX:+UseG1GC', '-XX:G1HeapRegionSize=8M', '-XX:G1HeapWastePercent=5',
    '-XX:G1MaxNewSizePercent=40', '-XX:G1MixedGCCountTarget=4', '-XX:G1MixedGCLiveThresholdPercent=90',
    '-XX:G1NewSizePercent=30', '-XX:G1RSetUpdatingPauseTimePercent=5', '-XX:G1ReservePercent=20',
    '-XX:InitiatingHeapOccupancyPercent=15', '-XX:MaxGCPauseMillis=200', '-XX:MaxTenuringThreshold=1',
    '-XX:SurvivorRatio=32', '-Dusing.aikars.flags=https://mcflags.emc.gs', '-Daikars.new.flags=true',
    '-Dfile.encoding=UTF-8',
    '-jar', $jar, '--nogui'
)

Write-Host "เปิด $jar (RAM $mem) ที่ $ServerDir — พิมพ์ stop เพื่อปิดอย่างปลอดภัย" -ForegroundColor Cyan
Push-Location $ServerDir
try {
    & java @flags
} finally {
    Pop-Location
}
