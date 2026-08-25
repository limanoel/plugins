package dev.limanoel.clashOneBlockRace;

import dev.limanoel.clashOneBlockRace.commands.LeaveCommand;
import dev.limanoel.clashOneBlockRace.commands.OneBlockRaceCommand;
import dev.limanoel.clashOneBlockRace.listener.GameStartListener;
import dev.limanoel.clashOneBlockRace.manager.BorderManager;
import dev.limanoel.clashOneBlockRace.manager.ConfigManager;
import dev.limanoel.clashOneBlockRace.manager.GameManager;
import dev.limanoel.clashOneBlockRace.manager.InventoryManager;
import dev.limanoel.clashOneBlockRace.manager.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClashOneBlockRace extends JavaPlugin {

    private ConfigManager configManager;
    private WorldManager worldManager;
    private GameManager gameManager;
    private InventoryManager inventoryManager;
    private BorderManager borderManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.worldManager = new WorldManager(this, configManager);
        this.gameManager = new GameManager(this, configManager);
        this.borderManager = new BorderManager(this, configManager, gameManager);

        this.worldManager.setInventoryManager(inventoryManager);
        this.worldManager.setGameManager(gameManager);
        this.gameManager.setBorderManager(borderManager);

        OneBlockRaceCommand raceCommand = new OneBlockRaceCommand(this, worldManager, gameManager, configManager);
        LeaveCommand leaveCommand = new LeaveCommand(this, gameManager, inventoryManager, configManager);

        this.getServer().getPluginManager().registerEvents(new GameStartListener(this, gameManager, inventoryManager, configManager, borderManager), this);
        this.getServer().getPluginManager().registerEvents(raceCommand, this);

        if (this.getCommand("oneblockrace") != null) {
            this.getCommand("oneblockrace").setExecutor(raceCommand);
        } else if (this.getCommand("oneblock") != null) {
            this.getCommand("oneblock").setExecutor(raceCommand);
        }

        if (this.getCommand("leave") != null) {
            this.getCommand("leave").setExecutor(leaveCommand);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            gameManager.cleanEmptyWorlds();
        }, 200L, 200L);
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.deleteAllOneBlockWorlds();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }
}