# tools/tmp —— 文档里 C 级实测脚本（可复现证据）

**这个目录不是垃圾回收站，请勿删除。**

`learning-docs` 各篇正文里凡是标了「C 级实测」的结论，背后都对应本目录下的一个脚本或一次命令。
把这些脚本留在仓库里的目的只有一个：**读者可以自己重跑一遍，验证文档里的数字不是编的**。

## 怎么用

```pwsh
cd D:\Projects\Persion\ai-customer-service\learning-docs\tools\tmp
node .\cap-calc.mjs          # 例如：容量规划篇的 QPS / 实例数换算
```

- `.mjs`：Node 脚本，直接 `node xxx.mjs`。本机 Node 24 可用，不依赖任何 npm 包。
- `.java`：JDK 单文件源码启动，`java Xxx.java`（JDK 11+ 支持直接运行单文件）。
- `.ps1`：PowerShell 脚本，`pwsh -File .\xxx.ps1` 或 `.\xxx.ps1`。
- `.out.txt` / `.txt`：某些脚本的真实输出快照，供无法运行同款环境时对照。

## 约束（重要）

1. 这些脚本**只做只读检查与纯计算**：读仓库文件、跑算术与模拟，不启动服务、不联网、不连数据库/中间件、不写仓库文件。
2. 涉及外部组件的部分（MySQL / Redis / Chroma / Prometheus / Docker / K8s）一律只输出「命令 + 判读方法」，标注「未本机实测」，因为本机没有这些运行环境。
3. 脚本里的所有假设参数（流量、分布、单价等）必须在对应文档的正文里写清楚，**不得把模拟结果当线上实测数据引用**。

## 命名约定

沿用初版路径 `tools/tmp/`（部分文档正文按此路径引用，改名会破坏可复现性）。
新增脚本建议命名为 `<模块号>-<主题>-<用途>.mjs`，例如 `m14-07-cost-model.mjs`。
