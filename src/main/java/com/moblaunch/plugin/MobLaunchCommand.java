package com.moblaunch.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 插件命令处理器
 */
public class MobLaunchCommand implements CommandExecutor, TabCompleter {
    private final MobLaunch plugin;

    public MobLaunchCommand(MobLaunch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查基础权限
        if (!sender.hasPermission("moblaunch.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("no-permission-admin"));
            return true;
        }

        // 如果没有参数，显示帮助信息
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // 处理不同子命令
        switch (args[0].toLowerCase()) {
            case "version":
                sendVersionInfo(sender);
                break;
            case "reload":
                reloadConfig(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command-help-title"));
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 检查基础权限
        if (!sender.hasPermission("moblaunch.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("version".startsWith(args[0].toLowerCase())) {
                completions.add("version");
            }
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        
        return Collections.emptyList();
    }

    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + plugin.getLanguageManager().getMessage("command-help-title"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command-help-version"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command-help-reload"));
    }

    /**
     * 发送版本信息
     * @param sender 命令发送者
     */
    private void sendVersionInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + plugin.getLanguageManager().getMessage("command-version-title"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command-version-version"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command-version-author"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command-version-description"));
    }

    /**
     * 重载配置文件
     * @param sender 命令发送者
     */
    private void reloadConfig(CommandSender sender) {
        try {
            plugin.reloadConfig();
            plugin.getConfigManager().loadConfig();
            plugin.getLanguageManager().loadLanguage();
            sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command-reload-success"));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command-reload-failed", e.getMessage()));
            e.printStackTrace();
        }
    }
}