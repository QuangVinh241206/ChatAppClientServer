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
        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        String cleanName = groupName.trim();
        for (String existing : groups.keySet()) {
            if (existing.equalsIgnoreCase(cleanName)) {
                return false;
            }
        }
        Set<String> members = Collections.synchronizedSet(new HashSet<>());
        if (creator != null && !creator.equals("System")) {
            members.add(creator.trim());
        }
        groups.put(cleanName, members);
        return true;
    }

    public synchronized boolean joinGroup(String groupName, String username) {
        if (groupName == null || groupName.trim().isEmpty()) return false;
        String name = findActualGroupName(groupName.trim());
        Set<String> members = groups.computeIfAbsent(name, k -> Collections.synchronizedSet(new HashSet<>()));
        if (username != null && !username.trim().isEmpty()) {
            return members.add(username.trim());
        }
        return true;
    }

    public synchronized boolean leaveGroup(String groupName, String username) {
        if (groupName == null) return false;
        String name = findActualGroupName(groupName.trim());
        Set<String> members = groups.get(name);
        if (members != null && username != null) {
            return members.remove(username.trim());
        }
        return false;
    }

    public Set<String> getGroupMembers(String groupName) {
        if (groupName == null) return Collections.emptySet();
        String name = findActualGroupName(groupName.trim());
        return groups.getOrDefault(name, Collections.emptySet());
    }

    private String findActualGroupName(String name) {
        for (String existing : groups.keySet()) {
            if (existing.equalsIgnoreCase(name)) {
                return existing;
            }
        }
        return name;
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
