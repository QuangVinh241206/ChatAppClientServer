package com.chatapp.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Bảng chọn Icon Biểu cảm (Emoji Picker) phong phú phong cách Zalo / Messenger
 */
public class EmojiPickerPopup {
    private final Popup popup;
    private final Consumer<String> onEmojiSelected;

    // Bộ sưu tập các biểu tượng cảm xúc phổ biến
    private static final String[] EMOJIS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖",
            "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯",
            "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔",
            "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦",
            "👍", "👎", "👏", "🙌", "👐", "🤝", "🙏", "✌️", "🤞", "🤟",
            "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐️",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "❣️",
            "💕", "💞", "💓", "💗", "💖", "💘", "💝", "🔥", "✨", "🎉",
            "🎁", "🎂", "☕", "🍕", "🍔", "🍺", "🍻", "🚀", "💻", "⭐"
    };

    public EmojiPickerPopup(Consumer<String> onEmojiSelected) {
        this.onEmojiSelected = onEmojiSelected;
        this.popup = new Popup();
        this.popup.setAutoHide(true);

        initUI();
    }

    private void initUI() {
        VBox container = new VBox();
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 4); " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        FlowPane emojiPane = new FlowPane();
        emojiPane.setHgap(6);
        emojiPane.setVgap(6);
        emojiPane.setPrefWrapLength(320);
        emojiPane.setAlignment(Pos.CENTER);

        for (String emoji : EMOJIS) {
            Button btn = new Button(emoji);
            btn.setStyle("-fx-font-size: 18px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 6;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 18px; -fx-background-color: #edf2f7; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 6;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 18px; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 6;"));

            btn.setOnAction(e -> {
                if (onEmojiSelected != null) {
                    onEmojiSelected.accept(emoji);
                }
                popup.hide();
            });
            emojiPane.getChildren().add(btn);
        }

        ScrollPane scrollPane = new ScrollPane(emojiPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(220);
        scrollPane.setPrefViewportWidth(320);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        container.getChildren().add(scrollPane);
        popup.getContent().add(container);
    }

    public void show(Window window, double screenX, double screenY) {
        if (popup.isShowing()) {
            popup.hide();
        } else {
            popup.show(window, screenX, screenY);
        }
    }
}
