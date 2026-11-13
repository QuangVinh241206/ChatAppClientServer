# ỨNG DỤNG CHAT CLIENT - SERVER BẰNG JAVAFX (LẬP TRÌNH MẠNG)

> **Dự án mô phỏng ứng dụng nhắn tin hiện đại tương tự Zalo / Messenger / WhatsApp**, xây dựng trên nền tảng **JavaFX** theo mô hình **Client - Server**.
> Không yêu cầu Database ngoài (quản lý In-Memory), hỗ trợ đầy đủ các yêu cầu chấm điểm của môn học Lập Trình Mạng.

---

## 📌 BẢNG ĐỐI CHIẾU YÊU CẦU & TÍNH NĂNG ĐÃ TRIỂN KHAI

| Yêu Cầu Đề Bài | Hiện Thực Trong Dự Án | Giao Thức Mạng |
| :--- | :--- | :---: |
| **1. Giao diện thân thiện (tương tự Zalo, Messenger)** | Thiết kế hiện đại với JavaFX: Sidebar avatar cá nhân, bong bóng chat xanh/xám, thời gian, trạng thái online, badge, thanh công cụ mượt mà. | JavaFX CSS / FXML |
| **2. Có Multicast và Broadcast** | - **Broadcast:** Kênh chung phát tán tin nhắn tới toàn bộ Client trên Server.<br>- **Multicast Room:** Nhóm chat theo phòng (Group Chat).<br>- **IP Multicast Lab:** Kỹ thuật UDP `MulticastSocket` IP lớp D `230.0.0.1:4446`. | **TCP** (Room/Broadcast)<br>& **UDP** (IP Multicast) |
| **3. Có Icon biểu cảm (Emoji)** | Bảng popup chọn Emoji phong phú (😀, 😂, 😍, 👍, ❤️, 🔥, 🎉, v.v.), chèn trực tiếp vào khung chat. | TCP |
| **4. Có text - Audio - Video** | - **Text:** Tin nhắn văn bản tức thời.<br>- **Audio Voice Note:** Ghi âm trực tiếp từ Microphone bằng `javax.sound.sampled`, gửi và nghe lại ngay trên bong bóng chat.<br>- **Voice Call realtime:** Gọi điện thoại đàm thoại trực tiếp qua luồng UDP.<br>- **Video Call realtime:** Cuộc gọi video trực tiếp truyền các frame hình ảnh động qua UDP Datagram. | **TCP** (Text, Voice Note)<br>**UDP** (Voice Call, Video Call) |
| **5. Đính kèm tệp tin** | Chọn và gửi bất kỳ tệp tin nào (Ảnh PNG/JPG có ảnh xem trước, file PDF, Word, Zip, MP4). Hỗ trợ tải về máy an toàn. | **TCP** (đảm bảo không rách/lỗi file) |
| **6. Không cần database** | Quản lý phiên, danh sách người dùng, phòng nhóm hoàn toàn trong bộ nhớ RAM máy chủ (`ConcurrentHashMap`). | In-Memory |
| **7. Chạy 5 đến 6 Client đồng thời** | Có sẵn script khởi động 1-click mở đồng thời 6 Client (`Alice`, `Bob`, `Charlie`, `David`, `Emma`, `Frank`) chat chéo nhau mượt mà. | Multi-threading |
| **8. Kiểm tra theo tình huống (TCP - UDP)** | Phân định rạch ròi kiến trúc TCP và UDP để sinh viên tự tin giải thích và trả lời vấn đáp với giảng viên. | Chi tiết bên dưới |

---

## 🚀 CÁCH KHỞI CHẠY NHANH NHẤT (1-CLICK RUN)

Dự án đã được biên dịch sẵn toàn bộ thư viện JavaFX và mã nguồn vào thư mục `target/`. Bạn chỉ cần click các file script sau:

### Cách 1: Sử dụng file `.bat` (Khuyên dùng trên Windows)
1. **Bước 1 (Bật Server):** Nhấp đúp chuột vào file **`run_server.bat`** (hoặc chuột phải chọn Open).
   - Cửa sổ **Server Dashboard** sẽ hiện ra và tự động mở cổng TCP `8888`.
2. **Bước 2 (Chạy 6 Client để chấm điểm):** Nhấp đúp chuột vào file **`run_multi_clients.bat`**.
   - Hệ thống sẽ tự động bật 6 cửa sổ Client với tên: `Alice`, `Bob`, `Charlie`, `David`, `Emma`, `Frank` và tự động đăng nhập kết nối đến Server!
   - Nếu chạy trên máy khác, mở PowerShell tại thư mục dự án và chạy `.\run_multi_clients.bat 192.168.1.10`.
3. Muốn mở thêm 1 Client tùy ý: chạy `run_client.bat <nickname> <IP-máy-server>`.
   - Trên **máy server**, dùng `ipconfig` và lấy địa chỉ IPv4 của card mạng đang dùng, ví dụ `192.168.1.10`.
   - Trên **máy client**, nhập `192.168.1.10` vào ô **Địa chỉ Server IP**, không dùng `127.0.0.1`.
   - Ví dụ: `run_client.bat Bob 192.168.1.10`

### Kết nối giữa hai máy trong cùng mạng LAN

1. Hai máy phải kết nối cùng Wi-Fi/LAN và nhìn thấy nhau. Từ máy client kiểm tra:
   ```powershell
   ping 192.168.1.10
   Test-NetConnection 192.168.1.10 -Port 8888
   ```
2. Bật server trước và giữ cửa sổ **Server Dashboard** đang chạy.
3. Nếu `Test-NetConnection` báo `TcpTestSucceeded: False`, cho phép Java hoặc TCP port `8888` qua Windows Firewall trên máy server. Có thể kiểm tra nhanh bằng cách tạm cho phép ứng dụng Java qua **Windows Defender Firewall > Allow an app**.
4. Chức năng voice/video dùng UDP và cần cho phép Java qua firewall trên cả hai máy. Không dùng mạng Guest/isolated Wi-Fi; loại mạng này thường chặn các máy trong cùng Wi-Fi.

`127.0.0.1` chỉ đúng khi server và client chạy trên cùng một máy. Server TCP lắng nghe trên mọi card mạng, nên không cần sửa mã nguồn để đổi IP.

### Cách 2: Chạy trên Linux (Ubuntu, Debian, Fedora, Arch)
Trước khi chạy lần đầu, cấp quyền thực thi cho các file script:
```bash
chmod +x *.sh
```

1. **Khởi động Server:**
   ```bash
   ./run_server.sh
   ```
2. **Khởi động đồng thời 6 Clients để chấm điểm:**
   ```bash
   ./run_multi_clients.sh
   ```
3. **Mở thêm 1 Client đơn lẻ:**
   ```bash
   ./run_client.sh           # hoặc ./run_client.sh Alice
   ```

### Cách 3: Sử dụng lệnh Maven trong Terminal / PowerShell
```powershell
# Chạy Server
mvn compile
java --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*" com.chatapp.server.ServerApp

# Mở Client (có thể mở nhiều cửa sổ terminal khác nhau)
java --module-path "target\lib" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "target\ChatAppClientServer-1.0-SNAPSHOT.jar;target\lib\*" com.chatapp.client.ClientApp Alice 192.168.1.10 8888
```

---

## 🎓 KIẾN THỨC VẤN ĐÁP: PHÂN TÍCH TÌNH HUỐNG TCP vs UDP

Khi giảng viên hỏi: *"Tại sao chức năng này dùng TCP mà chức năng kia lại dùng UDP? Nếu đổi ngược lại thì chuyện gì xảy ra?"*, bạn hãy trả lời theo bảng phân tích chuẩn mực sau:

```
                  ┌──────────────────────────────────────────────┐
                  │          MÔ HÌNH HỆ THỐNG MẠNG               │
                  └───────┬──────────────────────────────┬───────┘
                          │                              │
             ┌────────────▼──────────┐        ┌──────────▼──────────┐
             │       TCP SOCKET      │        │      UDP SOCKET     │
             │   (Port 8888 - Server)│        │   (Port 9000+ &     │
             │                       │        │    IP 230.0.0.1)    │
             └────────────┬──────────┘        └──────────┬──────────┘
                          │                              │
         ┌────────────────┴───────────────┐              ├─ Voice Call Realtime
         ├─ Đăng nhập / Quản lý phiên     │              ├─ Video Call Streaming
         ├─ Tin nhắn văn bản (Text Chat)  │              └─ IP Multicast Discovery
         ├─ Gửi file đính kèm (Ảnh, Doc)  │
         ├─ Voice Note (Ghi âm gửi file)  │
         └─ Broadcast & Group Routing     │
```

### 1. Tại sao Tin nhắn Text, File đính kèm và Voice Note dùng TCP?
- **Đặc tính TCP:** Là giao thức hướng kết nối (*Connection-oriented*), có bắt tay 3 bước (*3-way handshake*), bảo đảm **độ tin cậy 100%**:
  - Không bị mất gói tin (Packet loss) nhờ cơ chế ACK và tự động truyền lại (*Retransmission*).
  - Đảm bảo dữ liệu đến đúng thứ tự (*In-order delivery*).
  - Kiểm soát luồng và tránh tắc nghẽn (*Flow & Congestion control*).
- **Tình huống áp dụng:**
  - **Gửi File:** Nếu mất dù chỉ 1 byte thì file Word, Zip, hoặc Ảnh sẽ bị hỏng hoàn toàn (*Corrupted file*) không mở được. Do đó **bắt buộc phải dùng TCP**.
  - **Ghi âm Voice Note:** Đây là một file âm thanh hoàn chỉnh `.wav` được gửi đi để người nhận lưu và nghe lại trọn vẹn, cần độ toàn vẹn của TCP.

### 2. Tại sao Cuộc gọi Voice Call và Video Call dùng UDP?
- **Đặc tính UDP:** Là giao thức phi kết nối (*Connectionless*), không cần bắt tay, không có cơ chế ACK, không truyền lại gói tin bị mất:
  - Header cực kỳ nhỏ nhẹ (chỉ 8 bytes so với 20-60 bytes của TCP) $\rightarrow$ overhead cực thấp.
  - Tốc độ truyền tải tức thì (*Near-zero latency*), không bị nghẽn buffer.
- **Tình huống áp dụng:**
  - **Cuộc gọi thoại & video trực tiếp (Real-time Streaming):**
    - Yếu tố quan trọng số 1 là **tính thời gian thực (Real-time)**.
    - Giả sử gói âm thanh/hình ảnh ở giây thứ 1 bị rơi trên đường truyền, nếu dùng TCP thì cả cuộc gọi sẽ bị dừng lại (*Head-of-line blocking*) để chờ gửi lại gói cũ, gây ra hiện tượng giật lag, trễ tiếng (delay 2-5 giây).
    - Với UDP, gói tin bị rơi sẽ bị bỏ qua và tiếp tục phát các gói mới nhất ở giây thứ 2. Tai người chỉ nghe thấy một tiếng "tách" nhỏ hoặc hình ảnh chỉ mờ nhẹ trong 1 tích tắc nhưng cuộc trò chuyện vẫn diễn ra mượt mà, đồng bộ môi và tiếng ngay lập tức!

### 3. Kỹ thuật Multicast và Broadcast được thực hiện như thế nào?
- **Broadcast:** Client gửi thông điệp lên Server với đích đến `ALL`. Server duyệt qua danh sách tất cả các kết nối Client đang hoạt động trong `ConcurrentHashMap` và chuyển tiếp gói tin tới từng socket client.
- **Multicast (Tầng Ứng Dụng - Group Chat Room):** Mỗi Client tham gia vào một nhóm (ví dụ: `LapTrinhMang_K65`). Khi một thành viên gửi tin, Server chỉ lọc ra và chuyển tiếp gói tin đến các Client nằm trong danh sách thành viên của nhóm đó.
- **Multicast (Tầng Giao Vận/Mạng - UDP MulticastSocket):** Sử dụng địa chỉ IP lớp D `230.0.0.1` trên cổng `4446`. Tất cả các máy tính/client gọi hàm `multicastSocket.joinGroup(230.0.0.1)`. Khi bất kỳ ai gửi `DatagramPacket` tới IP này, card mạng và router sẽ tự nhân bản gói tin gửi tới tất cả các thành viên mà không cần thông qua máy chủ trung gian.

---

## 📁 CẤU TRÚC THƯ MỤC DỰ ÁN

```
ChatAppClientServer/
├── pom.xml                               # Cấu hình Maven, JavaFX 21, Gson
├── run_server.bat                        # Script khởi chạy nhanh Server
├── run_client.bat                        # Script khởi chạy 1 Client
├── run_multi_clients.bat                 # Script tự động mở 6 Clients để chấm điểm
├── run_server.ps1                        # Script PowerShell cho Server
├── run_multi_clients.ps1                 # Script PowerShell cho 6 Clients
├── README.md                             # Tài liệu hướng dẫn & câu hỏi vấn đáp
└── src/main/java/com/chatapp/
    ├── common/                           # Mô hình & giao thức dùng chung
    │   ├── MessageType.java              # Phân loại thông điệp (TCP & UDP Signals)
    │   ├── NetworkMessage.java           # Lớp đóng gói bản tin JSON
    │   └── AudioHelper.java              # Xử lý Microphone & Loa (javax.sound)
    ├── server/                           # Kiến trúc Máy chủ
    │   ├── ChatServer.java               # ServerSocket TCP, ThreadPool, định tuyến
    │   ├── ClientHandler.java            # Xử lý kết nối đa luồng cho từng Client
    │   ├── GroupManager.java             # Quản lý Room/Nhóm Multicast In-Memory
    │   └── ServerApp.java                # Giao diện Dashboard quản trị Server
    └── client/                           # Ứng dụng Khách
        ├── ClientApp.java                # Giao diện Đăng nhập & Điều hướng
        ├── ChatClient.java               # Quản lý Socket TCP kết nối tới Server
        ├── media/                        # Xử lý truyền thông đa phương tiện UDP
        │   ├── UdpAudioCall.java         # Voice Call Realtime qua UDP Datagram
        │   ├── UdpVideoCall.java         # Video Call Realtime qua UDP Datagram
        │   └── UdpMulticastService.java  # Tham gia nhóm IP Multicast 230.0.0.1
        └── ui/                           # Giao diện người dùng JavaFX
            ├── MainChatView.java         # Màn hình Chat chính phong cách Messenger
            ├── CallWindow.java           # Cửa sổ Gọi Video / Voice UDP
            └── EmojiPickerPopup.java     # Bảng chọn biểu cảm Emoji
```
