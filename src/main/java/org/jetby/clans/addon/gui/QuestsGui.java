package org.jetby.clans.addon.gui;

import org.jetby.clans.api.gui.Gui;
import org.jetby.clans.api.gui.GuiContext;
import org.jetby.clans.api.service.clan.member.Member;
import org.jetby.libb.gui.item.ItemWrapper;
import org.jetby.libb.gui.parser.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetby.clans.addon.Quests;
import org.jetby.clans.addon.model.Quest;
import org.jetby.clans.addon.quest.QuestProgressType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestsGui extends Gui {

    private final Quests addon;

    public QuestsGui(@NotNull GuiContext ctx) {
        super(ctx);
        this.addon = ctx.get(Quests.class);
        setup();
    }

    private void setup() {
        List<Item> questItems = getBySectionOption("type").stream()
                .filter(b -> {
                    String type = b.section().getString("type", "");
                    return type.equals("all_quests") || type.startsWith("category-");
                })
                .toList();

        if (questItems.isEmpty()) return;

        Item template = questItems.get(0);
        String templateType = template.section().getString("type", "");

        List<Integer> slots = questItems.stream()
                .flatMap(i -> i.slots().stream())
                .distinct()
                .toList();
        contentSlots(slots.toArray(new Integer[0]));

        List<Quest> quests = buildQuestList(templateType);
        if (quests.isEmpty()) return;

        Member member = getClan().getMember(getViewer().getUniqueId());

        for (Quest quest : quests) {
            int progress = addon.getQuestManager().getProgress(member, quest);

            setReplace("%status%", status(member, quest));
            setReplace("%quest_name%", quest.name());
            setReplace("%quest_description%", quest.description());
            setReplace("%quest_progress%", String.valueOf(progress));
            setReplace("%quest_target%", String.valueOf(quest.target()));
            setReplace("%quest_progress_type%", progressType(quest));

            ItemStack itemStack = template.itemStack().clone();
            ItemWrapper wrapper = new ItemWrapper(itemStack);
            wrapper.onClick(event -> event.setCancelled(true));
            wrapper.serializer(defaultSerializer);
            wrapper.displayName(applyPlaceholders(template.section().getString("display_name", "")));

            List<String> lore = new ArrayList<>(template.section().getStringList("lore"));
            lore.addAll(quest.rewardsDescription());
            wrapper.setLore(lore.stream().map(this::applyPlaceholders).collect(Collectors.toList()));

            if (template.customModelData() != 0) wrapper.customModelData(template.customModelData());
            if (template.enchanted()) wrapper.enchanted(true);

            addItem(wrapper);
        }
    }

    private List<Quest> buildQuestList(String templateType) {
        List<Quest> list = new ArrayList<>();
        if ("all_quests".equalsIgnoreCase(templateType)) {
            list.addAll(addon.getQuestsLoader().getQuests().values());
        } else if (templateType.startsWith("category-")) {
            String catId = templateType.substring(9);
            Set<Quest> cat = addon.getQuestsLoader().getCategories().get(catId);
            if (cat != null) list.addAll(cat);
        }
        return list;
    }

    @Override
    public boolean cancelRegistration(@NotNull Item item) {
        if (item.type() != null && item.section() != null) {
            String type = item.section().getString("type", "");
            return type.equalsIgnoreCase("all_quests") || type.startsWith("category-");
        }
        return false;
    }

    private String status(Member member, Quest quest) {
        return addon.getQuestManager().isCompleted(member, quest)
                ? addon.getConfig().getString("quest-status-completed")
                : addon.getConfig().getString("quest-status-uncompleted");
    }

    private String progressType(Quest quest) {
        return quest.progressType() == QuestProgressType.INDIVIDUAL
                ? addon.getConfig().getString("quest-progress-type-individual")
                : addon.getConfig().getString("quest-progress-type-global");
    }
}