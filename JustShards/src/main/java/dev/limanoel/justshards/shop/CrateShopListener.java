package dev.limanoel.justshards.shop;

import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class CrateShopListener implements Listener {

    private final CrateShopManager manager;
    private final LucoPointPlugin plugin;

    public CrateShopListener(CrateShopManager manager, LucoPointPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!manager.isAwaitingPrice(uuid)) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage();

        
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int price = Integer.parseInt(message);

                if (price <= 0) {
                    player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ §Der Preis muss größer als 0 sein!");
                    manager.removePriceInput(uuid);
                    return;
                }

                org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();

                if (item.getType().isAir()) {
                    player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ Du hältst kein Item in der Hand!");
                    manager.removePriceInput(uuid);
                    return;
                }

                manager.addItemToShop(uuid, item, price);

            } catch (NumberFormatException e) {
                player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ Bitte gib eine gültige Zahl ein!");
                manager.removePriceInput(uuid);
            }
        });
    }

    

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!manager.isShopInventory(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot < 0 || slot >= manager.MAX_SLOTS) {
            return;
        }

        manager.buyItem(player, slot);
    }
}
