package com.adum.greedyCompass;

import com.adum.greedyCompass.cmds.Track;
import com.adum.greedyCompass.listeners.CompassListener;
import com.adum.greedyCompass.managers.CompassManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GreedyCompass extends JavaPlugin {

    private CompassManager compassManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.compassManager = new CompassManager(this);
        this.compassManager.loadData();


        this.getCommand("track").setExecutor(new Track(this));
        getServer().getPluginManager().registerEvents(new CompassListener(this), this);


        this.compassManager.startClock();
    }

    @Override
    public void onDisable() {
        if (this.compassManager != null) {
            this.compassManager.saveData();
        }
    }

    public CompassManager getCompassManager() {
        return compassManager;
    }
}