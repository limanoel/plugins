package dev.limanoel.clashOneBlockRace.commands;

import dev.limanoel.clashOneBlockRace.manager.ConfigManager;
import dev.limanoel.clashOneBlockRace.manager.GameManager;
import dev.limanoel.clashOneBlockRace.manager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OneBlockRaceCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GameManager gameManager;
    private final ConfigManager configManager;
    private final NamespacedKey guiKey;

    public OneBlockRaceCommand(JavaPlugin plugin, WorldManager worldManager, GameManager gameManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.gameManager = gameManager;
        this.configManager = configManager;
        this.guiKey = new NamespacedKey(plugin, "gui_item");
    }

    public Inventory createJoinGUI() {
        Inventory gui = Bukkit.createInventory(
                null,
                configManager.getGuiSize(),
                configManager.getGuiTitle()
        );

        ItemStack joinItem = createGuiItem(
                configManager.getGuiJoinMaterial(),
                configManager.getGuiJoinName(),
                configManager.getGuiJoinLore()
        );
        gui.setItem(configManager.getGuiJoinSlot(), joinItem);

        ItemStack barrier = createGuiItem(
                configManager.getGuiBarrierMaterial(),
                configManager.getGuiBarrierName(),
                configManager.getGuiBarrierLore()
        );
        gui.setItem(configManager.getGuiBarrierSlot(), barrier);

        return gui;
    }

    private ItemStack createGuiItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(configManager.parseComponent(name));
            if (loreLines != null && !loreLines.isEmpty()) {
                meta.lore(loreLines.stream()
                        .map(configManager::parseComponent)
                        .toList());
            }

            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && currentItem.hasItemMeta()) {
            ItemMeta meta = currentItem.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(guiKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);

                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }

                configManager.playSound(player, "gui-click");

                Material type = currentItem.getType();

                if (type == configManager.getGuiBarrierMaterial()) {
                    player.closeInventory();
                } else if (type == configManager.getGuiJoinMaterial()) {
                    player.closeInventory();
                    configManager.sendMessage(player, "match-preparing");

                    org.bukkit.World zielWelt = null;

                    for (org.bukkit.World welt : org.bukkit.Bukkit.getWorlds()) {
                        if (welt.getName().startsWith("oneblockmultiplayer_") && gameManager.isState(welt.getName(), "WAITING")) {
                            zielWelt = welt;
                            break;
                        }
                    }

                    String finalWorldName;

                    if (zielWelt == null) {
                        int naechsteNummer = 1;

                        while (org.bukkit.Bukkit.getWorld("oneblockmultiplayer_" + naechsteNummer) != null) {
                            naechsteNummer++;
                        }

                        finalWorldName = "oneblockmultiplayer_" + naechsteNummer;

                        worldManager.createOneBlockWorldMultiplayer(finalWorldName);
                        gameManager.setGameState(finalWorldName, "WAITING");
                    } else {
                        finalWorldName = zielWelt.getName();
                    }

                    worldManager.setupMultiplayer(player, finalWorldName);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(guiKey, PersistentDataType.BYTE)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.parseComponent(configManager.getConfig().getString("messages.only-players", "Nur Spieler können diesen Befehl ausführen!")));
            return true;
        }

        player.openInventory(createJoinGUI());
        configManager.playSound(player, "gui-open");
        return true;
    }
}
