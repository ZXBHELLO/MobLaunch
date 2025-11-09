# MobLaunch Plugin

MobLaunch 是一个 Minecraft 服务器插件，允许玩家抱起生物并在蓄力后投掷出去。该插件支持 Folia、Spigot 和 Paper 服务器环境。

![Minecraft Version](https://img.shields.io/badge/Minecraft-Folia%2FSpigot%2FPaper-blue)
![License](https://img.shields.io/github/license/your-username/moblaunch)

## 功能特性

在这个奇妙的插件世界里，您将体验到以下精彩功能：

✨ **生物抱起** — 玩家可以抱起允许的生物类型，让生物乖乖待在您的头顶

⚡ **蓄力投掷** — 按住 SHIFT 键蓄力，松开时投掷生物，感受力量的释放

📊 **进度条显示** — 蓄力过程中在 ActionBar 显示进度条，让您精准掌握投掷时机

⏰ **超时自动放下** — 蓄力满后持续按住 SHIFT 键超过设定时间自动放下生物，贴心保护您的小伙伴

🌐 **多语言支持** — 支持多种语言，可自定义语言文件，让世界各地的玩家都能轻松上手

🛡️ **权限系统** — 细粒度权限控制，可控制玩家抱起特定生物的权限，服务器管理更轻松

⚙️ **可配置参数** — 支持配置生物白名单、蓄力速度、最大投掷速度等，打造属于您的独特玩法

👑 **生物所有权系统** — 玩家可以使用命名牌给生物命名来声明所有权，防止他人抱起，保护您的专属宠物

## 安装说明

1. 将 MobLaunch 插件 JAR 文件放入服务器的 `plugins` 文件夹
2. 启动服务器，插件会自动生成配置文件
3. 根据需要修改配置文件后重启服务器

## 使用方法

### 抱起生物

1. 确保拥有 `moblaunch.use` 权限
2. 潜行状态下（按住 SHIFT 键）
3. 空手右键点击允许的生物
4. 生物会出现在你的头部

### 蓄力和投掷

1. 抱起生物后，按住 SHIFT 键开始蓄力
2. ActionBar 会显示蓝色进度条
3. 蓄力满后进度条会保持显示
4. 松开 SHIFT 键，生物会根据蓄力程度被投掷出去

### 超时自动放下

1. 抱起生物后，按住 SHIFT 键开始蓄力
2. 蓄力满后继续按住 SHIFT 键
3. 如果超过设定时间（默认 1 秒），生物会自动放下

### 声明生物所有权

1. 获取一个命名牌并给它命名
2. 右键点击想要声明的生物
3. 该生物即被标记为你的私有生物
4. 只有你和管理员可以抱起这个生物

## 权限设置

| 权限节点 | 描述 | 默认值 |
|---------|------|--------|
| `moblaunch.use` | 使用插件基本功能的权限 | 所有玩家 |
| `moblaunch.use.*` | 抱起所有可抱起生物的权限（即使未配置） | OP |
| `moblaunch.use.[生物ID]` | 抱起特定生物的权限 | 无 |
| `moblaunch.admin` | 使用管理命令的权限 | OP |

## 配置说明

配置文件位于 `plugins/MobLaunch/config.yml`：

```yaml
allowed-mobs: # 可抱起的生物列表
  - "zombie"
  - "skeleton"
  - "creeper"
charge:
  increment-ticks: 1 # 每增加 5% 蓄力等级所需的 ticks 数
  auto-putdown-ticks: 20 # 蓄力条满后多少 tick 自动放下生物
launch:
  max-velocity: 2.0 # 最大投掷初速度
language: "zh_cn" # 语言设置
message-prefix: "&6[MobLaunch] " # 消息前缀
```

## 命令

| 命令 | 描述 |
|------|------|
| `/moblaunch version` | 显示插件版本信息 |
| `/moblaunch reload` | 重新加载配置文件 |

---
 
> 如果你想要改进这个项目，欢迎提交 PR！