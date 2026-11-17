package com.chatapp.server;

import com.chatapp.common.MessageType;
import com.chatapp.common.NetworkMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Lớp điều khiển chính của Server Chat
 * Sử dụng ServerSocket (TCP) hỗ trợ đồng thời nhiều client kết nối (Multi-threading).
 */
public class ChatServer {
    public static final int DEFAULT_PORT = 8888;
    private final int port;
    private ServerSocket serverSocket;
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private final GroupManager groupManager = new GroupManager();
    private ExecutorService threadPool;
    private volatile boolean isRunning = false;

    // Callback ghi nhật ký hiển thị lên UI Server
    private Consumer<String> logListener;
    private Consumer<Integer> clientCountListener;

    public ChatServer(int port) {
        this.port = port;
    }

    public synchronized void startServer() throws IOException {
        if (isRunning) return;
        this.serverSocket = new ServerSocket(this.port);
        this.threadPool = Executors.newCachedThreadPool();
        this.isRunning = true;

        log("Máy chủ Chat đã khởi động thành công trên cổng TCP " + port);

        threadPool.execute(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log("Chấp nhận kết nối mới từ: " + clientSocket.getRemoteSocketAddress());
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (!isRunning) break;
                    log("Lỗi tiếp nhận kết nối: " + e.getMessage());
                }
            }
        });
    }

    public synchronized void stopServer() {
        if (!isRunning) return;
        isRunning = false;

        log("Đang tắt máy chủ Chat...");

        // Ngắt kết nối tất cả các client
        for (ClientHandler handler : connectedClients.values()) {
            handler.close();
        }
        connectedClients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }

        updateClientCount();
        log("Máy chủ đã dừng hoạt động.");
    }

    public boolean isUsernameTaken(String username) {
        return connectedClients.containsKey(username.toLowerCase());
    }

    public void registerClient(String username, ClientHandler handler) {
        connectedClients.put(username.toLowerCase(), handler);
        log("Người dùng [" + username + "] đã đăng ký thành công.");
        updateClientCount();
    }

    public void unregisterClient(String username) {
        connectedClients.remove(username.toLowerCase());
        log("Người dùng [" + username + "] đã ngắt kết nối.");
        updateClientCount();
    }

    // --- KỸ THUẬT BROADCAST: Phát tán tới TOÀN BỘ Client ---
    public void broadcastMessage(NetworkMessage message) {
        log("BROADCAST tin nhắn từ [" + message.getSender() + "] tới tất cả (" + connectedClients.size() + " clients)");
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendResponse(message);
        }
    }

    // --- KỸ THUẬT MULTICAST: Phát tán tới danh sách Client thuộc 1 Nhóm/Phòng cụ thể ---
    public void multicastToGroup(NetworkMessage message) {
        String fullRecipient = message.getRecipient(); // Dạng "GROUP:TenNhom"
        String groupName = fullRecipient.startsWith("GROUP:") ? fullRecipient.substring(6) : fullRecipient;

        // Đảm bảo người gửi cũng tự động tham gia vào nhóm này
        if (message.getSender() != null) {
            groupManager.joinGroup(groupName, message.getSender());
        }

        Set<String> members = groupManager.getGroupMembers(groupName);

        log("MULTICAST tin nhắn từ [" + message.getSender() + "] tới nhóm [" + groupName + "] (" + members.size() + " thành viên: " + members + ")");

        for (String memberName : members) {
            ClientHandler handler = connectedClients.get(memberName.toLowerCase());
            if (handler != null) {
                handler.sendResponse(message);
            }
        }

        // Đảm bảo chính người gửi cũng nhận được tin nhắn nếu chưa có trong members
        if (message.getSender() != null) {
            ClientHandler senderHandler = connectedClients.get(message.getSender().toLowerCase());
            if (senderHandler != null && !members.contains(message.getSender())) {
                senderHandler.sendResponse(message);
            }
        }
    }

    // --- KỸ THUẬT PEER / DIRECT: Gửi tin nhắn riêng 1-1 ---
    public void sendDirectMessage(NetworkMessage message) {
        String recipient = message.getRecipient();
        ClientHandler targetHandler = connectedClients.get(recipient.toLowerCase());

        if (targetHandler != null) {
            targetHandler.sendResponse(message);
            // Gửi lại cho chính người gửi để cập nhật giao diện lịch sử
            ClientHandler senderHandler = connectedClients.get(message.getSender().toLowerCase());
            if (senderHandler != null && senderHandler != targetHandler) {
                senderHandler.sendResponse(message);
            }
        } else {
            // Thông báo người nhận không online
            ClientHandler senderHandler = connectedClients.get(message.getSender().toLowerCase());
            if (senderHandler != null) {
                senderHandler.sendNotification("Người dùng [" + recipient + "] hiện không online.");
            }
        }
    }

    public void broadcastUserList() {
        NetworkMessage msg = new NetworkMessage(MessageType.USER_LIST, "SERVER", "ALL", null);
        msg.setUserList(getOnlineUsers());
        broadcastMessage(msg);
    }

    public void broadcastGroupList() {
        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_LIST, "SERVER", "ALL", null);
        msg.setGroupList(groupManager.getAllGroupNames());
        broadcastMessage(msg);
    }

    public List<String> getOnlineUsers() {
        List<String> list = new ArrayList<>();
        for (ClientHandler h : connectedClients.values()) {
            if (h.getUsername() != null) {
                list.add(h.getUsername());
            }
        }
        return list;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setLogListener(Consumer<String> listener) {
        this.logListener = listener;
    }

    public void setClientCountListener(Consumer<Integer> listener) {
        this.clientCountListener = listener;
    }

    public void log(String text) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = "[" + time + "] " + text;
        System.out.println(entry);
        if (logListener != null) {
            logListener.accept(entry);
        }
    }

    private void updateClientCount() {
        if (clientCountListener != null) {
            clientCountListener.accept(connectedClients.size());
        }
    }
}
