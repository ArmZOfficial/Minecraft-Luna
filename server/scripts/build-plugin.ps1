<#
.SYNOPSIS
  Build FantasyCore ด้วย Gradle wrapper แล้วคัดลอก jar เข้าเซิร์ฟ staging (ถ้ามี)
.DESCRIPTION
  ต้องมี Java 25 (หรือให้ Gradle toolchain ดาวน์โหลดให้) — ครั้งแรก Gradle จะดาวน์โหลดตัวเองและ dependency
#>
[CmdletBinding()]
param(
    [switch]$SkipTests,
    [string]$ServerDir = ''
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ServerRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$RepoRoot = (Resolve-Path (Join-Path $ServerRoot '..')).Path
$Project = Join-Path $RepoRoot 'fantasycore'
if ([string]::IsNullOrWhiteSpace($ServerDir)) { $ServerDir = Join-Path $ServerRoot 'runtime' }

$tasks = if ($SkipTests) { @('clean', 'jar') } else { @('clean', 'build') }
Write-Host "==> gradlew $($tasks -join ' ')" -ForegroundColor Cyan
Push-Location $Project
try {
    & .\gradlew.bat @tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle ล้มเหลว (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

$jar = Get-ChildItem -LiteralPath (Join-Path $Project 'build\libs') -Filter 'FantasyCore-*.jar' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $jar) { throw 'ไม่พบไฟล์ FantasyCore-*.jar หลัง build' }
Write-Host "    [OK] $($jar.FullName)" -ForegroundColor Green

$plugins = Join-Path $ServerDir 'plugins'
if (Test-Path -LiteralPath $plugins) {
    Get-ChildItem -LiteralPath $plugins -Filter 'FantasyCore-*.jar' | Where-Object { $_.Name -ne $jar.Name } | ForEach-Object {
        $old = Join-Path $plugins '_old'
        New-Item -ItemType Directory -Force -Path $old | Out-Null
        Move-Item -LiteralPath $_.FullName -Destination (Join-Path $old $_.Name) -Force
    }
    Copy-Item -LiteralPath $jar.FullName -Destination $plugins -Force
    Write-Host "    [OK] คัดลอกเข้า $plugins แล้ว — รีสตาร์ตเซิร์ฟ (ห้ามใช้ /reload)" -ForegroundColor Green
} else {
    Write-Host '    ยังไม่มีโฟลเดอร์เซิร์ฟ — รัน setup-staging.cmd แล้วระบบจะคัดลอก jar ให้' -ForegroundColor Yellow
}
