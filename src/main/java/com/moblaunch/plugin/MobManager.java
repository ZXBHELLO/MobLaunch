package com.moblaunch.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 生物管理器，处理生物的抱起和抛出逻辑
 */
public class MobManager {
    private final MobLaunch plugin;
    private final Map<UUID, Entity> mountedMobs; // 存储玩家抱起的生物
    private final Map<UUID, ChargeTask> chargingPlayers; // 存储正在蓄力的玩家
    private final NamespacedKey mobLaunchKey;

    public MobManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.mountedMobs = new HashMap<>();
        this.chargingPlayers = new HashMap<>();
        this.mobLaunchKey = new NamespacedKey(plugin, "MobLaunchMounted");
    }

    /**
     * 抱起生物
     * @param player 玩家
     * @param entity 要抱起的生物
     * @return 是否成功抱起
     */
    public boolean pickupMob(Player player, Entity entity) {
        // 检查权限
        if (!player.hasPermission("moblaunch.use")) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("no-permission-use"));
            return false;
        }

        // 检查特定生物权限
        String entityTypeName = entity.getType().name().toLowerCase();
        if (!player.hasPermission("moblaunch.use.*") && !player.hasPermission("moblaunch.use." + entityTypeName)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("no-permission-use"));
            return false;
        }

        // 检查生物类型是否在白名单中（管理员跳过此检查）
        boolean isAdmin = player.hasPermission("moblaunch.admin");
        if (!isAdmin && !plugin.getConfigManager().isMobAllowed(entity.getType())) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("mob-not-allowed"));
            return false;
        }

        // 检查玩家是否已经抱起了生物
        if (isPlayerHoldingMob(player)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("already-holding-mob"));
            return false;
        }

        // 检查生物是否已经被其他玩家抱起
        if (isMobMounted(entity)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("mob-already-mounted"));
            return false;
        }

        // 让生物坐在玩家头上
        Location targetLocation = player.getLocation().add(0, player.getEyeHeight() + 0.3, 0);
        
        // 直接添加为乘客，不使用传送
        player.addPassenger(entity);
        
        // 标记生物为已抱起状态
        markMobAsMounted(entity);
        
        // 添加到已抱起生物列表
        mountedMobs.put(player.getUniqueId(), entity);
        
        player.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("pickup-success", entity.getName()));
        return true;
    }

    /**
     * 放下生物
     * @param player 玩家
     * @return 是否成功放下
     */
    public boolean putdownMob(Player player) {
        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null || !entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return false;
        }

        // 移除生物标记
        unmarkMobAsMounted(entity);
        
        // 从玩家身上移除生物
        if (player.isValid()) {
            player.removePassenger(entity);
        }
        
        // 从已抱起生物列表中移除
        mountedMobs.remove(player.getUniqueId());
        
        // 如果有蓄力任务，也一并取消
        ChargeTask chargeTask = chargingPlayers.get(player.getUniqueId());
        if (chargeTask != null) {
            try {
                chargeTask.cancel();
            } catch (Exception e) {
                // 忽略取消异常
            }
            chargingPlayers.remove(player.getUniqueId());
        }
        
        player.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("putdown-success", entity.getName()));
        return true;
    }

    /**
     * 开始蓄力
     * @param player 玩家
     */
    public void startCharging(Player player) {
        if (!isPlayerHoldingMob(player)) {
            return;
        }

        // 如果已有蓄力任务，先取消它
        ChargeTask existingTask = chargingPlayers.get(player.getUniqueId());
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception e) {
                // 忽略取消异常
            }
            chargingPlayers.remove(player.getUniqueId());
        }

        // 创建并启动新的蓄力任务
        ChargeTask chargeTask = new ChargeTask(player);
        
        // 使用Folia兼容的方式调度任务
        try {
            // 尝试使用Folia的区域调度
            Object task = player.getScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
                // 手动执行蓄力任务
                chargeTask.run();
            }, null, 1L, plugin.getConfigManager().getChargeIncrementTicks());

            // 只有在调度成功后才添加到 chargingPlayers 并标记为已调度
            chargingPlayers.put(player.getUniqueId(), chargeTask);
        } catch (Exception e) {
            // 如果Folia方法失败，则回退到传统Bukkit调度
            try {
                chargeTask.runTaskTimer(plugin, 1L, plugin.getConfigManager().getChargeIncrementTicks());
                // 标记任务已被调度并加入列表
                chargeTask.markScheduled();
                chargingPlayers.put(player.getUniqueId(), chargeTask);
            } catch (Exception ex) {
                plugin.getLogger().severe("无法启动蓄力任务: " + ex.getMessage());
            }
        }
    }

    /**
     * 停止蓄力并抛出生物
     * @param player 玩家
     */
    public void stopChargingAndLaunch(Player player) {
        ChargeTask chargeTask = chargingPlayers.get(player.getUniqueId());
        if (chargeTask == null) {
            return;
        }

        // 获取蓄力值
        int chargePercent = chargeTask.getChargePercent();
        
        // 只有当任务已被调度时才取消
        if (chargeTask.isScheduled()) {
            try {
                // 尝试取消任务 (Folia兼容)
                chargeTask.cancel();
            } catch (Exception e) {
                // 传统Bukkit方式取消任务
                try {
                    chargeTask.cancel();
                } catch (Exception ex) {
                    // 忽略取消异常
                }
            }
        }
        
        // 移除蓄力任务
        chargingPlayers.remove(player.getUniqueId());

        // 获取抱起的生物
        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null || !entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return;
        }

        // 移除生物标记
        unmarkMobAsMounted(entity);
        
        // 从玩家身上移除生物
        if (player.isValid()) {
            player.removePassenger(entity);
        }

        // 计算抛出向量
        Vector direction = player.getLocation().getDirection();
        double maxVelocity = 2.0; // 最大初速度
        double velocity = maxVelocity * (chargePercent / 100.0);
        Vector launchVelocity = direction.multiply(velocity);

        // 应用抛出力
        if (entity.isValid()) {
            entity.setVelocity(launchVelocity);
        }
        
        // 从已抱起生物列表中移除
        mountedMobs.remove(player.getUniqueId());
        
        if (player.isValid()) {
            player.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("launch-message", chargePercent, entity.getName()));
        }
    }

    /**
     * 检查玩家是否抱起了生物
     * @param player 玩家
     * @return 是否抱起生物
     */
    public boolean isPlayerHoldingMob(Player player) {
        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null) {
            return false;
        }
        
        // 检查生物是否仍然有效
        if (!entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return false;
        }
        
        // 检查玩家是否仍然骑乘着这个生物
        return player.getPassengers().contains(entity);
    }

    /**
     * 检查生物是否已被抱起
     * @param entity 生物
     * @return 是否已被抱起
     */
    public boolean isMobMounted(Entity entity) {
        // 首先检查我们自己的标记
        PersistentDataContainer container = entity.getPersistentDataContainer();
        boolean isMarked = container.has(mobLaunchKey, PersistentDataType.BYTE);
        
        if (!isMarked) {
            return false;
        }
        
        // 如果有标记，检查是否真的被玩家骑乘
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getPassengers().contains(entity)) {
                return true;
            }
        }
        
        // 如果没有玩家骑乘，移除标记
        unmarkMobAsMounted(entity);
        return false;
    }

    /**
     * 标记生物为已抱起状态
     * @param entity 生物
     */
    private void markMobAsMounted(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(mobLaunchKey, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * 取消生物的已抱起状态标记
     * @param entity 生物
     */
    private void unmarkMobAsMounted(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.remove(mobLaunchKey);
    }

    /**
     * 移除所有抱起的生物（插件禁用时调用）
     */
    public void removeAllMountedMobs() {
        for (Map.Entry<UUID, Entity> entry : mountedMobs.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Entity entity = entry.getValue();
            
            if (player != null && player.isOnline() && entity != null && entity.isValid()) {
                try {
                    player.removePassenger(entity);
                } catch (Exception e) {
                    // 忽略在服务器关闭时可能发生的异常
                }
            }
            
            if (entity != null && entity.isValid()) {
                unmarkMobAsMounted(entity);
            }
        }
    }

    /**
     * 蓄力任务类
     */
    private class ChargeTask extends BukkitRunnable {
        private final Player player;
        private int chargePercent = 0;
        private boolean isCancelled = false;
        private int fullChargeTicks = 0; // 记录满蓄力的ticks数
        private boolean isScheduled = false; // 标记任务是否已被调度

        public ChargeTask(Player player) {
            this.player = player;
            // 确保蓄力等级从0开始
            this.chargePercent = 0;
            this.fullChargeTicks = 0;
            this.isCancelled = false;
            this.isScheduled = false;
        }

        @Override
        public void run() {
            // 检查任务是否已取消
            if (isCancelled) {
                return;
            }
            
            // 检查玩家是否仍然抱着生物
            if (!isPlayerHoldingMob(player)) {
                cancel();
                return;
            }
            
            // 增加蓄力值（但不超过100）
            if (chargePercent < 100) {
                chargePercent = Math.min(100, chargePercent + 5);
                
                // 显示蓄力条
                displayChargeBar(player, chargePercent);
            } else {
                // 已经满100%，继续显示进度条
                displayChargeBar(player, chargePercent);
                
                // 增加满蓄力计时
                fullChargeTicks++;
                
                // 检查是否达到自动放下时间
                if (fullChargeTicks >= plugin.getConfigManager().getAutoPutdownTicks()) {
                    // 自动放下生物
                    putdownMob(player);
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("auto-putdown-message"));
                    }
                    // 从列表中移除任务
                    chargingPlayers.remove(player.getUniqueId());
                }
            }
        }

        /**
         * 获取蓄力百分比
         * @return 蓄力百分比
         */
        public int getChargePercent() {
            return chargePercent;
        }
        
        /**
         * 取消任务
         */
        @Override
        public synchronized void cancel() {
            isCancelled = true;
            if (isScheduled) {
                try {
                    super.cancel();
                } catch (IllegalStateException e) {
                    // 忽略尚未调度的任务取消异常
                }
            }
        }
        
        /**
         * 标记任务已被调度
         */
        public void markScheduled() {
            isScheduled = true;
        }
        
        /**
         * 检查任务是否已调度
         */
        public boolean isScheduled() {
            return isScheduled;
        }
        
        /**
         * 显示蓄力条在actionbar上
         * @param player 玩家
         * @param percent 蓄力百分比
         */
        private void displayChargeBar(Player player, int percent) {
            int filledBars = percent / 5; // 每5%填充一个格子
            StringBuilder bar = new StringBuilder();
            
            // 添加蓝色填充部分
            for (int i = 0; i < filledBars; i++) {
                bar.append(ChatColor.BLUE).append("|");
            }
            
            // 添加白色空白部分
            for (int i = filledBars; i < 20; i++) {
                bar.append(ChatColor.WHITE).append("|");
            }
            
            // 发送actionbar消息
            player.sendActionBar(bar.toString());
        }
    }
}