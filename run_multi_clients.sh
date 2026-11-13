#!/bin/bash

# ===================================================================
#   KHOI DONG DONG THOI 6 CLIENTS TREN LINUX DE CHAM DIEM
#   Cac Client: Alice, Bob, Charlie, David, Emma, Frank
# ===================================================================

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "==================================================================="
echo "  KHOI DONG DONG THOI 6 CLIENTS DE TEST THEO YEU CAU CHAM DIEM"
echo "  Cac Client: Alice, Bob, Charlie, David, Emma, Frank"
echo "==================================================================="

if [ ! -d "target/lib" ] || [ ! -f "target/ChatAppClientServer-1.0-SNAPSHOT.jar" ]; then
    echo "Chua tim thay thu vien da dong goi, tien hanh bien dich bang Maven..."
    mvn clean package -DskipTests
fi

JAVA_CMD="java --module-path target/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp target/ChatAppClientServer-1.0-SNAPSHOT.jar:target/lib/* com.chatapp.client.ClientApp"

CLIENTS=("Alice" "Bob" "Charlie" "David" "Emma" "Frank")

for user in "${CLIENTS[@]}"; do
    echo "Dang khoi dong Client: $user..."
    $JAVA_CMD "$user" &
    sleep 0.8
done

echo ""
echo "Da khoi dong toan bo 6 Clients tren Linux! Hay kiem tra cac cua so tren man hinh."
