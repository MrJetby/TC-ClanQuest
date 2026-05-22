package org.jetby.clans.addon.model;

import org.jetbrains.annotations.Nullable;
import org.jetby.clans.addon.quest.QuestProgressType;
import org.jetby.clans.addon.quest.QuestType;

import java.util.List;

public record Quest(
        String id,
        String name,
        String description,
        List<String> rewardsDescription,
        QuestProgressType progressType,
        QuestType questType,
        @Nullable String questProperty,
        int target,
        List<String> globalRewards,
        List<String> rewards,
        List<String> disabledWorlds

) {
}
