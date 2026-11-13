#!/bin/bash

# ====================================================
#   KHOI DONG 1 CLIENT CHAT JAVAFX TREN LINUX
# ====================================================

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

if [ ! -d "target/lib" ] || [ ! -f "target/ChatAppClientServer-1.0-SNAPSHOT.jar" ]; then
    echo "Chua tim thay thu vien da dong goi, tien hanh bien dich bang Maven..."
    mvn clean package -DskipTests
fi

java --module-path target/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -cp "target/ChatAppClientServer-1.0-SNAPSHOT.jar:target/lib/*" \
     com.chatapp.client.ClientApp "$@"
