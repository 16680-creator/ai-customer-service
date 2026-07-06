# VMware Workstation 26H1 一主二从虚拟机创建操作文档

本文档用于在 Windows 宿主机上，通过 VMware Workstation 26H1 创建 3 台 Ubuntu Server 虚拟机，作为后续部署 Kubernetes 一主二从集群、Docker 构建环境、Jenkins CI/CD 和微服务的基础环境。

目标结果：

| 虚拟机 | 主机名 | 角色 | IP 示例 | 说明 |
|---|---|---|---|---|
| VM1 | k8s-master | Kubernetes 主节点 | 192.168.56.10 | 控制平面、kubectl、Helm |
| VM2 | k8s-worker01 | Kubernetes 工作节点 | 192.168.56.11 | 运行微服务、中间件 |
| VM3 | k8s-worker02 | Kubernetes 工作节点 | 192.168.56.12 | 运行微服务、Jenkins、Docker 构建 |

> 说明：IP 网段需要根据你本机 VMware 的实际 VMnet 配置调整。本文以 `192.168.56.0/24` 为示例。

---

## 0. 从零开始操作总览

按下面顺序做，不要跳步骤：

```text
1. 下载 Ubuntu Server ISO 到 D:\DevTools\ISO
2. 用管理员身份打开 VMware Workstation 26H1
3. 配置 VMnet8 NAT 网络
4. 创建 ubuntu-template 模板虚拟机
5. 在模板机里安装 Ubuntu Server
6. 在模板机里安装 SSH、open-vm-tools 和常用工具
7. 关闭模板机并拍快照
8. 从模板机完整克隆出 k8s-master、k8s-worker01、k8s-worker02
9. 分别启动三台虚拟机
10. 分别配置主机名
11. 分别配置固定 IP
12. 三台机器都配置 hosts
13. 从 Windows 测试 SSH 连接
14. 三台机器关闭 swap，拍 ready-for-k8s 快照
```

建议你先只把三台虚拟机创建好并能互相 ping 通。Kubernetes、Jenkins、Docker 后面再按 09 文档继续做。

---

## 1. 宿主机准备

### 1.1 硬件建议

| 项目 | 最低配置 | 推荐配置 |
|---|---:|---:|
| CPU | 6 核 | 8 核及以上 |
| 内存 | 16GB | 24GB - 32GB |
| 可用磁盘 | 150GB | 200GB 以上 SSD |
| 虚拟化 | 开启 VT-x / AMD-V | 必须开启 |

如果宿主机只有 16GB 内存，可以先把每台虚拟机内存降低到 3GB，但后续同时运行 Jenkins、数据库、中间件和多个微服务时会比较吃紧。

### 1.2 Windows 功能检查

进入 BIOS / UEFI，确认已经开启：

```text
Intel VT-x
或
AMD-V
```

如果 VMware 虚拟机运行很慢，或提示虚拟化能力被占用，可以检查 Windows 是否启用了以下功能：

```text
Hyper-V
Windows Hypervisor Platform
Virtual Machine Platform
Windows Sandbox
```

学习环境中，如果 VMware 性能异常，可以关闭这些功能后重启 Windows。

### 1.3 软件准备

需要提前准备：

```text
VMware Workstation 26H1
Ubuntu Server 24.04.4 LTS ISO
稳定网络连接
```

当前环境建议使用：

```text
D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso
```

Ubuntu Server 24.04 LTS 是长期支持版本，适合 VMware Workstation 26H1、Docker、Jenkins 和 Kubernetes 学习环境。如果后续使用的 Kubernetes 教程明确要求 Ubuntu 22.04，再单独切换到 22.04 LTS。

### 1.4 ISO 下载地址

如果本机还没有 ISO，优先使用国内镜像源下载。

先创建保存目录。CMD 执行：

```cmd
mkdir D:\DevTools\ISO
```

如果提示目录已存在，可以忽略。

CMD 中执行下面这条命令，必须整行复制，不要拆成多行：

```cmd
curl.exe -L -C - -o "D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso" "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/24.04.4/ubuntu-24.04.4-live-server-amd64.iso"
```

如果清华源速度慢，改用中科大源：

```cmd
curl.exe -L -C - -o "D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso" "https://mirrors.ustc.edu.cn/ubuntu-releases/24.04.4/ubuntu-24.04.4-live-server-amd64.iso"
```

注意：CMD 不能使用 PowerShell 的反引号换行。如果命令拆成多行，会出现 `URL rejected: Bad hostname` 或 `-o 不是内部或外部命令`。

下载完成后，在 CMD 中检查文件是否存在：

```cmd
dir D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso
```

正常情况下文件大小约 3GB 以上。如果文件只有几 KB 或几十 MB，说明下载不完整，需要删除后重新下载。

---

## 2. VMware 网络规划

### 2.1 推荐网络模式

本地 Kubernetes 学习环境推荐使用 VMware 的 `NAT` 网络。

优点：

```text
虚拟机可以访问外网
宿主机可以访问虚拟机
虚拟机之间可以互相访问
IP 可以固定
不依赖公司或家庭路由器分配地址
```

如果你希望局域网其他电脑也访问 Jenkins 或 Kubernetes 服务，可以改用 `桥接网络`。但桥接网络更依赖当前路由器环境，IP 也更容易变化。

### 2.2 配置 VMnet8 NAT 网段

在 VMware Workstation 26H1 中打开：

```text
编辑 -> 虚拟网络编辑器
```

如果按钮不可编辑，点击：

```text
更改设置
```

如果仍然无法修改 VMnet8，关闭 VMware 后用管理员身份重新启动：

```text
右键 VMware Workstation -> 以管理员身份运行
```

然后按下面步骤配置：

```text
1. 选择 VMnet8
2. 确认类型是 NAT
3. 勾选“将主机虚拟适配器连接到此网络”
4. DHCP 可以开启，也可以关闭
5. 点击“NAT 设置”
6. 确认网关 IP
7. 点击“应用”
8. 点击“确定”
```

建议配置为：

```text
子网 IP：192.168.56.0
子网掩码：255.255.255.0
网关 IP：192.168.56.2
DHCP：可以开启，也可以关闭
```

如果你后面准备手动设置固定 IP，DHCP 开着也没关系，只要固定 IP 不和 DHCP 自动分配范围冲突即可。最简单的做法是使用 `.10`、`.11`、`.12` 这种靠前地址。

如果你的 VMnet8 已经是其他网段，比如 `192.168.100.0/24`，也可以继续使用，只要后续三台虚拟机 IP 都在同一网段即可。

如果你不确定当前 VMnet8 网段，在 Windows CMD 执行：

```cmd
ipconfig
```

找到类似下面的网卡：

```text
VMware Network Adapter VMnet8
```

它的 IPv4 地址通常类似：

```text
192.168.56.1
```

那么虚拟机网段就是 `192.168.56.0/24`，网关通常是 `192.168.56.2`。

### 2.3 IP 规划

本文示例：

| 主机名 | IP | 网关 | DNS |
|---|---|---|---|
| k8s-master | 192.168.56.10 | 192.168.56.2 | 8.8.8.8 / 114.114.114.114 |
| k8s-worker01 | 192.168.56.11 | 192.168.56.2 | 8.8.8.8 / 114.114.114.114 |
| k8s-worker02 | 192.168.56.12 | 192.168.56.2 | 8.8.8.8 / 114.114.114.114 |

---

## 3. 虚拟机配置规划

### 3.1 基础配置

| 节点 | CPU | 内存 | 磁盘 | 网络 |
|---|---:|---:|---:|---|
| k8s-master | 2 核 | 4GB | 40GB | NAT / VMnet8 |
| k8s-worker01 | 2 核 | 4GB | 40GB | NAT / VMnet8 |
| k8s-worker02 | 2 核 | 6GB | 60GB | NAT / VMnet8 |

如果 worker02 要运行 Jenkins，建议给 6GB 内存和 60GB 磁盘。Jenkins 构建镜像会占用较多磁盘。

### 3.2 VMware 虚拟机选项建议

| 配置项 | 建议值 |
|---|---|
| 硬件兼容性 | 使用 VMware 26H1 默认值 |
| 固件类型 | BIOS 或 UEFI 均可 |
| CPU | 2 个处理器核心 |
| 内存 | master/worker01 4GB，worker02 6GB |
| 网络适配器 | NAT，连接 VMnet8 |
| 磁盘控制器 | 默认即可 |
| 虚拟磁盘类型 | NVMe 或 SCSI |
| 磁盘存储 | 单个文件优先，便于性能；拆分文件便于移动 |
| 3D 加速 | 服务器虚拟机不需要 |
| 快照 | 每个关键阶段都创建 |

---

## 4. 创建第一台模板虚拟机

建议先创建一台 `ubuntu-template`，安装好系统和基础工具后，再克隆出三台虚拟机。这样比手动安装三遍更省时间。

### 4.1 新建虚拟机

在 VMware Workstation 26H1 中选择：

```text
文件 -> 新建虚拟机
```

推荐选择：

```text
自定义
```

虚拟机保存目录建议使用专门目录，不建议放在 `D:\DevTools\vm`：

```text
D:\VMwareVMs\ubuntu-template
```

后续三台虚拟机建议放在：

```text
D:\VMwareVMs\k8s-master
D:\VMwareVMs\k8s-worker01
D:\VMwareVMs\k8s-worker02
```

如果创建时提示 `Cannot open configuration file ... 拒绝访问`，优先换到上面的 `D:\VMwareVMs` 目录，并用管理员身份启动 VMware。

完整向导步骤如下：

| 向导项 | 选择 |
|---|---|
| 1. 配置类型 | 自定义 |
| 2. 硬件兼容性 | 默认，使用 26H1 推荐值 |
| 3. 安装来源 | 稍后安装操作系统，或选择 ISO 均可 |
| 4. 客户机操作系统 | Linux |
| 5. 版本 | Ubuntu 64-bit |
| 6. 虚拟机名称 | ubuntu-template |
| 7. 虚拟机位置 | `D:\VMwareVMs\ubuntu-template` |
| 8. 固件类型 | BIOS 或 UEFI 均可，建议默认 |
| 9. 处理器配置 | 1 个处理器，2 个核心 |
| 10. 内存 | 4096MB |
| 11. 网络类型 | 使用网络地址转换 NAT |
| 12. I/O 控制器 | 默认推荐 |
| 13. 磁盘类型 | NVMe 或 SCSI，默认即可 |
| 14. 磁盘 | 创建新虚拟磁盘 |
| 15. 磁盘容量 | 40GB |
| 16. 磁盘文件 | 单个文件或拆分文件均可 |
| 17. 完成前自定义硬件 | 建议点击 |

在“自定义硬件”里检查：

```text
内存：4096MB
处理器：2 核
网络适配器：NAT
CD/DVD：使用 ISO 映像文件
ISO 文件：D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso
显示器：不需要开启 3D 图形加速
USB 控制器：可保留默认
声卡：服务器不需要，可删除也可保留
打印机：不需要，可删除
```

如果前面选择了“稍后安装操作系统”，必须在这里手动挂载 ISO：

```text
编辑虚拟机设置 -> CD/DVD -> 使用 ISO 映像文件 -> 浏览
选择 D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso
勾选“启动时连接”
```

创建完成后，不要急着克隆，先安装 Ubuntu。

### 4.2 安装 Ubuntu Server

启动 `ubuntu-template`，按安装向导操作。

如果启动后没有进入 Ubuntu 安装界面，检查：

```text
1. CD/DVD 是否挂载 ISO
2. CD/DVD 是否勾选“启动时连接”
3. 虚拟机启动顺序是否优先从 CD/DVD 启动
```

Ubuntu Server 安装步骤：

```text
1. Try or Install Ubuntu Server
   选择 Install Ubuntu Server

2. 语言
   建议选 English，后续命令和报错更容易搜索

3. 键盘
   Layout 选择 English (US)
   Variant 选择 English (US)

4. 网络
   安装阶段先保持 DHCP 自动获取
   只要能看到网卡拿到 192.168.56.x 或类似地址即可

5. 代理 Proxy
   不填，直接 Done

6. Ubuntu archive mirror
   可以用默认地址
   如果下载慢，可以改为国内源

7. 磁盘
   选择 Use an entire disk
   不需要手动分区

8. 文件系统确认
   选择 Continue

9. Profile setup
   Your name：Admin
   Your server's name：ubuntu-template
   Pick a username：admin，例如 admin
   Password：设置一个你能记住的密码

10. SSH Setup
   勾选 Install OpenSSH server
   不导入 SSH identity

11. Featured Server Snaps
   暂时不要选任何软件包

12. 等待安装完成
   看到 Reboot Now 后重启
```

重启时如果提示移除安装介质：

```text
1. 在 VMware 菜单中断开 CD/DVD
2. 或进入虚拟机设置，把 CD/DVD 的“启动时连接”取消
3. 回到虚拟机按 Enter
```

安装完成后，用安装时创建的账号登录，例如：

```text
用户名：admin
密码：安装时设置的密码
```

---

## 5. 配置模板机基础环境

以下操作在 `ubuntu-template` 中执行。

建议先确认模板机能联网：

```bash
ip addr
ping -c 3 192.168.56.2
ping -c 3 www.baidu.com
```

如果 `ping 192.168.56.2` 不通，先回 VMware 检查虚拟机网络是否是 NAT。如果能 ping IP，但不能 ping 域名，通常是 DNS 问题。

### 5.1 更新系统

```bash
sudo apt update
sudo apt upgrade -y
```

### 5.2 安装常用工具

```bash
sudo apt install -y vim curl wget net-tools openssh-server ca-certificates gnupg lsb-release open-vm-tools
```

### 5.3 启用 SSH

```bash
sudo systemctl enable ssh
sudo systemctl start ssh
sudo systemctl status ssh
```

看到 `active (running)` 就说明 SSH 已启动。

在 Windows CMD 中可以先测试模板机 SSH。先在模板机里查看 IP：

```bash
ip addr
```

假设模板机当前 DHCP 地址是 `192.168.56.128`，Windows CMD 测试：

```cmd
ssh admin@192.168.56.128
```

能登录说明 SSH 正常。

### 5.4 清理模板机身份信息

为了避免克隆后三台虚拟机的 machine-id 一样，模板机关机前执行：

```bash
sudo truncate -s 0 /etc/machine-id
sudo rm -f /var/lib/dbus/machine-id
sudo ln -sf /etc/machine-id /var/lib/dbus/machine-id
```

清理 cloud-init 残留：

```bash
sudo cloud-init clean || true
```

关闭模板机：

```bash
sudo shutdown now
```

### 5.5 创建模板快照

在 VMware 中右键 `ubuntu-template`：

```text
快照 -> 拍摄快照
```

快照名称建议：

```text
clean-ubuntu-template
```

---

## 6. 克隆三台虚拟机

在 VMware 中右键 `ubuntu-template`：

```text
管理 -> 克隆
```

选择：

```text
从当前状态或 clean-ubuntu-template 快照克隆
创建完整克隆
```

分别克隆出：

```text
k8s-master
k8s-worker01
k8s-worker02
```

建议使用完整克隆。完整克隆占用磁盘更多，但每台虚拟机互相独立，更适合长期学习和实验。

### 6.1 克隆 k8s-master

```text
1. 右键 ubuntu-template
2. 选择 管理
3. 选择 克隆
4. 克隆源选择 clean-ubuntu-template 快照
5. 克隆类型选择 创建完整克隆
6. 虚拟机名称填写 k8s-master
7. 位置选择 D:\VMwareVMs\k8s-master
8. 点击完成
```

### 6.2 克隆 k8s-worker01

```text
1. 右键 ubuntu-template
2. 选择 管理
3. 选择 克隆
4. 克隆源选择 clean-ubuntu-template 快照
5. 克隆类型选择 创建完整克隆
6. 虚拟机名称填写 k8s-worker01
7. 位置选择 D:\VMwareVMs\k8s-worker01
8. 点击完成
```

### 6.3 克隆 k8s-worker02

```text
1. 右键 ubuntu-template
2. 选择 管理
3. 选择 克隆
4. 克隆源选择 clean-ubuntu-template 快照
5. 克隆类型选择 创建完整克隆
6. 虚拟机名称填写 k8s-worker02
7. 位置选择 D:\VMwareVMs\k8s-worker02
8. 点击完成
```

克隆完成后，先不要同时启动三台。建议一台一台启动并配置主机名和 IP，避免刚启动时 DHCP 地址或主机名混淆。

---

## 7. 调整每台虚拟机硬件配置

克隆完成后，分别关闭虚拟机，在 VMware 设置中调整：

入口：

```text
选中虚拟机 -> 编辑虚拟机设置
```

每台都检查：

```text
CD/DVD：不要继续挂载 ISO，或取消“启动时连接”
网络适配器：NAT
处理器：按规划设置
内存：按规划设置
硬盘：按规划设置
```

### 7.1 k8s-master

```text
CPU：2 核
内存：4GB
磁盘：40GB
网络：NAT / VMnet8
```

### 7.2 k8s-worker01

```text
CPU：2 核
内存：4GB
磁盘：40GB
网络：NAT / VMnet8
```

### 7.3 k8s-worker02

```text
CPU：2 核
内存：6GB
磁盘：60GB
网络：NAT / VMnet8
```

如果 worker02 克隆自 40GB 模板机，可以先使用 40GB，后续 Jenkins 镜像构建空间不足时再扩容。

如果要现在扩容 worker02 磁盘：

```text
1. 关闭 k8s-worker02
2. 编辑虚拟机设置
3. 选择硬盘
4. 点击扩展
5. 输入 60GB
6. 启动系统后再在 Linux 中扩展分区和文件系统
```

初学阶段可以先不扩容，等磁盘不够时再处理。

---

## 8. 配置主机名

分别启动三台虚拟机。

建议顺序：

```text
1. 先启动 k8s-master，配置完成后关机或保持运行
2. 再启动 k8s-worker01，配置完成后关机或保持运行
3. 最后启动 k8s-worker02
```

在 master 执行：

```bash
sudo hostnamectl set-hostname k8s-master
```

在 worker01 执行：

```bash
sudo hostnamectl set-hostname k8s-worker01
```

在 worker02 执行：

```bash
sudo hostnamectl set-hostname k8s-worker02
```

然后三台都重新登录一次，或者重启：

```bash
sudo reboot
```

### 8.1 推荐方式：从 Windows SSH 进去配置

VMware 黑色控制台里复制粘贴不稳定，推荐先在每台虚拟机里手动执行一条命令查看临时 IP：

```bash
ip addr
```

找到 `ens33` 下的 DHCP 地址，例如：

```text
k8s-master 临时 IP：192.168.56.129
k8s-worker01 临时 IP：192.168.56.130
k8s-worker02 临时 IP：192.168.56.131
```

然后在 Windows PowerShell 中用 SSH 连接进去执行后续配置。示例用户名为 `yexw`，如果安装系统时使用的是其他用户名，需要替换。

```powershell
ssh-keygen -R 192.168.56.129
ssh yexw@192.168.56.129
```

首次连接时输入：

```text
yes
```

然后输入 Linux 用户密码。

---

## 9. 配置固定 IP

以下以 Ubuntu Server 的 netplan 为例。

### 9.1 查看网卡名称

三台机器都执行：

```bash
ip addr
```

常见网卡名：

```text
ens33
ens160
```

下面示例使用 `ens33`。如果你的机器不是这个名称，需要替换成实际网卡名。

### 9.2 备份原网络配置

三台机器都先执行：

```bash
ls /etc/netplan
```

常见文件名是：

```text
00-installer-config.yaml
50-cloud-init.yaml
```

如果同时存在 `00-installer-config.yaml` 和 `50-cloud-init.yaml`，建议保留 `00-installer-config.yaml`，把 `50-cloud-init.yaml` 改名备份，避免两个 netplan 配置冲突。

先备份并处理 cloud-init 配置：

```bash
sudo cp /etc/netplan/00-installer-config.yaml /etc/netplan/00-installer-config.yaml.bak 2>/dev/null || true
sudo mv /etc/netplan/50-cloud-init.yaml /etc/netplan/50-cloud-init.yaml.bak 2>/dev/null || true
```

如果你的文件名不是 `00-installer-config.yaml`，后续命令需要换成实际文件名。

### 9.3 修改 master IP

在 `k8s-master` 执行：

```bash
sudo vim /etc/netplan/00-installer-config.yaml
```

写入完整内容：

```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.10/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
```

应用配置前先检查格式：

```bash
sudo netplan generate
```

如果没有输出错误，再应用：

```bash
sudo netplan apply
```

### 9.4 修改 worker01 IP

在 `k8s-worker01` 执行：

```bash
sudo vim /etc/netplan/00-installer-config.yaml
```

写入完整内容：

```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.11/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
```

检查并应用：

```bash
sudo netplan generate
sudo netplan apply
```

### 9.5 修改 worker02 IP

在 `k8s-worker02` 执行：

```bash
sudo vim /etc/netplan/00-installer-config.yaml
```

写入完整内容：

```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.12/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
```

检查并应用：

```bash
sudo netplan generate
sudo netplan apply
```

### 9.6 netplan 注意事项

YAML 对缩进很敏感，注意：

```text
1. 缩进使用空格，不要使用 Tab
2. ens33 必须换成你自己的网卡名
3. addresses、routes、nameservers 的层级不能写错
4. 三台机器 IP 不能重复
5. 网关必须是 VMnet8 的 NAT 网关
```

如果 `sudo netplan apply` 后网络断了，可以在 VMware 控制台里登录虚拟机，恢复备份：

```bash
sudo cp /etc/netplan/00-installer-config.yaml.bak /etc/netplan/00-installer-config.yaml
sudo netplan apply
```

### 9.7 验证 IP

三台都执行：

```bash
ip addr
ip route
ping -c 3 192.168.56.2
ping -c 3 www.baidu.com
```

如果能 ping 通网关和外网域名，说明网络正常。

Windows CMD 也测试一下：

```cmd
ping 192.168.56.10
ping 192.168.56.11
ping 192.168.56.12
```

如果 Windows 能 ping 通三台虚拟机，后续 SSH、Jenkins 访问、Kubernetes 调试会方便很多。

### 9.8 一键配置 master

适用于当前 master 临时 IP 类似 `192.168.56.129`，已通过 Windows PowerShell SSH 登录到 master 的情况。

```bash
sudo hostnamectl set-hostname k8s-master

sudo cp /etc/netplan/00-installer-config.yaml /etc/netplan/00-installer-config.yaml.bak 2>/dev/null || true
sudo mv /etc/netplan/50-cloud-init.yaml /etc/netplan/50-cloud-init.yaml.bak 2>/dev/null || true

sudo tee /etc/netplan/00-installer-config.yaml >/dev/null <<'EOF'
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.10/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
EOF

sudo tee -a /etc/hosts >/dev/null <<'EOF'

192.168.56.10 k8s-master
192.168.56.11 k8s-worker01
192.168.56.12 k8s-worker02
EOF

sudo systemctl enable --now ssh

sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system

sudo netplan generate
sudo netplan apply

sudo reboot
```

执行后 SSH 会断开，master 固定 IP 变为：

```text
192.168.56.10
```

重启后重新连接：

```powershell
ssh yexw@192.168.56.10
```

### 9.9 一键配置 worker01

适用于当前 worker01 临时 IP 类似 `192.168.56.130`，已通过 Windows PowerShell SSH 登录到 worker01 的情况。

```bash
sudo hostnamectl set-hostname k8s-worker01

sudo cp /etc/netplan/00-installer-config.yaml /etc/netplan/00-installer-config.yaml.bak 2>/dev/null || true
sudo mv /etc/netplan/50-cloud-init.yaml /etc/netplan/50-cloud-init.yaml.bak 2>/dev/null || true

sudo tee /etc/netplan/00-installer-config.yaml >/dev/null <<'EOF'
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.11/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
EOF

sudo tee -a /etc/hosts >/dev/null <<'EOF'

192.168.56.10 k8s-master
192.168.56.11 k8s-worker01
192.168.56.12 k8s-worker02
EOF

sudo systemctl enable --now ssh

sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system

sudo netplan generate
sudo netplan apply

sudo reboot
```

执行后 SSH 会断开，worker01 固定 IP 变为：

```text
192.168.56.11
```

重启后重新连接：

```powershell
ssh yexw@192.168.56.11
```

### 9.10 一键配置 worker02

适用于当前 worker02 临时 IP 类似 `192.168.56.131`，已通过 Windows PowerShell SSH 登录到 worker02 的情况。

```bash
sudo hostnamectl set-hostname k8s-worker02

sudo cp /etc/netplan/00-installer-config.yaml /etc/netplan/00-installer-config.yaml.bak 2>/dev/null || true
sudo mv /etc/netplan/50-cloud-init.yaml /etc/netplan/50-cloud-init.yaml.bak 2>/dev/null || true

sudo tee /etc/netplan/00-installer-config.yaml >/dev/null <<'EOF'
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.56.12/24
      routes:
        - to: default
          via: 192.168.56.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
EOF

sudo tee -a /etc/hosts >/dev/null <<'EOF'

192.168.56.10 k8s-master
192.168.56.11 k8s-worker01
192.168.56.12 k8s-worker02
EOF

sudo systemctl enable --now ssh

sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system

sudo netplan generate
sudo netplan apply

sudo reboot
```

执行后 SSH 会断开，worker02 固定 IP 变为：

```text
192.168.56.12
```

重启后重新连接：

```powershell
ssh yexw@192.168.56.12
```

### 9.11 三台机器最终验证

三台机器都重启完成后，在 Windows PowerShell 中测试：

```powershell
ssh yexw@192.168.56.10
ssh yexw@192.168.56.11
ssh yexw@192.168.56.12
```

每台机器登录后执行：

```bash
hostname
ip addr
free -h
ping -c 3 192.168.56.2
ping -c 3 k8s-master
ping -c 3 k8s-worker01
ping -c 3 k8s-worker02
ping -c 3 www.baidu.com
```

期望结果：

```text
k8s-master   -> 192.168.56.10
k8s-worker01 -> 192.168.56.11
k8s-worker02 -> 192.168.56.12
Swap 显示为 0
三台机器互相能 ping 通
三台机器都能访问外网域名
```

---

## 10. 配置 hosts

三台虚拟机都执行：

```bash
sudo vim /etc/hosts
```

加入：

```text
192.168.56.10 k8s-master
192.168.56.11 k8s-worker01
192.168.56.12 k8s-worker02
```

验证：

```bash
ping -c 3 k8s-master
ping -c 3 k8s-worker01
ping -c 3 k8s-worker02
```

三台之间都能互相 ping 通，后续 Kubernetes 才能正常加入节点。

---

## 11. 配置 SSH 连接

### 11.1 确认 SSH 服务

三台都执行：

```bash
sudo systemctl status ssh
```

如果没有启动：

```bash
sudo systemctl enable --now ssh
```

### 11.2 从 Windows 连接虚拟机

在 Windows PowerShell 中测试：

```powershell
ssh admin@192.168.56.10
ssh admin@192.168.56.11
ssh admin@192.168.56.12
```

把 `admin` 换成你安装 Ubuntu 时创建的用户名。

---

## 12. Kubernetes 前置设置

以下操作建议在三台虚拟机都执行。也可以等后续安装 Kubernetes 时再做。

### 12.1 关闭 swap

```bash
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab
```

验证：

```bash
free -h
```

`Swap` 应该显示为 0。

### 12.2 开启内核模块

```bash
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter
```

### 12.3 配置网络转发

```bash
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system
```

---

## 13. 创建阶段快照

当三台虚拟机完成以下项目后，建议分别拍快照：

```text
主机名已修改
固定 IP 已配置
hosts 已配置
SSH 可连接
三台机器互相 ping 通
swap 已关闭
Kubernetes 前置内核参数已配置
```

快照名称建议：

```text
ready-for-k8s
```

后续安装 Kubernetes 出错时，可以直接回滚到这个状态。

---

## 14. 最终验收清单

在 Windows PowerShell 中确认：

```powershell
ping 192.168.56.10
ping 192.168.56.11
ping 192.168.56.12
```

在三台虚拟机中确认：

```bash
hostname
ip addr
ping -c 3 k8s-master
ping -c 3 k8s-worker01
ping -c 3 k8s-worker02
ping -c 3 www.baidu.com
free -h
```

期望结果：

```text
三台虚拟机 IP 固定
三台虚拟机主机名正确
三台虚拟机之间互通
三台虚拟机可以访问外网
Windows 宿主机可以 SSH 到三台虚拟机
swap 已关闭
```

达到以上结果后，就可以继续安装：

```text
containerd
kubeadm / kubelet / kubectl
Calico 网络插件
Jenkins
Docker 构建环境
镜像仓库
微服务部署清单
```

---

## 15. 常见问题

### 15.1 虚拟机无法访问外网

检查：

```text
VMware VMnet8 是否启用 NAT
虚拟机网卡是否选择 NAT
固定 IP 的网关是否等于 VMnet8 网关
DNS 是否配置
Windows 防火墙是否拦截
```

虚拟机内测试：

```bash
ping -c 3 192.168.56.2
ping -c 3 8.8.8.8
ping -c 3 www.baidu.com
```

如果能 ping 通 `8.8.8.8`，但不能 ping 通域名，通常是 DNS 配置问题。

### 15.2 Windows 无法 SSH 到虚拟机

检查：

```text
虚拟机 SSH 服务是否启动
虚拟机 IP 是否正确
Windows 和虚拟机是否在同一 VMware NAT 网络可达
Ubuntu 防火墙是否拦截 22 端口
```

Ubuntu 中查看：

```bash
sudo systemctl status ssh
sudo ufw status
```

### 15.3 克隆后三台机器 IP 冲突

可能原因：

```text
三台机器仍然使用 DHCP
三台机器配置了相同静态 IP
克隆后没有重新生成机器身份
```

处理：

```bash
sudo truncate -s 0 /etc/machine-id
sudo rm -f /var/lib/dbus/machine-id
sudo systemd-machine-id-setup
sudo reboot
```

然后重新检查 IP 配置。

### 15.4 Kubernetes 节点加入失败

先确认基础网络：

```bash
ping -c 3 k8s-master
ping -c 3 192.168.56.10
```

再确认 swap：

```bash
free -h
```

如果 swap 没关闭，Kubernetes 初始化或加入节点可能失败。

### 15.5 虚拟机卡顿

建议：

```text
不要给虚拟机分配超过宿主机 70% 的内存
不要同时运行太多无关软件
虚拟磁盘放 SSD
关闭不需要的桌面环境，使用 Ubuntu Server
Jenkins 所在 worker02 给更大磁盘
```

### 15.6 创建虚拟机时报 VMX 拒绝访问

错误示例：

```text
Unable to create a new virtual machine:
Cannot open configuration file "D:\DevTools\vm\VM1.vmx": 拒绝访问.
```

处理方式：

```text
1. 关闭 VMware Workstation。
2. 右键 VMware Workstation，选择“以管理员身份运行”。
3. 不要继续使用 D:\DevTools\vm。
4. 新建并使用 D:\VMwareVMs 作为虚拟机目录。
5. 如果失败目录中有残留的 VM1 文件夹，确认不需要后手动删除。
```

推荐目录：

```text
D:\VMwareVMs\ubuntu-template
D:\VMwareVMs\k8s-master
D:\VMwareVMs\k8s-worker01
D:\VMwareVMs\k8s-worker02
```

### 15.7 CMD 下载 ISO 命令报错

如果看到：

```text
curl: (3) URL rejected: Bad hostname
'-o' 不是内部或外部命令
```

说明你在 CMD 中使用了 PowerShell 的换行写法。CMD 中必须使用一整行：

```cmd
curl.exe -L -C - -o "D:\DevTools\ISO\ubuntu-24.04.4-live-server-amd64.iso" "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/24.04.4/ubuntu-24.04.4-live-server-amd64.iso"
```
