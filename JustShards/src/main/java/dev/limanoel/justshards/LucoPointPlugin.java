package dev.limanoel.justshards;

import dev.limanoel.justshards.commands.AFKCommand;
import dev.limanoel.justshards.commands.Points;
import dev.limanoel.justshards.commands.CrateShopCommand;
import dev.limanoel.justshards.commands.ReloadCommand;
import dev.limanoel.justshards.placeholder.PointsPlaceholder;
import dev.limanoel.justshards.shop.CrateShopManager;
import dev.limanoel.justshards.shop.CrateShopListener;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LucoPointPlugin extends JavaPlugin implements Listener {

    private static dev.limanoel.justshards.LucoPointPlugin instance;

    private RegionSelect region;
    private RegionSelect afkRegion;

    private final Map<UUID, Long> enterTime = new HashMap<>();
    private final Map<UUID, Integer> shards = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Set<UUID> afkTeleporting = new HashSet<>();

    private File shardsFile;
    private FileConfiguration shardsConfig;

    private CrateShopManager shopManager;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        setupShardsFile();
        loadShards();
        loadRegion();
        loadAfkRegion();

        if (getCommand("afk") != null) getCommand("afk").setExecutor(new AFKCommand(this));
        if (getCommand("shards") != null) getCommand("shards").setExecutor(new Points(this));
        if (getCommand("shardreload") != null) getCommand("shardreload").setExecutor(new ReloadCommand(this));


        shopManager = new CrateShopManager(this);
        if (getCommand("shardshopadmin") != null) getCommand("shardshopadmin").setExecutor(new CrateShopCommand(this, shopManager));
        if (getCommand("store") != null) getCommand("store").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player)) return true;
            if (!sender.hasPermission("shardshop.use")) {
                sender.sendMessage(ChatColor.RED + "Dazu hast du keine Rechte!");
                return true;
            }
            Player player = (Player) sender;
            shopManager.openShop(player);
            return true;
        });


        if (getCommand("shardadmin") != null) {
            getCommand("shardadmin").setExecutor((sender, cmd, label, args) -> {


                if (!sender.hasPermission("shards.admin")) {
                    sender.sendMessage(ChatColor.RED + "Dazu hast du keine Rechte!");
                    return true;
                }


                if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
                    sender.sendMessage(ChatColor.RED + "Benutzung: /shardadmin set <Anzahl> <Spieler>");
                    return true;
                }


                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 0) {
                        sender.sendMessage(ChatColor.RED + "Die Anzahl darf nicht negativ sein!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Bitte gib eine gültige Zahl für die Punkte an!");
                    return true;
                }


                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Der Spieler " + args[2] + " wurde nicht gefunden.");
                    return true;
                }


                setShards(target.getUniqueId(), amount);


                sender.sendMessage(ChatColor.GREEN + "Du hast die Shards von " + target.getName() + " auf " + amount + " gesetzt.");
                target.sendMessage(ChatColor.GREEN + "Deine Shards wurden von einem Admin auf " + amount + " gesetzt.");

                return true;
            });
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new CrateShopListener(shopManager, this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PointsPlaceholder(this).register();
        }

        startTask();
    }

    @Override
    public void onDisable() {

        saveShards();

        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }

        bars.clear();
    }

    public static dev.limanoel.justshards.LucoPointPlugin getInstance() {
        return instance;
    }

    public Set<UUID> getAfkTeleporting() {
        return afkTeleporting;
    }



    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        if (!afkTeleporting.contains(uuid)) return;

        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) < 0.01) return;

        afkTeleporting.remove(uuid);

        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("afk-cancel", "&l&aSHARDS &8» &cTeleport abgebrochen, da du dich bewegt hast!")
        ));
    }



    private void loadRegion() {

         FileConfiguration cfg = getConfig();

         String worldName = cfg.getString("region.world");
         if (worldName == null) return;
         World world = Bukkit.getWorld(worldName);

         if (world == null) return;

         Location pos1 = new Location(world,
                 cfg.getDouble("region.pos1.x"),
                 cfg.getDouble("region.pos1.y"),
                 cfg.getDouble("region.pos1.z"));

         Location pos2 = new Location(world,
                 cfg.getDouble("region.pos2.x"),
                 cfg.getDouble("region.pos2.y"),
                 cfg.getDouble("region.pos2.z"));

         region = new RegionSelect(pos1, pos2);
     }

     private void loadAfkRegion() {

         FileConfiguration cfg = getConfig();

         String worldName = cfg.getString("afk-region.world");
         if (worldName == null) return;
         World world = Bukkit.getWorld(worldName);

         if (world == null) return;

         Location pos1 = new Location(world,
                 cfg.getDouble("afk-region.pos1.x"),
                 cfg.getDouble("afk-region.pos1.y"),
                 cfg.getDouble("afk-region.pos1.z"));

         Location pos2 = new Location(world,
                 cfg.getDouble("afk-region.pos2.x"),
                 cfg.getDouble("afk-region.pos2.y"),
                 cfg.getDouble("afk-region.pos2.z"));

         afkRegion = new RegionSelect(pos1, pos2);
     }



     private void startTask() {

         Bukkit.getScheduler().runTaskTimer(this, () -> {

             for (Player player : Bukkit.getOnlinePlayers()) {

                 UUID uuid = player.getUniqueId();

                 if (region == null && afkRegion == null) continue;

                 boolean inRegion = region != null && region.contains(player.getLocation());
                 boolean inAfkRegion = afkRegion != null && afkRegion.contains(player.getLocation());

                 if (inRegion || inAfkRegion) {

                     enterTime.putIfAbsent(uuid, System.currentTimeMillis());

                     long entered = enterTime.get(uuid);
                     int delay = getDelay(player);

                     long seconds = (System.currentTimeMillis() - entered) / 1000;

                     double progress = Math.min(1.0, (double) seconds / delay);
                     int remaining = (int) (delay - seconds);

                     BossBar bar = bars.get(uuid);

                     if (bar == null) {
                         bar = Bukkit.createBossBar(
                                 "§aNächste Shard...",
                                 BarColor.PURPLE,
                                 BarStyle.SOLID
                         );

                         bar.addPlayer(player);
                         bars.put(uuid, bar);
                     }

                     bar.setProgress(progress);
                      bar.setTitle(ChatColor.translateAlternateColorCodes('&', "&f Nächste Shard in &x&F&F&7&E&B&8" + Math.max(0, remaining) + "s"));

                     if (seconds >= delay) {

                           addShards(uuid, getConfig().getInt("shards-per-interval"));

                           String message = ChatColor.translateAlternateColorCodes('&',
                                   getConfig().getString("message", "&x&5&A&B&F&D&9&lSHARDS &8» &f +1 SHARD"));
                           message = message.replace("%player%", player.getName())
                                   .replace("%shards_amount%", String.valueOf(getShards(uuid)));
                           player.sendMessage(message);

                         enterTime.put(uuid, System.currentTimeMillis());
                     }

                 } else {

                     enterTime.remove(uuid);

                     BossBar bar = bars.remove(uuid);

                     if (bar != null) {
                         bar.removeAll();
                     }
                 }
             }

         }, 20L, 20L);
     }

    private int getDelay(Player player) {

        if (player.hasPermission("shards.delay.fast")) {
            return getConfig().getInt("vip-delay-seconds");
        }

        return getConfig().getInt("default-delay-seconds");
    }



    public int getShards(UUID uuid) {
        return shards.getOrDefault(uuid, 0);
    }

    public void setShards(UUID uuid, int amount) {
        shards.put(uuid, amount);
        shardsConfig.set("players." + uuid + ".shards", amount);
        saveShards();
    }

    public void addShards(UUID uuid, int amount) {
        setShards(uuid, getShards(uuid) + amount);
    }

    public boolean removeShards(UUID uuid, int amount) {

        int current = getShards(uuid);

        if (current < amount) return false;

        setShards(uuid, current - amount);
        return true;
    }

    private void setupShardsFile() {

        shardsFile = new File(getDataFolder(), "shards.yml");

        if (!shardsFile.exists()) {
            shardsFile.getParentFile().mkdirs();
            saveResource("shards.yml", false);
        }

        shardsConfig = YamlConfiguration.loadConfiguration(shardsFile);
    }

    private void loadShards() {

        if (!shardsConfig.contains("players")) return;

        org.bukkit.configuration.ConfigurationSection section = shardsConfig.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {

            UUID uuid = UUID.fromString(uuidString);

            int amount = shardsConfig.getInt("players." + uuidString + ".shards");

            shards.put(uuid, amount);
        }
    }

    public void saveShards() {

        for (UUID uuid : shards.keySet()) {
            shardsConfig.set("players." + uuid + ".shards", shards.get(uuid));
        }

        try {
            shardsConfig.save(shardsFile);
        } catch (IOException e) {
            getLogger().warning("Error saving shards: " + e.getMessage());
        }
    }

    public void reloadShardsConfig() {
        setupShardsFile();
        loadShards();
    }

    public void reloadCrateShopConfig() {
        reloadConfig();
        if (shopManager != null) {
            shopManager.reload();
        }
    }
}