package com.adum.greedyCompass.managers;

import com.adum.greedyCompass.GreedyCompass;
import com.adum.greedyCompass.TrackingSession;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CompassManager {

    private final GreedyCompass plugin;
    private final Map<UUID, TrackingSession> activeSessions = new HashMap<>();
    private final Map<UUID, Integer> playerStreaks = new HashMap<>();
    private long lastResetTime = System.currentTimeMillis();

    private final File dataFile;
    private FileConfiguration dataConfig;
    private final NamespacedKey trackingKey;

    public CompassManager(GreedyCompass plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.trackingKey = new NamespacedKey(plugin, "tracker_target");
    }

    public NamespacedKey getTrackingKey() { return trackingKey; }


    public static class CostInfo {
        private final int amount;
        private final org.bukkit.Material material;

        public CostInfo(int amount, org.bukkit.Material material) {
            this.amount = amount;
            this.material = material;
        }

        public int getAmount() { return amount; }
        public org.bukkit.Material getMaterial() { return material; }
    }

    public void startClock() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkStreakReset();

            Iterator<Map.Entry<UUID, TrackingSession>> iterator = activeSessions.entrySet().iterator();
            while (iterator.hasNext()) {
                TrackingSession session = iterator.next().getValue();
                Player tracker = Bukkit.getPlayer(session.getTrackerUuid());
                Player target = Bukkit.getPlayer(session.getTargetUuid());


                if (tracker == null || target == null) {
                    continue;
                }

                session.setRemainingSeconds(session.getRemainingSeconds() - 1);

                if (session.getRemainingSeconds() <= 0) {
                    removeCompassFromPlayer(tracker);
                    tracker.sendMessage(MiniMessage.miniMessage().deserialize(
                            plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.expired")
                    ));
                    iterator.remove();
                    saveData();
                    continue;
                }


                updateCompassTracking(tracker, target, session);
            }
        }, 20L, 20L);
    }

    private void updateCompassTracking(Player tracker, Player target, TrackingSession session) {
        String currentDimension = target.getWorld().getEnvironment().name();

        if (!currentDimension.equalsIgnoreCase(session.getLastDimension())) {
            tracker.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.dimension-change")
                                    .replace("<target>", target.getName())
                                    .replace("<dimension>", formatDimensionName(currentDimension))
            ));
            session.setLastDimension(currentDimension);
        }

        if (tracker.getWorld().equals(target.getWorld())) {
            tracker.setCompassTarget(target.getLocation());
        }
    }

    public CostInfo calculateCost(UUID uuid) {
        int nextStreakCount = playerStreaks.getOrDefault(uuid, 0) + 1;
        String path = "streak-costs." + nextStreakCount;

        if (plugin.getConfig().contains(path)) {
            return parseCostString(plugin.getConfig().getString(path));
        }


        int maxConfiguredTier = 7;
        CostInfo maxTierCost = parseCostString(plugin.getConfig().getString("streak-costs." + maxConfiguredTier, "60, DIAMOND"));
        CostInfo incrementCost = parseCostString(plugin.getConfig().getString("streak-costs.default-increment", "10, DIAMOND"));

        int extraTiers = nextStreakCount - maxConfiguredTier;
        int finalAmount = maxTierCost.getAmount() + (extraTiers * incrementCost.getAmount());


        return new CostInfo(finalAmount, incrementCost.getMaterial());
    }

    private CostInfo parseCostString(String raw) {
        if (raw == null || !raw.contains(",")) {
            return new CostInfo(10, Material.DIAMOND);
        }
        try {
            String[] parts = raw.split(",");
            int amount = Integer.parseInt(parts[0].trim());
            Material material = Material.valueOf(parts[1].trim().toUpperCase());
            return new CostInfo(amount, material);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse streak cost string: '" + raw + "'. Defaulting to 10 DIAMOND.");
            return new CostInfo(10, Material.DIAMOND);
        }
    }

    public void incrementStreak(UUID uuid) {
        playerStreaks.put(uuid, playerStreaks.getOrDefault(uuid, 0) + 1);
        saveData();
    }

    private void checkStreakReset() {
        if (!plugin.getConfig().getBoolean("enable-streak-reset", true)) return;
        long daysInMillis = plugin.getConfig().getLong("streak-reset-days", 4) * 24 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - lastResetTime >= daysInMillis) {
            playerStreaks.clear();
            lastResetTime = System.currentTimeMillis();
            saveData();
        }
    }

    public void startTracking(Player tracker, Player target) {
        int duration = plugin.getConfig().getInt("compass-duration", 960);
        TrackingSession session = new TrackingSession(tracker.getUniqueId(), target.getUniqueId(), duration, target.getWorld().getEnvironment().name());
        activeSessions.put(tracker.getUniqueId(), session);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.getPersistentDataContainer().set(trackingKey, PersistentDataType.STRING, target.getUniqueId().toString());
        meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Greedy Tracker Compass</bold></gold>"));
        compass.setItemMeta(meta);

        tracker.getInventory().addItem(compass);
        saveData();
    }

    private void removeCompassFromPlayer(Player player) {
        player.getInventory().remove(Material.COMPASS);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == Material.COMPASS) {
                if (contents[i].hasItemMeta() && contents[i].getItemMeta().getPersistentDataContainer().has(trackingKey, PersistentDataType.STRING)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    public boolean hasTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(trackingKey, PersistentDataType.STRING);
    }

    public String formatDimensionName(String environment) {
        switch (environment.toLowerCase()) {
            case "nether": return "The Nether";
            case "the_end": return "The End";
            default: return "Overworld";
        }
    }

    public void loadData() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        lastResetTime = dataConfig.getLong("last-reset", System.currentTimeMillis());

        if (dataConfig.contains("streaks")) {
            for (String key : dataConfig.getConfigurationSection("streaks").getKeys(false)) {
                playerStreaks.put(UUID.fromString(key), dataConfig.getInt("streaks." + key));
            }
        }
        if (dataConfig.contains("sessions")) {
            for (String key : dataConfig.getConfigurationSection("sessions").getKeys(false)) {
                UUID trackerId = UUID.fromString(key);
                UUID targetId = UUID.fromString(dataConfig.getString("sessions." + key + ".target"));
                int remaining = dataConfig.getInt("sessions." + key + ".remaining");
                String dim = dataConfig.getString("sessions." + key + ".dimension");
                activeSessions.put(trackerId, new TrackingSession(trackerId, targetId, remaining, dim));
            }
        }
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();
        dataConfig.set("last-reset", lastResetTime);
        for (Map.Entry<UUID, Integer> entry : playerStreaks.entrySet()) {
            dataConfig.set("streaks." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, TrackingSession> entry : activeSessions.entrySet()) {
            String base = "sessions." + entry.getKey().toString();
            dataConfig.set(base + ".target", entry.getValue().getTargetUuid().toString());
            dataConfig.set(base + ".remaining", entry.getValue().getRemainingSeconds());
            dataConfig.set(base + ".dimension", entry.getValue().getLastDimension());
        }
        try { dataConfig.save(dataFile); } catch (IOException ignored) {}
    }
}