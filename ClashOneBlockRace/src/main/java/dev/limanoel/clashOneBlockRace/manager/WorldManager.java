package dev.limanoel.clashOneBlockRace.manager;

import dev.limanoel.clashOneBlockRace.ClashOneBlockRace;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class WorldManager {

    private final ClashOneBlockRace plugin;
    private final ConfigManager configManager;
    private InventoryManager inventoryManager;
    private GameManager gameManager;

    public WorldManager(ClashOneBlockRace plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void setInventoryManager(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public World createOneBlockWorldMultiplayer(String oneblock_multiplayer) {
        World existWorld = Bukkit.getWorld(oneblock_multiplayer);
        if (existWorld != null){
            return existWorld;
        }

        WorldCreator creator = new WorldCreator(oneblock_multiplayer);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generatorSettings("{\"layers\": [], \"structures\": {\"structures\": {}}}");

        World multiplayer = Bukkit.createWorld(creator);
        if (multiplayer != null){
            multiplayer.setGameRule(GameRules.SPAWN_MOBS, false);
        }

        return multiplayer;
    }

    public void setupMultiplayer(Player player, String oneblock_multiplayer){
        World multiplayerWorld = createOneBlockWorldMultiplayer(oneblock_multiplayer);
        if (multiplayerWorld == null){
            configManager.sendMessage(player, "error-world-create");
            return;
        }

        // Inventar speichern und leeren bevor der Spieler teleportiert wird
        if (inventoryManager != null) {
            inventoryManager.saveAndClearInventory(player);
        }

        int playerCount = multiplayerWorld.getPlayers().size();
        int zOffset = playerCount * configManager.getSpawnZSpacing();

        // Yaw -90 = Spieler blickt nach Osten (+X). Das ist die Richtung, in die gebaut
        // werden muss, um vor der Border zu fliehen. Vorher hatten Spieler Yaw 0 (Süden),
        // was sie in eine völlig falsche Richtung schauen ließ.
        float buildDirectionYaw = -90f;
        Location spawnLocation = new Location(multiplayerWorld, 0.5, configManager.getSpawnY(), zOffset + 0.5, buildDirectionYaw, 0f);
        Location startBlock = new Location(multiplayerWorld, 0, configManager.getBedrockY(), zOffset);
        startBlock.getBlock().setType(Material.BEDROCK);

        if (gameManager != null) {
            gameManager.registerPlayerBedrock(player.getUniqueId(), startBlock);
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.teleport(spawnLocation);
        configManager.sendMessage(player, "match-teleported");

        spawnDirectionIndicator(multiplayerWorld, zOffset, player);
    }

    /**
     * Spawnt eine schwebende Text-Anzeige, die eindeutig zeigt in welche Richtung (+X)
     * gebaut werden muss, um der Border zu entkommen. Behebt: "Direction not clear".
     */
    private void spawnDirectionIndicator(World world, int zOffset, Player player) {
        if (!configManager.isDirectionIndicatorEnabled()) return;

        int offsetX = configManager.getDirectionIndicatorOffsetX();
        int offsetY = configManager.getDirectionIndicatorOffsetY();

        Location indicatorLoc = new Location(world, offsetX, configManager.getSpawnY() + offsetY, zOffset + 0.5);

        TextDisplay display = world.spawn(indicatorLoc, TextDisplay.class, entity -> {
            Component text = configManager.parseComponent(configManager.getDirectionIndicatorText());
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setPersistent(true);
        });

        if (gameManager != null) {
            gameManager.registerDirectionIndicator(player.getUniqueId(), display);
        }
    }
}
