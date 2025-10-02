# MobLaunch Plugin

[中文](#Moblaunch-插件) | [English](#Moblaunch-plugin)

---

## MobLaunch 插件

MobLaunch 是一个 Minecraft 服务器插件，允许玩家抱起生物并在蓄力后投掷出去。该插件支持 Folia、Spigot 和 Paper 服务器环境。

### 功能特性

- **生物抱起**：玩家可以抱起允许的生物类型
- **蓄力投掷**：按住 SHIFT 键蓄力，松开时投掷生物
- **进度条显示**：蓄力过程中在 ActionBar 显示进度条
- **超时自动放下**：蓄力满后持续按住 SHIFT 键超过设定时间自动放下生物
- **多语言支持**：支持多种语言，可自定义语言文件
- **权限系统**：细粒度权限控制，可控制玩家抱起特定生物的权限
- **可配置参数**：支持配置生物白名单、蓄力速度、最大投掷速度等
- **生物所有权系统**：玩家可以使用命名牌给生物命名来声明所有权，防止他人抱起

### 安装说明

1. 将 MobLaunch 插件 JAR 文件放入服务器的 `plugins` 文件夹
2. 启动服务器，插件会自动生成配置文件
3. 根据需要修改配置文件后重启服务器

### 使用方法

#### 抱起生物
1. 确保拥有 `moblaunch.use` 权限
2. 潜行状态下（按住 SHIFT 键）
3. 空手右键点击允许的生物
4. 生物会出现在你的头部

#### 蓄力和投掷
1. 抱起生物后，按住 SHIFT 键开始蓄力
2. ActionBar 会显示蓝色进度条
3. 蓄力满后进度条会保持显示
4. 松开 SHIFT 键，生物会根据蓄力程度被投掷出去

#### 超时自动放下
1. 抱起生物后，按住 SHIFT 键开始蓄力
2. 蓄力满后继续按住 SHIFT 键
3. 如果超过设定时间（默认 30 秒），生物会自动放下

#### 声明生物所有权
1. 获取一个命名牌并给它命名
2. 右键点击想要声明的生物
3. 该生物即被标记为你的私有生物
4. 只有你和管理员可以抱起这个生物

### 权限设置

- `moblaunch.use` - 使用插件基本功能的权限（默认：所有玩家）
- `moblaunch.use.*` - 抱起所有可抱起生物的权限（即使未配置）（默认：OP）
- `moblaunch.use.[生物ID]` - 抱起特定生物的权限（默认：无）
- `moblaunch.admin` - 使用管理命令的权限（默认：OP）

### 配置说明

配置文件位于 `plugins/MobLaunch/config.yml`：

- `allowed-mobs` - 可抱起的生物列表
- `charge.increment-ticks` - 每增加 5% 蓄力等级所需的 ticks 数
- `charge.auto-putdown-ticks` - 蓄力条满后多少 tick 自动放下生物
- `launch.max-velocity` - 最大投掷初速度
- `language` - 语言设置
- `message-prefix` - 消息前缀

### 命令

- `/moblaunch version` - 显示插件版本信息
- `/moblaunch reload` - 重新加载配置文件

---

## MobLaunch Plugin

MobLaunch is a Minecraft server plugin that allows players to pick up mobs and launch them after charging. This plugin supports Folia, Spigot, and Paper server environments.

### Features

- **Mob Pickup**: Players can pick up allowed mob types
- **Charge Launch**: Hold SHIFT key to charge, release to launch the mob
- **Progress Bar Display**: Progress bar displayed in ActionBar during charging
- **Auto Put Down**: Automatically put down mob after holding SHIFT for a set time when fully charged
- **Multi-language Support**: Supports multiple languages with customizable language files
- **Permission System**: Fine-grained permission control for picking up specific mobs
- **Configurable Parameters**: Supports configuring mob whitelist, charge speed, maximum launch velocity, etc.
- **Mob Ownership System**: Players can use name tags to claim ownership of mobs, preventing others from picking them up

### Installation Instructions

1. Place the MobLaunch plugin JAR file into the server's `plugins` folder
2. Start the server, and the plugin will automatically generate configuration files
3. Modify the configuration files as needed and restart the server

### Usage

#### Picking Up Mobs
1. Ensure you have the `moblaunch.use` permission
2. Sneak (hold SHIFT key)
3. Right-click on an allowed mob with an empty hand
4. The mob will appear above your head

#### Charging and Launching
1. After picking up a mob, hold SHIFT to start charging
2. ActionBar will display a blue progress bar
3. The progress bar will remain displayed when fully charged
4. Release SHIFT to launch the mob based on charge level

#### Auto Put Down
1. After picking up a mob, hold SHIFT to start charging
2. Continue holding SHIFT after fully charged
3. If held longer than the set time (default 30 seconds), the mob will be automatically put down

#### Claiming Mob Ownership
1. Obtain a name tag and give it a name
2. Right-click on the mob you want to claim
3. The mob is now marked as your private property
4. Only you and administrators can pick up this mob

### Permissions

- `moblaunch.use` - Permission to use basic plugin features (default: all players)
- `moblaunch.use.*` - Permission to pick up all mobs (even if not configured) (default: OP)
- `moblaunch.use.[mobID]` - Permission to pick up specific mobs (default: none)
- `moblaunch.admin` - Permission to use admin commands (default: OP)

### Configuration

Configuration file is located at `plugins/MobLaunch/config.yml`:

- `allowed-mobs` - List of mobs that can be picked up
- `charge.increment-ticks` - Number of ticks required to increase charge level by 5%
- `charge.auto-putdown-ticks` - Number of ticks after which mob is automatically put down when fully charged
- `launch.max-velocity` - Maximum launch initial velocity
- `language` - Language setting
- `message-prefix` - Message prefix

### Commands

- `/moblaunch version` - Show plugin version information
- `/moblaunch reload` - Reload configuration files
