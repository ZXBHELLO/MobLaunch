package com.moblaunch.plugin;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        // Check if player is using a name tag
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.NAME_TAG && itemInHand.hasItemMeta()) {
            ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                // Player is using a named name tag, set the entity owner
                plugin.getMobManager().setMobOwner(entity, player);
                return; // Don't process pickup logic when using name tags
            }
        }

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
     * Prevents player from interacting with mobs they are holding
     * @param event the event
     */
    @EventHandler
    public void onPlayerInteractWithHeldEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        // Check if the player is holding this specific entity
        if (plugin.getMobManager().isPlayerHoldingMob(player) && 
            player.getPassengers().contains(entity)) {
            // Cancel the interaction to prevent accidental actions
            event.setCancelled(true);
        }
    }

    /**
     * Prevents player from damaging mobs they are holding
     * @param event the event
     */
    @EventHandler
    public void onPlayerDamageHeldEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Entity entity = event.getEntity();
            
            // Check if the player is holding this specific entity
            if (plugin.getMobManager().isPlayerHoldingMob(player) && 
                player.getPassengers().contains(entity)) {
                // Cancel the damage to prevent accidental actions
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