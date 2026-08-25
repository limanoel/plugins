package dev.limanoel.justCrates.commands;

import dev.limanoel.justCrates.JustCrates;
import dev.limanoel.justCrates.managers.CrateManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class JCratesCommand implements CommandExecutor, TabCompleter {
   private final JustCrates plugin;

   public JCratesCommand(JustCrates plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
         if (!sender.hasPermission("justcrates.admin")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Dazu hast du keine Rechte!");
            return true;
         } else {
            this.plugin.reloadAll();
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "JustCrates Config und Kisten wurden neu geladen!");
            return true;
         }
      } else if (sender instanceof Player) {
         Player player = (Player)sender;
         if (!player.hasPermission("justcrates.admin")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dazu hast du keine Rechte!");
            return true;
         } else if (args.length == 0) {
            this.sendHelp(player);
            return true;
         } else {
            switch (args[0].toLowerCase()) {
               case "create":
                  if (args.length < 2) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates create <Name> (mit &-Colorcodes)");
                     return true;
                  }

                  String name = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length));
                  if (this.plugin.getCrateManager().createCrate(name)) {
                     String var24 = String.valueOf(ChatColor.GREEN);
                     player.sendMessage(var24 + "Kiste '" + this.plugin.getCrateManager().getCrateDisplayName(name) + String.valueOf(ChatColor.GREEN) + "' erstellt! (Mode: BOTH)");
                     this.plugin.getCrateListener().openEditGUI(player, name);
                  } else {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert bereits!");
                  }
                  break;
               case "edit":
                  if (args.length < 2) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates edit <Name>");
                     return true;
                  }

                  String name = args[1];
                  if (!this.plugin.getCrateManager().crateExists(name)) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                     return true;
                  }

                  this.plugin.getCrateListener().openEditGUI(player, name);
                  break;
               case "delete":
                  if (args.length < 2) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates delete <Name>");
                     return true;
                  }

                  String name = args[1];
                  if (!this.plugin.getCrateManager().crateExists(name)) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                     return true;
                  }

                  this.plugin.getCrateListener().openConfirmDeleteGUI(player, name);
                  break;
               case "open":
                  if (args.length < 2) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates open <Name>");
                     return true;
                  }

                  String name = args[1];
                  if (!this.plugin.getCrateManager().crateExists(name)) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                     return true;
                  }

                  this.plugin.getCrateManager().openCrate(player, name, player.getLocation());
                  break;
               case "link":
                  if (args.length < 2) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates link <Name>");
                     return true;
                  }

                  String name = args[1];
                  if (!this.plugin.getCrateManager().crateExists(name)) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                     return true;
                  }

                  player.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createLinkerItem(name)});
                  String var23 = String.valueOf(ChatColor.GREEN);
                  player.sendMessage(var23 + "Link-Tool für '" + this.plugin.getCrateManager().getCrateDisplayName(name) + String.valueOf(ChatColor.GREEN) + "' erhalten!");
                  break;
               case "unlink":
                  player.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createAntiLinkerItem()});
                  player.sendMessage(String.valueOf(ChatColor.GREEN) + "Anti-Link-Tool erhalten!");
                  break;
               case "key":
                  this.handleKeyCommand(player, args);
                  break;
               case "mode":
                  this.handleModeCommand(player, args);
                  break;
               case "rename":
                  this.handleRenameCommand(player, args);
                  break;
               case "setAccept":
                  this.handleSetAcceptCommand(player, args);
                  break;
               case "setDecline":
                  this.handleSetDeclineCommand(player, args);
                  break;
               case "list":
                  Set<String> crates = this.plugin.getCrateManager().getCrateNames();
                  if (crates.isEmpty()) {
                     player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Es gibt noch keine Kisten.");
                  } else {
                     String var10001 = String.valueOf(ChatColor.AQUA);
                     player.sendMessage(var10001 + String.valueOf(ChatColor.BOLD) + "=== JustCrates Kisten ===");

                     for(String crate : crates) {
                        String display = this.plugin.getCrateManager().getCrateDisplayName(crate);
                        int uses = this.plugin.getCrateManager().getUses(crate);
                        int rewards = this.plugin.getCrateManager().getCrateRewardItems(crate).size();
                        String mode = this.plugin.getCrateManager().getCrateMode(crate);
                        ChatColor modeColor = mode.equals("ITEM") ? ChatColor.AQUA : (mode.equals("COMMAND") ? ChatColor.LIGHT_PURPLE : ChatColor.GREEN);
                        var10001 = String.valueOf(ChatColor.YELLOW);
                        player.sendMessage(var10001 + "- " + display + String.valueOf(ChatColor.GRAY) + " [Mode: " + String.valueOf(modeColor) + mode + String.valueOf(ChatColor.GRAY) + "] (Öffnungen: " + uses + ", Rewards: " + rewards + ")");
                     }
                  }
                  break;
               default:
                  this.sendHelp(player);
            }

            return true;
         }
      } else {
         if (args.length > 0 && args[0].equalsIgnoreCase("key")) {
            this.handleKeyCommandConsole(sender, args);
         } else {
            sender.sendMessage("Dieser Befehl kann nur als Spieler ausgeführt werden! (Außer /jcrates reload und /jcrates key)");
         }

         return true;
      }
   }

   private void handleModeCommand(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates mode <Crate> <ITEM|COMMAND|BOTH>");
      } else {
         String crateName = args[1];
         if (!this.plugin.getCrateManager().crateExists(crateName)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
         } else {
            String mode = args[2].toUpperCase();
            if (!mode.equals("ITEM") && !mode.equals("COMMAND") && !mode.equals("BOTH")) {
               player.sendMessage(String.valueOf(ChatColor.RED) + "Ungültiger Mode! Erlaubt: ITEM, COMMAND, BOTH");
            } else {
               this.plugin.getCrateManager().setCrateMode(crateName, mode);
               String var10001 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var10001 + "Mode für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' auf " + String.valueOf(ChatColor.GOLD) + mode + String.valueOf(ChatColor.GREEN) + " gesetzt!");
            }
         }
      }
   }

   private void handleRenameCommand(Player player, String[] args) {
      if (args.length < 3) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates rename <Crate> <Neuer Name mit &-Colorcodes>");
      } else {
         String crateName = args[1];
         if (!this.plugin.getCrateManager().crateExists(crateName)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
         } else {
            String newName = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
            this.plugin.getCrateManager().renameCrate(crateName, newName);
            String var10001 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var10001 + "Anzeigename für '" + crateName + "' auf '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' geändert!");
            this.plugin.getCrateManager().getLinkedBlocks().forEach((loc, cName) -> {
               if (cName.equalsIgnoreCase(CrateManager.stripColorCodes(crateName).toLowerCase())) {
                  this.plugin.getHologramManager().updateHologram(loc, cName);
               }

            });
         }
      }
   }

   private void handleSetAcceptCommand(Player player, String[] args) {
      ItemStack item = player.getInventory().getItemInMainHand();
      if (item != null && !item.getType().isAir()) {
         ItemMeta meta = item.getItemMeta();
         int customModelData = 0;
         if (meta != null && meta.hasCustomModelData()) {
            customModelData = meta.getCustomModelData();
         }

         this.plugin.getCrateManager().setAcceptItemCustomModelData(customModelData);
         String var10001 = String.valueOf(ChatColor.GREEN);
         player.sendMessage(var10001 + "Accept-Item aktualisiert! Custom Model Data: " + customModelData);
      } else {
         player.sendMessage(String.valueOf(ChatColor.RED) + "Du musst ein Item in der Hand halten!");
      }
   }

   private void handleSetDeclineCommand(Player player, String[] args) {
      ItemStack item = player.getInventory().getItemInMainHand();
      if (item != null && !item.getType().isAir()) {
         ItemMeta meta = item.getItemMeta();
         int customModelData = 0;
         if (meta != null && meta.hasCustomModelData()) {
            customModelData = meta.getCustomModelData();
         }

         this.plugin.getCrateManager().setDeclineItemCustomModelData(customModelData);
         String var10001 = String.valueOf(ChatColor.GREEN);
         player.sendMessage(var10001 + "Decline-Item aktualisiert! Custom Model Data: " + customModelData);
      } else {
         player.sendMessage(String.valueOf(ChatColor.RED) + "Du musst ein Item in der Hand halten!");
      }
   }

   private void handleKeyCommand(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key <give|create|keyall> ...");
      } else {
         switch (args[1].toLowerCase()) {
            case "create":
               if (args.length < 3) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key create <Crate>");
                  return;
               }

               String crateName = args[2];
               if (!this.plugin.getCrateManager().crateExists(crateName)) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                  return;
               }

               player.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createKeyItem(crateName, 1)});
               String var16 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var16 + "Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' erhalten!");
               break;
            case "give":
               if (args.length < 4) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key give <Spieler> <Crate> [Menge]");
                  return;
               }

               Player target = Bukkit.getPlayer(args[2]);
               if (target == null) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Spieler nicht gefunden!");
                  return;
               }

               String crateName = args[3];
               if (!this.plugin.getCrateManager().crateExists(crateName)) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                  return;
               }

               int amount;
               try {
                  amount = args.length >= 5 ? Math.max(1, Integer.parseInt(args[4])) : 1;
               } catch (NumberFormatException var10) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Ungültige Menge!");
                  return;
               }

               target.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createKeyItem(crateName, amount)});
               String var15 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var15 + String.valueOf(amount) + "x Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' an " + target.getName() + " gegeben!");
               target.sendMessage(String.valueOf(ChatColor.GREEN) + "Du hast " + amount + "x Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' erhalten!");
               break;
            case "keyall":
               if (args.length < 3) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key keyall <Crate>");
                  return;
               }

               String crateName = args[2];
               if (!this.plugin.getCrateManager().crateExists(crateName)) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                  return;
               }

               for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  onlinePlayer.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createKeyItem(crateName, 1)});
               }

               String var10001 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var10001 + "Alle Spieler haben einen Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' erhalten!");
               break;
            default:
               player.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key <give|create|keyall> ...");
         }

      }
   }

   private void handleKeyCommandConsole(CommandSender sender, String[] args) {
      if (args.length < 2) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key <give|keyall> ...");
      } else {
         switch (args[1].toLowerCase()) {
            case "give":
               if (args.length < 4) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key give <Spieler> <Crate> [Menge]");
                  return;
               }

               Player target = Bukkit.getPlayer(args[2]);
               if (target == null) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Spieler nicht gefunden!");
                  return;
               }

               String crateName = args[3];
               if (!this.plugin.getCrateManager().crateExists(crateName)) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                  return;
               }

               int amount;
               try {
                  amount = args.length >= 5 ? Math.max(1, Integer.parseInt(args[4])) : 1;
               } catch (NumberFormatException var10) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Ungültige Menge!");
                  return;
               }

               target.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createKeyItem(crateName, amount)});
               String var14 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var14 + String.valueOf(amount) + "x Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' an " + target.getName() + " gegeben!");
               break;
            case "keyall":
               if (args.length < 3) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key keyall <Crate>");
                  return;
               }

               String crateName = args[2];
               if (!this.plugin.getCrateManager().crateExists(crateName)) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "Diese Kiste existiert nicht!");
                  return;
               }

               for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                  onlinePlayer.getInventory().addItem(new ItemStack[]{this.plugin.getCrateManager().createKeyItem(crateName, 1)});
               }

               String var10001 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var10001 + "Alle Spieler haben einen Schlüssel für '" + this.plugin.getCrateManager().getCrateDisplayName(crateName) + String.valueOf(ChatColor.GREEN) + "' erhalten!");
               break;
            default:
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Nutzung: /jcrates key <give|keyall> ...");
         }

      }
   }

   private void sendHelp(Player player) {
      String var10001 = String.valueOf(ChatColor.AQUA);
      player.sendMessage(var10001 + String.valueOf(ChatColor.BOLD) + "=== JustCrates Hilfe ===");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates create <Name> " + String.valueOf(ChatColor.GRAY) + "- Erstellt eine neue Kiste");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates edit <Name> " + String.valueOf(ChatColor.GRAY) + "- Bearbeitet die Rewards");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates delete <Name> " + String.valueOf(ChatColor.GRAY) + "- Löscht eine Kiste");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates open <Name> " + String.valueOf(ChatColor.GRAY) + "- Öffnet eine Kiste direkt");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates mode <Crate> <ITEM|COMMAND|BOTH> " + String.valueOf(ChatColor.GRAY) + "- Setzt den Reward-Mode");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates rename <Crate> <Neuer Name> " + String.valueOf(ChatColor.GRAY) + "- Ändert den Anzeigenamen (mit &-Colorcodes)");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates link <Name> " + String.valueOf(ChatColor.GRAY) + "- Gibt ein Link-Tool");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates unlink " + String.valueOf(ChatColor.GRAY) + "- Gibt ein Unlink-Tool");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates key create <Crate> " + String.valueOf(ChatColor.GRAY) + "- Erstellt einen Schlüssel");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates key give <Player> <Crate> [Menge] " + String.valueOf(ChatColor.GRAY) + "- Gibt Schlüssel");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates key keyall <Crate> " + String.valueOf(ChatColor.GRAY) + "- Gibt Schlüssel an alle");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates setAccept " + String.valueOf(ChatColor.GRAY) + "- Setzt das Accept-Item (Custom Model Data)");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates setDecline " + String.valueOf(ChatColor.GRAY) + "- Setzt das Decline-Item (Custom Model Data)");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates list " + String.valueOf(ChatColor.GRAY) + "- Zeigt alle Kisten an");
      var10001 = String.valueOf(ChatColor.YELLOW);
      player.sendMessage(var10001 + "/jcrates reload " + String.valueOf(ChatColor.GRAY) + "- Lädt Config und Kisten neu");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         completions.addAll(List.of("create", "edit", "delete", "open", "mode", "rename", "link", "unlink", "key", "list", "setAccept", "setDecline", "reload"));
      } else if (args.length == 2 && args[0].equalsIgnoreCase("key")) {
         completions.addAll(List.of("give", "create", "keyall"));
      } else if (args.length == 2 && args[0].equalsIgnoreCase("rename")) {
         completions.addAll(this.plugin.getCrateManager().getCrateNames());
      } else if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
         completions.addAll(this.plugin.getCrateManager().getCrateNames());
      } else if (args.length == 3 && args[0].equalsIgnoreCase("mode")) {
         completions.addAll(List.of("ITEM", "COMMAND", "BOTH"));
      } else if (args.length == 2 && !args[0].equalsIgnoreCase("key")) {
         completions.addAll(this.plugin.getCrateManager().getCrateNames());
      } else if (args.length == 3 && args[0].equalsIgnoreCase("key")) {
         if (args[1].equalsIgnoreCase("give")) {
            for(Player p : Bukkit.getOnlinePlayers()) {
               completions.add(p.getName());
            }
         } else if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("keyall")) {
            completions.addAll(this.plugin.getCrateManager().getCrateNames());
         }
      } else if (args.length == 4 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
         completions.addAll(this.plugin.getCrateManager().getCrateNames());
      } else if (args.length == 5 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
         completions.addAll(List.of("1", "2", "4", "8", "16", "32", "64"));
      }

      String currentArg = args[args.length - 1].toLowerCase();
      completions.removeIf((s) -> !s.toLowerCase().startsWith(currentArg));
      return completions;
   }
}
