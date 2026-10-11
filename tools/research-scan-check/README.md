# 研究目录检查

```powershell
./gradlew.bat -I tools/research-scan-check/init.gradle runResearchScanChecks --console=plain
```

验证全部内置研究仅出现在 `lyy_research/`、现有研究 ID 不变、星辉等模组的通用 `research/` 不会被打开，以及旧目录兼容、自定义命名空间、嵌套路径和新目录优先级。测试源集不进入发布 JAR；运行数据在 `build/research-scan-check/`。
