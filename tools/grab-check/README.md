# 粉色抓取绳索接入

二阶段首领 `lyycore:endless_demand` 的抓取技能使用
`blockbench/pink_energy_rope/source/pink_energy_rope.bbmodel` 中的模型和动画。
在现有战斗中触发抓取即可看到，无需新增按键或前置模组。

- 飞行阶段：绳根跟随首领口部，绳头跟随服务端抓取实体，普通/快速追踪速度分别为 3/10 格/秒，保留原有命中判定。
- 命中阶段：播放 `grab_wrap`，等待 8 tick 完成缠绕，然后以最高 6 格/秒的速度拉回玩家，接近终点时限制位移以免越过终点。
- 拉回阶段：播放 `grab_pull`，绳圈跟随玩家腰部；24 节牵引骨骼按照真实距离收短，保留横截面厚度。
- 吞噬阶段：播放循环 `bound_idle`。
- 结束或中断：播放 `release`，4 tick 后清理实体；未命中或尚未缠绕完毕时只消散已有绳段。

贴图全亮；原始六段动画均随模组打包。预览中的目标位移动画由游戏中的真实目标位置替代，
避免重复位移。模型的 64 节绳圈、双螺旋细丝、绳头及碎光保留。抓取目标范围仍使用已有技能规则。
服务端同步施放者、目标、动画阶段及起始时间；实体移除、区块卸载或存档重载不会遗留永久抓取。

重新导入素材（项目根目录）：

```powershell
python tools/guiding-content/import_rope.py
```

验证（使用 Java 21）：

```powershell
./gradlew.bat build
./gradlew.bat -I tools/grab-check/init.gradle runGrabChecks runGrabRenderCheck --console=plain
```

测试源集不进入发布 jar。GameTest 覆盖命中、缠绕等待、拉拽、束缚、释放、创建失败、
实体移除、旧回调、区块卸载和重载。客户端检查使用 Minecraft 的实际材质和网格渲染路径，
输出 `build/rope-phases.png` 与 `build/rope-render-result.txt`；这是离屏渲染检查，不是多人实机录像。
