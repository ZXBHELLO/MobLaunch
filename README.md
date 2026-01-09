# MobLaunch Plugin

> MobLaunch 是一个专为 Folia、Paper 和 Spigot 设计的轻量级实体投掷插件。它利用异步传送和区域调度器（Region Scheduler）确保在高版本服务端（如 Folia）上的稳定性，并引入了循环蓄力状态机机制。

![Minecraft Version](https://img.shields.io/badge/Minecraft-Folia%2FSpigot%2FPaper-blue)
![License](https://img.shields.io/github/license/your-username/moblaunch)



## 核心特性

*   **Folia 原生支持**：使用 `teleportAsync` 和区域线程调度，解决实体跨区块交互时的并发问题和 UUID 冲突。
*   **循环蓄力系统**：蓄力过程采用状态机逻辑（增加 -> 满力停顿 -> 减少 -> 零力停顿）。
*   **双空手检测**：为防止误操作，抱起和蓄力动作强制要求主副手均为空。
*   **安全放下机制**：在蓄力循环归零的停顿期间松开按键，可取消投掷并放下生物。
*   **所有权保护**：基于 NBT/PDC 的命名牌所有权绑定，防止非拥有者操作特定生物。

## 安装

1.  将 JAR 文件放入 `plugins` 目录。
2.  启动服务器生成默认配置。
3.  配置文件包含 `config.yml` (参数设置) 和 `lang.yml` (语言文件)。

## 操作指南

### 1. 抱起生物
*   **前提**：拥有 `moblaunch.use` 权限，且主副手均为空。
*   **操作**：按住 `Shift` + `右键` 目标生物。
*   **逻辑**：插件会异步将生物传送至玩家位置以同步区块数据，随即执行骑乘逻辑。

### 2. 蓄力与投掷
抱起生物后，**持续按住 Shift** 进入循环蓄力：

1.  **增长阶段**：力度从 0% 升至 100%。
2.  **满力停顿**：保持 100% 力度（时长可配）。
3.  **衰减阶段**：力度从 100% 降至 0%。
4.  **零力停顿**：保持 0% 力度（时长可配）。**此时松开 Shift 将执行"放下"操作。**

*注：ActionBar 会实时显示当前阶段与进度。*

## 配置说明

### config.yml
```yaml
allowed-mobs:           # 允许操作的生物类型白名单
  - PIG
  - VILLAGER

charge:
  increment-ticks: 1      # 状态更新频率 (Ticks)
  pause-at-max-ticks: 10  # 满力停顿窗口 (Ticks)
  pause-at-zero-ticks: 10 # 零力停顿窗口 (Ticks，此窗口内松开为放下)

launch:
  max-velocity: 2.0       # 满力时的最大标量速度
```

## 权限节点

| 权限                   | 描述                                      |
| :--------------------- | :---------------------------------------- |
| `moblaunch.use`        | 基础使用权限 (受白名单限制)               |
| `moblaunch.use.*`      | 忽略白名单限制                            |
| `moblaunch.use.<type>` | 特定生物操作权限 (如 `moblaunch.use.pig`) |
| `moblaunch.admin`      | 管理权限 (重载配置、操作他人保护的生物)   |

## 命令

*   `/moblaunch reload` - 重载配置文件
*   `/moblaunch version` - 查看版本信息


> 如果你想要改进这个项目，欢迎提交 PR！