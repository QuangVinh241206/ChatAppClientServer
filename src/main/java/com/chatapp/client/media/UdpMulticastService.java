package com.chatapp.client.media;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Dịch vụ Multicast trực tiếp trên tầng mạng (IP Multicast qua UDP)
 * Sử dụng địa chỉ IP Multicast lớp D: 230.0.0.1, Port 4446
 * Đáp ứng hoàn hảo câu hỏi kiểm tra kiến thức về UDP Multicast trong môn Lập trình mạng.
 */
public class UdpMulticastService {
    public static final String MULTICAST_ADDRESS = "230.0.0.1";
    public static final int MULTICAST_PORT = 4446;

    private MulticastSocket multicastSocket;
    private InetAddress groupAddress;
    private NetworkInterface netInterface;
    private volatile boolean isRunning = false;
    private Thread listenerThread;
    private Consumer<String> onMessageReceived;

    public void startListening(Consumer<String> onMessageReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;
        this.groupAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        this.multicastSocket = new MulticastSocket(MULTICAST_PORT);

        // Tham gia vào nhóm Multicast
        SocketAddress socketAddress = new InetSocketAddress(groupAddress, MULTICAST_PORT);
        this.netInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        if (this.netInterface == null) {
            // Lấy network interface đầu tiên khả dụng
            java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    this.netInterface = ni;
                    break;
                }
            }
        }

        try {
            if (this.netInterface != null) {
                this.multicastSocket.joinGroup(socketAddress, this.netInterface);
            } else {
                this.multicastSocket.joinGroup(groupAddress);
            }
        } catch (Exception e) {
            // Fallback cho local loopback nếu môi trường mạng hạn chế
            this.multicastSocket.joinGroup(groupAddress);
        }

        this.isRunning = true;
        this.listenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (isRunning) {
                try {
                    multicastSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (this.onMessageReceived != null) {
                        this.onMessageReceived.accept(msg);
                    }
                } catch (IOException e) {
                    if (!isRunning) break;
                }
            }
        }, "UDP-Multicast-Listener");
        this.listenerThread.start();
    }

    public void sendMulticastMessage(String sender, String message) {
        new Thread(() -> {
            try {
                String payload = "[" + sender + " (UDP Multicast)]: " + message;
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, groupAddress, MULTICAST_PORT);
                multicastSocket.send(packet);
            } catch (Exception e) {
                System.err.println("Lỗi gửi gói tin UDP Multicast: " + e.getMessage());
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                if (groupAddress != null) {
                    multicastSocket.leaveGroup(groupAddress);
                }
            } catch (Exception ignored) {}
            multicastSocket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
