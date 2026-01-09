package com.moblaunch.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置管理器，用于处理插件配置
 */
public class ConfigManager {
    private final MobLaunch plugin;
    // 使用 Set 自动去重且查询更快
    private Set<EntityType> allowedMobs;
    private int chargeIncrementTicks;
    private int pauseAtMaxTicks; // 满蓄力停顿tick
    private int pauseAtZeroTicks; // 0蓄力停顿tick
    private double maxVelocity;
    private String messagePrefix;

    public ConfigManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.allowedMobs = new HashSet<>();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        config.options().copyDefaults(true);

        loadAllowedMobs(config);

        // 加载蓄力相关配置
        chargeIncrementTicks = config.getInt("charge.increment-ticks", 1);
        pauseAtMaxTicks = config.getInt("charge.pause-at-max-ticks", 10);
        pauseAtZeroTicks = config.getInt("charge.pause-at-zero-ticks", 10);

        // 移除了语言设置读取

        maxVelocity = config.getDouble("launch.max-velocity", 2.0);
        messagePrefix = config.getString("message-prefix", "&6[MobLaunch] ");

        plugin.saveConfig();
    }

    private void loadAllowedMobs(FileConfiguration config) {
        allowedMobs.clear();
        List<String> mobStrings = config.getStringList("allowed-mobs");

        if (mobStrings.isEmpty()) {
            mobStrings.add("PIG");
            mobStrings.add("COW");
            mobStrings.add("CHICKEN");
            mobStrings.add("SHEEP");
            config.set("allowed-mobs", mobStrings);
        }

        for (String mobString : mobStrings) {
            try {
                EntityType entityType = EntityType.valueOf(mobString.toUpperCase());
                allowedMobs.add(entityType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的生物类型: " + mobString);
            }
        }
    }

    public List<EntityType> getAllowedMobs() {
        return new ArrayList<>(allowedMobs);
    }

    public boolean isMobAllowed(EntityType entityType) {
        return allowedMobs.contains(entityType);
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

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }
}