package dev.limanoel.justCrates.listeners;

import dev.limanoel.justCrates.JustCrates;
import dev.limanoel.justCrates.managers.CrateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CrateListener implements Listener {
   private final JustCrates plugin;
   private final Map<UUID, GuiType> playerGuiState = new HashMap();
   private final Map<UUID, String> editingCrate = new HashMap();
   private final Map<UUID, String> confirmingDeleteCrate = new HashMap();
   private final Map<UUID, String> rewardsCrate = new HashMap();
   private final Map<UUID, ItemStack> selectedReward = new HashMap();
   private final Map<UUID, String> selectedRewardCrate = new HashMap();
   private final Map<UUID, String> selectedRewardKey = new HashMap();
   private final Set<UUID> switchingGui = Collections.newSetFromMap(new HashMap());
   private final Set<UUID> closingAfterSave = Collections.newSetFromMap(new HashMap());

   public CrateListener(JustCrates plugin) {
      this.plugin = plugin;
   }

   public void openEditGUI(Player player, String crateName) {
      UUID uuid = player.getUniqueId();
      this.switchingGui.add(uuid);
      this.editingCrate.put(uuid, CrateManager.stripColorCodes(crateName).toLowerCase());
      this.playerGuiState.put(uuid, CrateListener.GuiType.EDIT);
      String display = this.plugin.getCrateManager().getCrateDisplayName(crateName);
      String var10002 = String.valueOf(ChatColor.DARK_BLUE);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, var10002 + "Crate Edit: " + display);
      ItemStack border = this.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
         inv.setItem(45 + i, border);
      }

      inv.setItem(49, this.createGuiItem(Material.LIME_STAINED_GLASS_PANE, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "Speichern", String.valueOf(ChatColor.GRAY) + "Klicke hier um die Belohnungen zu speichern!"));
      inv.setItem(45, this.createGuiItem(Material.RED_STAINED_GLASS_PANE, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "Leeren", String.valueOf(ChatColor.GRAY) + "Entfernt alle Belohnungen aus der Kiste."));
      List<ItemStack> items = this.plugin.getCrateManager().getCrateRewardItems(crateName);
      int slot = 9;

      for(ItemStack item : items) {
         if (item != null) {
            while((slot >= 45 || slot == 49) && slot < 54) {
               ++slot;
            }

            if (slot >= 45) {
               break;
            }

            inv.setItem(slot, item);
            ++slot;
         }
      }

      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 1.2F);
      this.switchingGui.remove(uuid);
   }

   public void openConfirmDeleteGUI(Player player, String crateName) {
      UUID uuid = player.getUniqueId();
      this.switchingGui.add(uuid);
      this.confirmingDeleteCrate.put(uuid, CrateManager.stripColorCodes(crateName).toLowerCase());
      this.playerGuiState.put(uuid, CrateListener.GuiType.DELETE_CONFIRM);
      String display = this.plugin.getCrateManager().getCrateDisplayName(crateName);
      String var10002 = String.valueOf(ChatColor.DARK_RED);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, var10002 + "Löschen: " + display + "?");
      this.fillBorders(inv);
      inv.setItem(11, this.createGuiItem(Material.GREEN_WOOL, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "Bestätigen", String.valueOf(ChatColor.RED) + "Löscht die Kiste unwiderruflich!"));
      inv.setItem(15, this.createGuiItem(Material.RED_WOOL, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "Abbrechen", String.valueOf(ChatColor.GRAY) + "Bricht den Vorgang ab."));
      player.openInventory(inv);
      this.switchingGui.remove(uuid);
   }

   public void openRewardsGUI(Player player, String crateName) {
      UUID uuid = player.getUniqueId();
      this.switchingGui.add(uuid);
      this.rewardsCrate.put(uuid, CrateManager.stripColorCodes(crateName).toLowerCase());
      this.playerGuiState.put(uuid, CrateListener.GuiType.REWARDS);
      String key = CrateManager.stripColorCodes(crateName).toLowerCase();
      int keyCount = this.countKeys(player, key);
      String display = this.plugin.getCrateManager().getCrateDisplayName(crateName);
      String var10002 = String.valueOf(ChatColor.GOLD);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, var10002 + "Crate: " + display + " " + String.valueOf(ChatColor.YELLOW) + "[" + keyCount + " Keys]");
      ItemStack border = this.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
         inv.setItem(45 + i, border);
      }

      List<ItemStack> items = this.plugin.getCrateManager().getCrateRewardItems(crateName);
      int slot = 9;

      for(ItemStack item : items) {
         if (item != null) {
            if (slot >= 45) {
               break;
            }

            inv.setItem(slot, item);
            ++slot;
         }
      }

      Material var10003 = Material.PAPER;
      String var10004 = String.valueOf(ChatColor.AQUA) + String.valueOf(ChatColor.BOLD) + "Deiner Keys";
      String[] var10005 = new String[2];
      String var10008 = String.valueOf(ChatColor.YELLOW);
      var10005[0] = var10008 + "Anzahl: " + String.valueOf(ChatColor.WHITE) + keyCount;
      var10005[1] = String.valueOf(ChatColor.GRAY) + "Klicke auf ein Item zum Auswählen.";
      inv.setItem(49, this.createGuiItem(var10003, var10004, var10005));
      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 1.0F);
      this.switchingGui.remove(uuid);
   }

   public void openRewardConfirmGUI(Player player, ItemStack selectedItem, String crateName, String rewardKey) {
      UUID uuid = player.getUniqueId();
      this.switchingGui.add(uuid);
      this.selectedReward.put(uuid, selectedItem.clone());
      this.selectedRewardCrate.put(uuid, CrateManager.stripColorCodes(crateName).toLowerCase());
      this.selectedRewardKey.put(uuid, rewardKey);
      this.playerGuiState.put(uuid, CrateListener.GuiType.REWARD_CONFIRM);
      String display = this.plugin.getCrateManager().getCrateDisplayName(crateName);
      String var10002 = String.valueOf(ChatColor.GOLD);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 45, var10002 + "Bestaetige: " + display);
      this.fillBorders(inv);
      inv.setItem(22, selectedItem.clone());
      inv.setItem(20, this.createGuiItem(Material.GREEN_WOOL, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "Bestätigen", String.valueOf(ChatColor.GRAY) + "Verbraucht 1 Schlüssel und gibt dir das Item."));
      inv.setItem(24, this.createGuiItem(Material.RED_WOOL, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "Abbrechen", String.valueOf(ChatColor.GRAY) + "Zurück zur Reward-Auswahl."));
      player.openInventory(inv);
      this.switchingGui.remove(uuid);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         GuiType state = (GuiType)this.playerGuiState.get(player.getUniqueId());
         if (state != null) {
            if (state != CrateListener.GuiType.EDIT) {
               event.setCancelled(true);
            }

            switch (state.ordinal()) {
               case 0 -> this.handleEditClick(player, event);
               case 1 -> this.handleDeleteConfirmClick(player, event);
               case 2 -> this.handleRewardsClick(player, event);
               case 3 -> this.handleRewardConfirmClick(player, event);
            }

         }
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player player) {
         GuiType state = (GuiType)this.playerGuiState.get(player.getUniqueId());
         if (state != null) {
            if (state != CrateListener.GuiType.EDIT) {
               event.setCancelled(true);
            } else {
               for(int slot : event.getRawSlots()) {
                  if (slot < 54 && (this.isBorderRowSlot(slot) || slot == 49 || slot == 45)) {
                     event.setCancelled(true);
                     return;
                  }
               }

            }
         }
      }
   }

   private void handleEditClick(Player player, InventoryClickEvent event) {
      UUID uuid = player.getUniqueId();
      int slot = event.getRawSlot();
      if (slot < 54 && (this.isBorderRowSlot(slot) || slot == 49 || slot == 45)) {
         event.setCancelled(true);
         if (slot == 49) {
            String crateName = (String)this.editingCrate.get(uuid);
            if (crateName == null) {
               return;
            }

            ItemStack[] contents = event.getInventory().getContents();
            this.plugin.getCrateManager().saveGUIItemsToConfig(crateName, contents);
            String var10001 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var10001 + "Belohnungen für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + "' gespeichert!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            this.closingAfterSave.add(uuid);
            player.closeInventory();
         } else if (slot == 45) {
            Inventory inv = event.getInventory();

            for(int i = 9; i < 45; ++i) {
               if (i != 45) {
                  ItemStack item = inv.getItem(i);
                  if (item != null && item.getType() != Material.AIR && item.getType() != Material.GRAY_STAINED_GLASS_PANE && item.getType() != Material.LIME_STAINED_GLASS_PANE && item.getType() != Material.RED_STAINED_GLASS_PANE) {
                     Map<Integer, ItemStack> leftovers = player.getInventory().addItem(new ItemStack[]{item});
                     if (leftovers.isEmpty()) {
                        inv.setItem(i, (ItemStack)null);
                     } else {
                        for(ItemStack leftover : leftovers.values()) {
                           player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }

                        inv.setItem(i, (ItemStack)null);
                     }
                  }
               }
            }

            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Alle Belohnungen entfernt! Nicht vergessen zu speichern.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
         }

      }
   }

   private void handleDeleteConfirmClick(Player player, InventoryClickEvent event) {
      ItemStack clicked = event.getCurrentItem();
      if (clicked != null && clicked.hasItemMeta()) {
         String displayName = clicked.getItemMeta().getDisplayName();
         String crateName = (String)this.confirmingDeleteCrate.get(player.getUniqueId());
         String var10001 = String.valueOf(ChatColor.GREEN);
         if (displayName.equals(var10001 + String.valueOf(ChatColor.BOLD) + "Bestätigen")) {
            this.plugin.getCrateManager().deleteCrate(crateName);
            var10001 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var10001 + "Kiste '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + "' wurde gelöscht!");
            player.closeInventory();
         } else {
            var10001 = String.valueOf(ChatColor.RED);
            if (displayName.equals(var10001 + String.valueOf(ChatColor.BOLD) + "Abbrechen")) {
               player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Löschen abgebrochen.");
               player.closeInventory();
            }
         }

      }
   }

   private void handleRewardsClick(Player player, InventoryClickEvent event) {
      UUID uuid = player.getUniqueId();
      ItemStack clicked = event.getCurrentItem();
      if (clicked != null && clicked.getType() != Material.AIR) {
         int slot = event.getRawSlot();
         if (!this.isBorderRowSlot(slot) && slot != 49) {
            String crateName = (String)this.rewardsCrate.get(uuid);
            if (crateName != null) {
               if (!this.hasKey(player, crateName)) {
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keinen Key für diese Kiste!");
               } else {
                  String rewardKey = this.findRewardKey(crateName, slot);
                  this.openRewardConfirmGUI(player, clicked, crateName, rewardKey);
               }
            }
         }
      }
   }

   private void handleRewardConfirmClick(Player player, InventoryClickEvent event) {
      UUID uuid = player.getUniqueId();
      ItemStack clicked = event.getCurrentItem();
      if (clicked != null && clicked.hasItemMeta()) {
         String displayName = clicked.getItemMeta().getDisplayName();
         String crateName = (String)this.selectedRewardCrate.get(uuid);
         ItemStack selectedItem = (ItemStack)this.selectedReward.get(uuid);
         String rewardKey = (String)this.selectedRewardKey.get(uuid);
         String var10001 = String.valueOf(ChatColor.GREEN);
         if (displayName.equals(var10001 + String.valueOf(ChatColor.BOLD) + "Bestätigen")) {
            boolean keyConsumed = this.consumeKey(player, crateName);
            if (keyConsumed) {
               this.plugin.getCrateManager().grantSelectedReward(player, crateName, selectedItem, rewardKey);
               String itemName = selectedItem.hasItemMeta() && selectedItem.getItemMeta().hasDisplayName() ? selectedItem.getItemMeta().getDisplayName() : selectedItem.getType().name();
               var10001 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var10001 + "Du hast " + itemName + " erhalten!");
               player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
               player.closeInventory();
            } else {
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keinen Key für diese Kiste!");
               player.closeInventory();
            }
         } else {
            var10001 = String.valueOf(ChatColor.RED);
            if (displayName.equals(var10001 + String.valueOf(ChatColor.BOLD) + "Abbrechen")) {
               this.selectedReward.remove(uuid);
               this.selectedRewardCrate.remove(uuid);
               this.selectedRewardKey.remove(uuid);
               if (crateName != null) {
                  this.openRewardsGUI(player, crateName);
               } else {
                  player.closeInventory();
               }
            }
         }

      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      HumanEntity var3 = event.getPlayer();
      if (var3 instanceof Player player) {
         UUID var7 = player.getUniqueId();
         if (!this.switchingGui.contains(var7)) {
            GuiType state = (GuiType)this.playerGuiState.get(var7);
            if (state != null) {
               if (state == CrateListener.GuiType.EDIT && !this.closingAfterSave.contains(var7)) {
                  String crateName = (String)this.editingCrate.get(var7);
                  if (crateName != null) {
                     ItemStack[] contents = event.getInventory().getContents();
                     this.plugin.getCrateManager().saveGUIItemsToConfig(crateName, contents);
                     String var10001 = String.valueOf(ChatColor.GREEN);
                     player.sendMessage(var10001 + "Belohnungen für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + "' automatisch gespeichert!");
                  }
               }

               this.closingAfterSave.remove(var7);
               this.playerGuiState.remove(var7);
               this.editingCrate.remove(var7);
               this.confirmingDeleteCrate.remove(var7);
               this.rewardsCrate.remove(var7);
               this.selectedReward.remove(var7);
               this.selectedRewardCrate.remove(var7);
               this.selectedRewardKey.remove(var7);
            }
         }
      }
   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta.getPersistentDataContainer().has(this.plugin.getCrateManager().getLinkerKey(), PersistentDataType.STRING)) {
            String crateName = (String)meta.getPersistentDataContainer().get(this.plugin.getCrateManager().getLinkerKey(), PersistentDataType.STRING);
            this.plugin.getCrateManager().linkBlock(event.getClickedBlock().getLocation(), crateName);
            String var10001 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var10001 + "Block erfolgreich mit '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + "' verlinkt!");
            event.setCancelled(true);
            return;
         }

         if (meta.getPersistentDataContainer().has(this.plugin.getCrateManager().getAntiLinkerKey(), PersistentDataType.STRING)) {
            this.plugin.getCrateManager().unlinkBlock(event.getClickedBlock().getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Block erfolgreich entlinkt!");
            event.setCancelled(true);
            return;
         }
      }

      if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) && event.getClickedBlock() != null) {
         String crateName = (String)this.plugin.getCrateManager().getLinkedBlocks().get(event.getClickedBlock().getLocation());
         if (crateName != null) {
            event.setCancelled(true);
            this.openRewardsGUI(player, crateName);
         }
      }

   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Location loc = event.getBlock().getLocation();
      String crateName = (String)this.plugin.getCrateManager().getLinkedBlocks().get(loc);
      if (crateName != null) {
         event.setCancelled(true);
         if (event.getPlayer().hasPermission("justcrates.admin") && event.getPlayer().isSneaking()) {
            this.plugin.getCrateManager().unlinkBlock(loc);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.YELLOW) + "Kiste entlinkt!");
            return;
         }

         this.openRewardsGUI(event.getPlayer(), crateName);
      }

   }

   private String findRewardKey(String crateName, int slot) {
      ConfigurationSection rewardsSec = this.plugin.getCrateManager().getDataConfig().getConfigurationSection("crates." + crateName.toLowerCase() + ".rewards");
      if (rewardsSec == null) {
         return null;
      } else {
         List<String> keys = new ArrayList(rewardsSec.getKeys(false));
         int index = slot - 9;
         return index >= 0 && index < keys.size() ? (String)keys.get(index) : null;
      }
   }

   public int countKeys(Player player, String crateName) {
      int count = 0;

      for(ItemStack item : player.getInventory().getContents()) {
         if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(this.plugin.getCrateManager().getCrateKey(), PersistentDataType.STRING)) {
               String keyCrate = (String)meta.getPersistentDataContainer().get(this.plugin.getCrateManager().getCrateKey(), PersistentDataType.STRING);
               if (keyCrate.equalsIgnoreCase(crateName)) {
                  count += item.getAmount();
               }
            }
         }
      }

      return count;
   }

   private boolean hasKey(Player player, String crateName) {
      return this.countKeys(player, crateName) > 0;
   }

   private boolean consumeKey(Player player, String crateName) {
      ItemStack[] contents = player.getInventory().getContents();

      for(int i = 0; i < contents.length; ++i) {
         ItemStack item = contents[i];
         if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(this.plugin.getCrateManager().getCrateKey(), PersistentDataType.STRING)) {
               String keyCrate = (String)meta.getPersistentDataContainer().get(this.plugin.getCrateManager().getCrateKey(), PersistentDataType.STRING);
               if (keyCrate.equalsIgnoreCase(crateName)) {
                  item.setAmount(item.getAmount() - 1);
                  return true;
               }
            }
         }
      }

      return false;
   }

   private ItemStack createGuiItem(Material material, String name, String... lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         meta.setLore(List.of(lore));
         item.setItemMeta(meta);
      }

      return item;
   }

   private void fillBorders(Inventory inv) {
      this.fillBorders(inv, inv.getSize());
   }

   private void fillBorders(Inventory inv, int size) {
      ItemStack border = this.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
      int lastRow = size - 9;

      for(int i = 0; i < size; ++i) {
         if (i < 9 || i >= lastRow || i % 9 == 0 || (i + 1) % 9 == 0) {
            inv.setItem(i, border);
         }
      }

   }

   private boolean isBorderRowSlot(int slot) {
      return slot < 9 || slot >= 45;
   }

   public static enum GuiType {
      EDIT,
      DELETE_CONFIRM,
      REWARDS,
      REWARD_CONFIRM;

      // $FF: synthetic method
      private static GuiType[] $values() {
         return new GuiType[]{EDIT, DELETE_CONFIRM, REWARDS, REWARD_CONFIRM};
      }
   }
}
