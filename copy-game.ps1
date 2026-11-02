$src = "game\build\web-mobile"
$dst = "web\public\game"

if (-not (Test-Path "$src\index.html")) {
    Write-Host "No build found at $src. Build in Cocos first." -ForegroundColor Red
    exit 1
}

# Delete the old copy, and STOP if it fails (no more silent errors)
if (Test-Path $dst) { Remove-Item -Recurse -Force $dst -ErrorAction Stop }

# Copy the CONTENTS of web-mobile (the \*), so it never nests
New-Item -ItemType Directory $dst | Out-Null
Copy-Item -Recurse "$src\*" $dst

Write-Host "Copied build from" (Get-Item "$src\index.html").LastWriteTime -ForegroundColor Green