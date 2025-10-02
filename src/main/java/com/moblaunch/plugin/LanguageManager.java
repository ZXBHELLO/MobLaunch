package com.moblaunch.plugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 语言管理器
 */
public class LanguageManager {
    private final MobLaunch plugin;
    private YamlConfiguration languageConfig;
    private String currentLanguage;

    public LanguageManager(MobLaunch plugin) {
        this.plugin = plugin;
        this.currentLanguage = "en_us"; // 默认语言设为英语
        loadLanguage();
    }

    /**
     * 加载语言文件
     */
    public void loadLanguage() {
        // 获取配置的语言
        currentLanguage = plugin.getConfig().getString("language", "en_us");
        
        // 确保lang目录存在并保存默认语言文件
        saveDefaultLanguageFiles();
        
        // 加载语言配置
        File languageFile = new File(plugin.getDataFolder(), "lang/" + currentLanguage + ".yml");
        if (languageFile.exists()) {
            languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        } else {
            // 如果指定的语言文件不存在，使用默认的英语文件
            InputStream defConfigStream = plugin.getResource("lang/en_us.yml");
            if (defConfigStream != null) {
                languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            }
        }
        
        // 替换版本和作者占位符
        replacePlaceholders();
    }

    /**
     * 替换语言配置中的占位符
     */
    private void replacePlaceholders() {
        if (languageConfig != null) {
            // 获取插件的版本和作者信息
            String version = plugin.getDescription().getVersion();
            String author = String.join(", ", plugin.getDescription().getAuthors());
            
            // 遍历所有键值对，替换包含占位符的值
            for (String key : languageConfig.getKeys(true)) {
                if (languageConfig.isString(key)) {
                    String value = languageConfig.getString(key);
                    if (value != null && (value.contains("{version}") || value.contains("{author}"))) {
                        value = value.replace("{version}", version);
                        value = value.replace("{author}", author);
                        languageConfig.set(key, value);
                    }
                }
            }
        }
    }

    /**
     * 保存默认语言文件
     */
    private void saveDefaultLanguageFiles() {
        // 确保lang目录存在
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // 保存默认的中文语言文件
        File zhCnFile = new File(langDir, "zh_cn.yml");
        if (!zhCnFile.exists()) {
            plugin.saveResource("lang/zh_cn.yml", false);
        }
        
        // 保存默认的英文语言文件
        File enUsFile = new File(langDir, "en_us.yml");
        if (!enUsFile.exists()) {
            plugin.saveResource("lang/en_us.yml", false);
        }
    }

    /**
     * 获取消息
     * @param key 消息键
     * @param params 参数
     * @return 格式化后的消息
     */
    public String getMessage(String key, Object... params) {
        if (languageConfig == null) {
            return key;
        }
        
        String message = languageConfig.getString(key, key);
        // 添加消息前缀
        String prefix = plugin.getConfigManager().getMessagePrefix();
        prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        
        if (params.length > 0) {
            return prefix + MessageFormat.format(message, params);
        }
        return prefix + message;
    }

    /**
     * 获取语言代码
     * @return 语言代码
     */
    /**
     * 获取当前语言
     * @return 当前语言代码
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}