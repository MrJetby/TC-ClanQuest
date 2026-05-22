package org.jetby.clans.addon.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestData {

    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Map<UUID, List<String>> completed = new HashMap<>();

    public Map<String, Integer> getProgress(UUID uuid) {
        return progress.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public int getProgress(UUID uuid, String questId) {
        return getProgress(uuid).getOrDefault(questId, 0);
    }

    public void setProgress(UUID uuid, String questId, int value) {
        getProgress(uuid).put(questId, value);
    }

    public List<String> getCompleted(UUID uuid) {
        return completed.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    public boolean isCompleted(UUID uuid, String questId) {
        List<String> list = completed.get(uuid);
        return list != null && list.contains(questId);
    }

    public void markCompleted(UUID uuid, String questId) {
        getCompleted(uuid).add(questId);
    }

    public Map<UUID, Map<String, Integer>> getAllProgress() {
        return progress;
    }

    public Map<UUID, List<String>> getAllCompleted() {
        return completed;
    }
}