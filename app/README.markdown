# LrcView - Android 歌词显示应用

LrcView 是一个 Android 应用，用于显示和播放 LRC 格式的歌词文件。该应用支持歌词高亮显示、进度控制、文件切换等功能。

## 功能特性

- 🎵 歌词显示与同步播放
- 🎯 当前歌词行高亮显示（红色粗体大字）
- ⏱️ 进度条控制播放进度
- 📂 多文件切换（上一首/下一首）
- 🖥️ 歌词自动滚动，当前行显示在屏幕1/3处
- 📱 现代化 Material Design 界面

## 界面预览

应用主界面包含以下元素：
- 歌曲标题显示区域
- 歌词显示区域
- 播放控制按钮（播放/暂停、上一首、下一首、返回）
- 进度条和时间显示（当前时间/总时间）

## 使用说明

1. 应用启动后会加载传入的歌词文件
2. 歌词会自动开始播放
3. 使用底部控制按钮：
   - ▶️/⏸️ 播放/暂停按钮控制播放状态
   - ⏮️ 上一首按钮切换到上一个歌词文件
   - ⏭️ 下一首按钮切换到下一个歌词文件
   - ↩️ 返回按钮退出当前界面
4. 拖动进度条可以跳转到指定时间位置

## 技术特点

- 使用 `SpannableString` 实现歌词样式（颜色、字体、大小）
- 通过 `ScrollView` 实现歌词自动滚动
- 支持 LRC 歌词文件解析
- 使用 `DocumentFile` API 处理文件访问
- 实现了完整的播放控制逻辑

## 主要组件

### LrcDetailActivity
主要的歌词显示和播放控制界面，包含以下功能：
- 歌词解析和显示
- 播放进度控制
- 歌词高亮和滚动
- 多文件切换

### LrcParser
歌词解析工具类，解析 LRC 格式歌词文件

### LrcLine
歌词行数据模型，包含时间和内容信息

## 安装要求

- Android 5.0 (API level 21) 或更高版本
- 需要存储权限以访问歌词文件

## 开发说明

该项目使用 Android Studio 开发，采用 Java 语言编写。

### 主要依赖
- AndroidX AppCompat
- DocumentFile API

### 项目结构
app/ ├── src/main/java/com/cch/LrcView/ │ ├── LrcDetailActivity.java // 主要活动 │ ├── LrcParser.java // 歌词解析器 │ └── LrcLine.java // 歌词行模型 └── src/main/res/ ├── layout/ │ └── activity_lrc_detail.xml // 界面布局 └── drawable/ └── 按钮图标资源

