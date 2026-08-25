package dev.limanoel.justCrates.listeners;

import dev.limanoel.justCrates.JustCrates;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramManager {
   private final JustCrates plugin;
   private final Map<Location, TextDisplay> holograms = new HashMap();
   private int actionBarTaskId = -1;

   public HologramManager(JustCrates plugin) {
      this.plugin = plugin;
      this.startActionBarTask();
   }

   private void startActionBarTask() {
      this.actionBarTaskId = (new BukkitRunnable() {
         public void run() {
            if (HologramManager.this.plugin.getCrateManager() != null && HologramManager.this.plugin.getCrateListener() != null) {
               Map<Location, String> linkedBlocks = HologramManager.this.plugin.getCrateManager().getLinkedBlocks();
               if (!linkedBlocks.isEmpty()) {
                  for(Player player : HologramManager.this.plugin.getServer().getOnlinePlayers()) {
                     Location playerLoc = player.getLocation();

                     for(Map.Entry<Location, String> entry : linkedBlocks.entrySet()) {
                        Location crateLoc = (Location)entry.getKey();
                        if (crateLoc.getWorld() != null && crateLoc.getWorld().equals(playerLoc.getWorld())) {
                           double distance = crateLoc.distance(playerLoc);
                           if (distance <= (double)5.0F) {
                              String crateName = (String)entry.getValue();
                              int keyCount = HologramManager.this.plugin.getCrateListener().countKeys(player, crateName);
                              int uses = HologramManager.this.plugin.getCrateManager().getUses(crateName);
                              String display = HologramManager.this.plugin.getCrateManager().getCrateDisplayName(crateName);
                              String message = ChatColor.translateAlternateColorCodes('&', HologramManager.this.plugin.getConfig().getString("action-bar.near-crate", "&b&l%crate% &7| &e%keys%x Schluessel &7| &8Oeffnungen: &e%uses%")).replace("%crate%", display).replace("%keys%", String.valueOf(keyCount)).replace("%uses%", String.valueOf(uses));
                              player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                              break;
                           }
                        }
                     }
                  }

               }
            }
         }
      }).runTaskTimer(this.plugin, 20L, 20L).getTaskId();
   }

   public void spawnHologram(Location blockLoc, String crateName) {
      if (this.plugin.getConfig().getBoolean("holograms.enabled", true)) {
         this.removeHologram(blockLoc);
         double offset = this.plugin.getConfig().getDouble("holograms.height-offset", (double)1.5F);
         Location holoLoc = blockLoc.clone().add((double)0.5F, offset, (double)0.5F);
         TextDisplay display = (TextDisplay)blockLoc.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
         display.setBillboard(Billboard.CENTER);
         display.setSeeThrough(false);
         display.setText(this.formatText(crateName));
         this.holograms.put(blockLoc, display);
      }
   }

   public void updateHologram(Location blockLoc, String crateName) {
      if (this.holograms.containsKey(blockLoc)) {
         ((TextDisplay)this.holograms.get(blockLoc)).setText(this.formatText(crateName));
      } else {
         this.spawnHologram(blockLoc, crateName);
      }

   }

   public void removeHologram(Location blockLoc) {
      if (this.holograms.containsKey(blockLoc)) {
         ((TextDisplay)this.holograms.get(blockLoc)).remove();
         this.holograms.remove(blockLoc);
      }

   }

   public void removeAllHolograms() {
      this.holograms.values().forEach(Entity::remove);
      this.holograms.clear();
   }

   public void reloadAllHolograms() {
      this.removeAllHolograms();

      for(Map.Entry<Location, String> entry : this.plugin.getCrateManager().getLinkedBlocks().entrySet()) {
         this.spawnHologram((Location)entry.getKey(), (String)entry.getValue());
      }

   }

   public void shutdown() {
      if (this.actionBarTaskId != -1) {
         this.plugin.getServer().getScheduler().cancelTask(this.actionBarTaskId);
         this.actionBarTaskId = -1;
      }

   }

   private String formatText(String crateName) {
      List<String> lines = this.plugin.getConfig().getStringList("holograms.lines");
      int uses = this.plugin.getCrateManager().getUses(crateName);
      String display = this.plugin.getCrateManager().getCrateDisplayName(crateName);
      if (lines.isEmpty()) {
         lines = List.of("&b&l%crate%", "&7Rechtsklick zum Oeffnen", "&8Oeffnungen: &e%uses%");
      }

      StringBuilder builder = new StringBuilder();

      for(int i = 0; i < lines.size(); ++i) {
         String line = ((String)lines.get(i)).replace("%crate%", display).replace("%uses%", String.valueOf(uses));
         builder.append(ChatColor.translateAlternateColorCodes('&', line));
         if (i < lines.size() - 1) {
            builder.append("\n");
         }
      }

      return builder.toString();
   }
}
