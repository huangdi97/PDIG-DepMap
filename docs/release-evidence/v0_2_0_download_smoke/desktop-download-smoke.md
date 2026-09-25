# v0.2.0 Desktop 下载后 smoke（Release attachment）
1. PDIG-0.2.0-windows-x64-portable.zip 下载自 GitHub Release product-v0.2.0，SHA256 MATCH。
2. 解压后运行 PDIG\PDIG.cmd（jlink runtime + javaw 引导）。
3. 实测：javaw pid=45140，窗口句柄 19864028，窗口标题 [PDIG 0.2.0 Preview] —— 真实 GUI 窗口。
4. 场景/计划/验证/备份恢复流程由 --smoke 16/16（同一 app jar）覆盖，连续 5 次全 PASS（G2 稳定性）。
5. setup.exe 同源（同一 app-image 由 NSIS 打包），安装树含 runtime\bin\java.exe/javaw.exe。
