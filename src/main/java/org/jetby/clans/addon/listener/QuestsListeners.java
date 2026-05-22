package org.jetby.clans.addon.listener;

import org.jetby.clans.api.service.clan.Clan;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetby.clans.addon.Quests;
import org.jetby.clans.addon.quest.QuestManager;
import org.jetby.clans.addon.quest.QuestType;

public class QuestsListeners implements Listener {

    private final Quests addon;
    private final QuestManager questManager;

    public QuestsListeners(Quests addon) {
        this.addon = addon;
        this.questManager = addon.getQuestManager();
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null) run(killer, QuestType.PLAYER_KILL, null);
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null) run(killer, QuestType.ENTITY_KILL, e.getEntity().getType().name());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        run(e.getPlayer(), QuestType.BLOCK_PLACE, e.getBlock().getType().name());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        run(e.getPlayer(), QuestType.BLOCK_BREAK, e.getBlock().getType().name());
    }

    private void run(Player player, QuestType type, String property) {
        var lookup = addon.getServiceManager().getClanManager().lookup();
        if (!lookup.isInClan(player.getUniqueId())) return;

        Clan clan = lookup.getClanByMember(player.getUniqueId());
        questManager.addProgress(player, clan.getMember(player.getUniqueId()), type, property, 1);
    }
}