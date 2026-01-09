package com.moblaunch.plugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * 语言管理器 (单文件版 - 颜色修复)
 */
public class LanguageManager {
    private final MobLaunch plugin;
    private YamlConfiguration languageConfig;
    private final String LANG_FILE_NAME = "lang.yml";

    public LanguageManager(MobLaunch plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    /**
     * 加载语言文件
     */
    public void loadLanguage() {
        File languageFile = new File(plugin.getDataFolder(), LANG_FILE_NAME);

        // 如果 lang.yml 不存在，从 jar 包中释放
        if (!languageFile.exists()) {
            plugin.saveResource(LANG_FILE_NAME, false);
        }

        // 加载配置
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // 如果文件加载失败，尝试加载 jar 包内的默认文件作为后备
        if (languageConfig.getKeys(false).isEmpty()) {
            InputStream defConfigStream = plugin.getResource(LANG_FILE_NAME);
            if (defConfigStream != null) {
                languageConfig = YamlConfiguration
                        .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
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
            String version = plugin.getDescription().getVersion();
            String author = String.join(", ", plugin.getDescription().getAuthors());

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
     * 获取消息
     * 
     * @param key    消息键
     * @param params 参数
     * @return 格式化后的消息
     */
    public String getMessage(String key, Object... params) {
        if (languageConfig == null) {
            return key;
        }

        String message = languageConfig.getString(key, key);

        // 修复：MessageFormat 默认会吃掉单引号，需要转义
        if (message != null) {
            message = message.replace("'", "''");
        }

        // 获取消息前缀 (这里先不转颜色，最后统一转)
        String prefix = plugin.getConfigManager().getMessagePrefix();

        // 组合最终字符串
        String result;
        if (params.length > 0) {
            result = prefix + MessageFormat.format(message, params);
        } else {
            result = prefix + message;
        }

        // 修复：最后对【整个字符串】进行颜色代码转换 (& -> §)
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}