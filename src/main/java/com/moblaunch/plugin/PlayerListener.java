package com.moblaunch.plugin;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
     * 处理玩家右键点击实体（抱起逻辑）
     * 
     * 兼容性更新：
     * 1. EventPriority.HIGH: 让我们在领地插件之后运行
     * 2. ignoreCancelled = true: 如果领地插件取消了事件，我们也忽略它
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // 双手必须为空且是主手交互
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // --- 再次检查取消状态 (双重保险) ---
        // 如果 Residence/Towny/WorldGuard 判定玩家无权交互，它们会将事件设为 Cancelled
        // 此时我们直接返回，不再执行抱起，这就实现了完美兼容。
        if (event.isCancelled()) {
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
            if (!isHandsEmpty(player)) {
                return;
            }

            if (plugin.getMobManager().isPlayerHoldingMob(player)) {
                player.sendMessage("§c" + plugin.getLanguageManager().getMessage("already-holding-mob"));
                event.setCancelled(true);
                return;
            }

            // 执行抱起
            if (plugin.getMobManager().pickupMob(player, entity)) {
                // 只有在成功抱起后，才取消原版交互
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

    /**
     * 防止与已抱起的生物交互
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractWithHeldEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (plugin.getMobManager().isPlayerHoldingMob(player) &&
                player.getPassengers().contains(entity)) {
            event.setCancelled(true);
        }
    }

    /**
     * 防止伤害已抱起的生物
     * 同样添加兼容性支持
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamageHeldEntity(EntityDamageByEntityEvent event) {
        // 如果 WorldGuard 禁止了 PVP/PVE，这里会自动跳过
        if (event.isCancelled()) {
            return;
        }

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
     * 这个事件通常不受领地插件控制，保持原样即可
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getMobManager().isPlayerHoldingMob(player)) {
            return;
        }

        if (event.isSneaking()) {
            if (!isHandsEmpty(player)) {
                return;
            }
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
}