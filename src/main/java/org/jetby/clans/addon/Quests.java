package org.jetby.clans.addon;

import lombok.Getter;
import org.jetby.clans.api.addons.JavaAddon;
import org.jetby.clans.api.addons.annotations.ClanAddon;
import org.jetby.clans.api.gui.ClanGuiData;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetby.clans.addon.configuration.QuestStorage;
import org.jetby.clans.addon.configuration.QuestsConfiguration;
import org.jetby.clans.addon.gui.QuestsGui;
import org.jetby.clans.addon.listener.QuestsListeners;
import org.jetby.clans.addon.quest.QuestManager;

@ClanAddon(
        id = "Quests",
        version = "1.0"
)
@Getter
public class Quests extends JavaAddon {

    private QuestsConfiguration questsLoader;
    private QuestManager questManager;
    private QuestStorage questStorage;
    private boolean isGradual = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.isGradual = getConfig().getBoolean("gradual-quest", true);

        questStorage = new QuestStorage(getDataFolder(), getLogger());
        questStorage.load();

        questManager = new QuestManager(this, questStorage);

        questsLoader = new QuestsConfiguration(getConfiguration("quests.yml"));
        questsLoader.load();

        FileConfiguration quests = getConfiguration("gui.yml");
        ClanGuiData gui = getServiceManager().getGuiFactory().parse(quests);
        getServiceManager().getGuiFactory().add(quests.getString("id", "quests"), gui);

        getServiceManager().getGuiFactory().register("quests", ctx -> {
            ctx.with(this);
            return new QuestsGui(ctx);
        });

        getServiceManager().getEventRegistrar().register(this, new QuestsListeners(this));
    }

    @Override
    public void onDisable() {
        if (questStorage != null) {
            questStorage.save();
        }
    }
}