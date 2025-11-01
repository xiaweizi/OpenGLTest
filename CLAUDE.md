# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

## 构建和运行

```bash
# 同步依赖
./gradlew build

# 运行到设备（需要连接 Android 设备或启动模拟器）
./gradlew installDebug

# 清理构建
./gradlew clean
```

**环境要求**：
- Gradle 8.0
- JDK 17
- Kotlin 1.9.0


## Git Commit Guidelines

- Commit message 使用中文
- 完成阶段性工作后及时创建 commit
- 格式：
  ```
  简短描述

  详细说明：
  - 变更点1
  - 变更点2

  Co-Authored-By: weizi <1012126908@qq.com>
  ```

