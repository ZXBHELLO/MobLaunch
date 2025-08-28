package com.moblaunch.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理器，用于处理插件配置
 */
public class ConfigManager {
    private final MobLaunch plugin;
    private List<EntityType> allowedMobs;
    private int chargeIncrementTicks;
    private int autoPutdownTicks;
    private String language;

    public ConfigManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.allowedMobs = new ArrayList<>();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        // 保存默认配置文件
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        FileConfiguration config = plugin.getConfig();
        
        // 设置默认值
        config.options().copyDefaults(true);
        
        // 加载配置项
        loadAllowedMobs(config);
        chargeIncrementTicks = config.getInt("charge.increment-ticks", 1);
        autoPutdownTicks = config.getInt("charge.auto-putdown-ticks", 20);
        language = config.getString("language", "zh_cn");
        
        // 保存配置
        plugin.saveConfig();
    }

    /**
     * 加载允许的生物类型列表
     * @param config 配置文件
     */
    private void loadAllowedMobs(FileConfiguration config) {
        allowedMobs.clear();
        List<String> mobStrings = config.getStringList("allowed-mobs");
        
        // 如果配置为空，添加一些默认生物
        if (mobStrings.isEmpty()) {
            mobStrings.add("PIG");
            mobStrings.add("COW");
            mobStrings.add("CHICKEN");
            mobStrings.add("SHEEP");
            config.set("allowed-mobs", mobStrings);
        }
        
        // 将字符串转换为EntityType
        for (String mobString : mobStrings) {
            try {
                EntityType entityType = EntityType.valueOf(mobString.toUpperCase());
                allowedMobs.add(entityType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的生物类型: " + mobString);
            }
        }
    }

    /**
     * 获取允许的生物列表
     * @return 允许的生物列表
     */
    public List<EntityType> getAllowedMobs() {
        return new ArrayList<>(allowedMobs);
    }

    /**
     * 检查特定生物是否被允许
     * @param entityType 生物类型
     * @return 是否被允许
     */
    public boolean isMobAllowed(EntityType entityType) {
        return allowedMobs.contains(entityType);
    }

    /**
     * 获取蓄力增加所需ticks
     * @return ticks数
     */
    public int getChargeIncrementTicks() {
        return chargeIncrementTicks;
    }

    /**
     * 获取自动放下生物的ticks数
     * @return ticks数
     */
    public int getAutoPutdownTicks() {
        return autoPutdownTicks;
    }

    /**
     * 获取语言设置
     * @return 语言代码
     */
    public String getLanguage() {
        return language;
    }
}