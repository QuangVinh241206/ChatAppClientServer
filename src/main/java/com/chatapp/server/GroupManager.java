package com.chatapp.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách các Nhóm chat (Multicast Rooms) trong bộ nhớ Server (In-Memory)
 */
public class GroupManager {
    // Tên nhóm -> Tập hợp username các thành viên trong nhóm
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();

    public GroupManager() {
        // Tạo sẵn các nhóm mặc định phục vụ kiểm thử nhanh
        createGroup("LapTrinhMang_K65", "System");
        createGroup("Team_DuAn_Java", "System");
        createGroup("GiaiTri_ChemGio", "System");
    }

    public synchronized boolean createGroup(String groupName, String creator) {
        if (groupName == null || groupName.trim().isEmpty() || groups.containsKey(groupName)) {
            return false;
        }
        Set<String> members = Collections.synchronizedSet(new HashSet<>());
        if (creator != null && !creator.equals("System")) {
            members.add(creator);
        }
        groups.put(groupName.trim(), members);
        return true;
    }

    public synchronized boolean joinGroup(String groupName, String username) {
        Set<String> members = groups.get(groupName);
        if (members != null) {
            return members.add(username);
        }
        return false;
    }

    public synchronized boolean leaveGroup(String groupName, String username) {
        Set<String> members = groups.get(groupName);
        if (members != null) {
            return members.remove(username);
        }
        return false;
    }

    public Set<String> getGroupMembers(String groupName) {
        return groups.getOrDefault(groupName, Collections.emptySet());
    }

    public List<String> getAllGroupNames() {
        return new ArrayList<>(groups.keySet());
    }

    public synchronized void removeUserFromAllGroups(String username) {
        for (Set<String> members : groups.values()) {
            members.remove(username);
        }
    }
}
