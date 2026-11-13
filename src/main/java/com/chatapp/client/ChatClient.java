package com.chatapp.client;

import com.chatapp.common.MessageType;
import com.chatapp.common.NetworkMessage;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Xử lý kết nối TCP Client tới Server
 * Duy trì kết nối, gửi nhận các gói tin NetworkMessage.
 */
public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;
    private volatile boolean isConnected = false;
    private Thread receiveThread;

    // Callbacks xử lý các sự kiện mạng
    private Consumer<NetworkMessage> onMessageReceived;
    private Consumer<List<String>> onUserListUpdated;
    private Consumer<List<String>> onGroupListUpdated;
    private Consumer<String> onSystemNotification;
    private Consumer<String> onConnectionError;
    private Runnable onLoginSuccess;
    private Consumer<String> onLoginFailed;

    public void connect(String host, int port, String username) {
        this.username = username;
        new Thread(() -> {
            try {
                this.socket = new Socket(host, port);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                this.isConnected = true;

                // Gửi thông điệp LOGIN
                NetworkMessage loginMsg = new NetworkMessage(MessageType.LOGIN, username, "SERVER", username);
                sendRawMessage(loginMsg);

                // Khởi động luồng đọc dữ liệu từ máy chủ
                startReceiveLoop();
            } catch (IOException e) {
                if (onConnectionError != null) {
                    Platform.runLater(() -> onConnectionError.accept("Không thể kết nối đến máy chủ (" + host + ":" + port + "): " + e.getMessage()));
                }
            }
        }, "TCP-Client-Receiver").start();
    }

    private void startReceiveLoop() {
        receiveThread = new Thread(() -> {
            String line;
            try {
                while (isConnected && (line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    NetworkMessage msg = NetworkMessage.fromJson(line);
                    if (msg != null) {
                        handleIncomingMessage(msg);
                    }
                }
            } catch (IOException e) {
                if (isConnected && onConnectionError != null) {
                    Platform.runLater(() -> onConnectionError.accept("Đã mất kết nối tới máy chủ!"));
                }
            } finally {
                disconnect();
            }
        });
        receiveThread.start();
    }

    private void handleIncomingMessage(NetworkMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case LOGIN_SUCCESS:
                    if (onLoginSuccess != null) onLoginSuccess.run();
                    if (msg.getUserList() != null && onUserListUpdated != null) {
                        onUserListUpdated.accept(msg.getUserList());
                    }
                    if (msg.getGroupList() != null && onGroupListUpdated != null) {
                        onGroupListUpdated.accept(msg.getGroupList());
                    }
                    break;

                case LOGIN_FAILED:
                    if (onLoginFailed != null) onLoginFailed.accept(msg.getContent());
                    disconnect();
                    break;

                case USER_LIST:
                    if (onUserListUpdated != null) onUserListUpdated.accept(msg.getUserList());
                    break;

                case GROUP_LIST:
                    if (onGroupListUpdated != null) onGroupListUpdated.accept(msg.getGroupList());
                    break;

                case SYSTEM_NOTIFICATION:
                    if (onSystemNotification != null) onSystemNotification.accept(msg.getContent());
                    if (onMessageReceived != null) onMessageReceived.accept(msg);
                    break;

                case CHAT_BROADCAST:
                case CHAT_GROUP:
                case CHAT_DIRECT:
                case FILE_TRANSFER:
                case VOICE_NOTE:
                case CALL_REQUEST:
                case CALL_ACCEPT:
                case CALL_REJECT:
                case CALL_END:
                    if (onMessageReceived != null) onMessageReceived.accept(msg);
                    break;

                default:
                    break;
            }
        });
    }

    public synchronized void sendRawMessage(NetworkMessage msg) {
        if (!isConnected || out == null) return;
        new Thread(() -> {
            try {
                out.write(msg.toJson());
                out.newLine();
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }).start();
    }

    // --- Các tiện ích gửi tin nhắn theo phân loại mạng ---

    public void sendBroadcastText(String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_BROADCAST, username, "ALL", text);
        sendRawMessage(msg);
    }

    public void sendGroupText(String groupName, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_GROUP, username, "GROUP:" + groupName, text);
        sendRawMessage(msg);
    }

    public void sendDirectText(String recipient, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_DIRECT, username, recipient, text);
        sendRawMessage(msg);
    }

    public void sendFile(String recipient, String fileName, byte[] fileBytes) {
        NetworkMessage msg = new NetworkMessage(MessageType.FILE_TRANSFER, username, recipient, "Đã gửi tập tin: " + fileName);
        msg.setFileName(fileName);
        msg.setFileSize(fileBytes.length);
        msg.setFileDataBase64(Base64.getEncoder().encodeToString(fileBytes));
        sendRawMessage(msg);
    }

    public void sendVoiceNote(String recipient, byte[] wavBytes) {
        NetworkMessage msg = new NetworkMessage(MessageType.VOICE_NOTE, username, recipient, "Tin nhắn thoại (" + (wavBytes.length / 1024) + " KB)");
        msg.setFileName("voice_" + System.currentTimeMillis() + ".wav");
        msg.setFileSize(wavBytes.length);
        msg.setFileDataBase64(Base64.getEncoder().encodeToString(wavBytes));
        sendRawMessage(msg);
    }

    public void createGroup(String groupName) {
        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_CREATE, username, "SERVER", groupName);
        sendRawMessage(msg);
    }

    public void joinGroup(String groupName) {
        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_JOIN, username, "SERVER", groupName);
        sendRawMessage(msg);
    }

    public void leaveGroup(String groupName) {
        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_LEAVE, username, "SERVER", groupName);
        sendRawMessage(msg);
    }

    // Tín hiệu cuộc gọi UDP
    public void sendCallRequest(String recipient, int udpPort, boolean isVideo) {
        NetworkMessage msg = new NetworkMessage(MessageType.CALL_REQUEST, username, recipient, isVideo ? "Yêu cầu Video Call..." : "Yêu cầu Voice Call...");
        msg.setUdpPort(udpPort);
        msg.setVideoCall(isVideo);
        sendRawMessage(msg);
    }

    public void sendCallAccept(String recipient, int myUdpPort) {
        NetworkMessage msg = new NetworkMessage(MessageType.CALL_ACCEPT, username, recipient, "Đã chấp nhận cuộc gọi.");
        msg.setUdpPort(myUdpPort);
        sendRawMessage(msg);
    }

    public void sendCallReject(String recipient) {
        NetworkMessage msg = new NetworkMessage(MessageType.CALL_REJECT, username, recipient, "Đã từ chối cuộc gọi.");
        sendRawMessage(msg);
    }

    public void sendCallEnd(String recipient) {
        NetworkMessage msg = new NetworkMessage(MessageType.CALL_END, username, recipient, "Cuộc gọi đã kết thúc.");
        sendRawMessage(msg);
    }

    public void disconnect() {
        isConnected = false;
        try {
            if (out != null) {
                NetworkMessage logout = new NetworkMessage(MessageType.LOGOUT, username, "SERVER", null);
                out.write(logout.toJson());
                out.newLine();
                out.flush();
            }
        } catch (Exception ignored) {}

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // Getters and Listeners
    public String getUsername() { return username; }
    public boolean isConnected() { return isConnected; }

    public void setOnMessageReceived(Consumer<NetworkMessage> listener) { this.onMessageReceived = listener; }
    public void setOnUserListUpdated(Consumer<List<String>> listener) { this.onUserListUpdated = listener; }
    public void setOnGroupListUpdated(Consumer<List<String>> listener) { this.onGroupListUpdated = listener; }
    public void setOnSystemNotification(Consumer<String> listener) { this.onSystemNotification = listener; }
    public void setOnConnectionError(Consumer<String> listener) { this.onConnectionError = listener; }
    public void setOnLoginSuccess(Runnable listener) { this.onLoginSuccess = listener; }
    public void setOnLoginFailed(Consumer<String> listener) { this.onLoginFailed = listener; }
}
