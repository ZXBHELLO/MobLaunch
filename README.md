# MobLaunch

> 专为 Folia、Paper 和 Spigot 设计的高性能实体投掷插件。利用异步传送与区域调度器（Region Scheduler）确保线程安全，核心采用循环蓄力状态机机制。

![Platform](https://img.shields.io/badge/Platform-Folia%20%7C%20Paper%20%7C%20Spigot-blue)
![License](https://img.shields.io/badge/License-Apache_2.0-green)

## 核心特性

*   **Folia 原生支持**：基于 `teleportAsync` 和区域线程模型，彻底解决跨区块交互时的 UUID 冲突与主线程阻塞。
*   **循环蓄力系统**：采用状态机逻辑（增长 ➔ 满力停顿 ➔ 衰减 ➔ 零力停顿）。
*   **双空手检测**：强制要求主副手均为空，防止交互误触。
*   **安全放下**：在蓄力归零的窗口期松开按键，可温柔放下生物而不投掷。
*   **所有权保护**：基于 NBT 的命名牌绑定机制，防止非法操作私有生物。
*   **开发者 API**：提供标准 Bukkit 事件，支持第三方领地/城镇插件拦截。

## 操作指南

### 1. 抱起
*   **条件**：拥有权限，且**双手（主/副）均为空**。
*   **操作**：`Shift` + `右键` 目标生物。

### 2. 蓄力与投掷
抱起后**持续按住 Shift** 进入循环，根据 ActionBar 提示操作：
1.  🟩 **增长**：力度上升。松开即投掷。
2.  🟨 **满力**：力度 100% 保持。
3.  🟥 **衰减**：力度下降。
4.  ⬜ **放下**：力度归零。**此时松开 Shift 将安全放下生物。**

### 3. 声明所有权
使用经过命名的**命名牌**右键生物，即可绑定所有权，阻止他人抱起。

## 配置与权限

插件启动后自动生成 `config.yml` (参数) 和 `lang.yml` (消息)。

| 权限节点          | 描述                         |
| :---------------- | :--------------------------- |
| `moblaunch.use`   | 基础使用权限 (受白名单限制)  |
| `moblaunch.use.*` | 忽略白名单限制               |
| `moblaunch.admin` | 管理员权限 (重载/无视所有权) |

**命令：**
*   `/moblaunch reload` - 重载配置
*   `/moblaunch version` - 查看版本

## 开发者 API

事件位于 `com.moblaunch.plugin` 包下，均实现了 `Cancellable` 接口：

*   **`MobPickupEvent`**: 玩家尝试抱起生物时触发。可用于领地检查。
*   **`MobLaunchEvent`**: 投掷前触发。支持修改速度向量 (`setVelocity`) 或取消投掷。

---
> 欢迎提交 PR 改进本项目！