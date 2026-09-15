package com.hybridac.listener;

import com.hybridac.menu.SuspectedMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class MenuListener implements Listener {

    public MenuListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof SuspectedMenu menu) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player moderator)) {
                return;
            }

            int slot = event.getRawSlot();
            if (slot < 0) {
                return;
            }
            UUID targetUuid = menu.getPlayerUuid(slot);
            if (targetUuid != null) {
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null && target.isOnline()) {
                    moderator.teleport(target.getLocation());
                    moderator.sendMessage("§7[§dHybridAC§7] §aВы телепортировались к §f" + target.getName());
                    moderator.closeInventory();
                } else {
                    moderator.sendMessage("§7[§dHybridAC§7] §cИгрок оффлайн.");
                }
            }
        }
    }
}
