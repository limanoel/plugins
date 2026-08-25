package dev.limanoel.justshards.commands;

import dev.limanoel.justshards.LucoPointPlugin;
import dev.limanoel.justshards.shop.CrateShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CrateShopCommand implements CommandExecutor {

    private final LucoPointPlugin plugin;
    private final CrateShopManager manager;

    public CrateShopCommand(LucoPointPlugin plugin, CrateShopManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ Nur Spieler können diesen Befehl verwenden!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("crateshop.admin")) {
            player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ Du hast keine Berechtigung!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ /crateshopadmin add §7- Item hinzufügen");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType().isAir()) {
                player.sendMessage("§x§5§A§B§F§D§9§lSHARDS ➔ Halte ein Item in der Hand!");
                return true;
            }


            manager.startPriceDialog(player, item);
            return true;
        }

        return false;
    }
}
