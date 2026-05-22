package org.jetby.clans.addon.quest;

import org.jetby.clans.api.service.clan.Clan;
import org.jetby.clans.api.service.clan.member.Member;
import org.jetby.libb.action.ActionContext;
import org.jetby.libb.action.ActionExecute;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetby.clans.addon.Quests;
import org.jetby.clans.addon.configuration.QuestStorage;
import org.jetby.clans.addon.model.Quest;
import org.jetby.clans.addon.model.QuestData;

import java.util.Set;

public class QuestManager {

    private final Quests addon;
    private final QuestStorage storage;

    public QuestManager(Quests addon, QuestStorage storage) {
        this.addon = addon;
        this.storage = storage;
    }

    public boolean isCompleted(@NotNull Member member, @NotNull Quest quest) {
        Clan clan = getClan(member);
        if (clan == null) return false;
        return storage.get(clan.getId()).isCompleted(member.getUuid(), quest.id());
    }

    public boolean isPassable(@NotNull Member member, @NotNull Quest quest) {
        if (!addon.isGradual()) return true;

        for (Set<Quest> categoryQuests : addon.getQuestsLoader().getCategories().values()) {
            boolean found = false;
            for (Quest q : categoryQuests) {
                if (q.id().equals(quest.id())) {
                    found = true;
                    break;
                }
                if (!isCompleted(member, q)) return false;
            }
            if (found) return true;
        }
        return false;
    }

    public int getProgress(@NotNull Member member, @NotNull Quest quest) {
        Clan clan = getClan(member);
        if (clan == null) return 0;
        return storage.get(clan.getId()).getProgress(member.getUuid(), quest.id());
    }

    public void setProgress(@NotNull Member member, @NotNull Quest quest, int progress) {
        Clan clan = getClan(member);
        if (clan == null) return;

        QuestData data = storage.get(clan.getId());
        data.setProgress(member.getUuid(), quest.id(), progress);

        if (progress >= quest.target() && !data.isCompleted(member.getUuid(), quest.id())) {
            data.markCompleted(member.getUuid(), quest.id());
        }
    }

    public void addProgress(@NotNull Player player, @NotNull Member member,
                            @NotNull QuestType type, @Nullable String property, int amount) {
        Clan clan = getClan(member);
        if (clan == null) return;

        for (Quest quest : addon.getQuestsLoader().getQuests().values()) {
            if (isCompleted(member, quest)) continue;
            if (!isPassable(member, quest)) continue;
            if (quest.disabledWorlds().contains(player.getWorld().getName())) continue;
            if (quest.questType() != type) continue;
            if (property != null && !property.equals(quest.questProperty())) continue;

            if (quest.progressType() == QuestProgressType.GLOBAL) {
                addGlobalProgress(player, member, clan, quest, amount);
            } else {
                addIndividualProgress(player, member, clan, quest, amount);
            }
            break;
        }
    }

    private void addIndividualProgress(@NotNull Player player, @NotNull Member member,
                                       @NotNull Clan clan, @NotNull Quest quest, int amount) {
        QuestData data = storage.get(clan.getId());
        int newProgress = data.getProgress(member.getUuid(), quest.id()) + amount;
        data.setProgress(member.getUuid(), quest.id(), newProgress);

        if (newProgress >= quest.target()) {
            finishIndividual(player, member, clan, quest, data);
        }
    }

    private void addGlobalProgress(@NotNull Player player, @NotNull Member member,
                                   @NotNull Clan clan, @NotNull Quest quest, int amount) {
        QuestData data = storage.get(clan.getId());
        int newProgress = data.getProgress(member.getUuid(), quest.id()) + amount;

        for (Member m : clan.getMembersWithLeader()) {
            data.setProgress(m.getUuid(), quest.id(), newProgress);
        }

        if (newProgress >= quest.target()) {
            finishGlobal(player, member, clan, quest, data);
        }
    }

    private void finishIndividual(@NotNull Player player, @NotNull Member member,
                                  @NotNull Clan clan, @NotNull Quest quest, @NotNull QuestData data) {
        if (data.isCompleted(member.getUuid(), quest.id())) return;
        data.markCompleted(member.getUuid(), quest.id());

        ActionExecute.run(buildContext(player, member, clan, quest), quest.rewards());
    }

    private void finishGlobal(@NotNull Player player, @NotNull Member member,
                              @NotNull Clan clan, @NotNull Quest quest, @NotNull QuestData data) {
        boolean alreadyCompleted = clan.getMembersWithLeader().stream()
                .anyMatch(m -> data.isCompleted(m.getUuid(), quest.id()));
        if (alreadyCompleted) return;

        ActionExecute.run(buildContext(player, member, clan, quest), quest.globalRewards());

        for (Member m : clan.getMembersWithLeader()) {
            data.markCompleted(m.getUuid(), quest.id());

            Player target = Bukkit.getPlayer(m.getUuid());
            if (target != null && target.isOnline()) {
                ActionExecute.run(buildContext(target, m, clan, quest), quest.rewards());
            }
        }
    }

    private ActionContext buildContext(Player player, Member member, Clan clan, Quest quest) {
        return ActionContext.of(player, addon.getServiceManager().getPlugin())
                .replace("%name%", quest.name())
                .replace("%id%", quest.id())
                .replace("%description%", quest.description())
                .replace("%target%", String.valueOf(quest.target()))
                .with(member)
                .with(clan);
    }

    @Nullable
    private Clan getClan(@NotNull Member member) {
        return addon.getServiceManager().getClanManager().lookup().getClanByMember(member);
    }
}