package com.chatapp.common;

/**
 * Định nghĩa các loại thông điệp trao đổi giữa Client và Server
 * Phục vụ cho cả giao thức TCP (tin cậy) và các tín hiệu điều khiển UDP
 */
public enum MessageType {
    // Quản lý phiên kết nối
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    USER_LIST,

    // Nhắn tin văn bản
    CHAT_BROADCAST,     // Broadcast: Phát tán tới tất cả client trên Server
    CHAT_GROUP,         // Multicast: Phát tán tới danh sách client trong 1 Room / Nhóm
    CHAT_DIRECT,        // Peer/1-1: Nhắn tin riêng tư giữa 2 client

    // Đính kèm & Media
    FILE_TRANSFER,      // Gửi tập tin bất kỳ (Ảnh, PDF, Doc, Video, ...)
    VOICE_NOTE,         // Gửi tin nhắn thoại đã ghi âm (Voice Audio)

    // Quản lý phòng / nhóm (Multicast Group)
    GROUP_CREATE,
    GROUP_JOIN,
    GROUP_LEAVE,
    GROUP_LIST,

    // Tín hiệu thiết lập cuộc gọi âm thanh & video qua UDP
    CALL_REQUEST,       // Yêu cầu gọi (kèm port UDP và loại Voice/Video)
    CALL_ACCEPT,        // Đồng ý nhận cuộc gọi
    CALL_REJECT,        // Từ chối cuộc gọi
    CALL_END,           // Kết thúc cuộc gọi

    // Thông báo từ hệ thống
    SYSTEM_NOTIFICATION
}
