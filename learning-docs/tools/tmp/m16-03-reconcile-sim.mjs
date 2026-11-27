#!/usr/bin/env node
/**
 * m16-03-reconcile-sim.mjs — 用纯计算模拟一次「调谐循环（reconcile）」里
 * 漂移检测 / 自愈 / prune 的四类边界，用来把「什么时候不该自动修」讲清楚。
 *
 * 真相声明：这不是 ArgoCD 源码，也不是对真实集群的观测。
 *   本机没有 kubectl / argocd / 集群（`Get-Command kubectl,argocd` 全空），
 *   所以这里只做**状态机推演**：把 ArgoCD 公开的判定口径
 *   （Synced / OutOfSync / Missing / Extra，以及 selfHeal 与 prune 各自管什么）
 *   落成可跑的代码，让你改一个开关就看到结论翻转。
 *   所有数值都是人造的教学参数，不是实测指标。
 */
const S = { git: { image: 'abc123', replicas: 1, exists: true }, live: { image: 'abc123', replicas: 1, exists: true } };
const ticks = [];
let prune = false;
let selfHeal = false;
const snap = (o) => (o.exists ? `img=${o.image} x${o.replicas}` : '(对象不存在)');

function step(tick, event, mutate) {
  if (mutate) mutate();
  const g = { ...S.git };
  const l = { ...S.live };
  let status;
  if (!g.exists && !l.exists) status = 'Synced';
  else if (g.exists && !l.exists) status = 'OutOfSync/Missing';
  else if (!g.exists && l.exists) status = 'OutOfSync/Extra';
  else if (g.image !== l.image || g.replicas !== l.replicas) status = 'OutOfSync/Drift';
  else status = 'Synced';

  let action = '无（已收敛）';
  if (status === 'OutOfSync/Missing' && selfHeal) { S.live = { ...S.git }; action = '重建对象（selfHeal 管，与 prune 无关）'; }
  else if (status === 'OutOfSync/Drift' && selfHeal) { S.live = { ...S.git }; action = '把 live 拉回 git（Pod 重建）'; }
  else if (status === 'OutOfSync/Extra' && prune) { S.live = { ...S.git }; action = '删除对象（prune=true）'; }
  else if (status === 'OutOfSync/Extra' && !prune) action = '只标 OutOfSync，不删（prune=false）';
  else if (status !== 'Synced') action = '只标 OutOfSync，不改集群（automated 未开）';

  ticks.push({ tick, event, status, action, gitSnap: snap(g), liveSnap: snap(l), after: snap(S.live) });
}

function show(title, from) {
  console.log(`\n== ${title}（prune=${prune} selfHeal=${selfHeal}）==`);
  console.log('tick | 事件                          | git 期望      | cluster 实况  | 判定              | ArgoCD 动作');
  console.log('-----+-------------------------------+---------------+---------------+-------------------+--------------------------------');
  for (const t of ticks.slice(from)) {
    console.log(
      `${String(t.tick).padEnd(4)} | ${t.event.padEnd(29)} | ${t.gitSnap.padEnd(13)} | ${t.liveSnap.padEnd(13)} | ${t.status.padEnd(17)} | ${t.action}`,
    );
  }
  const last = ticks[ticks.length - 1];
  console.log(`     → 该轮结束后 cluster 实况：${last.after}`);
}

// 阶段一：只装探针，不开自动化 —— 先看清"漂移长什么样"
step(1, 'git 提交 v2（img=def456）', () => { S.git.image = 'def456'; });
step(2, '有人 kubectl set image :latest', () => { S.live.image = 'latest'; });
step(3, '有人 kubectl delete deployment', () => { S.live.exists = false; });
show('阶段一：manual（automated 未开）', 0);

// 阶段二：开 selfHeal，不开 prune —— "能修不删"，第一阶段口径
selfHeal = true;
step(4, '再次 reconcile（selfHeal 生效）', null);
step(5, '有人 kubectl scale --replicas=3', () => { S.live.replicas = 3; });
step(6, 'git 把该对象删掉（期望下线）', () => { S.git.exists = false; });
step(7, '再次 reconcile（prune=false）', null);
show('阶段二：selfHeal=true / prune=false', 3);

// 阶段三：开 prune —— 删除自动化，风险最高的一档
prune = true;
step(8, '再次 reconcile（prune=true）', null);
show('阶段三：selfHeal=true / prune=true', 7);

// 阶段四：回滚两条路的差别
console.log('\n== 阶段四：回滚两条路的差别 ==');
console.log('kubectl rollout undo / kubectl set image : 改的是集群；git 没变 → 下一次 reconcile 又漂回去，revision 也不留痕');
console.log('git revert → 提交 → ArgoCD 同步           : 改的是期望态；历史在 git log 里，任何人可复现（本仓 59 次提交可当教材）');
console.log(`\n最后状态：git=${JSON.stringify(S.git)}  live=${JSON.stringify(S.live)}`);
