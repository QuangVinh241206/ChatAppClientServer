package com.chatapp.server;

import com.chatapp.common.MessageType;
import com.chatapp.common.NetworkMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Giao diện điều khiển Máy chủ Chat (Server Dashboard)
 * Giúp giảng viên và sinh viên quan sát trực tiếp trạng thái mạng, các kết nối TCP,
 * các luồng Multicast/Broadcast, và nhật ký hoạt động thời gian thực.
 */
public class ServerApp extends Application {
    private ChatServer chatServer;
    private TextArea logArea;
    private Label statusLabel;
    private Label clientCountLabel;
    private Button startStopBtn;
    private TextField portField;
    private TextField broadcastField;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("⚡ Chat Server Dashboard - Lập Trình Mạng");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f4f6f9; -fx-font-family: 'Segoe UI', sans-serif;");

        // --- TOP PANEL: Thẻ cấu hình & trạng thái Server ---
        VBox topBox = new VBox(10);
        HBox headerBar = new HBox(15);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Trình Quản Lý Máy Chủ Chat (TCP / UDP)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("● ĐÃ DỪNG");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e53e3e; -fx-background-color: #fed7d7; -fx-padding: 4 10; -fx-background-radius: 12;");

        clientCountLabel = new Label("Clients: 0");
        clientCountLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2b6cb0; -fx-background-color: #bee3f8; -fx-padding: 4 10; -fx-background-radius: 12;");

        headerBar.getChildren().addAll(title, spacer, clientCountLabel, statusLabel);

        HBox controlBar = new HBox(10);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setStyle("-fx-background-color: #ffffff; -fx-padding: 10; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");

        Label portLabel = new Label("Cổng TCP:");
        portLabel.setStyle("-fx-font-weight: bold;");
        portField = new TextField("8888");
        portField.setPrefWidth(80);

        Label udpInfo = new Label("UDP Ports: 9000+ (Voice/Video) | Multicast IP: 230.0.0.1:4446");
        udpInfo.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        startStopBtn = new Button("▶ Khởi động Server");
        startStopBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 6; -fx-cursor: hand;");
        startStopBtn.setOnAction(e -> toggleServer());

        controlBar.getChildren().addAll(portLabel, portField, startStopBtn, spacer2, udpInfo);
        topBox.getChildren().addAll(headerBar, controlBar);
        root.setTop(topBox);

        // --- CENTER PANEL: Màn hình hiển thị Log thời gian thực ---
        VBox centerBox = new VBox(8);
        centerBox.setPadding(new Insets(12, 0, 12, 0));

        Label logTitle = new Label("Nhật ký sự kiện mạng (Realtime Network Logs):");
        logTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #1a202c; -fx-font-family: 'Consolas', monospace; -fx-highlight-fill: #4a5568; -fx-text-fill: #68d391; -fx-font-size: 12px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        centerBox.getChildren().addAll(logTitle, logArea);
        root.setCenter(centerBox);

        // --- BOTTOM PANEL: Công cụ Quản trị viên (Gửi thông báo toàn Server) ---
        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 10; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");

        Label adminLabel = new Label("Thông báo hệ thống (Broadcast):");
        adminLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        broadcastField = new TextField();
        broadcastField.setPromptText("Nhập thông báo gửi tới tất cả Client...");
        HBox.setHgrow(broadcastField, Priority.ALWAYS);
        broadcastField.setOnAction(e -> sendAdminBroadcast());

        Button sendBtn = new Button("Gửi Broadcast");
        sendBtn.setStyle("-fx-background-color: #dd6b20; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> sendAdminBroadcast());

        Button clearLogBtn = new Button("Xóa Log");
        clearLogBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
        clearLogBtn.setOnAction(e -> logArea.clear());

        bottomBox.getChildren().addAll(adminLabel, broadcastField, sendBtn, clearLogBtn);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 850, 560);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (chatServer != null && chatServer.isRunning()) {
                chatServer.stopServer();
            }
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        // Tự động khởi động Server khi mở app
        toggleServer();
    }

    private void toggleServer() {
        if (chatServer == null || !chatServer.isRunning()) {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("Cổng mạng không hợp lệ!");
                return;
            }

            chatServer = new ChatServer(port);
            chatServer.setLogListener(entry -> Platform.runLater(() -> logArea.appendText(entry + "\n")));
            chatServer.setClientCountListener(count -> Platform.runLater(() -> clientCountLabel.setText("Clients: " + count)));

            try {
                chatServer.startServer();
                statusLabel.setText("● ĐANG HOẠT ĐỘNG");
                statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #38a169; -fx-background-color: #c6f6d5; -fx-padding: 4 10; -fx-background-radius: 12;");
                startStopBtn.setText("⏹ Dừng Server");
                startStopBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 6; -fx-cursor: hand;");
                portField.setDisable(true);
            } catch (IOException e) {
                showAlert("Không thể khởi động Server: " + e.getMessage());
            }
        } else {
            chatServer.stopServer();
            statusLabel.setText("● ĐÃ DỪNG");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e53e3e; -fx-background-color: #fed7d7; -fx-padding: 4 10; -fx-background-radius: 12;");
            startStopBtn.setText("▶ Khởi động Server");
            startStopBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 6; -fx-cursor: hand;");
            portField.setDisable(false);
        }
    }

    private void sendAdminBroadcast() {
        String msg = broadcastField.getText().trim();
        if (msg.isEmpty() || chatServer == null || !chatServer.isRunning()) return;

        NetworkMessage netMsg = new NetworkMessage(MessageType.SYSTEM_NOTIFICATION, "HỆ THỐNG", "ALL", "📢 [THÔNG BÁO TỪ MÁY CHỦ]: " + msg);
        chatServer.broadcastMessage(netMsg);
        broadcastField.clear();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
