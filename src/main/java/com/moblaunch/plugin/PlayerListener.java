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
import org.bukkit.inventory.EquipmentSlot;
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
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // 1. 命名牌逻辑 (优先处理)
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem.getType() == Material.NAME_TAG && mainHandItem.hasItemMeta()) {
            ItemMeta itemMeta = mainHandItem.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                plugin.getMobManager().setMobOwner(entity, player);
                return;
            }
        }

        // 2. 抱起逻辑
        if (player.isSneaking()) {
            // 要求：主手和副手必须都为空
            if (!isHandsEmpty(player)) {
                return;
            }

            if (plugin.getMobManager().isPlayerHoldingMob(player)) {
                player.sendMessage("§c" + plugin.getLanguageManager().getMessage("already-holding-mob"));
                event.setCancelled(true);
                return;
            }

            if (plugin.getMobManager().pickupMob(player, entity)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 辅助方法：检查双手是否为空
     */
    private boolean isHandsEmpty(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return (mainHand == null || mainHand.getType() == Material.AIR) &&
                (offHand == null || offHand.getType() == Material.AIR);
    }

    @EventHandler
    public void onPlayerInteractWithHeldEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (plugin.getMobManager().isPlayerHoldingMob(player) &&
                player.getPassengers().contains(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageHeldEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            Entity entity = event.getEntity();

            if (plugin.getMobManager().isPlayerHoldingMob(player) &&
                    player.getPassengers().contains(entity)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 潜行状态切换 (蓄力控制)
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getMobManager().isPlayerHoldingMob(player)) {
            return;
        }

        if (event.isSneaking()) {
            // 要求：蓄力时也必须双手为空
            if (!isHandsEmpty(player)) {
                return;
            }

            plugin.getMobManager().startCharging(player);
        } else {
            // 松开 Shift，停止蓄力并根据进度决定是投掷还是放下
            plugin.getMobManager().stopChargingAndLaunch(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMobManager().isPlayerHoldingMob(player)) {
            plugin.getMobManager().putdownMob(player);
        }
    }
}