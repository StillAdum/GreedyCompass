package com.adum.greedyCompass.listeners;

import com.adum.greedyCompass.managers.CompassManager;
import com.adum.greedyCompass.GreedyCompass;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CompassListener implements Listener {

    private final GreedyCompass plugin;

    public CompassListener(GreedyCompass plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String titleString = MiniMessage.miniMessage().serialize(event.getView().title());
        String configTitle = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.title")));

        if (!titleString.equals(configTitle)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        ItemStack infoItem = inv.getItem(4);

        if (infoItem == null || !infoItem.hasItemMeta()) return;
        String targetUuidStr = infoItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "gui_target"), PersistentDataType.STRING);
        if (targetUuidStr == null) return;

        UUID targetUuid = UUID.fromString(targetUuidStr);
        Player target = Bukkit.getPlayer(targetUuid);

        if (event.getRawSlot() == 6) {
            player.closeInventory();
        } else if (event.getRawSlot() == 2) {
            player.closeInventory();
            if (target == null || !target.isOnline()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
                return;
            }

            CompassManager.CostInfo costInfo = plugin.getCompassManager().calculateCost(player.getUniqueId());
            Material costMat = costInfo.getMaterial();
            int costAmount = costInfo.getAmount();

            if (!player.getInventory().containsAtLeast(new ItemStack(costMat), costAmount)) {
                String cleanItemName = java.util.Arrays.stream(costMat.name().toLowerCase().split("_"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(java.util.stream.Collectors.joining(" "));

                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-enough-items")
                                .replace("<cost>", String.valueOf(costAmount))
                                .replace("<item>", cleanItemName)
                ));
                return;
            }

            player.getInventory().removeItem(new ItemStack(costMat, costAmount));
            plugin.getCompassManager().incrementStreak(player.getUniqueId());
            plugin.getCompassManager().startTracking(player, target);

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.started-tracking").replace("<target>", target.getName())
            ));


            if (!player.getWorld().equals(target.getWorld())) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.dimension-info")
                                .replace("<target>", target.getName())
                                .replace("<dimension>", plugin.getCompassManager().formatDimensionName(target.getWorld().getEnvironment().name()))
                ));
            }
        }
    }


    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (plugin.getCompassManager().hasTrackerCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        String titleString = MiniMessage.miniMessage().serialize(event.getView().title());
        String configTitle = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.title")));

        if (titleString.equals(configTitle)) {
            event.setCancelled(true);
            handleGuiInteraction(event);
            return;
        }


        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        Inventory topInventory = event.getView().getTopInventory();


        if (topInventory.getType() != InventoryType.PLAYER && topInventory.getType() != InventoryType.CRAFTING) {


            if (plugin.getCompassManager().hasTrackerCompass(clicked) || plugin.getCompassManager().hasTrackerCompass(cursor)) {
                event.setCancelled(true);
                return;
            }


            if (event.isShiftClick() && event.getCurrentItem() != null) {
                if (plugin.getCompassManager().hasTrackerCompass(event.getCurrentItem())) {
                    event.setCancelled(true);
                    return;
                }
            }


            if (event.getClick().isKeyboardClick()) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (plugin.getCompassManager().hasTrackerCompass(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }


        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE && plugin.getCompassManager().hasTrackerCompass(cursor)) {
            event.setCancelled(true);
        }
    }


    private void handleGuiInteraction(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        ItemStack infoItem = inv.getItem(4);

        if (infoItem == null || !infoItem.hasItemMeta()) return;
        String targetUuidStr = infoItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "gui_target"), PersistentDataType.STRING);
        if (targetUuidStr == null) return;

        UUID targetUuid = UUID.fromString(targetUuidStr);
        Player target = Bukkit.getPlayer(targetUuid);

        if (event.getRawSlot() == 6) {
            player.closeInventory();
        } else if (event.getRawSlot() == 2) {
            player.closeInventory();
            if (target == null || !target.isOnline()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")));
                return;
            }

            CompassManager.CostInfo costInfo = plugin.getCompassManager().calculateCost(player.getUniqueId());
            Material costMat = costInfo.getMaterial();
            int costAmount = costInfo.getAmount();

            if (!player.getInventory().containsAtLeast(new ItemStack(costMat), costAmount)) {
                String cleanItemName = java.util.Arrays.stream(costMat.name().toLowerCase().split("_"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(java.util.stream.Collectors.joining(" "));

                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.not-enough-items")
                                .replace("<cost>", String.valueOf(costAmount))
                                .replace("<item>", cleanItemName)
                ));
                return;
            }

            player.getInventory().removeItem(new ItemStack(costMat, costAmount));
            plugin.getCompassManager().incrementStreak(player.getUniqueId());
            plugin.getCompassManager().startTracking(player, target);

            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.started-tracking").replace("<target>", target.getName())
            ));

            if (!player.getInventory().isEmpty() && !player.getWorld().equals(target.getWorld())) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.dimension-info")
                                .replace("<target>", target.getName())
                                .replace("<dimension>", plugin.getCompassManager().formatDimensionName(target.getWorld().getEnvironment().name()))
                ));
            }
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getCompassManager().hasTrackerCompass(event.getOldCursor())) {
            if (event.getView().getTopInventory().getType() != InventoryType.PLAYER) {
                for (int slot : event.getRawSlots()) {
                    if (slot < event.getView().getTopInventory().getSize()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> plugin.getCompassManager().hasTrackerCompass(item));
    }
}