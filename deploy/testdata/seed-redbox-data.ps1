#Requires -Version 5.1
<#
.SYNOPSIS
  红框六菜单（知识库管理/全文搜索/Prompt管理/售后Agent/知识图谱/RAG评估）测试数据一键种子脚本。
.DESCRIPTION
  通过网关 API 写入：
    1) 知识文档 2001~2006（经 /api/knowledge 创建，RocketMQ 自动向量化到 knowledge 库；已存在则走更新触发重新向量化）
    2) 售后规则 ASR-001/002/003 文本（入 knowledge 向量库，供售后 Agent 规则检索）
    3) 全文搜索索引文档（knowledge 索引 3 条 + faq-test 索引 2 条，含创建 faq-test 索引）
    4) 知识图谱三元组 6 条（含「退款政策」两跳链）
  可重复执行；单条失败仅告警不中断。
.PARAMETER Gateway
  网关地址，默认 http://localhost:8080
.PARAMETER Username / Password
  登录账号，默认 admin / admin123
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File deploy\testdata\seed-redbox-data.ps1
#>
param(
    [string]$Gateway = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

$ErrorActionPreference = "Continue"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )
    $p = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
        $p.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
    }
    return Invoke-RestMethod @p
}

function Write-Step([string]$msg) { Write-Host "`n== $msg ==" -ForegroundColor Cyan }
function Write-Ok([string]$msg) { Write-Host "   [OK] $msg" -ForegroundColor Green }
function Write-Warn2([string]$msg) { Write-Host "   [跳过/告警] $msg" -ForegroundColor Yellow }

# ==================== 0) 登录获取 Token ====================
Write-Step "登录 $Gateway（$Username）"
try {
    $login = Invoke-Api -Method POST -Url "$Gateway/api/user/login" -Body @{ username = $Username; password = $Password }
    if ($login.code -ne 200) { throw "登录失败: $($login.message)" }
    $token = $login.data.token
    $H = @{ Authorization = "Bearer $token" }
    Write-Ok "登录成功"
}
catch {
    Write-Host "登录失败，请确认网关与 user 服务已启动: $_" -ForegroundColor Red
    exit 1
}

# ==================== 1) 知识库管理：知识文档 2001~2006 ====================
Write-Step "1) 知识库管理：创建知识文档 2001~2006（自动向量化）"
$kbDocs = @(
    @{ id = 2001; title = "型号 ABC-123 保修期说明"; content = "型号 ABC-123 保修期为 1 年，自签收之日起计算。保修期内非人为损坏可享受免费维修服务，人为损坏或超期维修需收取配件费用。"; docType = "txt"; tags = "保修,售后" },
    @{ id = 2002; title = "订单退款申请流程"; content = "订单怎么申请退款？进入订单详情页点击申请退款，填写退款原因提交即可。审核通过后退款 1-3 个工作日原路退回。"; docType = "txt"; tags = "退款,订单" },
    @{ id = 2003; title = "退货运费承担规则"; content = "运费由谁承担？非质量问题退货运费由买家承担；质量问题退货运费由卖家承担。"; docType = "txt"; tags = "运费,退货" },
    @{ id = 2004; title = "平台支持的支付方式"; content = "支持哪些支付方式？支持微信、支付宝、银行卡支付，当前不支持货到付款。"; docType = "txt"; tags = "支付" },
    @{ id = 2005; title = "发货与送达时效"; content = "发货后多久能收到？现货商品支付成功后 24 小时内发货，一般 3-5 个工作日送达。"; docType = "txt"; tags = "发货,物流" },
    @{ id = 2006; title = "修改收货地址规则"; content = "如何修改收货地址？在订单详情页修改收货地址，发货前可改；已发货需联系客服协调改址。"; docType = "txt"; tags = "地址,订单" }
)
foreach ($d in $kbDocs) {
    try {
        Invoke-Api -Method POST -Url "$Gateway/api/knowledge" -Body $d -Headers $H | Out-Null
        Write-Ok "创建文档 $($d.id) $($d.title)"
    }
    catch {
        try {
            Invoke-Api -Method PUT -Url "$Gateway/api/knowledge" -Body $d -Headers $H | Out-Null
            Write-Ok "文档 $($d.id) 已存在，更新并触发重新向量化"
        }
        catch { Write-Warn2 "文档 $($d.id) 写入失败: $_" }
    }
}

# ==================== 2) 售后规则文本入 knowledge 向量库 ====================
Write-Step "2) 售后 Agent 规则：ASR-001/002/003 入 knowledge 向量库"
$asrTexts = @(
    "条款编号：ASR-001；适用动作：换货（EXCHANGE）；适用商品：耳机类商品；条件：商品存在非人为质量问题（如无法开机、声音异常）；期限：自签收之日起 15 天内可申请换货；流程：用户提交换货申请，附质量问题描述。",
    "条款编号：ASR-002；适用动作：退货（RETURN）；适用商品：耳机类商品；条件：商品完好、配件齐全、不影响二次销售；期限：自签收之日起 7 天内无理由退货；流程：用户提交退货申请，退货完成后退款。",
    "条款编号：ASR-003；适用动作：退款（REFUND）；条件：退货完成或订单取消后进入退款；期限：退货确认完成后 3 个工作日内原路退款。"
)
foreach ($t in $asrTexts) {
    try {
        $u = "$Gateway/api/rag/knowledge-base/text?knowledgeBase=knowledge&text=$([uri]::EscapeDataString($t))"
        Invoke-Api -Method POST -Url $u -Headers $H | Out-Null
        Write-Ok "规则入库: $($t.Substring(5, 7))"
    }
    catch { Write-Warn2 "规则入库失败: $_" }
}

# ==================== 3) 全文搜索：索引与索引文档 ====================
Write-Step "3) 全文搜索：创建 faq-test 索引并写入索引文档"
try {
    Invoke-Api -Method POST -Url "$Gateway/api/search/index/faq-test" -Body @{} -Headers $H | Out-Null
    Write-Ok "索引 faq-test 创建成功"
}
catch { Write-Warn2 "索引 faq-test 创建失败（可能已存在）: $_" }

$searchDocs = @(
    @{ index = "knowledge"; doc = @{ title = "退款到账时效"; content = "退款审核通过后 1-3 个工作日到账，原路退回支付账户。"; tags = "退款,到账" } },
    @{ index = "knowledge"; doc = @{ title = "保修政策摘要"; content = "型号 ABC-123 保修期为 1 年，保修期内非人为损坏免费维修。"; tags = "保修" } },
    @{ index = "knowledge"; doc = @{ title = "物流查询方式"; content = "订单详情页可查看实时物流轨迹，物流超 48 小时未更新可联系客服催件。"; tags = "物流" } },
    @{ index = "faq-test"; doc = @{ title = "优惠券使用规则"; content = "优惠券不可叠加使用，每笔订单限用一张，需满足满减门槛。"; tags = "优惠券" } },
    @{ index = "faq-test"; doc = @{ title = "客服工作时间"; content = "人工客服工作时间为每日 9:00-21:00，节假日无休。"; tags = "客服,人工" } }
)
foreach ($s in $searchDocs) {
    try {
        Invoke-Api -Method POST -Url "$Gateway/api/search/document/$($s.index)" -Body $s.doc -Headers $H | Out-Null
        Write-Ok "[$($s.index)] 索引文档: $($s.doc.title)"
    }
    catch { Write-Warn2 "[$($s.index)] 索引文档失败 $($s.doc.title): $_" }
}

# ==================== 4) 知识图谱：三元组 ====================
Write-Step "4) 知识图谱：写入三元组 6 条"
$triples = @(
    @{ subject = "退款政策"; predicate = "指向"; object = "订单详情页申请入口" },
    @{ subject = "订单详情页申请入口"; predicate = "适用于"; object = "已支付订单" },
    @{ subject = "耳机类商品"; predicate = "适用"; object = "ASR-001 换货规则" },
    @{ subject = "ASR-001 换货规则"; predicate = "期限"; object = "签收后 15 天内" },
    @{ subject = "退货运费"; predicate = "承担方"; object = "买家（非质量问题）" },
    @{ subject = "退款"; predicate = "到账时效"; object = "1-3 个工作日" }
)
foreach ($t in $triples) {
    try {
        $body = @{
            knowledgeBase   = "knowledge"
            subject         = $t.subject
            predicate       = $t.predicate
            object          = $t.object
            sourceDocumentId = $null
        }
        Invoke-Api -Method POST -Url "$Gateway/api/rag/graph/triple" -Body $body -Headers $H | Out-Null
        Write-Ok "三元组: $($t.subject) - $($t.predicate) -> $($t.object)"
    }
    catch { Write-Warn2 "三元组写入失败 $($t.subject): $_" }
}

# ==================== 完成 ====================
Write-Host "`n种子数据写入完成！" -ForegroundColor Green
Write-Host @"

后续验证（详见 docs/24-红框菜单功能操作手册.md）：
  - 知识库管理：搜索「退款」应命中 2002/2003
  - 全文搜索：knowledge 搜「保修」；faq-test 搜「优惠券」
  - 知识图谱：实体「退款政策」深度 2 检索出两跳链
  - 售后 Agent：zhangsan/123456 登录，发送「我买的耳机无法开机，要求换货」
  - RAG 评估：golden 集路径填
    file:$PSScriptRoot/golden-set-demo.json
    命中率阈值 0.6、评分阈值 1，运行应得命中率 1.0 / 门禁 PASS
"@
