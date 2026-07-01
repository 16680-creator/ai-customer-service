pipeline {
    agent any

    // ============================================================
    // 构建参数 - 在 Jenkins 页面点击"Build with Parameters"填写
    // ============================================================
    parameters {
        // 主机信息
        string(name: 'HOST1_IP',         defaultValue: '192.168.1.101',  description: '主机1 IP（部署 Gateway/User/Nacos/MySQL Master/Redis1）')
        string(name: 'HOST1_PASSWORD',   defaultValue: '',               description: '主机1 root 密码')
        string(name: 'HOST2_IP',         defaultValue: '192.168.1.102',  description: '主机2 IP（部署 Knowledge/Chat/Search/MySQL Slave1/Redis2）')
        string(name: 'HOST2_PASSWORD',   defaultValue: '',               description: '主机2 root 密码')
        string(name: 'HOST3_IP',         defaultValue: '192.168.1.103',  description: '主机3 IP（部署 Message/Notify/ES/RocketMQ/MySQL Slave2/Redis3）')
        string(name: 'HOST3_PASSWORD',   defaultValue: '',               description: '主机3 root 密码')

        // 构建选项
        choice(name: 'DEPLOY_MODE',      choices: ['full', 'build-only', 'deploy-only', 'infra-only'], description: '部署模式')
        string(name: 'VERSION',          defaultValue: '',               description: '镜像版本号（留空则使用 Jenkins BUILD_NUMBER）')

        // 中间件密码
        string(name: 'MYSQL_ROOT_PASSWORD', defaultValue: 'root',       description: 'MySQL root 密码')
        string(name: 'MINIO_USER',           defaultValue: 'minioadmin', description: 'MinIO 用户名')
        string(name: 'MINIO_PASSWORD',       defaultValue: 'minioadmin', description: 'MinIO 密码')

        // AI 配置
        string(name: 'OPENAI_API_KEY',   defaultValue: 'demo-key',      description: 'OpenAI API Key（使用 Ollama 填 demo-key）')
        string(name: 'OPENAI_BASE_URL',  defaultValue: 'http://host.docker.internal:11434/v1', description: 'OpenAI/Ollama 地址')
        string(name: 'OPENAI_MODEL',     defaultValue: 'qwen2.5:7b',    description: '模型名称')

        // 高级选项
        booleanParam(name: 'SKIP_TESTS',      defaultValue: true,  description: '跳过测试')
        booleanParam(name: 'INIT_HOSTS',      defaultValue: false, description: '首次部署：初始化主机环境（安装 Docker 等）')
        booleanParam(name: 'VERIFY_DEPLOY',   defaultValue: true,  description: '部署后自动验证')
    }

    environment {
        // 项目配置
        PROJECT_DIR = 'ai-customer-service'

        // 服务列表
        SERVICES = 'ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify'

        // 版本号（优先使用参数，否则用 BUILD_NUMBER）
        IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"
    }

    stages {

        // ============================================================
        // Stage 1: 初始化主机环境（首次部署）
        // ============================================================
        stage('初始化主机') {
            when { expression { params.INIT_HOSTS } }
            steps {
                script {
                    def hosts = [
                        [name: 'Host1', ip: params.HOST1_IP, pwd: params.HOST1_PASSWORD],
                        [name: 'Host2', ip: params.HOST2_IP, pwd: params.HOST2_PASSWORD],
                        [name: 'Host3', ip: params.HOST3_IP, pwd: params.HOST3_PASSWORD]
                    ]
                    parallel hosts.collectEntries { host ->
                        [("初始化-${host.name}") : {
                            sh """
                                echo "初始化 ${host.name} (${host.ip}) ..."
                                sshpass -p '${host.pwd}' ssh -o StrictHostKeyChecking=no root@${host.ip} 'bash -s' < deploy/scripts/init-host.sh
                            """
                        }]
                    }
                }
            }
        }

        // ============================================================
        // Stage 2: Maven 编译 + 单元测试
        // ============================================================
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

        // ============================================================
        // Stage 3: 构建 Docker 镜像
        // ============================================================
        stage('构建镜像') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only' } }
            steps {
                script {
                    def services = SERVICES.split(' ')
                    parallel services.collectEntries { svc ->
                        [("构建-${svc}") : {
                            sh """
                                cd ${PROJECT_DIR}
                                docker build \
                                    -f ${svc}/Dockerfile \
                                    -t ${svc}:${IMAGE_VERSION} \
                                    -t ${svc}:latest \
                                    .
                            """
                        }]
                    }
                }
            }
        }

        // ============================================================
        // Stage 4: 导出镜像 + 分发到三台主机
        // ============================================================
        stage('导出并分发镜像') {
            when { expression { params.DEPLOY_MODE != 'deploy-only' && params.DEPLOY_MODE != 'infra-only' } }
            steps {
                script {
                    // 4.1 导出所有业务镜像为 tar 文件
                    echo "导出所有业务镜像为 tar 文件..."
                    sh """
                        cd ${PROJECT_DIR}
                        mkdir -p images-export

                        for svc in ${SERVICES}; do
                            echo "  导出 \${svc}:${IMAGE_VERSION} ..."
                            docker save -o images-export/\${svc}-${IMAGE_VERSION}.tar \${svc}:${IMAGE_VERSION}
                        done

                        echo "镜像导出完成:"
                        ls -lh images-export/
                    """

                    // 4.2 并行分发镜像到三台主机
                    def hosts = [
                        [name: 'Host1', ip: params.HOST1_IP, pwd: params.HOST1_PASSWORD, services: 'ai-cs-gateway ai-cs-user'],
                        [name: 'Host2', ip: params.HOST2_IP, pwd: params.HOST2_PASSWORD, services: 'ai-cs-knowledge ai-cs-chat ai-cs-search'],
                        [name: 'Host3', ip: params.HOST3_IP, pwd: params.HOST3_PASSWORD, services: 'ai-cs-message ai-cs-notify']
                    ]

                    parallel hosts.collectEntries { host ->
                        [("分发镜像-${host.name}") : {
                            sh """
                                cd ${PROJECT_DIR}

                                # 确保 sshpass 可用
                                if ! command -v sshpass &> /dev/null; then
                                    apt-get update -qq && apt-get install -y -qq sshpass 2>/dev/null || \
                                    yum install -y sshpass 2>/dev/null || true
                                fi

                                # 创建远程镜像目录
                                sshpass -p '${host.pwd}' ssh -o StrictHostKeyChecking=no root@${host.ip} "mkdir -p /opt/aics/images"

                                # 分发该主机需要的镜像
                                for svc in ${host.services}; do
                                    echo "  分发 \${svc}-${IMAGE_VERSION}.tar 到 ${host.name} ..."
                                    sshpass -p '${host.pwd}' scp -o StrictHostKeyChecking=no \
                                        images-export/\${svc}-${IMAGE_VERSION}.tar \
                                        root@${host.ip}:/opt/aics/images/
                                done

                                echo "  ${host.name} 镜像分发完成"
                            """
                        }]
                    }
                }
            }
        }

        // ============================================================
        // Stage 5: 部署到三台主机
        // ============================================================
        stage('部署到主机') {
            when { expression { params.DEPLOY_MODE != 'build-only' } }
            steps {
                script {
                    // 准备部署文件
                    sh """
                        cd ${PROJECT_DIR}

                        # 创建部署包
                        mkdir -p deploy-package

                        # 复制 docker-compose 文件
                        cp deploy/docker-compose/docker-compose-host1.yml deploy-package/
                        cp deploy/docker-compose/docker-compose-host2.yml deploy-package/
                        cp deploy/docker-compose/docker-compose-host3.yml deploy-package/

                        # 复制配置文件
                        cp -r deploy/mysql deploy-package/
                        cp -r deploy/redis deploy-package/
                    """

                    // 并行部署到三台主机
                    parallel(
                        '部署-Host1': {
                            deployToHost(
                                'Host1', params.HOST1_IP, params.HOST1_PASSWORD,
                                'deploy/scripts/deploy-host1.sh',
                                'docker-compose-host1.yml'
                            )
                        },
                        '部署-Host2': {
                            deployToHost(
                                'Host2', params.HOST2_IP, params.HOST2_PASSWORD,
                                'deploy/scripts/deploy-host2.sh',
                                'docker-compose-host2.yml'
                            )
                        },
                        '部署-Host3': {
                            deployToHost(
                                'Host3', params.HOST3_IP, params.HOST3_PASSWORD,
                                'deploy/scripts/deploy-host3.sh',
                                'docker-compose-host3.yml'
                            )
                        }
                    )
                }
            }
        }

        // ============================================================
        // Stage 6: 部署后验证
        // ============================================================
        stage('部署验证') {
            when { expression { params.VERIFY_DEPLOY && params.DEPLOY_MODE != 'build-only' } }
            steps {
                echo "等待 30 秒让所有服务完成启动..."
                sleep 30

                sh """
                    cd ${PROJECT_DIR}

                    # 生成验证用的变量文件
                    cat > deploy/scripts/deploy-vars.sh << EOF
export HOST1_IP="${params.HOST1_IP}"
export HOST1_PASSWORD="${params.HOST1_PASSWORD}"
export HOST2_IP="${params.HOST2_IP}"
export HOST2_PASSWORD="${params.HOST2_PASSWORD}"
export HOST3_IP="${params.HOST3_IP}"
export HOST3_PASSWORD="${params.HOST3_PASSWORD}"
export MYSQL_ROOT_PASSWORD="${params.MYSQL_ROOT_PASSWORD}"
export VERSION="${IMAGE_VERSION}"
EOF

                    bash deploy/scripts/verify-deploy.sh
                """
            }
        }
    }

    // ============================================================
    // 构建后操作
    // ============================================================
    post {
        success {
            echo """
                ==========================================
                 部署成功！
                 版本: ${IMAGE_VERSION}
                 访问地址: http://${params.HOST1_IP}:8080
                 Nacos 控制台: http://${params.HOST1_IP}:8848/nacos
                 MinIO 控制台: http://${params.HOST1_IP}:9001
                ==========================================
            """
        }
        failure {
            echo """
                ==========================================
                 部署失败！请检查上方日志排查问题。
                 常见问题:
                 1. 主机 SSH 连通性
                 2. Docker 是否已安装
                 3. 端口是否被占用
                 4. 磁盘空间是否充足
                ==========================================
            """
        }
        always {
            cleanWs()
        }
    }
}

// ============================================================
// 部署函数：将文件传输到目标主机并执行部署脚本
// ============================================================
def deployToHost(String name, String ip, String password, String deployScript, String composeFile) {
    script {
        echo "=========================================="
        echo " 部署到 ${name} (${ip})"
        echo "=========================================="

        sh """
            cd ${PROJECT_DIR}

            # 安装 sshpass（如果未安装）
            if ! command -v sshpass &> /dev/null; then
                apt-get update -qq && apt-get install -y -qq sshpass 2>/dev/null || \
                yum install -y sshpass 2>/dev/null || true
            fi

            # 创建远程目录
            sshpass -p '${password}' ssh -o StrictHostKeyChecking=no root@${ip} "mkdir -p /opt/aics/{mysql,redis,config,logs}"

            # 传输配置文件
            sshpass -p '${password}' scp -o StrictHostKeyChecking=no -r \
                deploy-package/mysql/* root@${ip}:/opt/aics/mysql/

            sshpass -p '${password}' scp -o StrictHostKeyChecking=no -r \
                deploy-package/redis/* root@${ip}:/opt/aics/redis/

            sshpass -p '${password}' scp -o StrictHostKeyChecking=no \
                deploy-package/${composeFile} root@${ip}:/opt/aics/docker-compose.yml

            # 传输并执行部署脚本
            sshpass -p '${password}' scp -o StrictHostKeyChecking=no \
                ${deployScript} root@${ip}:/opt/aics/deploy.sh

            sshpass -p '${password}' ssh -o StrictHostKeyChecking=no root@${ip} "
                export HOST1_IP='${params.HOST1_IP}'
                export HOST2_IP='${params.HOST2_IP}'
                export HOST3_IP='${params.HOST3_IP}'
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