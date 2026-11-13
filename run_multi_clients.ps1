# Khoi dong 6 Client dong thoi de kiem thu theo yeu cau cham diem
Set-Location -Path $PSScriptRoot
$serverHost = "127.0.0.1"
if ($args.Count -gt 0 -and -not [string]::IsNullOrWhiteSpace($args[0])) {
    $serverHost = $args[0]
}
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "  KHOI DONG DONG THOI 6 CLIENTS DE TEST THEO YEU CAU CHAM DIEM" -ForegroundColor Green
Write-Host "  Cac Client: Alice, Bob, Charlie, David, Emma, Frank" -ForegroundColor Yellow
Write-Host "===================================================================" -ForegroundColor Cyan

$clients = @("Alice", "Bob", "Charlie", "David", "Emma", "Frank")
$javaArgs = @(
    "--module-path", "target\lib",
    "--add-modules", "javafx.controls,javafx.fxml,javafx.media",
    "-cp", "target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*",
    "com.chatapp.client.ClientApp"
)

foreach ($user in $clients) {
    Write-Host "Dang khoi dong Client: $user..." -ForegroundColor White
    Start-Process java -ArgumentList ($javaArgs + @($user, $serverHost))
    Start-Sleep -Milliseconds 800
}

Write-Host "`nDa mo xong 6 Clients! Hay kiem tra cac cua so tren man hinh." -ForegroundColor Green
