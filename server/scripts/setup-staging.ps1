<#
.SYNOPSIS
  ตั้งเซิร์ฟ staging ของ Luma ตาม server/manifest/compatibility-manifest.json

.DESCRIPTION
  1) ตรวจ Java 25
  2) ดาวน์โหลด Paper ตาม build ที่ล็อก และตรวจ SHA-256 จาก API ของ PaperMC
  3) ดาวน์โหลดปลั๊กอินจาก Modrinth ตามเวอร์ชันที่ล็อก และตรวจ SHA-512 จาก Modrinth
  4) บันทึก/ตรวจ SHA-256 ใน server/manifest/plugins.lock.json (รอบแรกสร้าง รอบถัดไปต้องตรง)
  5) คัดลอก FantasyCore jar และไฟล์ตั้งต้น (ไม่เขียนทับไฟล์ที่แก้แล้ว)
  ไม่ยอมรับ EULA ให้เอง — ใส่ -AcceptEula เมื่ออ่าน https://aka.ms/MinecraftEULA แล้วเท่านั้น

.EXAMPLE
  .\setup-staging.cmd
  .\setup-staging.cmd -AcceptEula
#>
[CmdletBinding()]
param(
    [string]$ServerDir = '',
    [switch]$AcceptEula,
    [switch]$SkipPlugins
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$UserAgent = 'Luma-Staging/0.1 (+https://github.com/ArmZOfficial/Minecraft-Luna)'
$Headers = @{ 'User-Agent' = $UserAgent }
$ServerRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$RepoRoot = (Resolve-Path (Join-Path $ServerRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($ServerDir)) { $ServerDir = Join-Path $ServerRoot 'runtime' }
$ManifestPath = Join-Path $ServerRoot 'manifest\compatibility-manifest.json'
$LockPath = Join-Path $ServerRoot 'manifest\plugins.lock.json'
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Step([string]$text) { Write-Host "==> $text" -ForegroundColor Cyan }
function Ok([string]$text) { Write-Host "    [OK] $text" -ForegroundColor Green }
function Warn([string]$text) { Write-Host "    [!] $text" -ForegroundColor Yellow }
function Fail([string]$text) { Write-Host "    [X] $text" -ForegroundColor Red; exit 1 }
function FileHash([string]$path, [string]$algorithm) {
    return (Get-FileHash -LiteralPath $path -Algorithm $algorithm).Hash.ToLowerInvariant()
}
function Download([string]$url, [string]$dest) {
    $tmp = "$dest.part"
    if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force }
    Invoke-WebRequest -Uri $url -Headers $Headers -OutFile $tmp -UseBasicParsing
    Move-Item -LiteralPath $tmp -Destination $dest -Force
}
function Retire([string]$dir, [string]$fileName) {
    # ย้ายไฟล์เวอร์ชันเก่าไป _old\ (ไม่ลบทิ้ง) เพื่อไม่ให้ปลั๊กอินซ้ำสองตัว
    $path = Join-Path $dir $fileName
    if (Test-Path -LiteralPath $path) {
        $old = Join-Path $dir '_old'
        New-Item -ItemType Directory -Force -Path $old | Out-Null
        Move-Item -LiteralPath $path -Destination (Join-Path $old $fileName) -Force
        Warn "ย้าย $fileName เก่าไปไว้ที่ plugins\_old\"
    }
}

$Manifest = Get-Content -LiteralPath $ManifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$Lock = $null
if (Test-Path -LiteralPath $LockPath) {
    $Lock = Get-Content -LiteralPath $LockPath -Raw -Encoding UTF8 | ConvertFrom-Json
}
$NewLock = [ordered]@{
    generated_at = (Get-Date).ToString('s')
    note         = 'สร้างโดย setup-staging.ps1 — commit ไฟล์นี้เพื่อล็อก hash ของ JAR ที่ผ่าน staging'
    paper        = $null
    plugins      = [ordered]@{}
}

function LockedSha([string]$id) {
    if ($null -eq $Lock) { return $null }
    if ($id -eq 'paper') { if ($Lock.paper) { return $Lock.paper } else { return $null } }
    if ($null -eq $Lock.plugins) { return $null }
    $entry = $Lock.plugins.PSObject.Properties[$id]
    if ($entry) { return $entry.Value } else { return $null }
}

# ---------------------------------------------------------------- 1. Java
Step 'ตรวจ Java'
$javaOut = ''
try { $javaOut = (cmd /c "java -version 2>&1") | Out-String } catch { $javaOut = '' }
if ($javaOut -notmatch 'version "(\d+)') {
    Fail "ไม่พบคำสั่ง java — ติดตั้ง $($Manifest.java.vendor) $($Manifest.java.major): $($Manifest.java.download)"
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt [int]$Manifest.java.major) {
    Fail "พบ Java $javaMajor แต่ Paper $($Manifest.backend.minecraft) ต้องใช้ Java $($Manifest.java.major) ขึ้นไป — $($Manifest.java.download)"
}
Ok "Java $javaMajor"

New-Item -ItemType Directory -Force -Path $ServerDir | Out-Null
$PluginsDir = Join-Path $ServerDir 'plugins'
New-Item -ItemType Directory -Force -Path $PluginsDir | Out-Null

# ---------------------------------------------------------------- 2. Paper
$b = $Manifest.backend
Step "Paper $($b.minecraft) build $($b.build)"
$buildUrl = "https://fill.papermc.io/v3/projects/paper/versions/$($b.minecraft)/builds/$($b.build)"
$build = Invoke-RestMethod -Uri $buildUrl -Headers $Headers
$download = $build.downloads.'server:default'
if ($null -eq $download) { Fail "API ของ PaperMC ไม่มี downloads.server:default ที่ $buildUrl" }
if ($build.channel) { Ok "channel จาก API: $($build.channel)" }
$paperJar = Join-Path $ServerDir $download.name
$expected = $download.checksums.sha256.ToLowerInvariant()
if (-not (Test-Path -LiteralPath $paperJar) -or (FileHash $paperJar 'SHA256') -ne $expected) {
    Download $download.url $paperJar
}
$actual = FileHash $paperJar 'SHA256'
if ($actual -ne $expected) { Remove-Item -LiteralPath $paperJar -Force; Fail "SHA-256 ของ $($download.name) ไม่ตรงกับ PaperMC" }
$locked = LockedSha 'paper'
if ($locked -and $locked.sha256 -and $locked.file -eq $download.name -and $locked.sha256 -ne $actual) {
    Fail "SHA-256 ของ $($download.name) ไม่ตรงกับ plugins.lock.json — หยุดเพื่อให้ตรวจสอบ"
}
$NewLock.paper = [ordered]@{ file = $download.name; sha256 = $actual; source = $download.url }
# ให้ start-staging.ps1 รู้ชื่อไฟล์
[IO.File]::WriteAllText((Join-Path $ServerDir 'paper-jar.txt'), $download.name, $Utf8NoBom)
Ok "$($download.name) ($actual)"

# ---------------------------------------------------------------- 3. Plugins
if (-not $SkipPlugins) {
    foreach ($p in $Manifest.plugins) {
        switch ($p.source) {
            'modrinth' {
                Step "$($p.name) $($p.version) (Modrinth)"
                # กรองด้วย loader + เวอร์ชันเกมของ backend เพื่อให้รายการเล็ก แล้วเลือก version_number ที่ล็อกไว้
                $gameVersion = if ($p.modrinth_game_version) { $p.modrinth_game_version } else { $b.minecraft }
                if ($gameVersion -ne $b.minecraft) { Warn "$($p.name) ยังไม่ประกาศรองรับ $($b.minecraft) บน Modrinth (ใช้รุ่นที่ประกาศ $gameVersion) — ต้องยืนยันบน staging" }
                $gv = [Uri]::EscapeDataString('["' + $gameVersion + '"]')
                $api = "https://api.modrinth.com/v2/project/$($p.slug)/version?loaders=%5B%22paper%22%5D&game_versions=$gv&include_changelog=false"
                $versions = Invoke-RestMethod -Uri $api -Headers $Headers
                $v = $versions | Where-Object { $_.version_number -eq $p.version } | Select-Object -First 1
                if ($null -eq $v) { Fail "ไม่พบ $($p.name) $($p.version) ที่ประกาศรองรับ paper + $gameVersion บน Modrinth — ตรวจ manifest" }
                $file = $v.files | Where-Object { $_.primary } | Select-Object -First 1
                if ($null -eq $file) { $file = $v.files | Select-Object -First 1 }
                $dest = Join-Path $PluginsDir $file.filename
                $prev = LockedSha $p.id
                if ($prev -and $prev.file -and $prev.file -ne $file.filename) { Retire $PluginsDir $prev.file }
                if (-not (Test-Path -LiteralPath $dest) -or (FileHash $dest 'SHA512') -ne $file.hashes.sha512) {
                    Download $file.url $dest
                }
                if ((FileHash $dest 'SHA512') -ne $file.hashes.sha512) {
                    Remove-Item -LiteralPath $dest -Force
                    Fail "SHA-512 ของ $($file.filename) ไม่ตรงกับ Modrinth"
                }
                $sha256 = FileHash $dest 'SHA256'
                if ($prev -and $prev.version -eq $p.version -and $prev.sha256 -and $prev.sha256 -ne $sha256) {
                    Fail "SHA-256 ของ $($file.filename) ไม่ตรงกับ plugins.lock.json — หยุดเพื่อให้ตรวจสอบ"
                }
                $NewLock.plugins[$p.id] = [ordered]@{ version = $p.version; file = $file.filename; sha256 = $sha256; source = $file.url }
                Ok "$($file.filename)"
            }
            'manual' {
                Step "$($p.name) (ดาวน์โหลดเอง — ไม่บังคับ)"
                $found = Get-ChildItem -LiteralPath $PluginsDir -Filter "$($p.file_prefix)*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($found) {
                    $NewLock.plugins[$p.id] = [ordered]@{ version = 'manual'; file = $found.Name; sha256 = (FileHash $found.FullName 'SHA256'); source = $p.url }
                    Ok "พบ $($found.Name)"
                } else {
                    Warn "ไม่มี $($p.name) — ถ้าต้องการ ดาวน์โหลดจาก $($p.url) แล้ววางใน $PluginsDir"
                }
            }
            'local' {
                Step "$($p.name) $($p.version) (build ในเครื่อง)"
                $src = Join-Path $RepoRoot $p.path
                if (Test-Path -LiteralPath $src) {
                    Get-ChildItem -LiteralPath $PluginsDir -Filter "$($p.name)-*.jar" -ErrorAction SilentlyContinue |
                        Where-Object { $_.Name -ne (Split-Path $src -Leaf) } |
                        ForEach-Object { Retire $PluginsDir $_.Name }
                    Copy-Item -LiteralPath $src -Destination $PluginsDir -Force
                    Ok "คัดลอก $(Split-Path $src -Leaf)"
                } else {
                    Warn "ยังไม่มี $src — รัน build-plugin.cmd ก่อน แล้วรัน setup อีกครั้ง"
                }
            }
        }
    }
}

# ---------------------------------------------------------------- 4. ไฟล์ตั้งต้น (ไม่เขียนทับ)
Step 'ไฟล์ตั้งต้น'
$templates = Join-Path $ServerRoot 'templates'
Get-ChildItem -LiteralPath $templates -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($templates.Length).TrimStart('\', '/')
    $target = Join-Path $ServerDir $relative
    if (Test-Path -LiteralPath $target) {
        Write-Host "    [=] มีอยู่แล้ว ไม่เขียนทับ: $relative"
    } else {
        New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target
        Ok "สร้าง $relative"
    }
}

# ---------------------------------------------------------------- 5. lock + EULA
$json = $NewLock | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText($LockPath, $json, $Utf8NoBom)
Ok "บันทึก $LockPath"

$eulaPath = Join-Path $ServerDir 'eula.txt'
if ($AcceptEula) {
    $text = "# ยอมรับโดยผู้ดูแลผ่าน setup-staging.ps1 -AcceptEula เมื่อ $(Get-Date -Format s)`r`n# https://aka.ms/MinecraftEULA`r`neula=true`r`n"
    [IO.File]::WriteAllText($eulaPath, $text, $Utf8NoBom)
    Ok 'eula=true'
} elseif (-not (Test-Path -LiteralPath $eulaPath) -or -not (Select-String -LiteralPath $eulaPath -Pattern 'eula=true' -Quiet)) {
    Warn 'ยังไม่ได้ยอมรับ Minecraft EULA — อ่าน https://aka.ms/MinecraftEULA แล้วรัน setup-staging.cmd -AcceptEula'
}

Write-Host ''
Write-Host "เสร็จแล้ว — โฟลเดอร์เซิร์ฟ: $ServerDir" -ForegroundColor Green
Write-Host 'ขั้นต่อไป: start-staging.cmd แล้วทำตาม server\README-th.md (setup LuckPerms/WorldGuard + checklist)'
