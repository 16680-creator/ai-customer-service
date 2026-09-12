# OrcaTerm 内置模型 → Trae CN 桥接

把腾讯云 **OrcaTerm 桌面版** 内置的 AI 模型（限时免费）接入 **Trae CN** 的自定义模型，供 IDE 内直接使用。

## 原理

OrcaTerm 的内置模型没有公开 API，本桥接在本地起一个 **OpenAI 兼容服务**：

```
Trae CN ──(OpenAI 协议)──> 桥接服务 127.0.0.1:8317 ──(CDP 驱动 UI)──> OrcaTerm Agent 面板 ──> 腾讯云内置模型
```

桥接通过 WebView2 远程调试协议(CDP) 自动完成：新建会话 → 切换模型 → 注入 Prompt → 发送 → 等待回答完成 → 提取回答文本，并转换为 OpenAI 响应格式（支持流式）。内置自愈：OrcaTerm 未运行时自动带调试端口重启、页面崩溃自动重载、发送失败自动重试。

## 使用步骤

1. **启动桥接**（保持 OrcaTerm 已登录腾讯云账号）：
   ```
   orcaterm-trae-bridge\start.bat
   ```
   首次会自动重启 OrcaTerm 以开启调试端口 9333。

2. **Trae CN 已添加的自定义模型**（本方案已自动写入，设置 → 模型管理 可见）：

   | 模型 ID | 展示名称 |
   |---|---|
   | hy4-preview | OrcaTerm Hy4-preview |
   | hy3 | OrcaTerm Hy3 |
   | kimi-k3 | OrcaTerm Kimi-K3 |
   | glm-5.3 | OrcaTerm GLM-5.3 |
   | glm-5.3-flash | OrcaTerm GLM-5.3-Flash |
   | glm-5.2 | OrcaTerm GLM-5.2 |
   | deepseek-v4-flash | OrcaTerm DeepSeek-V4-Flash |
   | deepseek-v4-pro | OrcaTerm DeepSeek-V4-Pro |

   配置均为：服务商 `自定义(OpenAI Compatible)`、地址 `http://127.0.0.1:8317/v1`、API Key `orcaterm-local`（桥接不校验）。

3. 在 Trae 聊天面板的模型选择器里选任一 **OrcaTerm *** 模型即可对话。

## 手动添加模型（如需重建）

Trae 设置 → 模型 → 添加模型 → 自定义模型：
- API 格式：OpenAI Chat Completions
- 完整 URL：`http://127.0.0.1:8317/v1`
- 模型 ID / 展示名称：按上表填写
- API 密钥：任意值

> 注意：Trae 的"测试并添加"会真实调用一次（走桥接→OrcaTerm 全链路，约 30~60 秒），也可点"直接保存"跳过。

## 验证

```bash
curl http://127.0.0.1:8317/health
curl http://127.0.0.1:8317/v1/models
curl -X POST http://127.0.0.1:8317/v1/chat/completions ^
  -H "Content-Type: application/json" ^
  -d "{\"model\":\"glm-5.3\",\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}"
```

## 文件说明

| 文件 | 用途 |
|---|---|
| `bridge.py` | 桥接服务主程序（OpenAI 兼容 HTTP + CDP 面板自动化） |
| `start.bat` | 一键启动 |
| `trae_add_models.py` | 通过 CDP 批量把模型写入 Trae（设置→模型管理→添加模型） |
| `add_trae_models.py` | （备用）直接写 state.vscdb 的脚本——已验证会被云端同步覆盖，不推荐 |

## 已知限制

- 桥接依赖 OrcaTerm 登录态；Token 约 4 小时过期，OrcaTerm 会自动续期，若对话报权限错误，重新打开 OrcaTerm 登录即可。
- 每次请求串行（同一时刻只处理一个对话），回答延迟约 20~60 秒（取决于模型思考时间）。
- 模型菜单与界面类名可能随 OrcaTerm/Trae 版本更新变化，届时需同步调整 `bridge.py` 中的 JS 选择器。
