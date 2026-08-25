package dev.limanoel.justshards.shop;

import dev.limanoel.justshards.LucoPointPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class CrateShopManager {

    private final LucoPointPlugin plugin;

    private File shopFile;
    private FileConfiguration shopConfig;

    private final Map<UUID, Integer> priceInputting = new HashMap<>();

    public int MAX_SLOTS;
    public String SHOP_TITLE;

    public CrateShopManager(LucoPointPlugin plugin) {

        this.plugin = plugin;

        int configSize = plugin.getConfig().getInt("crateshop.size", 9);

        if (configSize % 9 != 0 || configSize < 9 || configSize > 54) {

            getLogger().warning("§eUngültige CrateShop Größe: " + configSize);
            getLogger().warning("§eNutze Standardgröße 9");

            this.MAX_SLOTS = 9;

        } else {
            this.MAX_SLOTS = configSize;
        }

        this.SHOP_TITLE = plugin.getConfig().getString(
                "crateshop.title",
                ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &9Shop")
        );

        setupShopFile();
    }



    private void setupShopFile() {

        shopFile = new File(plugin.getDataFolder(), "crateshop.yml");

        if (!shopFile.exists()) {

            shopFile.getParentFile().mkdirs();

            plugin.saveResource("crateshop.yml", false);
        }

        shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        getLogger().info("LucoPoint Shop geladen.");
    }

    private void saveShopFile() {

        try {

            shopConfig.save(shopFile);

            shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        } catch (IOException e) {
            getLogger().warning("Error saving shop file: " + e.getMessage());
        }
    }

    public void reload() {
        setupShopFile();
        this.MAX_SLOTS = plugin.getConfig().getInt("crateshop.size", 9);
        this.SHOP_TITLE = plugin.getConfig().getString("crateshop.title", ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &9Shop"));
    }



    public void startPriceDialog(Player player, ItemStack item) {

        Bukkit.getScheduler().runTask(plugin, () -> {

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &fGib den Preis in Shards ein:"));

            priceInputting.put(player.getUniqueId(), 0);
        });
    }

    public boolean isAwaitingPrice(UUID uuid) {
        return priceInputting.containsKey(uuid);
    }

    public void setPriceInput(UUID uuid) {
        priceInputting.put(uuid, 1);
    }

    public void removePriceInput(UUID uuid) {
        priceInputting.remove(uuid);
    }



    public void addItemToShop(UUID playerUuid, ItemStack item, int price) {

        try {

            shopConfig = YamlConfiguration.loadConfiguration(shopFile);

            int slot = -1;

            for (int i = 0; i < MAX_SLOTS; i++) {

                if (!shopConfig.contains("items." + i)) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1) {

                Player player = Bukkit.getPlayer(playerUuid);

                if (player != null) {
                    player.sendMessage("§cDer Shop ist voll!");
                }

                return;
            }

            String path = "items." + slot;

            shopConfig.set(path + ".material", item.getType().name());
            shopConfig.set(path + ".amount", item.getAmount());
            shopConfig.set(path + ".price", price);
            shopConfig.set(path + ".action", "GIVE_ITEM");

            if (item.hasItemMeta()) {

                ItemMeta meta = item.getItemMeta();

                if (meta != null) {

                    if (meta.hasDisplayName()) {
                        shopConfig.set(path + ".displayName", meta.getDisplayName());
                    }

                    if (meta.hasLore()) {
                        shopConfig.set(path + ".lore", meta.getLore());
                    }

                    if (meta.hasCustomModelData()) {
                        shopConfig.set(path + ".customModelData", meta.getCustomModelData());
                    }

                    if (meta.hasEnchants()) {

                        Map<String, Integer> enchants = new HashMap<>();

                        for (Map.Entry<Enchantment, Integer> entry :
                                meta.getEnchants().entrySet()) {

                            enchants.put(
                                    entry.getKey().getKey().toString(),
                                    entry.getValue()
                            );
                        }

                        shopConfig.set(path + ".enchantments", enchants);
                    }
                }
            }

            saveShopFile();

            Player player = Bukkit.getPlayer(playerUuid);

            if (player != null) {

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aItem erfolgreich hinzugefügt!"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Preis: &c" + price + " Shards"));

                player.getInventory().getItemInMainHand().setAmount(0);
            }

            removePriceInput(playerUuid);

        } catch (Exception e) {

            getLogger().warning("Fehler beim Hinzufügen:");
            e.printStackTrace();
        }
    }

    

    public boolean buyItem(Player player, int slot) {

        try {

            shopConfig = YamlConfiguration.loadConfiguration(shopFile);

            String path = "items." + slot;

            if (!shopConfig.contains(path)) {

                player.sendMessage("§cDieses Item existiert nicht!");

                return false;
            }

            String action = shopConfig.getString(
                    path + ".action",
                    "GIVE_ITEM"
            ).toUpperCase();

            int price = shopConfig.getInt(path + ".price", 0);

            int playerShards = plugin.getShards(player.getUniqueId());

            if (playerShards < price) {

                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &cNicht genug Shards! (" +
                                playerShards +
                                "/" +
                                price +
                                ")")
                );

                return false;
            }



            if (action.equals("COMMAND")) {

                String command = shopConfig.getString(path + ".command");

                if (command == null || command.trim().isEmpty()) {

                    player.sendMessage("§cKein Command gesetzt!");

                    return false;
                }

                if (command.startsWith("/")) {
                    command = command.substring(1);
                }

                command = command
                        .replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());

                final String finalCommand = command;

                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(
                                Bukkit.getConsoleSender(),
                                finalCommand
                        )
                );

                plugin.removeShards(player.getUniqueId(), price);

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &fErfolgreich gekauft!"));

                return true;
            }



            String material = shopConfig.getString(path + ".material");

            if (material == null) {

                player.sendMessage("§cMaterial fehlt!");

                return false;
            }

            Material mat = Material.valueOf(material);

            int amount = shopConfig.getInt(path + ".amount", 1);

            ItemStack item = new ItemStack(mat, amount);

            ItemMeta meta = item.getItemMeta();

            if (meta != null) {

                if (shopConfig.contains(path + ".displayName")) {

                    meta.setDisplayName(
                            shopConfig.getString(path + ".displayName")
                    );
                }

                if (shopConfig.contains(path + ".lore")) {

                    meta.setLore(
                            shopConfig.getStringList(path + ".lore")
                    );
                }

                if (shopConfig.contains(path + ".customModelData")) {

                    meta.setCustomModelData(
                            shopConfig.getInt(path + ".customModelData")
                    );
                }

                if (shopConfig.contains(path + ".enchantments")) {

                    ConfigurationSection enchants =
                            shopConfig.getConfigurationSection(
                                    path + ".enchantments"
                            );

                    if (enchants != null) {

                        for (String key : enchants.getKeys(false)) {

                            Enchantment enchant =
                                    Enchantment.getByKey(
                                            NamespacedKey.minecraft(
                                                    key.replace("minecraft:", "")
                                            )
                                    );

                            if (enchant != null) {

                                meta.addEnchant(
                                        enchant,
                                        enchants.getInt(key),
                                        true
                                );
                            }
                        }
                    }
                }

                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);

            plugin.removeShards(player.getUniqueId(), price);

            player.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', "&x&F&F&7&E&B&8&lSHARDS &8» &fItem gekauft! &7(-" +
                            price +
                            " Shards)")
            );

            return true;

        } catch (Exception e) {

            getLogger().warning("Fehler beim Kaufen:");
            e.printStackTrace();

            player.sendMessage("§cEin Fehler ist aufgetreten!");

            return false;
        }
    }



    public void openShop(Player player) {

        Inventory inv = Bukkit.createInventory(
                null,
                MAX_SLOTS,
                SHOP_TITLE
        );

        shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        int loadedItems = 0;

        for (int i = 0; i < MAX_SLOTS; i++) {

            String path = "items." + i;

            if (!shopConfig.contains(path)) {
                continue;
            }

            try {

                String material = shopConfig.getString(path + ".material");

                if (material == null) {
                    continue;
                }

                Material mat = Material.valueOf(material);

                int amount = shopConfig.getInt(path + ".amount", 1);
                int price = shopConfig.getInt(path + ".price", 0);

                ItemStack item = new ItemStack(mat, amount);

                ItemMeta meta = item.getItemMeta();

                if (meta != null) {

                    if (shopConfig.contains(path + ".displayName")) {

                        meta.setDisplayName(
                                shopConfig.getString(path + ".displayName")
                        );
                    }

                    List<String> lore = new ArrayList<>();

                    if (shopConfig.contains(path + ".lore")) {
                        lore.addAll(
                                shopConfig.getStringList(path + ".lore")
                        );
                    }

                    lore.add(" ");
                    lore.add("§9§lPreis: §c" + price + " Shards");

                    meta.setLore(lore);

                    if (shopConfig.contains(path + ".customModelData")) {

                        meta.setCustomModelData(
                                shopConfig.getInt(path + ".customModelData")
                        );
                    }

                    if (shopConfig.contains(path + ".enchantments")) {

                        ConfigurationSection enchants =
                                shopConfig.getConfigurationSection(
                                        path + ".enchantments"
                                );

                        if (enchants != null) {

                            for (String key : enchants.getKeys(false)) {

                                Enchantment enchant =
                                        Enchantment.getByKey(
                                                NamespacedKey.minecraft(
                                                        key.replace("minecraft:", "")
                                                )
                                        );

                                if (enchant != null) {

                                    meta.addEnchant(
                                            enchant,
                                            enchants.getInt(key),
                                            true
                                    );
                                }
                            }
                        }
                    }

                    item.setItemMeta(meta);
                }

                inv.setItem(i, item);

                loadedItems++;

            } catch (Exception e) {

                getLogger().warning(
                        "Fehler beim Laden von Slot " + i
                );

                e.printStackTrace();
            }
        }

        if (loadedItems == 0) {

            ItemStack barrier = new ItemStack(Material.BARRIER);

            ItemMeta meta = barrier.getItemMeta();

            if (meta != null) {

                meta.setDisplayName("§cShop ist leer");

                barrier.setItemMeta(meta);
            }

            inv.setItem(4, barrier);
        }

        player.openInventory(inv);
    }



    public List<Map<?, ?>> getShopItems() {
        return shopConfig.getMapList("items.list");
    }

    public boolean isShopInventory(String title) {
        return title.equals(SHOP_TITLE);
    }
}
