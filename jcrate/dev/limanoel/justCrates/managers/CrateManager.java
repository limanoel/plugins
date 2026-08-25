package dev.limanoel.justCrates.managers;

import dev.limanoel.justCrates.JustCrates;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CrateManager {
   private final JustCrates plugin;
   private final File dataFile;
   private FileConfiguration dataConfig;
   private final Map<String, Integer> crateUses = new HashMap();
   private final Map<String, String> crateModes = new HashMap();
   private final Map<String, String> displayNames = new HashMap();
   private final Map<Location, String> linkedBlocks = new HashMap();
   private final Map<Location, Integer> glowTasks = new HashMap();
   private final NamespacedKey crateKey;
   private final NamespacedKey linkerKey;
   private final NamespacedKey antiLinkerKey;

   public CrateManager(JustCrates plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "crates.yml");
      this.crateKey = new NamespacedKey(plugin, "crate_key_id");
      this.linkerKey = new NamespacedKey(plugin, "linker_crate_target");
      this.antiLinkerKey = new NamespacedKey(plugin, "anti_linker_tool");
   }

   public void loadCrates() {
      if (!this.dataFile.exists()) {
         this.plugin.saveResource("crates.yml", false);
      }

      this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
      this.crateUses.clear();
      this.crateModes.clear();
      this.displayNames.clear();
      this.linkedBlocks.clear();
      if (this.dataConfig.contains("crates")) {
         for(String key : this.dataConfig.getConfigurationSection("crates").getKeys(false)) {
            String lower = key.toLowerCase();
            int uses = this.dataConfig.getInt("crates." + key + ".uses", 0);
            this.crateUses.put(lower, uses);
            String mode = this.dataConfig.getString("crates." + key + ".mode", "BOTH").toUpperCase();
            this.crateModes.put(lower, mode);
            String display = this.dataConfig.getString("crates." + key + ".display-name", key);
            this.displayNames.put(lower, ChatColor.translateAlternateColorCodes('&', display));
         }
      }

      if (this.dataConfig.contains("locations")) {
         for(String locString : this.dataConfig.getConfigurationSection("locations").getKeys(false)) {
            String crateName = this.dataConfig.getString("locations." + locString);
            Location loc = this.stringToLocation(locString);
            if (loc != null) {
               String key = crateName.toLowerCase();
               this.linkedBlocks.put(loc, key);
               this.plugin.getHologramManager().spawnHologram(loc, key);
               this.startGlowEffect(loc);
            }
         }
      }

   }

   public void reloadCrates() {
      for(int taskId : this.glowTasks.values()) {
         Bukkit.getScheduler().cancelTask(taskId);
      }

      this.glowTasks.clear();
      this.plugin.getHologramManager().removeAllHolograms();
      this.loadCrates();
   }

   public void saveCrates() {
      if (this.dataConfig != null) {
         this.dataConfig.set("locations", (Object)null);

         for(Map.Entry<Location, String> entry : this.linkedBlocks.entrySet()) {
            this.dataConfig.set("locations." + this.locationToString((Location)entry.getKey()), entry.getValue());
         }

         for(Map.Entry<String, Integer> entry : this.crateUses.entrySet()) {
            this.dataConfig.set("crates." + (String)entry.getKey() + ".uses", entry.getValue());
         }

         for(Map.Entry<String, String> entry : this.crateModes.entrySet()) {
            this.dataConfig.set("crates." + (String)entry.getKey() + ".mode", entry.getValue());
         }

         for(Map.Entry<String, String> entry : this.displayNames.entrySet()) {
            String raw = this.dataConfig.getString("crates." + (String)entry.getKey() + ".display-name", (String)entry.getValue());
            this.dataConfig.set("crates." + (String)entry.getKey() + ".display-name", raw);
         }

         try {
            this.dataConfig.save(this.dataFile);
         } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save crates.yml: " + e.getMessage());
         }

      }
   }

   public boolean createCrate(String name) {
      String key = stripColorCodes(name).toLowerCase();
      if (key.isEmpty()) {
         return false;
      } else if (this.dataConfig.contains("crates." + key)) {
         return false;
      } else {
         String displayName = ChatColor.translateAlternateColorCodes('&', name);
         this.dataConfig.set("crates." + key + ".display-name", name);
         this.dataConfig.set("crates." + key + ".mode", "BOTH");
         this.dataConfig.set("crates." + key + ".uses", 0);
         this.dataConfig.createSection("crates." + key + ".rewards");
         this.crateUses.put(key, 0);
         this.crateModes.put(key, "BOTH");
         this.displayNames.put(key, displayName);
         this.saveCrates();
         return true;
      }
   }

   public boolean renameCrate(String name, String newDisplayName) {
      String key = stripColorCodes(name).toLowerCase();
      if (!this.dataConfig.contains("crates." + key)) {
         return false;
      } else {
         String displayName = ChatColor.translateAlternateColorCodes('&', newDisplayName);
         this.dataConfig.set("crates." + key + ".display-name", newDisplayName);
         this.displayNames.put(key, displayName);
         this.saveCrates();
         return true;
      }
   }

   public String getCrateDisplayName(String name) {
      String key = stripColorCodes(name).toLowerCase();
      return (String)this.displayNames.getOrDefault(key, key);
   }

   public static String stripColorCodes(String input) {
      return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
   }

   public boolean setCrateMode(String name, String mode) {
      String lower = stripColorCodes(name).toLowerCase();
      if (!this.dataConfig.contains("crates." + lower)) {
         return false;
      } else {
         String upper = mode.toUpperCase();
         if (!upper.equals("ITEM") && !upper.equals("COMMAND") && !upper.equals("BOTH")) {
            return false;
         } else {
            this.dataConfig.set("crates." + lower + ".mode", upper);
            this.crateModes.put(lower, upper);
            this.saveCrates();
            return true;
         }
      }
   }

   public String getCrateMode(String name) {
      return (String)this.crateModes.getOrDefault(stripColorCodes(name).toLowerCase(), "BOTH");
   }

   public boolean deleteCrate(String name) {
      String lower = stripColorCodes(name).toLowerCase();
      if (!this.dataConfig.contains("crates." + lower)) {
         return false;
      } else {
         this.dataConfig.set("crates." + lower, (Object)null);
         this.crateUses.remove(lower);
         this.crateModes.remove(lower);
         this.displayNames.remove(lower);
         this.linkedBlocks.entrySet().removeIf((entry) -> {
            if (((String)entry.getValue()).equalsIgnoreCase(lower)) {
               this.plugin.getHologramManager().removeHologram((Location)entry.getKey());
               if (this.glowTasks.containsKey(entry.getKey())) {
                  Bukkit.getScheduler().cancelTask((Integer)this.glowTasks.get(entry.getKey()));
                  this.glowTasks.remove(entry.getKey());
               }

               return true;
            } else {
               return false;
            }
         });
         this.saveCrates();
         return true;
      }
   }

   public void linkBlock(Location loc, String crateName) {
      String key = stripColorCodes(crateName).toLowerCase();
      this.linkedBlocks.put(loc, key);
      this.plugin.getHologramManager().spawnHologram(loc, key);
      this.saveCrates();
      this.startGlowEffect(loc);
   }

   public void unlinkBlock(Location loc) {
      if (this.linkedBlocks.containsKey(loc)) {
         this.plugin.getHologramManager().removeHologram(loc);
         if (this.glowTasks.containsKey(loc)) {
            Bukkit.getScheduler().cancelTask((Integer)this.glowTasks.get(loc));
            this.glowTasks.remove(loc);
         }

         this.linkedBlocks.remove(loc);
         this.saveCrates();
      }
   }

   private void startGlowEffect(Location loc) {
      if (this.glowTasks.containsKey(loc)) {
         Bukkit.getScheduler().cancelTask((Integer)this.glowTasks.get(loc));
      }

      int taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.linkedBlocks.containsKey(loc)) {
            if (loc.getWorld() != null) {
               loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add((double)0.5F, 1.2, (double)0.5F), 3, 0.3, 0.3, 0.3, 0.01);
               loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add((double)0.5F, (double)0.5F, (double)0.5F), 5, 0.4, 0.4, 0.4, 0.01);
               loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add((double)0.5F, (double)0.5F, (double)0.5F), 2, 0.2, 0.2, 0.2, 0.01);
            }
         }
      }, 10L, 10L).getTaskId();
      this.glowTasks.put(loc, taskId);
   }

   public ItemStack createKeyItem(String crateName, int amount) {
      ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, amount));
      ItemMeta meta = key.getItemMeta();
      if (meta != null) {
         String display = this.getCrateDisplayName(crateName);
         String var10001 = String.valueOf(ChatColor.YELLOW);
         meta.setDisplayName(var10001 + "Schlüssel: " + display);
         meta.setLore(List.of(String.valueOf(ChatColor.GRAY) + "Öffnet 1x " + display, String.valueOf(ChatColor.DARK_GRAY) + "JustCrates"));
         meta.getPersistentDataContainer().set(this.crateKey, PersistentDataType.STRING, stripColorCodes(crateName).toLowerCase());
         key.setItemMeta(meta);
      }

      return key;
   }

   public ItemStack createLinkerItem(String crateName) {
      ItemStack tool = new ItemStack(Material.valueOf(this.plugin.getConfig().getString("link-tool.material", "BLAZE_ROD")));
      ItemMeta meta = tool.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("link-tool.name", "&b&lCrate Linker")));
         List<String> lore = this.plugin.getConfig().getStringList("link-tool.lore");
         if (!lore.isEmpty()) {
            meta.setLore(lore.stream().map((l) -> ChatColor.translateAlternateColorCodes('&', l)).toList());
         }

         meta.getPersistentDataContainer().set(this.linkerKey, PersistentDataType.STRING, stripColorCodes(crateName).toLowerCase());
         tool.setItemMeta(meta);
      }

      return tool;
   }

   public ItemStack createAntiLinkerItem() {
      ItemStack tool = new ItemStack(Material.valueOf(this.plugin.getConfig().getString("link-tool.material", "BLAZE_ROD")));
      ItemMeta meta = tool.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lAnti-Link-Tool"));
         meta.setLore(List.of(String.valueOf(ChatColor.GRAY) + "Rechtsklick auf einen Block,", String.valueOf(ChatColor.GRAY) + "um ihn von einer Kiste zu entlinken."));
         meta.getPersistentDataContainer().set(this.antiLinkerKey, PersistentDataType.STRING, "anti_linker");
         tool.setItemMeta(meta);
      }

      return tool;
   }

   public void openCrate(Player player, String crateName, Location blockLoc) {
      String lower = stripColorCodes(crateName).toLowerCase();
      ConfigurationSection rewardsSec = this.dataConfig.getConfigurationSection("crates." + lower + ".rewards");
      if (rewardsSec != null && !rewardsSec.getKeys(false).isEmpty()) {
         String mode = this.getCrateMode(lower);
         String selectedKey = this.selectWeightedReward(rewardsSec);
         ConfigurationSection rewardSec = rewardsSec.getConfigurationSection(selectedKey);
         String rewardName = "Belohnung";
         if (rewardSec != null) {
            if ((mode.equals("ITEM") || mode.equals("BOTH")) && rewardSec.contains("item")) {
               ItemStack item = rewardSec.getItemStack("item");
               if (item != null) {
                  player.getInventory().addItem(new ItemStack[]{item.clone()});
                  rewardName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
               }
            }

            if ((mode.equals("COMMAND") || mode.equals("BOTH")) && rewardSec.contains("commands")) {
               for(String cmd : rewardSec.getStringList("commands")) {
                  Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
               }

               if (mode.equals("COMMAND")) {
                  rewardName = "Befehl ausgeführt";
               }
            }
         }

         player.sendMessage(this.msg("messages.crate-opened").replace("%reward%", rewardName));
         if (blockLoc != null && blockLoc.getWorld() != null) {
            blockLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, blockLoc.clone().add((double)0.5F, (double)1.0F, (double)0.5F), 35);
            blockLoc.getWorld().playSound(blockLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
         }

         int currentUses = (Integer)this.crateUses.getOrDefault(lower, 0) + 1;
         this.crateUses.put(lower, currentUses);
         this.linkedBlocks.forEach((loc, cName) -> {
            if (cName.equalsIgnoreCase(lower)) {
               this.plugin.getHologramManager().updateHologram(loc, lower);
            }

         });
         this.saveCrates();
      } else {
         player.sendMessage(this.msg("messages.crate-opened").replace("%reward%", String.valueOf(ChatColor.RED) + "Keine Belohnungen konfiguriert!"));
      }
   }

   public void grantSelectedReward(Player player, String crateName, ItemStack selectedItem, String rewardKey) {
      String lower = stripColorCodes(crateName).toLowerCase();
      String mode = this.getCrateMode(lower);
      if (mode.equals("ITEM") || mode.equals("BOTH")) {
         player.getInventory().addItem(new ItemStack[]{selectedItem.clone()});
      }

      if ((mode.equals("COMMAND") || mode.equals("BOTH")) && rewardKey != null) {
         ConfigurationSection rewardSec = this.dataConfig.getConfigurationSection("crates." + lower + ".rewards." + rewardKey);
         if (rewardSec != null && rewardSec.contains("commands")) {
            for(String cmd : rewardSec.getStringList("commands")) {
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
         }
      }

      int currentUses = (Integer)this.crateUses.getOrDefault(lower, 0) + 1;
      this.crateUses.put(lower, currentUses);
      this.linkedBlocks.forEach((loc, cName) -> {
         if (cName.equalsIgnoreCase(lower)) {
            this.plugin.getHologramManager().updateHologram(loc, lower);
         }

      });
      this.saveCrates();
   }

   private String selectWeightedReward(ConfigurationSection rewardsSec) {
      List<String> rewardKeys = new ArrayList(rewardsSec.getKeys(false));
      List<Double> weights = new ArrayList();
      double totalWeight = (double)0.0F;

      for(String key : rewardKeys) {
         double chance = rewardsSec.getDouble(key + ".chance", (double)100.0F);
         weights.add(chance);
         totalWeight += chance;
      }

      if (totalWeight <= (double)0.0F) {
         return (String)rewardKeys.get((new Random()).nextInt(rewardKeys.size()));
      } else {
         double random = (new Random()).nextDouble() * totalWeight;
         double cumulative = (double)0.0F;
         String selected = (String)rewardKeys.get(rewardKeys.size() - 1);

         for(int i = 0; i < rewardKeys.size(); ++i) {
            cumulative += (Double)weights.get(i);
            if (random < cumulative) {
               selected = (String)rewardKeys.get(i);
               break;
            }
         }

         return selected;
      }
   }

   public void saveGUIItemsToConfig(String crateName, ItemStack[] contents) {
      String lower = stripColorCodes(crateName).toLowerCase();
      this.dataConfig.set("crates." + lower + ".rewards", (Object)null);
      int index = 1;

      for(int i = 0; i < contents.length; ++i) {
         if (!isBorderSlot(i) && i != 49) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.GRAY_STAINED_GLASS_PANE && item.getType() != Material.LIME_STAINED_GLASS_PANE && item.getType() != Material.RED_STAINED_GLASS_PANE) {
               String path = "crates." + lower + ".rewards." + index;
               this.dataConfig.set(path + ".chance", (double)100.0F);
               this.dataConfig.set(path + ".item", item);
               ++index;
            }
         }
      }

      this.saveCrates();
   }

   public static boolean isBorderSlot(int slot) {
      return slot < 9 || slot >= 45;
   }

   public List<ItemStack> getCrateRewardItems(String crateName) {
      List<ItemStack> items = new ArrayList();
      ConfigurationSection rewardsSec = this.dataConfig.getConfigurationSection("crates." + stripColorCodes(crateName).toLowerCase() + ".rewards");
      if (rewardsSec != null) {
         for(String key : rewardsSec.getKeys(false)) {
            ItemStack item = rewardsSec.getItemStack(key + ".item");
            if (item != null) {
               items.add(item);
            }
         }
      }

      return items;
   }

   public Set<String> getCrateNames() {
      Set<String> names = new TreeSet();
      if (this.dataConfig.contains("crates")) {
         names.addAll(this.dataConfig.getConfigurationSection("crates").getKeys(false));
      }

      return names;
   }

   public boolean crateExists(String name) {
      return this.dataConfig.contains("crates." + stripColorCodes(name).toLowerCase());
   }

   private String locationToString(Location loc) {
      String var10000 = loc.getWorld().getName();
      return var10000 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
   }

   private Location stringToLocation(String str) {
      String[] parts = str.split(",");
      if (parts.length < 4) {
         return null;
      } else {
         World world = Bukkit.getWorld(parts[0]);
         return world == null ? null : new Location(world, (double)Integer.parseInt(parts[1]), (double)Integer.parseInt(parts[2]), (double)Integer.parseInt(parts[3]));
      }
   }

   public String msg(String path) {
      String prefix = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("messages.prefix", ""));
      String msg = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString(path, path));
      return prefix + msg;
   }

   public NamespacedKey getCrateKey() {
      return this.crateKey;
   }

   public NamespacedKey getLinkerKey() {
      return this.linkerKey;
   }

   public NamespacedKey getAntiLinkerKey() {
      return this.antiLinkerKey;
   }

   public Map<Location, String> getLinkedBlocks() {
      return this.linkedBlocks;
   }

   public FileConfiguration getDataConfig() {
      return this.dataConfig;
   }

   public int getUses(String crateName) {
      return (Integer)this.crateUses.getOrDefault(stripColorCodes(crateName).toLowerCase(), 0);
   }

   public Set<String> getCrateItems() {
      return this.crateUses.keySet();
   }

   public ItemStack createAcceptItem() {
      String path = "accept-item";
      Material material = Material.valueOf(this.plugin.getConfig().getString(path + ".material", "LIME_WOOL"));
      String name = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString(path + ".name", "&a&lANNEHMEN"));
      int customModelData = this.plugin.getConfig().getInt(path + ".custom-model-data", 0);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public ItemStack createDeclineItem() {
      String path = "decline-item";
      Material material = Material.valueOf(this.plugin.getConfig().getString(path + ".material", "RED_WOOL"));
      String name = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString(path + ".name", "&c&lABLEHNEN"));
      int customModelData = this.plugin.getConfig().getInt(path + ".custom-model-data", 0);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public void setAcceptItemCustomModelData(int customModelData) {
      this.plugin.getConfig().set("accept-item.custom-model-data", customModelData);
      this.plugin.saveConfig();
   }

   public void setDeclineItemCustomModelData(int customModelData) {
      this.plugin.getConfig().set("decline-item.custom-model-data", customModelData);
      this.plugin.saveConfig();
   }
}
