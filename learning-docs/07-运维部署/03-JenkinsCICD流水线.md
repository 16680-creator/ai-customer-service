# Jenkins CI/CD 流水线

> 本项目根目录有完整的 `Jenkinsfile`，实现从编译到部署的全自动化。
> 对应项目文件：`Jenkinsfile`、`deploy/scripts/deploy.sh`

---

## 一、什么是 CI/CD？

```
CI（持续集成）：代码提交 → 自动编译 → 自动测试 → 发现问题立即反馈
CD（持续部署）：测试通过 → 自动构建镜像 → 自动部署到服务器

【没有 CI/CD】
  开发 → 手动编译 → 手动测试 → 手动打包 → 手动上传 → 手动重启
  耗时 30 分钟，容易出错

【有 CI/CD】
  git push → 自动完成一切 → 5 分钟后上线
```

---

## 二、本项目的 Jenkinsfile 详解

```groovy
pipeline {
    agent any    // 在任何可用的 Jenkins 节点上运行

    // ===== 参数化构建（手动触发时可选） =====
    parameters {
        string(name: 'TARGET_HOST', defaultValue: '192.168.1.100', description: '目标部署主机')
        choice(name: 'DEPLOY_MODE', choices: ['full', 'build-only', 'deploy-only', 'infra-only'])
        string(name: 'VERSION', defaultValue: '', description: '镜像版本号')
        string(name: 'OPENAI_API_KEY', defaultValue: 'demo-key')
        booleanParam(name: 'SKIP_TESTS', defaultValue: true, description: '跳过测试')
        booleanParam(name: 'VERIFY_DEPLOY', defaultValue: true, description: '部署后验证')
    }

    // ===== 环境变量 =====
    environment {
        PROJECT_DIR = 'ai-customer-service'
        SERVICES = 'ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify'
        IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"  // 版本号或构建号
    }

    stages {
        // ===== 阶段 1：Maven 编译 =====
        stage('Maven 编译') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' } }
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    mvn clean package ${params.SKIP_TESTS ? '-DskipTests' : ''} -B
                """
            }
        }

        // ===== 阶段 2：构建 Docker 镜像（并行） =====
        stage('构建镜像') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' } }
            steps {
                script {
                    def services = SERVICES.split(' ')
                    parallel services.collectEntries { svc ->
                        [("build-${svc}"): {
                            sh "docker build -f ${svc}/Dockerfile -t ${svc}:${IMAGE_VERSION} ."
                        }]
                    }
                }
            }
        }

        // ===== 阶段 3：分发镜像到目标服务器 =====
        stage('导出并分发镜像') {
            steps {
                sh """
                    for svc in ${SERVICES}; do
                        docker save -o \${svc}.tar \${svc}:${IMAGE_VERSION}
                        sshpass -p '${params.TARGET_PASSWORD}' scp \${svc}.tar root@${params.TARGET_HOST}:/opt/aics/images/
                    done
                """
            }
        }

        // ===== 阶段 4：远程部署 =====
        stage('部署') {
            when { expression { params.DEPLOY_MODE != 'build-only' } }
            steps {
                sh """
                    sshpass -p '${params.TARGET_PASSWORD}' ssh root@${params.TARGET_HOST} "
                        export VERSION='${IMAGE_VERSION}'
                        bash /opt/aics/deploy.sh
                    "
                """
            }
        }

        // ===== 阶段 5：部署验证 =====
        stage('部署验证') {
            when { expression { params.VERIFY_DEPLOY } }
            steps {
                sleep 30    // 等服务启动
                sh """
                    curl -sf http://${params.TARGET_HOST}:8080/actuator/health || echo "WARN: 网关未响应"
                    curl -sf http://${params.TARGET_HOST}:8848/nacos/ || echo "WARN: Nacos 未响应"
                """
            }
        }
    }

    // ===== 构建后操作 =====
    post {
        success {
            echo "部署成功！版本: ${IMAGE_VERSION}"
        }
        failure {
            echo "部署失败！请检查日志。"
        }
        always {
            cleanWs()    // 清理工作空间
        }
    }
}
```

---

## 三、流水线可视化

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│  Maven   │ → │  构建    │ → │  分发    │ → │  部署    │ → │  验证    │
│  编译    │   │  镜像    │   │  镜像    │   │  远程    │   │  健康    │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
     ↓              ↓
  可跳过测试    7个服务并行构建
```

---

## 四、部署模式

| 模式 | 说明 | 使用场景 |
|------|------|---------|
| `full` | 编译 + 构建 + 部署 | 正常发版 |
| `build-only` | 只编译和构建镜像 | 本地验证 |
| `deploy-only` | 只部署（用已有镜像） | 回滚、重新部署 |
| `infra-only` | 只部署基础设施 | 初始化环境 |

---

## 五、Git Webhook 自动触发

```
开发者 git push → GitHub/GitLab → Webhook 通知 Jenkins → 自动构建

配置步骤：
1. Jenkins 安装 "GitHub Plugin"
2. Job 配置 → 构建触发器 → GitHub hook trigger
3. GitHub 仓库 → Settings → Webhooks → 添加 Jenkins URL
```

---

## 六、生产环境改进建议

```groovy
// 1. 使用 Harbor 私有镜像仓库（替代 docker save/scp）
stage('推送镜像') {
    steps {
        sh "docker tag ai-cs-chat:${VERSION} harbor.company.com/aics/ai-cs-chat:${VERSION}"
        sh "docker push harbor.company.com/aics/ai-cs-chat:${VERSION}"
    }
}

// 2. 通知（钉钉/企业微信/邮件）
post {
    success {
        dingtalk(robot: 'xxx', message: "✅ 部署成功 v${VERSION}")
    }
    failure {
        dingtalk(robot: 'xxx', message: "❌ 部署失败！")
    }
}

// 3. 蓝绿部署 / 金丝雀发布
// 4. 集成 SonarQube 代码质量检查
// 5. 集成自动化测试（不跳过）
```

---

## 七、动手练习

1. 阅读项目根目录的 `Jenkinsfile`，理解每个 stage 的作用
2. 本地模拟：手动执行 `mvn clean package -DskipTests`
3. 手动构建一个 Docker 镜像
4. 理解 `deploy/scripts/deploy.sh` 的部署逻辑
5. （可选）搭建 Jenkins，配置 Pipeline Job

---

## 学习检查清单

- [ ] 理解 CI/CD 的价值
- [ ] 理解 Jenkinsfile 的 Pipeline 语法
- [ ] 理解 stage / steps / when / post 的作用
- [ ] 理解参数化构建
- [ ] 理解镜像构建和分发流程
- [ ] 理解部署验证的意义
- [ ] 了解 Webhook 自动触发机制

---

## 下一步

→ [08-测试/01-JUnit5单元测试](../08-测试/01-JUnit5单元测试.md)
