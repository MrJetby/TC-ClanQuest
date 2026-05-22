package org.jetby.clans.addon.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetby.clans.addon.model.QuestData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class QuestStorage {

    private final File file;
    private final Logger logger;
    private final Map<String, QuestData> data = new HashMap<>();

    public QuestStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "quest-data.yml");
        this.logger = logger;
    }

    public QuestData get(String clanId) {
        return data.computeIfAbsent(clanId, k -> new QuestData());
    }

    public void remove(String clanId) {
        data.remove(clanId);
    }

    public void load() {
        data.clear();
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String clanId : config.getKeys(false)) {
            QuestData questData = new QuestData();
            ConfigurationSection clanSection = config.getConfigurationSection(clanId);
            if (clanSection == null) continue;

            ConfigurationSection progressSection = clanSection.getConfigurationSection("progress");
            if (progressSection != null) {
                for (String questId : progressSection.getKeys(false)) {
                    ConfigurationSection playersSection = progressSection.getConfigurationSection(questId);
                    if (playersSection == null) continue;
                    for (String uuidStr : playersSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            int value = playersSection.getInt(uuidStr, 0);
                            questData.setProgress(uuid, questId, value);
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid UUID in quest-data.yml: " + uuidStr);
                        }
                    }
                }
            }

            ConfigurationSection completedSection = clanSection.getConfigurationSection("completed");
            if (completedSection != null) {
                for (String uuidStr : completedSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        List<String> completedQuests = completedSection.getStringList(uuidStr);
                        completedQuests.forEach(questId -> questData.markCompleted(uuid, questId));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid UUID in quest-data.yml: " + uuidStr);
                    }
                }
            }

            data.put(clanId, questData);
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, QuestData> entry : data.entrySet()) {
            String clanId = entry.getKey();
            QuestData questData = entry.getValue();

            for (Map.Entry<UUID, Map<String, Integer>> progressEntry : questData.getAllProgress().entrySet()) {
                UUID uuid = progressEntry.getKey();
                for (Map.Entry<String, Integer> questEntry : progressEntry.getValue().entrySet()) {
                    config.set(clanId + ".progress." + questEntry.getKey() + "." + uuid, questEntry.getValue());
                }
            }

            for (Map.Entry<UUID, List<String>> completedEntry : questData.getAllCompleted().entrySet()) {
                if (!completedEntry.getValue().isEmpty()) {
                    config.set(clanId + ".completed." + completedEntry.getKey(), completedEntry.getValue());
                }
            }
        }

        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            logger.severe("Failed to save quest-data.yml: " + e.getMessage());
        }
    }
}