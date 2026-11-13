# Khoi dong Chat Server Dashboard
Set-Location -Path $PSScriptRoot
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "  KHOI DONG CHAT SERVER (TCP Port: 8888, UDP Voice/Video)" -ForegroundColor Yellow
Write-Host "====================================================" -ForegroundColor Cyan

java --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*" com.chatapp.server.ServerApp
