package org.jetby.clans.addon.configuration;

import org.jetby.clans.addon.model.QuestData;
import org.jetby.clans.api.TreexClansAPI;
import org.jetby.clans.api.service.clan.Clan;
import org.jetby.clans.api.storage.Storage;
import org.jetby.clans.api.storage.base.BaseSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestStorage {

    private final Storage storage;

    public QuestStorage() {
        this.storage = TreexClansAPI.get().getStorage();
    }

    private final Map<Clan, QuestData> data = new HashMap<>();

    public QuestData get(Clan clan) {
        return data.computeIfAbsent(clan, k -> new QuestData());
    }

    public void remove(Clan clan) {
        data.remove(clan);
    }

    public void load() {
        data.clear();

        for (Clan clan : storage.getClanList(Integer.MAX_VALUE)) {
            QuestData questData = new QuestData();
            BaseSection base = storage.getSection().of(clan).section("Quests");

            BaseSection progressSection = base.section("progress");
            if (progressSection != null) {

                for (String questId : progressSection.keys().join()) {
                    BaseSection playersSection = progressSection.section(questId);
                    if (playersSection == null) continue;
                    for (String uuidStr : playersSection.keys().join()) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            int value = playersSection.getInt(uuidStr).join();
                            questData.setProgress(uuid, questId, value);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid UUID in quest-data.yml", e);
                        }
                    }
                }

            }

            BaseSection completedSection = base.section("completed");
            if (completedSection != null) {
                for (String uuidStr : completedSection.keys().join()) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        List<String> completedQuests = completedSection.getStringList(uuidStr).join();
                        completedQuests.forEach(questId -> questData.markCompleted(uuid, questId));
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid UUID in quest-data.yml", e);
                    }
                }
            }

            data.put(clan, questData);
        }
    }

    public void save() {


        for (Map.Entry<Clan, QuestData> entry : data.entrySet()) {
            Clan clan = entry.getKey();
            QuestData questData = entry.getValue();

            BaseSection base = storage.getSection().of(clan).section("Quests");

            for (Map.Entry<UUID, Map<String, Integer>> progressEntry : questData.getAllProgress().entrySet()) {
                UUID uuid = progressEntry.getKey();

                for (Map.Entry<String, Integer> questEntry : progressEntry.getValue().entrySet()) {
                    base.section("progress").section(questEntry.getKey()).set(uuid.toString(), questEntry.getValue());
                }
            }

            for (Map.Entry<UUID, List<String>> completedEntry : questData.getAllCompleted().entrySet()) {
                if (!completedEntry.getValue().isEmpty()) {
                    base.section("completed").set(completedEntry.getKey().toString(), completedEntry.getValue());
                }
            }
        }

    }
}