package com.adum.greedyCompass.cmds;

import com.adum.greedyCompass.GreedyCompass;
import com.adum.greedyCompass.managers.CompassManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class Track implements CommandExecutor {

    private final GreedyCompass plugin;

    public Track(GreedyCompass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.player-only")));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /track <playername></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.player-not-found")
            ));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.cannot-track-self")
            ));
            return true;
        }

        openConfirmationGui(player, target);
        return true;
    }

    private void openConfirmationGui(Player player, Player target) {
        
        CompassManager.CostInfo costInfo = plugin.getCompassManager().calculateCost(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 9, MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.title")));

        ItemStack accept = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta acceptMeta = accept.getItemMeta();
        acceptMeta.displayName(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.confirm-button")));
        accept.setItemMeta(acceptMeta);

        
        String cleanItemName = java.util.Arrays.stream(costInfo.getMaterial().name().toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));

        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.info-button")));
        infoMeta.lore(Collections.singletonList(
                MiniMessage.miniMessage().deserialize("<gray>Target: </gray><white>" + target.getName() + "</white><br><gray>Cost: </gray><yellow>" + costInfo.getAmount() + " " + cleanItemName + "(s)</yellow>")
        ));
        infoMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_target"), PersistentDataType.STRING, target.getUniqueId().toString());
        info.setItemMeta(infoMeta);

        ItemStack deny = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta denyMeta = deny.getItemMeta();
        denyMeta.displayName(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.deny-button")));
        deny.setItemMeta(denyMeta);

        gui.setItem(2, accept);
        gui.setItem(4, info);
        gui.setItem(6, deny);

        player.openInventory(gui);
    }
}