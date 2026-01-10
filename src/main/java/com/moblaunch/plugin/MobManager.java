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

/**
 * 生物管理器
 * 更新：增加减少进度时的动态音效
 */
public class MobManager {
    private final MobLaunch plugin;
    private final Map<UUID, Entity> mountedMobs;
    private final Map<UUID, ChargeTask> chargingPlayers;
    private final NamespacedKey mobLaunchKey;
    private final NamespacedKey noFallKey;

    public MobManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.mountedMobs = new HashMap<>();
        this.chargingPlayers = new HashMap<>();
        this.mobLaunchKey = new NamespacedKey(plugin, "MobLaunchMounted");
        this.noFallKey = new NamespacedKey(plugin, "MobLaunchNoFall");
    }

    public NamespacedKey getNoFallKey() {
        return noFallKey;
    }

    // --- 播放音效辅助方法 ---
    private void playSound(Player player, String configPath) {
        playSound(player, configPath, -1);
    }

    private void playSound(Player player, String configPath, float overridePitch) {
        ConfigManager.SoundConfig soundConfig = plugin.getConfigManager().getSound(configPath);
        if (soundConfig.enabled && soundConfig.sound != null) {
            float finalPitch = (overridePitch != -1) ? overridePitch : soundConfig.pitch;
            player.playSound(player.getLocation(), soundConfig.sound, soundConfig.volume, finalPitch);
        }
    }

    // --- 抱起逻辑 (Pickup) ---
    public boolean pickupMob(Player player, Entity entity) {
        if (entity == null || !entity.isValid())
            return false;
        if (entity.getUniqueId().equals(player.getUniqueId()))
            return false;

        if (player.isInsideVehicle()) {
            player.sendMessage(
                    ChatColor.RED + plugin.getLanguageManager().getMessage("pickup-failed-vehicle", "你必须离开载具"));
            return false;
        }
        if (player.isGliding())
            return false;
        if (entity.isInsideVehicle()) {
            player.sendMessage(
                    ChatColor.RED + plugin.getLanguageManager().getMessage("pickup-failed-vehicle", "目标在载具内"));
            return false;
        }

        if (!player.hasPermission("moblaunch.use")) {
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("no-permission-use"));
            return false;
        }

        boolean hasWildcard = player.hasPermission("moblaunch.use.*");
        boolean hasSpecific = player.hasPermission("moblaunch.use." + entity.getType().name().toLowerCase());

        if (!hasWildcard && !hasSpecific) {
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

        MobPickupEvent event = new MobPickupEvent(player, entity);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        Runnable mountLogic = () -> {
            if (!player.isValid() || !entity.isValid())
                return;

            player.addPassenger(entity);
            markMobAsMounted(entity);
            mountedMobs.put(player.getUniqueId(), entity);

            playSound(player, "pickup");

            player.sendMessage(
                    ChatColor.GREEN + plugin.getLanguageManager().getMessage("pickup-success", entity.getName()));
        };

        try {
            entity.teleportAsync(player.getLocation()).thenAccept(success -> {
                if (success) {
                    try {
                        entity.getScheduler().run(plugin, (task) -> mountLogic.run(), null);
                    } catch (Throwable e) {
                        mountLogic.run();
                    }
                }
            });
        } catch (Throwable e) {
            entity.teleport(player.getLocation());
            mountLogic.run();
        }

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
        if (player.isValid())
            player.removePassenger(entity);
        mountedMobs.remove(player.getUniqueId());

        ChargeTask chargeTask = chargingPlayers.get(player.getUniqueId());
        if (chargeTask != null) {
            try {
                chargeTask.cancel();
            } catch (Exception e) {
            }
            chargingPlayers.remove(player.getUniqueId());
        }

        if (player.isValid()) {
            playSound(player, "putdown");
            player.sendMessage(
                    ChatColor.GREEN + plugin.getLanguageManager().getMessage("putdown-success", entity.getName()));
        }
        return true;
    }

    // --- 蓄力与投掷 ---
    public void startCharging(Player player) {
        if (!isPlayerHoldingMob(player))
            return;

        ChargeTask existing = chargingPlayers.get(player.getUniqueId());
        if (existing != null) {
            try {
                existing.cancel();
            } catch (Exception e) {
            }
            chargingPlayers.remove(player.getUniqueId());
        }

        ChargeTask chargeTask = new ChargeTask(player);
        int tickRate = plugin.getConfigManager().getChargeIncrementTicks();

        try {
            player.getScheduler().runAtFixedRate(plugin, (t) -> chargeTask.run(), null, 1L, tickRate);
            chargingPlayers.put(player.getUniqueId(), chargeTask);
        } catch (Throwable e) {
            chargeTask.runTaskTimer(plugin, 1L, tickRate);
            chargeTask.markScheduled();
            chargingPlayers.put(player.getUniqueId(), chargeTask);
        }
    }

    public void stopChargingAndLaunch(Player player) {
        ChargeTask task = chargingPlayers.get(player.getUniqueId());
        if (task == null)
            return;

        int percent = task.getChargePercent();
        try {
            task.cancel();
        } catch (Exception e) {
        }
        chargingPlayers.remove(player.getUniqueId());

        if (percent <= 0) {
            putdownMob(player);
            return;
        }

        Entity entity = mountedMobs.get(player.getUniqueId());
        if (entity == null || !entity.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        double chargeRatio = percent / 100.0;

        // 物理计算
        Vector lookDir = player.getLocation().getDirection();
        double speed = cfg.getVelocityMultiplier() * chargeRatio;
        Vector velocity = lookDir.multiply(speed);
        velocity.add(new Vector(0, cfg.getVerticalBias() * chargeRatio, 0));

        MobLaunchEvent event = new MobLaunchEvent(player, entity, velocity);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            putdownMob(player);
            return;
        }

        unmarkMobAsMounted(entity);
        if (player.isValid())
            player.removePassenger(entity);
        mountedMobs.remove(player.getUniqueId());

        Runnable launch = () -> {
            if (entity.isValid()) {
                if (cfg.isDisableFallDamage()) {
                    entity.getPersistentDataContainer().set(noFallKey, PersistentDataType.BYTE, (byte) 1);
                }

                entity.setVelocity(event.getVelocity());
                playSound(player, "launch");
            }
        };

        try {
            entity.getScheduler().runDelayed(plugin, (t) -> launch.run(), null, 1L);
        } catch (Throwable e) {
            Bukkit.getScheduler().runTaskLater(plugin, launch, 1L);
        }

        if (player.isValid()) {
            player.sendMessage(ChatColor.GREEN
                    + plugin.getLanguageManager().getMessage("launch-message", percent, entity.getName()));
        }
    }

    // --- 辅助方法 ---
    public boolean isPlayerHoldingMob(Player player) {
        Entity e = mountedMobs.get(player.getUniqueId());
        if (e == null)
            return false;
        if (!e.isValid()) {
            mountedMobs.remove(player.getUniqueId());
            return false;
        }
        return player.getPassengers().contains(e);
    }

    public boolean isMobMounted(Entity entity) {
        if (entity == null || !entity.isValid())
            return false;
        boolean isMarked = entity.getPersistentDataContainer().has(mobLaunchKey, PersistentDataType.BYTE);
        if (!isMarked)
            return false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getPassengers().contains(entity))
                return true;
        }
        unmarkMobAsMounted(entity);
        cleanupMountedMobsMap(entity);
        return false;
    }

    private void cleanupMountedMobsMap(Entity entity) {
        UUID pid = null;
        for (Map.Entry<UUID, Entity> en : mountedMobs.entrySet()) {
            if (en.getValue().equals(entity)) {
                pid = en.getKey();
                break;
            }
        }
        if (pid != null) {
            mountedMobs.remove(pid);
            ChargeTask t = chargingPlayers.get(pid);
            if (t != null) {
                try {
                    t.cancel();
                } catch (Exception e) {
                }
                chargingPlayers.remove(pid);
            }
        }
    }

    private void markMobAsMounted(Entity e) {
        e.getPersistentDataContainer().set(mobLaunchKey, PersistentDataType.BYTE, (byte) 1);
    }

    private void unmarkMobAsMounted(Entity e) {
        e.getPersistentDataContainer().remove(mobLaunchKey);
    }

    public void removeAllMountedMobs() {
        for (Map.Entry<UUID, Entity> en : mountedMobs.entrySet()) {
            Player p = Bukkit.getPlayer(en.getKey());
            Entity e = en.getValue();
            if (p != null && p.isOnline() && e != null && e.isValid())
                p.removePassenger(e);
            if (e != null && e.isValid())
                unmarkMobAsMounted(e);
        }
    }

    private boolean checkMobOwnership(Player p, Entity e) {
        if (e.getCustomName() == null)
            return true;
        NamespacedKey k = new NamespacedKey(plugin, "MobLaunchOwner");
        if (e.getPersistentDataContainer().has(k, PersistentDataType.STRING)) {
            String uuid = e.getPersistentDataContainer().get(k, PersistentDataType.STRING);
            if (!p.getUniqueId().toString().equals(uuid) && !p.hasPermission("moblaunch.admin"))
                return false;
        }
        return true;
    }

    public void setMobOwner(Entity e, Player p) {
        e.getPersistentDataContainer().set(new NamespacedKey(plugin, "MobLaunchOwner"), PersistentDataType.STRING,
                p.getUniqueId().toString());
    }

    // --- 蓄力任务 ---
    private class ChargeTask extends BukkitRunnable {
        private final Player player;
        private int chargePercent = 0;
        private boolean isCancelled = false;
        private boolean isScheduled = false;

        private enum State {
            INCREASING, MAX_PAUSE, DECREASING, ZERO_PAUSE
        }

        private State currentState = State.INCREASING;
        private int pauseTicks = 0;
        private final ConfigManager cfg;

        public ChargeTask(Player p) {
            this.player = p;
            this.cfg = plugin.getConfigManager();
        }

        @Override
        public void run() {
            if (isCancelled || !isPlayerHoldingMob(player)) {
                cancel();
                return;
            }

            int step = cfg.getChargeStep();

            switch (currentState) {
                case INCREASING:
                    chargePercent += step;
                    if (chargePercent >= 100) {
                        chargePercent = 100;
                        currentState = State.MAX_PAUSE;
                        pauseTicks = 0;
                        playSound(player, "max-charge");
                    } else {
                        // 蓄力音效：每10%播放一次，音调升高
                        if (chargePercent % (step * 2) == 0) {
                            ConfigManager.SoundConfig sc = cfg.getSound("charging");
                            if (sc.enabled) {
                                float dynPitch = sc.pitch + (chargePercent / 100.0f);
                                playSound(player, "charging", dynPitch);
                            }
                        }
                    }
                    break;
                case MAX_PAUSE:
                    pauseTicks++;
                    if (pauseTicks >= cfg.getPauseAtMaxTicks())
                        currentState = State.DECREASING;
                    break;
                case DECREASING:
                    chargePercent -= step;
                    if (chargePercent <= 0) {
                        chargePercent = 0;
                        currentState = State.ZERO_PAUSE;
                        pauseTicks = 0;
                        // 归零音效 (已配置)
                        playSound(player, "zero-charge");
                    } else {
                        // 新增：减少时的音效
                        if (chargePercent % (step * 2) == 0) {
                            ConfigManager.SoundConfig sc = cfg.getSound("decreasing");
                            if (sc.enabled) {
                                // 动态音调：随着百分比降低，音调也降低 (模拟泄气)
                                float dynPitch = sc.pitch + (chargePercent / 100.0f);
                                playSound(player, "decreasing", dynPitch);
                            }
                        }
                    }
                    break;
                case ZERO_PAUSE:
                    pauseTicks++;
                    if (pauseTicks >= cfg.getPauseAtZeroTicks())
                        currentState = State.INCREASING;
                    break;
            }
            if (cfg.isEnableActionBar()) {
                displayBar(player, chargePercent);
            }
        }

        public int getChargePercent() {
            return chargePercent;
        }

        @Override
        public synchronized void cancel() {
            isCancelled = true;
            if (isScheduled)
                try {
                    super.cancel();
                } catch (Exception e) {
                }
        }

        public void markScheduled() {
            isScheduled = true;
        }

        private void displayBar(Player p, int pct) {
            if (currentState == State.ZERO_PAUSE) {
                p.sendActionBar(ChatColor.GRAY + "[ " + ChatColor.YELLOW + "松开潜行放下生物" + ChatColor.GRAY + " ]");
                return;
            }
            int len = cfg.getBarLength();
            int filled = (int) (len * (pct / 100.0));

            StringBuilder sb = new StringBuilder();

            String color = cfg.getColorCharging();
            if (currentState == State.MAX_PAUSE)
                color = cfg.getColorFull();
            else if (currentState == State.DECREASING)
                color = cfg.getColorDecreasing();

            sb.append(ChatColor.translateAlternateColorCodes('&', color));
            String ch = cfg.getBarChar();

            for (int i = 0; i < filled; i++)
                sb.append(ch);
            sb.append(ChatColor.WHITE);
            for (int i = filled; i < len; i++)
                sb.append(ch);

            p.sendActionBar(sb.toString());
        }
    }
}