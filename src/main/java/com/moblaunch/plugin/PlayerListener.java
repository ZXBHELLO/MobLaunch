package com.moblaunch.plugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {
    private final MobLaunch plugin;

    public PlayerListener(MobLaunch plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem.getType() == Material.NAME_TAG && mainHandItem.hasItemMeta()) {
            ItemMeta itemMeta = mainHandItem.getItemMeta();
            if (itemMeta.hasDisplayName()) {
                plugin.getMobManager().setMobOwner(entity, player);
                player.sendMessage(ChatColor.GREEN + "已绑定生物所有权: " + itemMeta.getDisplayName());

                // 检查配置：创造模式是否消耗
                boolean consumeInCreative = plugin.getConfigManager().isConsumeNametagCreative();
                if (player.getGameMode() != GameMode.CREATIVE || consumeInCreative) {
                    mainHandItem.subtract(1);
                }
                return;
            }
        }

        if (player.isSneaking()) {
            if (!isHandsEmpty(player))
                return;
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        Entity entity = event.getEntity();
        if (entity.getPersistentDataContainer().has(plugin.getMobManager().getNoFallKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            entity.getPersistentDataContainer().remove(plugin.getMobManager().getNoFallKey());
        }
    }

    private boolean isHandsEmpty(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return (mainHand == null || mainHand.getType() == Material.AIR) &&
                (offHand == null || offHand.getType() == Material.AIR);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractWithHeldEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMobManager().isPlayerHoldingMob(player)
                && player.getPassengers().contains(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamageHeldEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (plugin.getMobManager().isPlayerHoldingMob(player)
                    && player.getPassengers().contains(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getMobManager().isPlayerHoldingMob(player))
            return;
        if (event.isSneaking()) {
            if (!isHandsEmpty(player))
                return;
            plugin.getMobManager().startCharging(player);
        } else {
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 强制执行放下逻辑，清理插件内部状态
        Player player = event.getEntity();
        if (plugin.getMobManager().isPlayerHoldingMob(player)) {
            plugin.getMobManager().putdownMob(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 传送时强制放下，防止Bug
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            Player player = event.getPlayer();
            if (plugin.getMobManager().isPlayerHoldingMob(player)) {
                plugin.getMobManager().putdownMob(player);
            }
        }
    }
}