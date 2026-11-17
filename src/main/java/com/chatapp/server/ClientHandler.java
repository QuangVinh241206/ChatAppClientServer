package com.chatapp.server;

import com.chatapp.common.MessageType;
import com.chatapp.common.NetworkMessage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý kết nối TCP riêng biệt của từng Client trên một Thread độc lập.
 * Nhận, phân tích và định tuyến các gói tin theo mô hình Client-Server.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String line;
            while (isRunning && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                NetworkMessage msg = NetworkMessage.fromJson(line);
                if (msg != null) {
                    processMessage(msg);
                }
            }
        } catch (IOException e) {
            // Client mất kết nối hoặc đóng cửa sổ
        } finally {
            close();
        }
    }

    private void processMessage(NetworkMessage msg) {
        server.log("Nhận packet [" + msg.getType() + "] từ '" + (username != null ? username : socket.getRemoteSocketAddress()) + "'");

        switch (msg.getType()) {
            case LOGIN:
                handleLogin(msg);
                break;

            case LOGOUT:
                close();
                break;

            case CHAT_BROADCAST:
                msg.setSender(this.username);
                server.broadcastMessage(msg);
                break;

            case CHAT_GROUP:
                msg.setSender(this.username);
                server.multicastToGroup(msg);
                break;

            case CHAT_DIRECT:
                msg.setSender(this.username);
                server.sendDirectMessage(msg);
                break;

            case FILE_TRANSFER:
            case VOICE_NOTE:
                msg.setSender(this.username);
                if ("ALL".equalsIgnoreCase(msg.getRecipient())) {
                    server.broadcastMessage(msg);
                } else if (msg.getRecipient() != null && msg.getRecipient().startsWith("GROUP:")) {
                    server.multicastToGroup(msg);
                } else {
                    server.sendDirectMessage(msg);
                }
                break;

            case USER_LIST:
                NetworkMessage userListMsg = new NetworkMessage(MessageType.USER_LIST, "SERVER", this.username, null);
                userListMsg.setUserList(server.getOnlineUsers());
                sendResponse(userListMsg);
                break;

            case GROUP_LIST:
                NetworkMessage groupListMsg = new NetworkMessage(MessageType.GROUP_LIST, "SERVER", this.username, null);
                groupListMsg.setGroupList(server.getGroupManager().getAllGroupNames());
                sendResponse(groupListMsg);
                break;

            case GROUP_CREATE:
                boolean created = server.getGroupManager().createGroup(msg.getContent(), this.username);
                if (created) {
                    server.getGroupManager().joinGroup(msg.getContent(), this.username);
                    server.broadcastGroupList();
                    sendNotification("Đã tạo nhóm '" + msg.getContent() + "' thành công!");
                } else {
                    sendNotification("Tạo nhóm thất bại: Tên nhóm đã tồn tại hoặc không hợp lệ.");
                }
                break;

            case GROUP_JOIN:
                server.getGroupManager().joinGroup(msg.getContent(), this.username);
                server.broadcastGroupList();
                sendNotification("Bạn đã tham gia nhóm '" + msg.getContent() + "'.");
                break;

            case GROUP_LEAVE:
                server.getGroupManager().leaveGroup(msg.getContent(), this.username);
                server.broadcastGroupList();
                sendNotification("Bạn đã rời nhóm '" + msg.getContent() + "'.");
                break;

            case CALL_REQUEST:
            case CALL_ACCEPT:
            case CALL_REJECT:
            case CALL_END:
                // Định tuyến tín hiệu cuộc gọi UDP tới đúng người nhận
                msg.setSender(this.username);
                msg.setSenderHost(socket.getInetAddress().getHostAddress());
                server.sendDirectMessage(msg);
                break;

            default:
                break;
        }
    }

    private void handleLogin(NetworkMessage msg) {
        String reqUser = msg.getContent() != null ? msg.getContent().trim() : "";
        if (reqUser.isEmpty()) {
            sendResponse(new NetworkMessage(MessageType.LOGIN_FAILED, "SERVER", null, "Tên người dùng không được để trống!"));
            return;
        }

        if (server.isUsernameTaken(reqUser)) {
            sendResponse(new NetworkMessage(MessageType.LOGIN_FAILED, "SERVER", null, "Tên đăng nhập '" + reqUser + "' đã có người sử dụng!"));
            return;
        }

        this.username = reqUser;
        server.registerClient(this.username, this);

        // Tự động thêm người dùng vào tất cả các phòng nhóm mặc định trên Server
        for (String groupName : server.getGroupManager().getAllGroupNames()) {
            server.getGroupManager().joinGroup(groupName, this.username);
        }

        // Gửi phản hồi đăng nhập thành công
        NetworkMessage successMsg = new NetworkMessage(MessageType.LOGIN_SUCCESS, "SERVER", this.username, "Đăng nhập thành công!");
        successMsg.setUserList(server.getOnlineUsers());
        successMsg.setGroupList(server.getGroupManager().getAllGroupNames());
        sendResponse(successMsg);

        // Phát tán cập nhật danh sách người online cho tất cả client
        server.broadcastUserList();

        // Gửi thông báo chào mừng
        NetworkMessage joinNotice = new NetworkMessage(MessageType.SYSTEM_NOTIFICATION, "SERVER", "ALL", "Người dùng [" + this.username + "] vừa tham gia phòng chat.");
        server.broadcastMessage(joinNotice);
    }

    public synchronized void sendResponse(NetworkMessage msg) {
        try {
            if (out != null) {
                out.write(msg.toJson());
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {
            close();
        }
    }

    public void sendNotification(String text) {
        sendResponse(new NetworkMessage(MessageType.SYSTEM_NOTIFICATION, "SERVER", this.username, text));
    }

    public void close() {
        if (!isRunning) return;
        isRunning = false;

        if (this.username != null) {
            server.unregisterClient(this.username);
            server.getGroupManager().removeUserFromAllGroups(this.username);
            server.broadcastUserList();
            server.broadcastMessage(new NetworkMessage(MessageType.SYSTEM_NOTIFICATION, "SERVER", "ALL", "Người dùng [" + this.username + "] đã rời khỏi hệ thống."));
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }
}
