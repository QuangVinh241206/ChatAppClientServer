package com.chatapp.client.media;

import javafx.application.Platform;
import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Xử lý cuộc gọi Video thời gian thực qua giao thức UDP (DatagramSocket)
 * Mô phỏng/truyền frame hình ảnh (320x240) nén JPEG gửi qua UDP packet.
 * Giúp sinh viên minh họa trực quan kỹ thuật truyền streaming đa phương tiện qua UDP.
 */
public class UdpVideoCall {
    private static final int FRAME_WIDTH = 320;
    private static final int FRAME_HEIGHT = 240;
    private static final int MAX_UDP_PACKET_SIZE = 65000;

    private DatagramSocket udpSocket;
    private String remoteHost;
    private int remotePort;

    private volatile boolean isRunning = false;
    private volatile boolean isCameraOn = true;

    private Thread captureThread;
    private Thread receiveThread;

    private Consumer<Image> onRemoteFrameReceived;
    private Consumer<Image> onLocalFrameReceived;
    private String callerName = "User";

    public int startCall(String callerName, String remoteHost, int remotePort,
                         Consumer<Image> onRemoteFrame, Consumer<Image> onLocalFrame) throws SocketException {
        this.callerName = callerName;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.onRemoteFrameReceived = onRemoteFrame;
        this.onLocalFrameReceived = onLocalFrame;

        this.udpSocket = new DatagramSocket();
        int localPort = udpSocket.getLocalPort();
        this.isRunning = true;

        startReceiveLoop();
        startCaptureLoop();

        return localPort;
    }

    public void setRemoteDestination(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    private void startCaptureLoop() {
        captureThread = new Thread(() -> {
            int frameCount = 0;
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (Exception ignored) {}

            while (isRunning) {
                try {
                    frameCount++;
                    BufferedImage frame = generateOrCaptureFrame(robot, frameCount);
                    byte[] jpegBytes = compressToJpeg(frame, 0.45f);

                    // Hiển thị preview hình ảnh của chính mình
                    if (onLocalFrameReceived != null) {
                        Image localImg = new Image(new ByteArrayInputStream(jpegBytes));
                        Platform.runLater(() -> onLocalFrameReceived.accept(localImg));
                    }

                    // Gửi qua UDP cho đối phương
                    if (isCameraOn && remotePort > 0 && jpegBytes.length < MAX_UDP_PACKET_SIZE) {
                        InetAddress addr = InetAddress.getByName(remoteHost);
                        DatagramPacket packet = new DatagramPacket(jpegBytes, jpegBytes.length, addr, remotePort);
                        udpSocket.send(packet);
                    }

                    // Giới hạn ~15-20 frames/giây để tiết kiệm băng thông và mượt mà
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    if (isRunning) System.err.println("Lỗi gửi frame UDP video: " + e.getMessage());
                }
            }
        }, "UDP-Video-Capture-Thread");
        captureThread.start();
    }

    private BufferedImage generateOrCaptureFrame(Robot robot, int frameCount) {
        BufferedImage img = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!isCameraOn) {
            // Camera Tắt
            g2d.setColor(new Color(30, 30, 30));
            g2d.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            g2d.drawString("Camera đang tắt", 90, 120);
            g2d.dispose();
            return img;
        }

        // Tạo khung hình video động (Camera stream mô phỏng sinh động với avatar và sóng âm thanh)
        // Nền gradient công nghệ
        int hueShift = (frameCount * 2) % 360;
        Color topColor = Color.getHSBColor(hueShift / 360.0f, 0.6f, 0.4f);
        Color bottomColor = new Color(20, 24, 33);
        GradientPaint gp = new GradientPaint(0, 0, topColor, 0, FRAME_HEIGHT, bottomColor);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);

        // Khung đại diện Video Call
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.fillRoundRect(20, 20, FRAME_WIDTH - 40, FRAME_HEIGHT - 40, 20, 20);

        // Vòng tròn Avatar ở giữa
        int avatarX = FRAME_WIDTH / 2 - 40;
        int avatarY = FRAME_HEIGHT / 2 - 50;
        g2d.setColor(new Color(0, 132, 255));
        g2d.fillOval(avatarX, avatarY, 80, 80);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(avatarX, avatarY, 80, 80);

        // Chữ cái đại diện
        String initial = callerName != null && !callerName.isEmpty() ? callerName.substring(0, 1).toUpperCase() : "U";
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        int charX = avatarX + (80 - fm.stringWidth(initial)) / 2;
        int charY = avatarY + ((80 - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(initial, charX, charY);

        // Tên caller
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        fm = g2d.getFontMetrics();
        g2d.drawString(callerName, (FRAME_WIDTH - fm.stringWidth(callerName)) / 2, avatarY + 105);

        // Chỉ số UDP Streaming & Live Timestamp
        g2d.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10));
        g2d.setColor(new Color(0, 255, 128));
        String timeStr = "LIVE UDP " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SS"));
        g2d.drawString(timeStr, 25, 35);

        // Hiệu ứng sóng âm thanh chuyển động (Audio visualizer bar)
        for (int i = 0; i < 12; i++) {
            int barHeight = (int) (Math.sin((frameCount + i * 5) * 0.3) * 15 + 18);
            g2d.setColor(new Color(0, 200, 255, 200));
            g2d.fillRoundRect(70 + i * 15, FRAME_HEIGHT - 35 - barHeight, 8, barHeight, 4, 4);
        }

        g2d.dispose();
        return img;
    }

    private byte[] compressToJpeg(BufferedImage image, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
        ImageWriter writer = writers.next();
        try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(mcios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        }
        writer.dispose();
        return baos.toByteArray();
    }

    private void startReceiveLoop() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (isRunning) {
                try {
                    udpSocket.receive(packet);
                    if (packet.getLength() > 0 && onRemoteFrameReceived != null) {
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                        Image frameImg = new Image(new ByteArrayInputStream(data));
                        Platform.runLater(() -> onRemoteFrameReceived.accept(frameImg));
                    }
                } catch (Exception e) {
                    if (!isRunning) break;
                }
            }
        }, "UDP-Video-Receive-Thread");
        receiveThread.start();
    }

    public void setCameraOn(boolean cameraOn) {
        this.isCameraOn = cameraOn;
    }

    public boolean isCameraOn() {
        return isCameraOn;
    }

    public void endCall() {
        isRunning = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        try {
            if (captureThread != null) captureThread.interrupt();
            if (receiveThread != null) receiveThread.interrupt();
        } catch (Exception ignored) {}
    }

    public int getLocalPort() {
        return udpSocket != null ? udpSocket.getLocalPort() : -1;
    }
}
