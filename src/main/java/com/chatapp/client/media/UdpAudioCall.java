package com.chatapp.client.media;

import com.chatapp.common.AudioHelper;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;

/**
 * Xử lý cuộc gọi thoại thời gian thực qua giao thức UDP (DatagramSocket)
 * Ưu điểm của UDP trong Voice Call:
 * - Không có độ trễ truyền lại (no retransmission delay)
 * - Tốc độ truyền tức thời (real-time streaming)
 * - Thích hợp truyền luồng âm thanh liên tục
 */
public class UdpAudioCall {
    private DatagramSocket udpSocket;
    private String remoteHost;
    private int remotePort;

    private TargetDataLine micLine;
    private SourceDataLine speakerLine;

    private volatile boolean isRunning = false;
    private volatile boolean isMuted = false;

    private Thread captureThread;
    private Thread playbackThread;

    public int startCall(String remoteHost, int remotePort) throws SocketException {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.udpSocket = new DatagramSocket(); // Mở cổng UDP ngẫu nhiên
        int localPort = udpSocket.getLocalPort();

        this.isRunning = true;
        initSpeaker();
        initMic();

        startPlaybackLoop();
        startCaptureLoop();

        return localPort;
    }

    public void setRemoteDestination(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    private void initSpeaker() {
        try {
            AudioFormat format = AudioHelper.getStandardFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speakerLine = (SourceDataLine) AudioSystem.getLine(info);
            speakerLine.open(format);
            speakerLine.start();
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo Loa: " + e.getMessage());
        }
    }

    private void initMic() {
        try {
            AudioFormat format = AudioHelper.getStandardFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                micLine = (TargetDataLine) AudioSystem.getLine(info);
                micLine.open(format);
                micLine.start();
            }
        } catch (Exception e) {
            System.err.println("Không thể mở Microphone: " + e.getMessage());
        }
    }

    private void startCaptureLoop() {
        if (micLine == null) return;
        captureThread = new Thread(() -> {
            byte[] buffer = new byte[640]; // 20ms audio frame ở 16kHz 16bit mono
            try {
                InetAddress address = InetAddress.getByName(remoteHost);
                while (isRunning) {
                    int bytesRead = micLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0 && !isMuted && remotePort > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, remotePort);
                        udpSocket.send(packet);
                    }
                }
            } catch (Exception e) {
                if (isRunning) System.err.println("Lỗi gửi gói tin UDP Audio: " + e.getMessage());
            }
        }, "UDP-Voice-Capture-Thread");
        captureThread.start();
    }

    private void startPlaybackLoop() {
        playbackThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (isRunning) {
                try {
                    udpSocket.receive(packet);
                    if (speakerLine != null && packet.getLength() > 0) {
                        speakerLine.write(packet.getData(), 0, packet.getLength());
                    }
                } catch (IOException e) {
                    if (!isRunning) break;
                }
            }
        }, "UDP-Voice-Playback-Thread");
        playbackThread.start();
    }

    public void setMute(boolean muted) {
        this.isMuted = muted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void endCall() {
        isRunning = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (micLine != null) {
            try { micLine.stop(); micLine.close(); } catch (Exception ignored) {}
        }
        if (speakerLine != null) {
            try { speakerLine.stop(); speakerLine.close(); } catch (Exception ignored) {}
        }
        try {
            if (captureThread != null) captureThread.interrupt();
            if (playbackThread != null) playbackThread.interrupt();
        } catch (Exception ignored) {}
    }

    public int getLocalPort() {
        return udpSocket != null ? udpSocket.getLocalPort() : -1;
    }
}
