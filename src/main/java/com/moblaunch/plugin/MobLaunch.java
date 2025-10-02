package com.moblaunch.plugin;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * MobLaunch 插件主类
 * 允许玩家抱起生物并在蓄力后抛出
 */
public class MobLaunch extends JavaPlugin {

    private static MobLaunch instance;
    private MobManager mobManager;
    private ConfigManager configManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 重新加载语言配置
        languageManager.loadLanguage();
        
        // 初始化生物管理器
        mobManager = new MobManager(this);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 注册命令
        MobLaunchCommand mobLaunchCommand = new MobLaunchCommand(this);
        this.getCommand("moblaunch").setExecutor(mobLaunchCommand);
        this.getCommand("moblaunch").setTabCompleter(mobLaunchCommand);
        
        getLogger().info("MobLaunch 插件已启用!");
    }

    @Override
    public void onDisable() {
        // 清理被抱起的生物
        if (mobManager != null) {
            mobManager.removeAllMountedMobs();
        }
        getLogger().info("MobLaunch 插件已禁用!");
    }
    
    /**
     * 获取插件实例
     * @return MobLaunch实例
     */
    public static MobLaunch getInstance() {
        return instance;
    }
    
    /**
     * 获取生物管理器
     * @return MobManager实例
     */
    public MobManager getMobManager() {
        return mobManager;
    }
    
    /**
     * 获取配置管理器
     * @return ConfigManager实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取语言管理器
     * @return LanguageManager实例
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}