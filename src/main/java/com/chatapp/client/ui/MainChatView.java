package com.chatapp.client.ui;

import com.chatapp.client.ChatClient;
import com.chatapp.client.media.UdpMulticastService;
import com.chatapp.common.AudioHelper;
import com.chatapp.common.MessageType;
import com.chatapp.common.NetworkMessage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * Giao diện Chat chính phong cách Zalo / Messenger hiện đại
 * Tích hợp đầy đủ:
 * - Broadcast (Kênh chung toàn server)
 * - Multicast (Nhóm chat phòng riêng & UDP Multicast IP)
 * - Direct / Peer (Chat 1-1)
 * - Biểu tượng cảm xúc Emoji
 * - Đính kèm tệp tin (Ảnh, Tài liệu, Media)
 * - Tin nhắn thoại (Audio Voice Notes)
 * - Cuộc gọi Audio/Video thời gian thực qua UDP
 */
public class MainChatView {
    private final ChatClient chatClient;
    private final Stage stage;

    // Chat target
    public enum ChatTargetType { BROADCAST, GROUP, DIRECT, UDP_MULTICAST }
    private ChatTargetType currentTargetType = ChatTargetType.BROADCAST;
    private String currentTargetName = "ALL";

    // UI Controls
    private Label chatTitleLabel;
    private Label chatSubTitleLabel;
    private Button voiceCallBtn;
    private Button videoCallBtn;
    private VBox messageContainer;
    private ScrollPane messageScrollPane;
    private TextField inputField;
    private Button voiceRecordBtn;
    private Button emojiBtn;
    private EmojiPickerPopup emojiPicker;

    // Sidebar lists
    private ListView<String> groupListView;
    private ListView<String> userListView;
    private Label myUsernameLabel;

    // Voice & Media
    private final AudioHelper.AudioRecorder audioRecorder = new AudioHelper.AudioRecorder();
    private final AudioHelper.AudioPlayer audioPlayer = new AudioHelper.AudioPlayer();
    private CallWindow activeCallWindow;

    // UDP Multicast Service demo
    private UdpMulticastService udpMulticastService;
    private TextArea udpMulticastLogArea;

    public MainChatView(ChatClient chatClient, Stage stage) {
        this.chatClient = chatClient;
        this.stage = stage;
    }

    public BorderPane createRootPane() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f2f5; -fx-font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;");

        root.setLeft(createSidebar());
        root.setCenter(createChatArea());

        setupClientListeners();
        initUdpMulticast();

        return root;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e4e6eb; -fx-border-width: 0 1 0 0;");
        sidebar.setPadding(new Insets(15));

        // My Profile Header
        HBox profileBox = new HBox(12);
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setStyle("-fx-padding: 8; -fx-background-color: #f7fafc; -fx-background-radius: 10; -fx-border-color: #edf2f7; -fx-border-radius: 10;");

        Label avatarLabel = new Label(chatClient.getUsername().substring(0, 1).toUpperCase());
        avatarLabel.setPrefSize(42, 42);
        avatarLabel.setAlignment(Pos.CENTER);
        avatarLabel.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;");

        VBox userDetailBox = new VBox(2);
        myUsernameLabel = new Label(chatClient.getUsername());
        myUsernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a202c;");

        Label statusDot = new Label("● Online (TCP Connected)");
        statusDot.setStyle("-fx-font-size: 11px; -fx-text-fill: #38a169;");

        userDetailBox.getChildren().addAll(myUsernameLabel, statusDot);
        profileBox.getChildren().addAll(avatarLabel, userDetailBox);

        // Sidebar Navigation Buttons
        Button broadcastBtn = new Button("📢 Kênh Chung (Broadcast)");
        broadcastBtn.setMaxWidth(Double.MAX_VALUE);
        broadcastBtn.setStyle("-fx-background-color: #ebf8ff; -fx-text-fill: #2b6cb0; -fx-font-weight: bold; -fx-padding: 8 12; -fx-alignment: BASELINE_LEFT; -fx-background-radius: 8; -fx-cursor: hand;");
        broadcastBtn.setOnAction(e -> switchChatTarget(ChatTargetType.BROADCAST, "ALL"));

        Button udpLabBtn = new Button("📻 UDP Multicast Demo (IP: 230.0.0.1)");
        udpLabBtn.setMaxWidth(Double.MAX_VALUE);
        udpLabBtn.setStyle("-fx-background-color: #fefcbf; -fx-text-fill: #744210; -fx-font-weight: bold; -fx-padding: 8 12; -fx-alignment: BASELINE_LEFT; -fx-background-radius: 8; -fx-cursor: hand;");
        udpLabBtn.setOnAction(e -> switchChatTarget(ChatTargetType.UDP_MULTICAST, "230.0.0.1"));

        // Multicast Group Section
        HBox groupHeader = new HBox(8);
        groupHeader.setAlignment(Pos.CENTER_LEFT);
        Label groupTitle = new Label("NHÓM CHAT (MULTICAST)");
        groupTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #718096;");
        Region gSpacer = new Region();
        HBox.setHgrow(gSpacer, Priority.ALWAYS);

        Button addGroupBtn = new Button("+ Tạo Nhóm");
        addGroupBtn.setStyle("-fx-background-color: #edf2f7; -fx-text-fill: #4a5568; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 6; -fx-cursor: hand;");
        addGroupBtn.setOnAction(e -> showCreateGroupDialog());
        groupHeader.getChildren().addAll(groupTitle, gSpacer, addGroupBtn);

        groupListView = new ListView<>();
        groupListView.setPrefHeight(140);
        groupListView.setStyle("-fx-background-radius: 6; -fx-border-color: #edf2f7; -fx-border-radius: 6;");
        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                switchChatTarget(ChatTargetType.GROUP, newVal);
            }
        });

        // Direct / Peer Users Section
        HBox userHeader = new HBox(8);
        userHeader.setAlignment(Pos.CENTER_LEFT);
        Label userTitle = new Label("BẠN BÈ ONLINE (PEER DIRECT)");
        userTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #718096;");
        userHeader.getChildren().add(userTitle);

        userListView = new ListView<>();
        userListView.setPrefHeight(180);
        userListView.setStyle("-fx-background-radius: 6; -fx-border-color: #edf2f7; -fx-border-radius: 6;");
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(chatClient.getUsername())) {
                switchChatTarget(ChatTargetType.DIRECT, newVal);
            }
        });

        sidebar.getChildren().addAll(
                profileBox,
                broadcastBtn,
                udpLabBtn,
                groupHeader,
                groupListView,
                userHeader,
                userListView
        );
        return sidebar;
    }

    private BorderPane createChatArea() {
        BorderPane chatArea = new BorderPane();

        // 1. Chat Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");

        VBox titleBox = new VBox(2);
        chatTitleLabel = new Label("📢 Kênh Chung (Toàn Server)");
        chatTitleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #050505;");

        chatSubTitleLabel = new Label("Kỹ thuật: TCP Broadcast (Phát tán tới tất cả Client đang kết nối)");
        chatSubTitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        titleBox.getChildren().addAll(chatTitleLabel, chatSubTitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons Voice Call & Video Call (Active khi chat 1-1 Direct)
        voiceCallBtn = new Button("📞 Voice Call (UDP)");
        voiceCallBtn.setStyle("-fx-background-color: #e7f3ff; -fx-text-fill: #0084ff; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 18; -fx-cursor: hand;");
        voiceCallBtn.setVisible(false);
        voiceCallBtn.setOnAction(e -> startCall(false));

        videoCallBtn = new Button("📹 Video Call (UDP)");
        videoCallBtn.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 18; -fx-cursor: hand;");
        videoCallBtn.setVisible(false);
        videoCallBtn.setOnAction(e -> startCall(true));

        header.getChildren().addAll(titleBox, spacer, voiceCallBtn, videoCallBtn);
        chatArea.setTop(header);

        // 2. Message History Container
        messageContainer = new VBox(10);
        messageContainer.setPadding(new Insets(15));
        messageContainer.setStyle("-fx-background-color: #ffffff;");

        messageScrollPane = new ScrollPane(messageContainer);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle("-fx-background-color: transparent; -fx-background: #ffffff;");
        chatArea.setCenter(messageScrollPane);

        // 3. Bottom Input Bar
        chatArea.setBottom(createInputBar());

        return chatArea;
    }

    private HBox createInputBar() {
        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");

        // Nút Đính Kèm File
        Button attachBtn = new Button("📎");
        attachBtn.setTooltip(new Tooltip("Đính kèm tệp tin (Ảnh, Video, Âm thanh, Tài liệu)"));
        attachBtn.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;");
        attachBtn.setOnAction(e -> chooseAndSendFile());

        // Nút Emoji
        emojiBtn = new Button("😊");
        emojiBtn.setTooltip(new Tooltip("Chọn biểu tượng cảm xúc Emoji"));
        emojiBtn.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;");
        emojiPicker = new EmojiPickerPopup(emoji -> {
            inputField.appendText(emoji);
            inputField.requestFocus();
        });
        emojiBtn.setOnAction(e -> {
            var bounds = emojiBtn.localToScreen(emojiBtn.getBoundsInLocal());
            emojiPicker.show(stage, bounds.getMinX(), bounds.getMinY() - 240);
        });

        // Nút Voice Note (Ghi âm giọng nói)
        voiceRecordBtn = new Button("🎙️");
        voiceRecordBtn.setTooltip(new Tooltip("Ghi âm tin nhắn thoại (Audio Voice Note)"));
        voiceRecordBtn.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;");
        voiceRecordBtn.setOnAction(e -> toggleVoiceRecording());

        // Ô nhập nội dung tin nhắn
        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");
        inputField.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 20; -fx-padding: 8 16; -fx-font-size: 14px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendTextMessage());

        // Nút Gửi
        Button sendBtn = new Button("➤ Gửi");
        sendBtn.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 20; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> sendTextMessage());

        inputBar.getChildren().addAll(attachBtn, emojiBtn, voiceRecordBtn, inputField, sendBtn);
        return inputBar;
    }

    private void switchChatTarget(ChatTargetType type, String target) {
        this.currentTargetType = type;
        this.currentTargetName = target;

        // Reset tin nhắn hiển thị hoặc load lại
        messageContainer.getChildren().clear();

        boolean isDirect = (type == ChatTargetType.DIRECT);
        voiceCallBtn.setVisible(isDirect);
        videoCallBtn.setVisible(isDirect);

        switch (type) {
            case BROADCAST:
                chatTitleLabel.setText("📢 Kênh Chung (Toàn Server)");
                chatSubTitleLabel.setText("Kỹ thuật: TCP Broadcast (Tất cả client nhận được tin nhắn)");
                break;
            case GROUP:
                chatTitleLabel.setText("👥 Nhóm: " + target);
                chatSubTitleLabel.setText("Kỹ thuật: TCP Multicast Room (Định tuyến tới các client trong nhóm)");
                break;
            case DIRECT:
                chatTitleLabel.setText("💬 Trò chuyện 1-1 với: " + target);
                chatSubTitleLabel.setText("Kỹ thuật: TCP Direct / Peer Messaging & UDP Call");
                break;
            case UDP_MULTICAST:
                chatTitleLabel.setText("📻 Thử nghiệm UDP Multicast Socket (IP: 230.0.0.1)");
                chatSubTitleLabel.setText("Kỹ thuật: IP Multicast Datagram trên tầng giao vận UDP");
                setupUdpMulticastView();
                return;
        }

        addSystemMessage("Bạn đang trò chuyện ở chế độ: " + chatTitleLabel.getText());
    }

    private void setupUdpMulticastView() {
        VBox labBox = new VBox(10);
        labBox.setPadding(new Insets(10));

        Label explanation = new Label("💡 Đây là kỹ thuật Multicast trên tầng Mạng/Giao vận (UDP MulticastSocket tham gia nhóm IP 230.0.0.1 Port 4446).\n" +
                "Bất kỳ Client nào chạy trên cùng mạng LAN/máy tính gửi gói tin UDP Multicast, tất cả Client tham gia nhóm sẽ nhận được ngay lập tức!");
        explanation.setStyle("-fx-background-color: #feebc8; -fx-padding: 10; -fx-background-radius: 8; -fx-text-fill: #7b341e;");

        udpMulticastLogArea = new TextArea();
        udpMulticastLogArea.setEditable(false);
        udpMulticastLogArea.setPrefHeight(260);
        udpMulticastLogArea.setStyle("-fx-control-inner-background: #2d3748; -fx-text-fill: #9ae6b4; -fx-font-family: 'Consolas';");

        labBox.getChildren().addAll(explanation, udpMulticastLogArea);
        messageContainer.getChildren().add(labBox);
    }

    private void initUdpMulticast() {
        try {
            udpMulticastService = new UdpMulticastService();
            udpMulticastService.startListening(msg -> Platform.runLater(() -> {
                if (udpMulticastLogArea != null) {
                    udpMulticastLogArea.appendText(msg + "\n");
                }
                if (currentTargetType == ChatTargetType.UDP_MULTICAST) {
                    addSystemMessage(msg);
                }
            }));
        } catch (Exception e) {
            System.err.println("Không thể khởi tạo UDP Multicast Socket: " + e.getMessage());
        }
    }

    private void sendTextMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        switch (currentTargetType) {
            case BROADCAST:
                chatClient.sendBroadcastText(text);
                break;
            case GROUP:
                chatClient.sendGroupText(currentTargetName, text);
                break;
            case DIRECT:
                chatClient.sendDirectText(currentTargetName, text);
                break;
            case UDP_MULTICAST:
                if (udpMulticastService != null) {
                    udpMulticastService.sendMulticastMessage(chatClient.getUsername(), text);
                }
                break;
        }
        inputField.clear();
    }

    private void chooseAndSendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn tệp tin để gửi");
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 50 * 1024 * 1024) { // 50MB limit
            showWarning("Tệp tin vượt quá dung lượng cho phép (tối đa 50MB)!");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String recipient = getEffectiveRecipient();
            chatClient.sendFile(recipient, file.getName(), fileBytes);
        } catch (Exception e) {
            showWarning("Lỗi đọc tệp tin: " + e.getMessage());
        }
    }

    private void toggleVoiceRecording() {
        if (!audioRecorder.isRecording()) {
            boolean started = audioRecorder.startRecording();
            if (started) {
                voiceRecordBtn.setText("🔴 Thu...");
                voiceRecordBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #fed7d7; -fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 12;");
            } else {
                showWarning("Không thể khởi động microphone để thu âm!");
            }
        } else {
            byte[] wavBytes = audioRecorder.stopRecording();
            voiceRecordBtn.setText("🎙️");
            voiceRecordBtn.setStyle("-fx-font-size: 16px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 8;");

            if (wavBytes != null && wavBytes.length > 0) {
                String recipient = getEffectiveRecipient();
                chatClient.sendVoiceNote(recipient, wavBytes);
            }
        }
    }

    private String getEffectiveRecipient() {
        switch (currentTargetType) {
            case GROUP:
                return "GROUP:" + currentTargetName;
            case DIRECT:
                return currentTargetName;
            case BROADCAST:
            default:
                return "ALL";
        }
    }

    // --- Xử lý hiển thị tin nhắn (Bong bóng Chat) ---

    public void displayMessage(NetworkMessage msg) {
        boolean isMe = chatClient.getUsername().equalsIgnoreCase(msg.getSender());

        HBox bubbleRow = new HBox();
        bubbleRow.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleRow.setPadding(new Insets(2, 0, 2, 0));

        VBox bubbleBox = new VBox(4);
        bubbleBox.setMaxWidth(450);

        // Header info: Tên người gửi & Thời gian
        if (!isMe) {
            Label nameLabel = new Label(msg.getSender() + " • " + msg.getTimestamp());
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b; -fx-font-weight: bold;");
            bubbleBox.getChildren().add(nameLabel);
        }

        // Nội dung hiển thị theo loại
        if (msg.getType() == MessageType.FILE_TRANSFER && msg.getFileDataBase64() != null) {
            byte[] fileBytes = Base64.getDecoder().decode(msg.getFileDataBase64());
            String fileName = msg.getFileName() != null ? msg.getFileName() : "file";
            boolean isImage = fileName.matches("(?i).*\\.(png|jpg|jpeg|gif|bmp)");

            if (isImage) {
                // Hiển thị ảnh xem trước
                ImageView imgView = new ImageView(new Image(new ByteArrayInputStream(fileBytes)));
                imgView.setFitWidth(240);
                imgView.setPreserveRatio(true);
                imgView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

                Button saveBtn = new Button("💾 Lưu ảnh: " + fileName);
                saveBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #edf2f7; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 6;");
                saveBtn.setOnAction(e -> saveFileToDisk(fileName, fileBytes));

                VBox imgBox = new VBox(6, imgView, saveBtn);
                imgBox.setStyle(isMe ?
                        "-fx-background-color: #0084ff; -fx-padding: 8; -fx-background-radius: 14;" :
                        "-fx-background-color: #e4e6eb; -fx-padding: 8; -fx-background-radius: 14;");
                bubbleBox.getChildren().add(imgBox);
            } else {
                // File đính kèm tài liệu / zip / video
                HBox fileBox = new HBox(8);
                fileBox.setAlignment(Pos.CENTER_LEFT);
                fileBox.setStyle(isMe ?
                        "-fx-background-color: #0084ff; -fx-padding: 8 12; -fx-background-radius: 14;" :
                        "-fx-background-color: #e4e6eb; -fx-padding: 8 12; -fx-background-radius: 14;");

                Label fileIcon = new Label("📁");
                fileIcon.setStyle("-fx-font-size: 20px;");

                VBox fDetail = new VBox(2);
                Label fName = new Label(fileName);
                fName.setStyle(isMe ? "-fx-text-fill: white; -fx-font-weight: bold;" : "-fx-text-fill: #050505; -fx-font-weight: bold;");

                Label fSize = new Label((msg.getFileSize() / 1024) + " KB");
                fSize.setStyle(isMe ? "-fx-text-fill: #ebf8ff; -fx-font-size: 11px;" : "-fx-text-fill: #65676b; -fx-font-size: 11px;");
                fDetail.getChildren().addAll(fName, fSize);

                Button dlBtn = new Button("⬇ Tải về");
                dlBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #ffffff; -fx-cursor: hand; -fx-padding: 4 10; -fx-background-radius: 6;");
                dlBtn.setOnAction(e -> saveFileToDisk(fileName, fileBytes));

                fileBox.getChildren().addAll(fileIcon, fDetail, dlBtn);
                bubbleBox.getChildren().add(fileBox);
            }
        } else if (msg.getType() == MessageType.VOICE_NOTE && msg.getFileDataBase64() != null) {
            // Tin nhắn thoại Voice Note
            byte[] wavBytes = Base64.getDecoder().decode(msg.getFileDataBase64());
            HBox voiceBox = new HBox(10);
            voiceBox.setAlignment(Pos.CENTER_LEFT);
            voiceBox.setStyle(isMe ?
                    "-fx-background-color: #0084ff; -fx-padding: 8 12; -fx-background-radius: 14;" :
                    "-fx-background-color: #e4e6eb; -fx-padding: 8 12; -fx-background-radius: 14;");

            Label micIcon = new Label("🎙️");
            micIcon.setStyle("-fx-font-size: 18px;");

            Label voiceLabel = new Label("Tin nhắn thoại (" + (wavBytes.length / 1024) + " KB)");
            voiceLabel.setStyle(isMe ? "-fx-text-fill: white;" : "-fx-text-fill: #050505;");

            Button playBtn = new Button("▶ Nghe");
            playBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #ffffff; -fx-cursor: hand; -fx-padding: 4 10; -fx-background-radius: 6;");
            playBtn.setOnAction(e -> {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.stop();
                    playBtn.setText("▶ Nghe");
                } else {
                    playBtn.setText("⏹ Dừng");
                    audioPlayer.play(wavBytes, () -> Platform.runLater(() -> playBtn.setText("▶ Nghe")));
                }
            });

            voiceBox.getChildren().addAll(micIcon, voiceLabel, playBtn);
            bubbleBox.getChildren().add(voiceBox);
        } else {
            // Tin nhắn văn bản thông thường
            Label textLabel = new Label(msg.getContent());
            textLabel.setWrapText(true);
            textLabel.setStyle(isMe ?
                    "-fx-background-color: #0084ff; -fx-text-fill: white; -fx-padding: 9 14; -fx-background-radius: 18; -fx-font-size: 14px;" :
                    "-fx-background-color: #e4e6eb; -fx-text-fill: #050505; -fx-padding: 9 14; -fx-background-radius: 18; -fx-font-size: 14px;");

            bubbleBox.getChildren().add(textLabel);
        }

        if (isMe) {
            Label timeLabel = new Label(msg.getTimestamp());
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0aec0; -fx-alignment: BASELINE_RIGHT;");
            bubbleBox.getChildren().add(timeLabel);
        }

        bubbleRow.getChildren().add(bubbleBox);
        messageContainer.getChildren().add(bubbleRow);

        // Cuộn xuống dòng cuối
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    public void addSystemMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label label = new Label("🔔 " + text);
        label.setStyle("-fx-background-color: #f7fafc; -fx-text-fill: #718096; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px;");
        row.getChildren().add(label);

        messageContainer.getChildren().add(row);
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private void saveFileToDisk(String fileName, byte[] data) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu tệp tin");
        chooser.setInitialFileName(fileName);
        File dest = chooser.showSaveDialog(stage);
        if (dest != null) {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(data);
                showInfo("Đã lưu tệp tin thành công tại:\n" + dest.getAbsolutePath());
            } catch (Exception e) {
                showWarning("Lỗi lưu file: " + e.getMessage());
            }
        }
    }

    // --- Quản lý cuộc gọi Audio / Video UDP ---

    private void startCall(boolean isVideo) {
        if (currentTargetType != ChatTargetType.DIRECT) return;

        activeCallWindow = new CallWindow(chatClient, currentTargetName, isVideo, false);
        int localUdpPort = activeCallWindow.prepareIncomingReceiver("0.0.0.0");

        chatClient.sendCallRequest(currentTargetName, localUdpPort, isVideo);
        addSystemMessage("Đang gửi yêu cầu " + (isVideo ? "Video Call" : "Voice Call") + " tới [" + currentTargetName + "]...");
    }

    private void handleCallRequest(NetworkMessage msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cuộc gọi đến");
        alert.setHeaderText("[" + msg.getSender() + "] đang gọi " + (msg.isVideoCall() ? "Video" : "Thoại") + " qua UDP!");
        alert.setContentText("Bạn có muốn nhận cuộc gọi không?");

        ButtonType acceptBtn = new ButtonType("Nhận cuộc gọi");
        ButtonType rejectBtn = new ButtonType("Từ chối", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(acceptBtn, rejectBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == acceptBtn) {
            activeCallWindow = new CallWindow(chatClient, msg.getSender(), msg.isVideoCall(), true);
            int myUdpPort = activeCallWindow.prepareIncomingReceiver("0.0.0.0");
            activeCallWindow.connectedWithRemote(msg.getSenderHost(), msg.getUdpPort());
            chatClient.sendCallAccept(msg.getSender(), myUdpPort);
        } else {
            chatClient.sendCallReject(msg.getSender());
        }
    }

    private void handleCallAccept(NetworkMessage msg) {
        if (activeCallWindow != null) {
            activeCallWindow.connectedWithRemote(msg.getSenderHost(), msg.getUdpPort());
            addSystemMessage("Cuộc gọi UDP đã được kết nối với [" + msg.getSender() + "]!");
        }
    }

    private void handleCallReject(NetworkMessage msg) {
        if (activeCallWindow != null) {
            activeCallWindow.endCall();
            activeCallWindow = null;
        }
        showInfo("[" + msg.getSender() + "] đã từ chối cuộc gọi.");
    }

    private void handleCallEnd(NetworkMessage msg) {
        if (activeCallWindow != null) {
            activeCallWindow.onRemoteEnded();
            activeCallWindow = null;
        }
        addSystemMessage("Cuộc gọi với [" + msg.getSender() + "] đã kết thúc.");
    }

    // --- Quản lý Nhóm Chat ---

    private void showCreateGroupDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo Nhóm Chat Mới");
        dialog.setHeaderText("Tạo phòng chat Multicast mới");
        dialog.setContentText("Nhập tên nhóm:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                chatClient.createGroup(trimmed);
            }
        });
    }

    // --- Client Event Listeners ---

    private void setupClientListeners() {
        chatClient.setOnMessageReceived(msg -> {
            switch (msg.getType()) {
                case CALL_REQUEST:
                    handleCallRequest(msg);
                    break;
                case CALL_ACCEPT:
                    handleCallAccept(msg);
                    break;
                case CALL_REJECT:
                    handleCallReject(msg);
                    break;
                case CALL_END:
                    handleCallEnd(msg);
                    break;
                case SYSTEM_NOTIFICATION:
                    addSystemMessage(msg.getContent());
                    break;
                case CHAT_BROADCAST:
                case CHAT_GROUP:
                case CHAT_DIRECT:
                case FILE_TRANSFER:
                case VOICE_NOTE:
                    displayMessage(msg);
                    break;
                default:
                    break;
            }
        });

        chatClient.setOnUserListUpdated(users -> {
            userListView.getItems().clear();
            for (String u : users) {
                if (!u.equalsIgnoreCase(chatClient.getUsername())) {
                    userListView.getItems().add(u);
                }
            }
        });

        chatClient.setOnGroupListUpdated(groups -> {
            groupListView.getItems().clear();
            groupListView.getItems().addAll(groups);
        });

        chatClient.setOnConnectionError(err -> {
            showWarning("Lỗi mạng: " + err);
        });
    }

    public void cleanup() {
        if (audioPlayer != null) audioPlayer.stop();
        if (audioRecorder != null && audioRecorder.isRecording()) audioRecorder.stopRecording();
        if (activeCallWindow != null) activeCallWindow.endCall();
        if (udpMulticastService != null) udpMulticastService.stop();
        if (chatClient != null) chatClient.disconnect();
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
