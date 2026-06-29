# 鼠鼠卡牌收集记录器

一款 Material Design 3 风格的 Android 扑克牌收集记录应用，帮助你追踪三角洲游戏 54 张扑克牌的收集进度。

## 功能特性

- **网格布局**：直观展示 A~K 共 13 种牌 + 王牌，每种牌显示 4 种花色（♠♥♦♣）
- **收集记录**：点击花色方框即可标记已收集状态
- **进度追踪**：顶部标题栏实时显示已收集数量（如 12/54 张）
- **花色高亮**：某张牌所有花色收集完毕后，边框自动变为绿色高亮
- **编辑模式**：右上角进入编辑模式，修改后需点击保存才会生效
- **数据导入导出**：支持 JSON 格式的数据备份与恢复
- **彩蛋**：54 张牌全部收集完成后弹出祝贺弹窗，显示总耗时（精确到天）
- **新手教程**：首次打开自动显示使用教程

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material Design 3
- **数据库**：SQLite
- **最低支持**：Android 13 (API 33)

## 项目结构

```
app/src/main/java/dm/app/card/fuck/df/
├── MainActivity.kt          # 主界面与业务逻辑
├── data/
│   └── CardDatabaseHelper.kt # SQLite 数据库管理
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 构建运行

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 连接设备或启动模拟器
4. 点击 Run 运行

```bash
git clone https://github.com/ax1742/card-fuck-df.git
```

## 作者

- **作者**：大美·格里尔斯
- **邮箱**：alexvictor17427@gmail.com

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件
