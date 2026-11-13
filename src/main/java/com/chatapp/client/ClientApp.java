package com.chatapp.client;

import com.chatapp.client.ui.MainChatView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Điểm khởi chạy ứng dụng Client JavaFX
 * Hỗ trợ giao diện Đăng nhập hiện đại và chuyển tiếp sang màn hình Chat chính.
 */
public class ClientApp extends Application {
    private Stage primaryStage;
    private ChatClient chatClient;
    private MainChatView mainChatView;

    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private Label errorLabel;
    private Button loginBtn;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.chatClient = new ChatClient();

        showLoginScreen();

        // Kiểm tra xem có truyền username qua command-line arguments không (tiện cho script chạy multi-client)
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty() && !args.get(0).trim().isEmpty()) {
            usernameField.setText(args.get(0).trim());
            Platform.runLater(this::performLogin);
        }
    }

    private void showLoginScreen() {
        primaryStage.setTitle("Đăng nhập - Chat App (Client-Server)");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', sans-serif;");

        // Icon Header
        Label appIcon = new Label("💬");
        appIcon.setStyle("-fx-font-size: 54px;");

        Label appTitle = new Label("Zalo / Messenger Chat");
        appTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");

        Label appSubtitle = new Label("Mô hình Client-Server • TCP & UDP");
        appSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");

        // Form Fields
        VBox formBox = new VBox(10);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setMaxWidth(340);

        Label hostLabel = new Label("Địa chỉ Server IP:");
        hostLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 12px;");
        hostField = new TextField("127.0.0.1");
        hostField.setStyle("-fx-background-radius: 8; -fx-padding: 8 12;");

        Label portLabel = new Label("Cổng mạng (Port TCP):");
        portLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 12px;");
        portField = new TextField("8888");
        portField.setStyle("-fx-background-radius: 8; -fx-padding: 8 12;");

        Label userLabel = new Label("Biệt danh (Nickname):");
        userLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 12px;");
        usernameField = new TextField();
        usernameField.setPromptText("Ví dụ: Alice, Bob, Charlie...");
        usernameField.setStyle("-fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 14px;");
        usernameField.setOnAction(e -> performLogin());

        // Gợi ý tên ngẫu nhiên
        HBox quickNamesBox = new HBox(6);
        quickNamesBox.setAlignment(Pos.CENTER_LEFT);
        Label quickLabel = new Label("Chọn nhanh:");
        quickLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
        quickNamesBox.getChildren().add(quickLabel);

        String[] sampleNames = {"Alice", "Bob", "Charlie", "David", "Emma", "Frank"};
        for (String sample : sampleNames) {
            Button nameBtn = new Button(sample);
            nameBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #edf2f7; -fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;");
            nameBtn.setOnAction(e -> usernameField.setText(sample));
            quickNamesBox.getChildren().add(nameBtn);
        }

        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: 12px;");

        loginBtn = new Button("Đăng Nhập Ngay ➤");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 8; -fx-cursor: hand;");
        loginBtn.setOnAction(e -> performLogin());

        formBox.getChildren().addAll(
                hostLabel, hostField,
                portLabel, portField,
                userLabel, usernameField, quickNamesBox,
                errorLabel,
                loginBtn
        );

        root.getChildren().addAll(appIcon, appTitle, appSubtitle, formBox);

        Scene scene = new Scene(root, 420, 560);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void performLogin() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty()) {
            errorLabel.setText("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Cổng mạng phải là số hợp lệ!");
            return;
        }

        errorLabel.setText("Đang kết nối tới " + host + ":" + port + "...");
        loginBtn.setDisable(true);

        chatClient.setOnLoginSuccess(() -> {
            Platform.runLater(() -> openMainChatScreen(username));
        });

        chatClient.setOnLoginFailed(reason -> {
            Platform.runLater(() -> {
                errorLabel.setText("Đăng nhập thất bại: " + reason);
                loginBtn.setDisable(false);
            });
        });

        chatClient.setOnConnectionError(err -> {
            Platform.runLater(() -> {
                errorLabel.setText("Lỗi kết nối: " + err);
                loginBtn.setDisable(false);
            });
        });

        chatClient.connect(host, port, username);
    }

    private void openMainChatScreen(String username) {
        primaryStage.setTitle("Chat App - [" + username + "] (Online)");
        primaryStage.setResizable(true);

        mainChatView = new MainChatView(chatClient, primaryStage);
        BorderPane chatRoot = mainChatView.createRootPane();

        Scene scene = new Scene(chatRoot, 980, 680);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();

        primaryStage.setOnCloseRequest(e -> {
            if (mainChatView != null) {
                mainChatView.cleanup();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
