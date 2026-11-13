@echo off
title Khoi Chay 6 Chat Clients
echo ===================================================================
echo   KHOI DONG DONG THOI 6 CLIENTS DE TEST THEO YEU CAU CHAM DIEM
echo   Cac Client: Alice, Bob, Charlie, David, Emma, Frank
echo ===================================================================
cd /d "%~dp0"

set "SERVER_HOST=%~1"
if "%SERVER_HOST%"=="" set "SERVER_HOST=127.0.0.1"

set "JAVA_CMD=java --module-path target\lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\* com.chatapp.client.ClientApp"

echo Dang khoi dong Client 1: Alice...
start "Chat - Alice" %JAVA_CMD% Alice %SERVER_HOST%
timeout /t 1 /nobreak >nul

echo Dang khoi dong Client 2: Bob...
start "Chat - Bob" %JAVA_CMD% Bob %SERVER_HOST%
timeout /t 1 /nobreak >nul

echo Dang khoi dong Client 3: Charlie...
start "Chat - Charlie" %JAVA_CMD% Charlie %SERVER_HOST%
timeout /t 1 /nobreak >nul

echo Dang khoi dong Client 4: David...
start "Chat - David" %JAVA_CMD% David %SERVER_HOST%
timeout /t 1 /nobreak >nul

echo Dang khoi dong Client 5: Emma...
start "Chat - Emma" %JAVA_CMD% Emma %SERVER_HOST%
timeout /t 1 /nobreak >nul

echo Dang khoi dong Client 6: Frank...
start "Chat - Frank" %JAVA_CMD% Frank %SERVER_HOST%

echo.
echo Da khoi dong toan bo 6 Clients! Hay kiem tra cac cua so tren man hinh.
pause
