#!/bin/bash

# ====================================================
#   KHOI DONG CHAT SERVER TREN LINUX (Ubuntu, Debian, Fedora, Arch)
#   TCP Port: 8888, UDP Voice/Video
# ====================================================

# Di chuyen den thu muc chua script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "===================================================="
echo "  KHOI DONG CHAT SERVER (TCP Port: 8888, UDP Voice/Video)"
echo "===================================================="

# Neu chua bien dich target/lib tren Linux, tu dong chay mvn package
if [ ! -d "target/lib" ] || [ ! -f "target/ChatAppClientServer-1.0-SNAPSHOT.jar" ]; then
    echo "Chua tim thay thu vien da dong goi, tien hanh bien dich bang Maven..."
    mvn clean package -DskipTests
fi

# Chay ung dung Server tren Linux voi classpath su dung dau hai cham (:)
java --module-path target/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -cp "target/classes:target/ChatAppClientServer-1.0-SNAPSHOT.jar:target/lib/*" \
     com.chatapp.server.ServerApp "$@"
