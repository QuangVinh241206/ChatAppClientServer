@echo off
title Chat Client
echo ====================================================
echo   KHOI DONG 1 CLIENT CHAT JAVAFX
echo ====================================================
cd /d "%~dp0"

java --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*" com.chatapp.client.ClientApp %*
pause
