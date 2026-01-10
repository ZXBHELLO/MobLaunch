package com.moblaunch.plugin;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * 配置管理器 - 修复音效读取与物理参数
 */
public class ConfigManager {
    private final MobLaunch plugin;
    private Set<EntityType> allowedMobs;

    // 物理参数 (简化为力度+偏移，保证抛物线自然)
    private double velocityMultiplier;
    private double verticalBias;

    // 蓄力参数
    private int chargeStep;
    private int chargeIncrementTicks;
    private int pauseAtMaxTicks;
    private int pauseAtZeroTicks;

    // 开关
    private boolean disableFallDamage;
    private boolean consumeNametagCreative;
    private boolean enableActionBar;

    // 视觉
    private String barChar;
    private int barLength;
    private String colorCharging;
    private String colorFull;
    private String colorDecreasing;

    private String messagePrefix;

    public ConfigManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.allowedMobs = new HashSet<>();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);

        loadAllowedMobs(config);

        // 物理
        velocityMultiplier = config.getDouble("launch.velocity-multiplier", 1.8);
        verticalBias = config.getDouble("launch.vertical-bias", 0.3);

        // 蓄力
        chargeStep = config.getInt("charge.step-percentage", 5);
        chargeIncrementTicks = config.getInt("charge.increment-ticks", 1);
        pauseAtMaxTicks = config.getInt("charge.pause-at-max-ticks", 15);
        pauseAtZeroTicks = config.getInt("charge.pause-at-zero-ticks", 15);

        // 保护与开关
        disableFallDamage = config.getBoolean("protection.disable-fall-damage", true);
        consumeNametagCreative = config.getBoolean("protection.consume-nametag-creative", false);

        // 视觉
        enableActionBar = config.getBoolean("visuals.enable-action-bar", true);
        barChar = config.getString("visuals.bar-char", "|");
        barLength = config.getInt("visuals.bar-length", 40);
        colorCharging = config.getString("visuals.color-charging", "&a");
        colorFull = config.getString("visuals.color-full", "&6");
        colorDecreasing = config.getString("visuals.color-decreasing", "&c");

        messagePrefix = config.getString("message-prefix", "&6[MobLaunch] ");

        plugin.saveConfig();
    }

    private void loadAllowedMobs(FileConfiguration config) {
        allowedMobs.clear();
        List<String> mobStrings = config.getStringList("allowed-mobs");
        if (mobStrings.isEmpty()) {
            mobStrings.add("PIG");
            config.set("allowed-mobs", mobStrings);
        }
        for (String mobString : mobStrings) {
            try {
                allowedMobs.add(EntityType.valueOf(mobString.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的生物类型: " + mobString);
            }
        }
    }

    // --- 音效读取辅助类 ---
    public static class SoundConfig {
        public Sound sound;
        public float volume;
        public float pitch;
        public boolean enabled;

        public SoundConfig(Sound sound, double volume, double pitch, boolean enabled) {
            this.sound = sound;
            this.volume = (float) volume;
            this.pitch = (float) pitch;
            this.enabled = enabled;
        }
    }

    public SoundConfig getSound(String path) {
        FileConfiguration config = plugin.getConfig();

        // 1. 检查是否启用
        if (!config.getBoolean("sounds." + path + ".enabled", true)) {
            return new SoundConfig(null, 0, 0, false);
        }

        String soundName = config.getString("sounds." + path + ".sound", "none");
        if ("none".equalsIgnoreCase(soundName)) {
            return new SoundConfig(null, 0, 0, false);
        }

        Sound sound = null;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 2. 如果音效名无效，打印警告，方便排查
            plugin.getLogger().log(Level.WARNING, "配置文件中的音效名称无效: " + soundName + " (路径: sounds." + path + ")");
            plugin.getLogger().log(Level.WARNING, "请检查拼写或服务器版本支持的音效列表。");
            return new SoundConfig(null, 0, 0, false);
        }

        double vol = config.getDouble("sounds." + path + ".volume", 1.0);
        double pit = config.getDouble("sounds." + path + ".pitch", 1.0);

        return new SoundConfig(sound, vol, pit, true);
    }

    // Getters
    public List<EntityType> getAllowedMobs() {
        return new ArrayList<>(allowedMobs);
    }

    public boolean isMobAllowed(EntityType type) {
        return allowedMobs.contains(type);
    }

    public double getVelocityMultiplier() {
        return velocityMultiplier;
    }

    public double getVerticalBias() {
        return verticalBias;
    }

    public int getChargeStep() {
        return chargeStep;
    }

    public int getChargeIncrementTicks() {
        return chargeIncrementTicks;
    }

    public int getPauseAtMaxTicks() {
        return pauseAtMaxTicks;
    }

    public int getPauseAtZeroTicks() {
        return pauseAtZeroTicks;
    }

    public boolean isDisableFallDamage() {
        return disableFallDamage;
    }

    public boolean isConsumeNametagCreative() {
        return consumeNametagCreative;
    }

    public boolean isEnableActionBar() {
        return enableActionBar;
    }

    public String getBarChar() {
        return barChar;
    }

    public int getBarLength() {
        return barLength;
    }

    public String getColorCharging() {
        return colorCharging;
    }

    public String getColorFull() {
        return colorFull;
    }

    public String getColorDecreasing() {
        return colorDecreasing;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }
}