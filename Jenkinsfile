pipeline {
    agent any

    parameters {
        string(name: 'TARGET_HOST',      defaultValue: '192.168.1.100',  description: '目标部署主机 IP')
        string(name: 'TARGET_PASSWORD',  defaultValue: '',               description: '目标主机 root 密码')
        choice(name: 'DEPLOY_MODE',      choices: ['full', 'build-only', 'deploy-only', 'infra-only'], description: '部署模式')
        string(name: 'VERSION',          defaultValue: '',               description: '镜像版本号')
        string(name: 'MYSQL_ROOT_PASSWORD', defaultValue: 'root',       description: 'MySQL root 密码')
        string(name: 'MINIO_USER',           defaultValue: 'minioadmin', description: 'MinIO 用户名')
        string(name: 'MINIO_PASSWORD',       defaultValue: 'minioadmin', description: 'MinIO 密码')
        string(name: 'OPENAI_API_KEY',   defaultValue: 'demo-key',      description: 'OpenAI API Key')
        string(name: 'OPENAI_BASE_URL',  defaultValue: 'https://api.minimaxi.com', description: 'OpenAI API 地址')
        string(name: 'OPENAI_MODEL',     defaultValue: 'MiniMax-M3',    description: '模型名称')
        booleanParam(name: 'SKIP_TESTS',      defaultValue: true,  description: '跳过测试')
        booleanParam(name: 'VERIFY_DEPLOY',   defaultValue: true,  description: '部署后自动验证')
    }

    environment {
        PROJECT_DIR = 'ai-customer-service'
        SERVICES = 'ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify'
        IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"
    }

    stages {
        stage('Maven 编译') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only' } }
            steps {
                echo "版本号: ${IMAGE_VERSION}"
                sh """
                    cd ${PROJECT_DIR}
                    mvn clean package ${params.SKIP_TESTS ? '-DskipTests' : ''} -B
                """
            }
        }

        stage('构建镜像') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only' } }
            steps {
                script {
                    def services = SERVICES.split(' ')
                    parallel services.collectEntries { svc ->
                        [("build-${svc}") : {
                            sh """
                                cd ${PROJECT_DIR}
                                docker build -f ${svc}/Dockerfile -t ${svc}:${IMAGE_VERSION} -t ${svc}:latest .
                            """
                        }]
                    }
                }
            }
        }

        stage('导出并分发镜像') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only' } }
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    if ! command -v sshpass > /dev/null 2>&1; then
                        apt-get update -qq && apt-get install -y -qq sshpass 2>/dev/null || yum install -y sshpass 2>/dev/null || true
                    fi
                    sshpass -p '${params.TARGET_PASSWORD}' ssh -o StrictHostKeyChecking=no root@${params.TARGET_HOST} "mkdir -p /opt/aics/images"
                    mkdir -p images-export
                    for svc in ${SERVICES}; do
                        docker save -o images-export/\${svc}-${IMAGE_VERSION}.tar \${svc}:${IMAGE_VERSION}
                        sshpass -p '${params.TARGET_PASSWORD}' scp -o StrictHostKeyChecking=no images-export/\${svc}-${IMAGE_VERSION}.tar root@${params.TARGET_HOST}:/opt/aics/images/
                    done
                """
            }
        }

        stage('部署') {
            when { expression { params.DEPLOY_MODE != 'build-only' } }
            steps {
                sh """
                    cd ${PROJECT_DIR}
                    if ! command -v sshpass > /dev/null 2>&1; then
                        apt-get update -qq && apt-get install -y -qq sshpass 2>/dev/null || yum install -y sshpass 2>/dev/null || true
                    fi
                    sshpass -p '${params.TARGET_PASSWORD}' ssh -o StrictHostKeyChecking=no root@${params.TARGET_HOST} "mkdir -p /opt/aics/{mysql,redis,config,logs}"
                    sshpass -p '${params.TARGET_PASSWORD}' scp -o StrictHostKeyChecking=no -r deploy/mysql/* root@${params.TARGET_HOST}:/opt/aics/mysql/
                    sshpass -p '${params.TARGET_PASSWORD}' scp -o StrictHostKeyChecking=no -r deploy/redis/* root@${params.TARGET_HOST}:/opt/aics/redis/
                    sshpass -p '${params.TARGET_PASSWORD}' scp -o StrictHostKeyChecking=no deploy/docker-compose/docker-compose-all.yml root@${params.TARGET_HOST}:/opt/aics/docker-compose-all.yml
                    sshpass -p '${params.TARGET_PASSWORD}' scp -o StrictHostKeyChecking=no deploy/scripts/deploy.sh root@${params.TARGET_HOST}:/opt/aics/deploy.sh
                    sshpass -p '${params.TARGET_PASSWORD}' ssh -o StrictHostKeyChecking=no root@${params.TARGET_HOST} "
                        export VERSION='${IMAGE_VERSION}'
                        export MYSQL_ROOT_PASSWORD='${params.MYSQL_ROOT_PASSWORD}'
                        export MINIO_USER='${params.MINIO_USER}'
                        export MINIO_PASSWORD='${params.MINIO_PASSWORD}'
                        export OPENAI_API_KEY='${params.OPENAI_API_KEY}'
                        export OPENAI_BASE_URL='${params.OPENAI_BASE_URL}'
                        export OPENAI_MODEL='${params.OPENAI_MODEL}'
                        chmod +x /opt/aics/deploy.sh
                        bash /opt/aics/deploy.sh
                    "
                """
            }
        }

        stage('部署验证') {
            when { expression { params.VERIFY_DEPLOY && params.DEPLOY_MODE != 'build-only' } }
            steps {
                echo "等待 30 秒让服务启动..."
                sleep 30
                sh """
                    curl -sf http://${params.TARGET_HOST}:8080/actuator/health || echo "WARN: 网关未响应"
                    curl -sf http://${params.TARGET_HOST}:8848/nacos/v1/console/health/readiness || echo "WARN: Nacos 未响应"
                    echo "验证完成"
                """
            }
        }
    }

    post {
        success {
            echo """
                ==========================================
                 单机部署成功！版本: ${IMAGE_VERSION}
                 API 网关:      http://${params.TARGET_HOST}:8080
                 Nacos 控制台:  http://${params.TARGET_HOST}:8848/nacos
                 MinIO 控制台:  http://${params.TARGET_HOST}:9001
                ==========================================
            """
        }
        failure {
            echo "部署失败！请检查日志。"
        }
        always {
            cleanWs()
        }
    }
}
