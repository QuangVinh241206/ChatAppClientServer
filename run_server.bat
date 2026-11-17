@echo off
title Chat Server Dashboard
echo ====================================================
echo   KHOI DONG CHAT SERVER (TCP Port: 8888, UDP Voice/Video)
echo ====================================================
cd /d "%~dp0"

java --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\classes;target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*" com.chatapp.server.ServerApp
pause
