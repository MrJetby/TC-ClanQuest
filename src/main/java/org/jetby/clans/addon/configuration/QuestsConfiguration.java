package org.jetby.clans.addon.configuration;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetby.clans.addon.model.Quest;
import org.jetby.clans.addon.quest.QuestProgressType;
import org.jetby.clans.addon.quest.QuestType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class QuestsConfiguration {

    private static final Logger LOGGER = Logger.getLogger("QuestsConfiguration");

    private final FileConfiguration configuration;

    @Getter
    private final LinkedHashMap<String, Set<Quest>> categories = new LinkedHashMap<>();
    @Getter
    private final LinkedHashMap<String, Quest> quests = new LinkedHashMap<>();

    public QuestsConfiguration(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    public void load() {
        quests.clear();
        categories.clear();

        for (String key : configuration.getKeys(false)) {
            if (key.equals("category")) continue;
            ConfigurationSection section = configuration.getConfigurationSection(key);
            if (section == null) continue;

            String type = section.getString("type");
            if (type == null) {
                LOGGER.warning("Quest '" + key + "' has no type, skipping.");
                continue;
            }

            String[] typeArgs = type.split(";");
            QuestType questType;
            try {
                questType = QuestType.valueOf(typeArgs[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Quest '" + key + "' has unknown type '" + typeArgs[0] + "', skipping.");
                continue;
            }

            String questProperty = typeArgs.length == 2 ? typeArgs[1].toUpperCase() : null;
            QuestProgressType progressType = QuestProgressType.valueOf(
                    section.getString("progress", "GLOBAL").toUpperCase());

            quests.put(key, new Quest(
                    key,
                    section.getString("name"),
                    section.getString("description"),
                    section.getStringList("rewards-description"),
                    progressType,
                    questType,
                    questProperty,
                    section.getInt("target"),
                    section.getStringList("global-rewards"),
                    section.getStringList("rewards"),
                    section.getStringList("disabled-worlds")
            ));
        }

        ConfigurationSection categoriesSection = configuration.getConfigurationSection("category");
        if (categoriesSection == null) return;

        for (String key : categoriesSection.getKeys(false)) {
            List<String> questIds = categoriesSection.getStringList(key);
            if (questIds.isEmpty()) continue;

            Set<Quest> questSet = new LinkedHashSet<>();
            for (String questId : questIds) {
                Quest q = quests.get(questId);
                if (q != null) {
                    questSet.add(q);
                } else {
                    LOGGER.warning("Quest '" + questId + "' in category '" + key + "' not found.");
                }
            }

            if (!questSet.isEmpty()) categories.put(key, questSet);
        }
    }
}