# 空间传输塔检查

在项目根目录运行：

```powershell
./gradlew.bat -I tools/tower-check/init.gradle runTowerChecks --console=plain
```

8 项服务端 GameTest 覆盖多目标独立 IE/FE 额度、坐标和多方块去重、卸载与失效目标、满电与部分接收、能力缓存失效、目标列表保存及旧存档迁移，以及坐标仪记录／追加绑定／消耗规则。测试源集和测试用接收器不进入正常发布 JAR，运行数据写入 `build/tower-check/`。
