package com.hybridac.menu;

import com.hybridac.config.MenuSettings;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public final class SuspectedMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, UUID> slotToPlayerUuid = new HashMap<>();

    public SuspectedMenu(MenuSettings settings) {
        this.inventory = Bukkit.createInventory(this, settings.size(), ColorUtil.colorize(settings.title()));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getPlayerUuid(int slot) {
        return slotToPlayerUuid.get(slot);
    }

    public void populate(PlayerDataService playerDataService, MenuSettings settings) {
        inventory.clear();
        slotToPlayerUuid.clear();

        Material borderMat = Material.matchMaterial(settings.borderMaterial());
        if (borderMat == null) {
            borderMat = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack border = new ItemStack(borderMat);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(ColorUtil.colorize(settings.borderName()));
            border.setItemMeta(borderMeta);
        }

        Set<Integer> borderSlots = new HashSet<>(settings.borderSlots());
        for (int slot : borderSlots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, border);
            }
        }

        List<Player> suspects = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = playerDataService.get(player.getUniqueId());
            if (data == null) {
                continue;
            }
            double prob = Math.max(data.suspicionState().smoothedConfidence(), data.suspicionState().lastMlProbabilityRaw());
            if (prob >= settings.minProbability()) {
                suspects.add(player);
            }
        }

        suspects.sort((p1, p2) -> {
            PlayerData d1 = playerDataService.get(p1.getUniqueId());
            PlayerData d2 = playerDataService.get(p2.getUniqueId());
            double prob1 = d1 != null ? Math.max(d1.suspicionState().smoothedConfidence(), d1.suspicionState().lastMlProbabilityRaw()) : 0.0D;
            double prob2 = d2 != null ? Math.max(d2.suspicionState().smoothedConfidence(), d2.suspicionState().lastMlProbabilityRaw()) : 0.0D;
            return Double.compare(prob2, prob1);
        });

        int suspectIndex = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (borderSlots.contains(slot)) {
                continue;
            }
            if (suspectIndex >= suspects.size()) {
                break;
            }
            Player targetPlayer = suspects.get(suspectIndex++);
            PlayerData data = playerDataService.get(targetPlayer.getUniqueId());
            if (data == null) {
                continue;
            }

            ItemStack head = createPlayerHead(targetPlayer, data, settings);
            inventory.setItem(slot, head);
            slotToPlayerUuid.put(slot, targetPlayer.getUniqueId());
        }
    }

    private ItemStack createPlayerHead(Player targetPlayer, PlayerData data, MenuSettings settings) {
        Material mat = Material.matchMaterial(settings.headMaterial());
        if (mat == null) {
            mat = Material.PLAYER_HEAD;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(targetPlayer);
            }

            double prob = data.suspicionState().smoothedConfidence();
            int pct = (int) Math.round(prob * 100.0D);

            Map<String, String> placeholders = Map.of(
                    "player", targetPlayer.getName(),
                    "probability", String.valueOf(pct),
                    "confidence", String.valueOf(pct)
            );

            meta.setDisplayName(ColorUtil.colorize(settings.headName(), placeholders));

            List<String> coloredLore = new ArrayList<>();
            for (String line : settings.headLore()) {
                coloredLore.add(ColorUtil.colorize(line, placeholders));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
