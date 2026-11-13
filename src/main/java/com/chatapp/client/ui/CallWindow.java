package com.chatapp.client.ui;

import com.chatapp.client.ChatClient;
import com.chatapp.client.media.UdpAudioCall;
import com.chatapp.client.media.UdpVideoCall;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Cửa sổ thực hiện Cuộc gọi Đa Phương Tiện (Audio / Video Call) qua UDP
 */
public class CallWindow {
    private final Stage stage;
    private final ChatClient chatClient;
    private final String peerName;
    private final boolean isVideo;
    private final boolean isIncoming;

    private UdpAudioCall audioCall;
    private UdpVideoCall videoCall;

    private ImageView remoteVideoView;
    private ImageView localVideoView;
    private Label statusLabel;
    private Button muteBtn;
    private Button cameraBtn;
    private Button hangUpBtn;

    public CallWindow(ChatClient client, String peerName, boolean isVideo, boolean isIncoming) {
        this.chatClient = client;
        this.peerName = peerName;
        this.isVideo = isVideo;
        this.isIncoming = isIncoming;
        this.stage = new Stage();

        initUI();
    }

    private void initUI() {
        stage.setTitle((isVideo ? "📹 Video Call" : "📞 Voice Call") + " - " + peerName + " [UDP Stream]");
        stage.setWidth(isVideo ? 640 : 420);
        stage.setHeight(isVideo ? 520 : 360);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a202c; -fx-font-family: 'Segoe UI', sans-serif;");

        // Top info bar
        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(15, 10, 10, 10));

        Label callWithLabel = new Label((isVideo ? "Video Call UDP với " : "Voice Call UDP với ") + peerName);
        callWithLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        statusLabel = new Label(isIncoming ? "Cuộc gọi đến..." : "Đang kết nối qua luồng UDP Datagram...");
        statusLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px;");

        topBox.getChildren().addAll(callWithLabel, statusLabel);
        root.setTop(topBox);

        // Center display
        if (isVideo) {
            StackPane videoContainer = new StackPane();
            videoContainer.setPadding(new Insets(10));

            remoteVideoView = new ImageView();
            remoteVideoView.setFitWidth(480);
            remoteVideoView.setFitHeight(340);
            remoteVideoView.setPreserveRatio(true);
            remoteVideoView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

            // Khung camera của mình (Góc nhỏ - PiP)
            localVideoView = new ImageView();
            localVideoView.setFitWidth(140);
            localVideoView.setFitHeight(105);
            localVideoView.setPreserveRatio(true);
            StackPane.setAlignment(localVideoView, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(localVideoView, new Insets(10, 10, 10, 10));

            videoContainer.getChildren().addAll(remoteVideoView, localVideoView);
            root.setCenter(videoContainer);
        } else {
            // Voice Call: Hiển thị avatar tròn sinh động
            VBox voiceBox = new VBox(20);
            voiceBox.setAlignment(Pos.CENTER);

            Label avatarCircle = new Label(peerName.substring(0, 1).toUpperCase());
            avatarCircle.setPrefSize(100, 100);
            avatarCircle.setAlignment(Pos.CENTER);
            avatarCircle.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold; -fx-background-radius: 50;");

            Label protocolInfo = new Label("Giao thức: UDP Datagram 16kHz PCM Audio Stream");
            protocolInfo.setStyle("-fx-text-fill: #48bb78; -fx-font-size: 11px;");

            voiceBox.getChildren().addAll(avatarCircle, protocolInfo);
            root.setCenter(voiceBox);
        }

        // Bottom control buttons
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(15));
        controlBox.setStyle("-fx-background-color: #2d3748;");

        muteBtn = new Button("🎙️ Bật Mic");
        muteBtn.setStyle("-fx-background-color: #4a5568; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        muteBtn.setOnAction(e -> toggleMute());

        hangUpBtn = new Button("🔴 Kết thúc");
        hangUpBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;");
        hangUpBtn.setOnAction(e -> endCall());

        controlBox.getChildren().addAll(muteBtn);

        if (isVideo) {
            cameraBtn = new Button("📹 Tắt Cam");
            cameraBtn.setStyle("-fx-background-color: #4a5568; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
            cameraBtn.setOnAction(e -> toggleCamera());
            controlBox.getChildren().add(cameraBtn);
        }

        controlBox.getChildren().add(hangUpBtn);
        root.setBottom(controlBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> endCall());
    }

    public void startAsCaller(String remoteHost, int remoteUdpPort) {
        try {
            audioCall = new UdpAudioCall();
            int localAudioPort = audioCall.startCall(remoteHost, remoteUdpPort);

            if (isVideo) {
                videoCall = new UdpVideoCall();
                int localVideoPort = videoCall.startCall(
                        chatClient.getUsername(),
                        remoteHost,
                        remoteUdpPort + 1,
                        img -> remoteVideoView.setImage(img),
                        img -> localVideoView.setImage(img)
                );
            }

            statusLabel.setText("Đang truyền gói tin UDP tới " + remoteHost + ":" + remoteUdpPort);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Lỗi khởi tạo UDP: " + e.getMessage());
        }
    }

    public int prepareIncomingReceiver(String remoteHost) {
        try {
            audioCall = new UdpAudioCall();
            int localPort = audioCall.startCall(remoteHost, 0); // Port ngẫu nhiên

            if (isVideo) {
                videoCall = new UdpVideoCall();
                videoCall.startCall(
                        chatClient.getUsername(),
                        remoteHost,
                        0,
                        img -> remoteVideoView.setImage(img),
                        img -> localVideoView.setImage(img)
                );
            }
            return localPort;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void connectedWithRemote(String remoteHost, int remoteUdpPort) {
        if (audioCall != null) {
            audioCall.setRemoteDestination(remoteHost, remoteUdpPort);
        }
        if (isVideo && videoCall != null) {
            videoCall.setRemoteDestination(remoteHost, remoteUdpPort + 1);
        }
        statusLabel.setText("Đang đàm thoại trực tuyến qua UDP (Port: " + remoteUdpPort + ")");
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    private void toggleMute() {
        if (audioCall != null) {
            boolean currentMute = audioCall.isMuted();
            audioCall.setMute(!currentMute);
            muteBtn.setText(!currentMute ? "🔇 Đã tắt Mic" : "🎙️ Bật Mic");
            muteBtn.setStyle(!currentMute ?
                    "-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;" :
                    "-fx-background-color: #4a5568; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 20; -fx-cursor: hand;");
        }
    }

    private void toggleCamera() {
        if (isVideo && videoCall != null) {
            boolean cam = videoCall.isCameraOn();
            videoCall.setCameraOn(!cam);
            cameraBtn.setText(cam ? "🚫 Bật Cam" : "📹 Tắt Cam");
        }
    }

    public void endCall() {
        if (audioCall != null) {
            audioCall.endCall();
            audioCall = null;
        }
        if (videoCall != null) {
            videoCall.endCall();
            videoCall = null;
        }
        chatClient.sendCallEnd(peerName);
        stage.close();
    }

    public void onRemoteEnded() {
        if (audioCall != null) audioCall.endCall();
        if (videoCall != null) videoCall.endCall();
        Platform.runLater(() -> {
            statusLabel.setText("Đối phương đã ngắt kết nối.");
            stage.close();
        });
    }

    public Stage getStage() {
        return stage;
    }
}
