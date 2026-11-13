package com.chatapp.common;

import com.google.gson.Gson;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Đối tượng gói tin đóng gói thông điệp gửi qua mạng.
 * Dễ dàng chuyển đổi hai chiều sang JSON qua Gson.
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MessageType type;
    private String sender;
    private String recipient; // "ALL" cho Broadcast, "GROUP:<name>" cho Multicast, hoặc "<username>" cho Direct
    private String content;
    private String fileName;
    private long fileSize;
    private String fileDataBase64; // Dữ liệu nhị phân (Ảnh, Audio, Video, File) mã hoá Base64
    private String timestamp;

    // Phục vụ cho cuộc gọi UDP Audio / Video
    private int udpPort;
    private boolean isVideoCall;

    // Danh sách kèm theo nếu là USER_LIST hoặc GROUP_LIST
    private List<String> userList;
    private List<String> groupList;

    public NetworkMessage() {
        this.timestamp = LocalTime.now().format(TIME_FORMATTER);
    }

    public NetworkMessage(MessageType type, String sender, String recipient, String content) {
        this();
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    public static NetworkMessage fromJson(String json) {
        return GSON.fromJson(json, NetworkMessage.class);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getFileDataBase64() { return fileDataBase64; }
    public void setFileDataBase64(String fileDataBase64) { this.fileDataBase64 = fileDataBase64; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getUdpPort() { return udpPort; }
    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }

    public boolean isVideoCall() { return isVideoCall; }
    public void setVideoCall(boolean videoCall) { isVideoCall = videoCall; }

    public List<String> getUserList() { return userList; }
    public void setUserList(List<String> userList) { this.userList = userList; }

    public List<String> getGroupList() { return groupList; }
    public void setGroupList(List<String> groupList) { this.groupList = groupList; }
}
