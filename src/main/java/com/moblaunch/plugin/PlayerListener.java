package com.moblaunch.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Player event listener
 */
public class PlayerListener implements Listener {
    private final MobLaunch plugin;

    public PlayerListener(MobLaunch plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player right-click on entity event
     * @param event the event
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Check if player is sneaking and holding nothing
        if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isAir()) {
            // Check if the player is already holding a mob
            if (plugin.getMobManager().isPlayerHoldingMob(player)) {
                player.sendMessage("§c" + plugin.getLanguageManager().getMessage("already-holding-mob"));
                event.setCancelled(true);
                return;
            }
            
            // Attempt to pick up the mob
            if (plugin.getMobManager().pickupMob(player, entity)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles player sneak state change event
     * @param event the event
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player is holding a mob
        if (!plugin.getMobManager().isPlayerHoldingMob(player)) {
            return;
        }

        if (event.isSneaking()) {
            // Start charging
            plugin.getMobManager().startCharging(player);
        } else {
            // Stop charging and launch the mob
            plugin.getMobManager().stopChargingAndLaunch(player);
        }
    }

    /**
     * Handles player quit event
     * @param event the event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // If the player is holding a mob, drop it
        if (plugin.getMobManager().isPlayerHoldingMob(player)) {
            plugin.getMobManager().putdownMob(player);
        }
    }
}