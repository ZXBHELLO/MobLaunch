package com.moblaunch.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 生物管理器，处理生物的抱起和抛出逻辑
 */
public class MobManager {
    private final MobLaunch plugin;
    private final Map<UUID, Entity> mountedMobs;
    private final Map<UUID, ChargeTask> chargingPlayers;
    private final NamespacedKey mobLaunchKey;

    public MobManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.mountedMobs = new HashMap<>();
        this.chargingPlayers = new HashMap<>();
        this.mobLaunchKey = new NamespacedKey(plugin, "MobLaunchMounted");
    }

    // --- 抱起逻辑 (Pickup) ---
    public boolean pickupMob(Player player, Entity entity) {
        // 基础检查
        if (entity == null || !entity.isValid())
            return false;

        // 防止抱起玩家自己
        if (entity.getUniqueId().equals(player.getUniqueId()))
            return false;

        if (!player.hasPermission("moblaunch.use")) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("no-permission-use"));
            return false;
        }

        boolean hasWildcardPermission = player.hasPermission("moblaunch.use.*");
        String entityTypeName = entity.getType().name().toLowerCase();
        boolean hasSpecificPermission = player.hasPermission("moblaunch.use." + entityTypeName);

        if (!hasWildcardPermission && !hasSpecificPermission) {
            boolean isAdmin = player.hasPermission("moblaunch.admin");
            if (!isAdmin && !plugin.getConfigManager().isMobAllowed(entity.getType())) {
                player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("mob-not-allowed"));
                return false;
            }
        }

        if (!checkMobOwnership(player, entity)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("mob-not-owned"));
            return false;
        }

        if (isPlayerHoldingMob(player)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("already-holding-mob"));
            return false;
        }

        if (isMobMounted(entity)) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("mob-already-mounted"));
            return false;
        }

        // --- 修复：Folia 兼容的异步传送 ---

        // 定义抱起逻辑的核心代码（封装成 Runnable 以便复用）
        Runnable mountLogic = () -> {
            // 再次检查有效性（因为异步过程可能有时间差）
            if (!player.isValid() || !entity.isValid())
                return;

            // 执行抱起
            player.addPassenger(entity);

            // 更新数据
            markMobAsMounted(entity);
            mountedMobs.put(player.getUniqueId(), entity);
            player.sendMessage(
                    ChatColor.GREEN + plugin.getLanguageManager().getMessage("pickup-success", entity.getName()));
        };

        try {
            // 尝试使用 Paper/Folia 的异步传送 API
            entity.teleportAsync(player.getLocation()).thenAccept(success -> {
                if (success) {
                    // 传送成功后，在下一 tick 执行抱起
                    // 这里使用 entity.getScheduler() 确保在正确的区域线程执行
                    try {
                        entity.getScheduler().run(plugin, (task) -> mountLogic.run(), null);
                    } catch (Throwable e) {
                        // 如果 entity.getScheduler() 不存在 (旧版 Spigot)，直接运行
                        mountLogic.run();
                    }
                }
            });
        } catch (Throwable e) {
            // 如果不支持 teleportAsync (非常旧的 Spigot)，回退到同步传送
            // 注意：Folia 不会进这里，因为 Folia 支持 teleportAsync
            try {
                entity.teleport(player.getLocation());
                mountLogic.run();
            } catch (Exception ex) {
                plugin.getLogger().warning("抱起生物失败: " + ex.getMessage());
                return false;
            }
        }

        // 返回 true 以取消原本的交互事件（防止打开生物界面）
        // 实际的抱起会在异步回调中发生
        return true;
    }

    // --- 放下逻辑 (Putdown) ---
    public boolean putdownMob(Player player) {
        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null || !entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return false;
        }

        unmarkMobAsMounted(entity);

        if (player.isValid()) {
            player.removePassenger(entity);
        }

        mountedMobs.remove(player.getUniqueId());

        // 清理蓄力任务
        ChargeTask chargeTask = chargingPlayers.get(player.getUniqueId());
        if (chargeTask != null) {
            try {
                chargeTask.cancel();
            } catch (Exception e) {
            }
            chargingPlayers.remove(player.getUniqueId());
        }

        player.sendMessage(
                ChatColor.GREEN + plugin.getLanguageManager().getMessage("putdown-success", entity.getName()));
        return true;
    }

    // --- 开始蓄力 (Start Charging) ---
    public void startCharging(Player player) {
        if (!isPlayerHoldingMob(player)) {
            return;
        }

        // 清理旧任务
        ChargeTask existingTask = chargingPlayers.get(player.getUniqueId());
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception e) {
            }
            chargingPlayers.remove(player.getUniqueId());
        }

        ChargeTask chargeTask = new ChargeTask(player);

        // Folia / Spigot 兼容调度
        try {
            // Folia
            Object task = player.getScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
                chargeTask.run();
            }, null, 1L, plugin.getConfigManager().getChargeIncrementTicks());
            chargingPlayers.put(player.getUniqueId(), chargeTask);
        } catch (Throwable e) {
            // Spigot
            try {
                chargeTask.runTaskTimer(plugin, 1L, plugin.getConfigManager().getChargeIncrementTicks());
                chargeTask.markScheduled();
                chargingPlayers.put(player.getUniqueId(), chargeTask);
            } catch (Exception ex) {
                plugin.getLogger().severe("无法启动蓄力任务: " + ex.getMessage());
            }
        }
    }

    // --- 停止蓄力并执行动作 (Stop & Launch/Drop) ---
    public void stopChargingAndLaunch(Player player) {
        ChargeTask chargeTask = chargingPlayers.get(player.getUniqueId());
        if (chargeTask == null) {
            return;
        }

        int chargePercent = chargeTask.getChargePercent();

        // 停止任务
        if (chargeTask.isScheduled()) {
            try {
                chargeTask.cancel();
            } catch (Throwable e) {
                try {
                    chargeTask.cancel();
                } catch (Exception ex) {
                }
            }
        }
        chargingPlayers.remove(player.getUniqueId());

        // 如果蓄力值为0（处于底部停顿期），则直接放下生物
        if (chargePercent <= 0) {
            putdownMob(player);
            return;
        }

        // --- 否则执行投掷逻辑 ---

        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null || !entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return;
        }

        unmarkMobAsMounted(entity);
        if (player.isValid()) {
            player.removePassenger(entity);
        }

        mountedMobs.remove(player.getUniqueId());

        // 计算向量
        Vector direction = player.getLocation().getDirection();
        double maxVelocity = plugin.getConfigManager().getMaxVelocity();
        double velocity = maxVelocity * (chargePercent / 100.0);
        Vector launchVelocity = direction.multiply(velocity);

        // 延迟 1 tick 应用速度 (兼容性修复)
        Runnable launchRunnable = () -> {
            if (entity.isValid()) {
                entity.setVelocity(launchVelocity);
            }
        };

        try {
            entity.getScheduler().runDelayed(plugin, (task) -> launchRunnable.run(), null, 1L);
        } catch (Throwable e) {
            Bukkit.getScheduler().runTaskLater(plugin, launchRunnable, 1L);
        }

        if (player.isValid()) {
            player.sendMessage(ChatColor.GREEN
                    + plugin.getLanguageManager().getMessage("launch-message", chargePercent, entity.getName()));
        }
    }

    // --- 辅助方法 ---

    public boolean isPlayerHoldingMob(Player player) {
        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null)
            return false;
        if (!entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return false;
        }
        return player.getPassengers().contains(entity);
    }

    public boolean isMobMounted(Entity entity) {
        if (entity == null || !entity.isValid())
            return false;

        PersistentDataContainer container = entity.getPersistentDataContainer();
        boolean isMarked = container.has(mobLaunchKey, PersistentDataType.BYTE);
        if (!isMarked)
            return false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getPassengers().contains(entity)) {
                return true;
            }
        }
        unmarkMobAsMounted(entity);
        cleanupMountedMobsMap(entity);
        return false;
    }

    private void cleanupMountedMobsMap(Entity entity) {
        UUID playerUUID = null;
        for (Map.Entry<UUID, Entity> entry : mountedMobs.entrySet()) {
            if (entry.getValue().equals(entity)) {
                playerUUID = entry.getKey();
                break;
            }
        }
        if (playerUUID != null) {
            mountedMobs.remove(playerUUID);
            ChargeTask chargeTask = chargingPlayers.get(playerUUID);
            if (chargeTask != null) {
                try {
                    chargeTask.cancel();
                } catch (Exception e) {
                }
                chargingPlayers.remove(playerUUID);
            }
        }
    }

    private void markMobAsMounted(Entity entity) {
        entity.getPersistentDataContainer().set(mobLaunchKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void unmarkMobAsMounted(Entity entity) {
        entity.getPersistentDataContainer().remove(mobLaunchKey);
    }

    public void removeAllMountedMobs() {
        for (Map.Entry<UUID, Entity> entry : mountedMobs.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Entity entity = entry.getValue();
            if (player != null && player.isOnline() && entity != null && entity.isValid()) {
                try {
                    player.removePassenger(entity);
                } catch (Exception e) {
                }
            }
            if (entity != null && entity.isValid()) {
                unmarkMobAsMounted(entity);
            }
        }
    }

    private boolean checkMobOwnership(Player player, Entity entity) {
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            PersistentDataContainer container = entity.getPersistentDataContainer();
            NamespacedKey ownerKey = new NamespacedKey(plugin, "MobLaunchOwner");
            if (container.has(ownerKey, PersistentDataType.STRING)) {
                String ownerUUID = container.get(ownerKey, PersistentDataType.STRING);
                if (!player.getUniqueId().toString().equals(ownerUUID)) {
                    if (!player.hasPermission("moblaunch.admin")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void setMobOwner(Entity entity, Player player) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        NamespacedKey ownerKey = new NamespacedKey(plugin, "MobLaunchOwner");
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
    }

    public void removeMobOwner(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        NamespacedKey ownerKey = new NamespacedKey(plugin, "MobLaunchOwner");
        container.remove(ownerKey);
    }

    // --- 蓄力任务类 (ChargeTask) ---
    private class ChargeTask extends BukkitRunnable {
        private final Player player;
        private int chargePercent = 0;
        private boolean isCancelled = false;
        private boolean isScheduled = false;

        // 状态机枚举
        private enum ChargeState {
            INCREASING, // 增加中
            MAX_PAUSE, // 满力停顿
            DECREASING, // 减少中
            ZERO_PAUSE // 零力停顿
        }

        private ChargeState currentState = ChargeState.INCREASING;
        private int pauseTicksCounter = 0; // 用于计算停顿时间

        public ChargeTask(Player player) {
            this.player = player;
            this.chargePercent = 0;
        }

        @Override
        public void run() {
            if (isCancelled)
                return;
            if (!isPlayerHoldingMob(player)) {
                cancel();
                return;
            }

            // 状态机逻辑
            switch (currentState) {
                case INCREASING:
                    chargePercent += 5; // 每次增加5%
                    if (chargePercent >= 100) {
                        chargePercent = 100;
                        currentState = ChargeState.MAX_PAUSE;
                        pauseTicksCounter = 0;
                    }
                    break;

                case MAX_PAUSE:
                    pauseTicksCounter++;
                    if (pauseTicksCounter >= plugin.getConfigManager().getPauseAtMaxTicks()) {
                        currentState = ChargeState.DECREASING;
                    }
                    break;

                case DECREASING:
                    chargePercent -= 5; // 每次减少5%
                    if (chargePercent <= 0) {
                        chargePercent = 0;
                        currentState = ChargeState.ZERO_PAUSE;
                        pauseTicksCounter = 0;
                    }
                    break;

                case ZERO_PAUSE:
                    pauseTicksCounter++;
                    if (pauseTicksCounter >= plugin.getConfigManager().getPauseAtZeroTicks()) {
                        currentState = ChargeState.INCREASING;
                    }
                    break;
            }

            displayChargeBar(player, chargePercent);
        }

        public int getChargePercent() {
            return chargePercent;
        }

        @Override
        public synchronized void cancel() {
            isCancelled = true;
            if (isScheduled) {
                try {
                    super.cancel();
                } catch (IllegalStateException e) {
                }
            }
        }

        public void markScheduled() {
            isScheduled = true;
        }

        public boolean isScheduled() {
            return isScheduled;
        }

        private void displayChargeBar(Player player, int percent) {
            // 特殊状态显示：当处于0停顿时，提示可以放下
            if (currentState == ChargeState.ZERO_PAUSE) {
                player.sendActionBar(ChatColor.GRAY + "[ " + ChatColor.YELLOW + "松开潜行放下生物" + ChatColor.GRAY + " ]");
                return;
            }

            int totalBars = 40;
            int filledBars = (int) (percent / 2.5);

            StringBuilder bar = new StringBuilder();

            // 根据状态改变颜色
            ChatColor fillColor = (currentState == ChargeState.INCREASING) ? ChatColor.GREEN : ChatColor.RED;
            if (currentState == ChargeState.MAX_PAUSE)
                fillColor = ChatColor.GOLD;

            bar.append(fillColor);
            for (int i = 0; i < filledBars; i++) {
                bar.append("|");
            }

            bar.append(ChatColor.WHITE);
            for (int i = filledBars; i < totalBars; i++) {
                bar.append("|");
            }

            player.sendActionBar(bar.toString());
        }
    }
}