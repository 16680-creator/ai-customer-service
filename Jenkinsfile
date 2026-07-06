pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        string(name: 'REGISTRY', defaultValue: '192.168.56.12:5000', description: '本地 Docker Registry 地址，默认部署在 k8s-worker02')
        string(name: 'NAMESPACE', defaultValue: 'ai-customer-service', description: 'Kubernetes 命名空间')
        string(name: 'VERSION', defaultValue: '', description: '镜像版本。留空则使用 Jenkins BUILD_NUMBER')
        choice(name: 'DEPLOY_MODE', choices: ['full', 'build-only', 'deploy-only', 'infra-only'], description: 'full=构建并部署，build-only=只构建推送，deploy-only=只部署业务，infra-only=只部署基础设施')
        string(name: 'SERVICES', defaultValue: 'ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify', description: '需要处理的服务模块，空格分隔')

        booleanParam(name: 'SKIP_TESTS', defaultValue: true, description: '跳过 Maven 测试')
        booleanParam(name: 'DEPLOY_INFRA', defaultValue: false, description: '部署或更新 MySQL、Nacos、Redis、Elasticsearch、RocketMQ、MinIO')
        booleanParam(name: 'INIT_DATABASE', defaultValue: false, description: '初始化 MySQL 数据库。首次部署勾选，重复执行会覆盖部分初始化逻辑')
        booleanParam(name: 'VERIFY_DEPLOY', defaultValue: true, description: '部署后查看 Pod、Service 和 rollout 状态')

        string(name: 'OPENAI_API_KEY', defaultValue: 'demo-key', description: 'AI 服务 API Key。本地 Ollama 可填 demo-key')
        string(name: 'OPENAI_BASE_URL', defaultValue: 'http://host.docker.internal:11434/v1', description: 'OpenAI 兼容接口地址')
        string(name: 'OPENAI_MODEL', defaultValue: 'qwen2.5:7b', description: '模型名称')
    }

    environment {
        IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"
    }

    stages {
        stage('拉取代码') {
            steps {
                checkout scm
                sh 'git rev-parse --short HEAD'
            }
        }

        stage('环境检查') {
            steps {
                sh '''
                    set -e
                    docker version
                    kubectl version --client
                    kubectl cluster-info
                '''
            }
        }

        stage('Maven 测试') {
            when {
                expression {
                    return !params.SKIP_TESTS && params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only'
                }
            }
            steps {
                sh '''
                    set -e
                    docker run --rm \
                      -v "$PWD":/workspace \
                      -v "$HOME/.m2":/root/.m2 \
                      -w /workspace \
                      maven:3.9-eclipse-temurin-17 \
                      mvn test -B
                '''
            }
        }

        stage('写入 Kubernetes Secret') {
            when {
                expression {
                    return params.DEPLOY_MODE != 'build-only'
                }
            }
            steps {
                sh '''
                    set -e
                    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
                    kubectl create secret generic aics-secrets \
                      -n "$NAMESPACE" \
                      --from-literal=openai-api-key="$OPENAI_API_KEY" \
                      --from-literal=openai-base-url="$OPENAI_BASE_URL" \
                      --from-literal=openai-model="$OPENAI_MODEL" \
                      --dry-run=client -o yaml | kubectl apply -f -
                '''
            }
        }

        stage('部署基础设施') {
            when {
                expression {
                    return params.DEPLOY_INFRA || params.DEPLOY_MODE == 'infra-only'
                }
            }
            steps {
                sh '''
                    set -e
                    chmod +x deploy/scripts/k8s-apply-infra.sh deploy/scripts/k8s-init-mysql.sh
                    NAMESPACE="$NAMESPACE" INIT_DATABASE="$INIT_DATABASE" bash deploy/scripts/k8s-apply-infra.sh
                '''
            }
        }

        stage('构建并推送镜像') {
            when {
                expression {
                    return params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only'
                }
            }
            steps {
                sh '''
                    set -e
                    chmod +x deploy/scripts/k8s-build-push.sh
                    REGISTRY="$REGISTRY" VERSION="$IMAGE_VERSION" SERVICES="$SERVICES" bash deploy/scripts/k8s-build-push.sh
                '''
            }
        }

        stage('部署业务服务') {
            when {
                expression {
                    return params.DEPLOY_MODE != 'build-only' && params.DEPLOY_MODE != 'infra-only'
                }
            }
            steps {
                sh '''
                    set -e
                    chmod +x deploy/scripts/k8s-deploy-services.sh
                    REGISTRY="$REGISTRY" VERSION="$IMAGE_VERSION" NAMESPACE="$NAMESPACE" SERVICES="$SERVICES" bash deploy/scripts/k8s-deploy-services.sh
                '''
            }
        }

        stage('部署验证') {
            when {
                expression {
                    return params.VERIFY_DEPLOY && params.DEPLOY_MODE != 'build-only'
                }
            }
            steps {
                sh '''
                    set -e
                    kubectl get nodes -o wide
                    kubectl get pods -n "$NAMESPACE" -o wide
                    kubectl get svc -n "$NAMESPACE" -o wide
                '''
            }
        }
    }

    post {
        success {
            echo """
部署完成
镜像版本: ${IMAGE_VERSION}
命名空间: ${params.NAMESPACE}
网关访问: http://任意K8s节点IP:30080
本地示例: http://192.168.56.10:30080
"""
        }
        failure {
            echo """
部署失败。优先检查：
1. Jenkins 机器是否能执行 docker 和 kubectl
2. ${params.REGISTRY} 是否可访问
3. 三台 Kubernetes 节点是否已配置 containerd 拉取 HTTP 私有仓库
4. kubectl get pods -n ${params.NAMESPACE} 的具体错误
"""
        }
    }
}
