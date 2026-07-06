# 07 - VMware 三台 Linux 虚拟机搭建

## 目录

- [1. 目标说明](#1-目标说明)
- [2. 宿主机要求](#2-宿主机要求)
- [3. 虚拟机规划](#3-虚拟机规划)
- [4. VMware 网络规划](#4-vmware-网络规划)
- [5. 创建第一台 Linux 模板机](#5-创建第一台-linux-模板机)
- [6. 配置 Linux 基础环境](#6-配置-linux-基础环境)
- [7. 克隆生成三台虚拟机](#7-克隆生成三台虚拟机)
- [8. 配置固定 IP](#8-配置固定-ip)
- [9. 配置主机名和 hosts](#9-配置主机名和-hosts)
- [10. 配置 SSH 免密登录](#10-配置-ssh-免密登录)
- [11. 稳定性优化](#11-稳定性优化)
- [12. 验证检查](#12-验证检查)
- [13. 常见问题](#13-常见问题)

---

## 1. 目标说明

本文档用于在 Windows 宿主机上使用 VMware Workstation 搭建 3 台 Linux 虚拟机，适合作为 Docker、Kubernetes、微服务、中间件集群或数据库集群的基础实验环境。

搭建完成后，将得到 3 台可互相访问、IP 固定、主机名固定、支持 SSH 登录的 Linux 虚拟机。

---

## 2. 宿主机要求

| 项目 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 4 核 | 8 核及以上 |
| 内存 | 16GB | 32GB 及以上 |
| 磁盘 | 150GB 可用空间 | SSD，200GB 以上可用空间 |
| 虚拟化 | 开启 VT-x / AMD-V | BIOS 中确认已开启 |
| VMware | Workstation 16+ | Workstation Pro 17+ |

建议不要给虚拟机分配超过宿主机 70% 的内存和 CPU，否则宿主机会明显卡顿，虚拟机也不稳定。

---

## 3. 虚拟机规划

### 3.1 操作系统建议

推荐选择以下任意一个 Linux 发行版：

| 系统 | 说明 |
|------|------|
| Ubuntu Server 22.04 LTS | 社区资料多，适合学习和部署 Docker/Kubernetes |
| Rocky Linux 9 | 接近企业级 CentOS/RHEL 环境 |
| AlmaLinux 9 | 接近企业级 CentOS/RHEL 环境 |

如果没有特殊要求，建议使用 `Ubuntu Server 22.04 LTS`。

### 3.2 三台虚拟机规划

| 节点 | 主机名 | IP 地址 | CPU | 内存 | 磁盘 |
|------|--------|---------|-----|------|------|
| 节点 1 | linux-node1 | 192.168.100.101 | 2 核 | 4GB | 40GB |
| 节点 2 | linux-node2 | 192.168.100.102 | 2 核 | 4GB | 40GB |
| 节点 3 | linux-node3 | 192.168.100.103 | 2 核 | 4GB | 40GB |

如果宿主机只有 16GB 内存，可以把每台虚拟机内存调整为 2GB。

---

## 4. VMware 网络规划

推荐使用 VMware 的 `NAT` 网络模式。

### 4.1 NAT 模式优点

- 虚拟机可以访问外网。
- 宿主机可以访问虚拟机。
- 三台虚拟机之间可以互相访问。
- 不依赖公司或家庭路由器分配 IP。
- 网络环境更稳定，适合长期学习和测试。

### 4.2 配置 VMware NAT 网段

打开 VMware：

```text
编辑 -> 虚拟网络编辑器 -> 更改设置 -> 选择 VMnet8
```

建议配置如下：

```text
子网 IP：192.168.100.0
子网掩码：255.255.255.0
网关 IP：192.168.100.2
```

如果当前 VMnet8 已经使用其他网段，也可以继续使用现有网段，只要后续三台虚拟机 IP 在同一网段即可。

---

## 5. 创建第一台 Linux 模板机

### 5.1 新建虚拟机

在 VMware 中选择：

```text
文件 -> 新建虚拟机 -> 自定义
```

建议配置：

| 配置项 | 建议值 |
|--------|--------|
| 硬件兼容性 | 默认即可 |
| 安装来源 | Linux ISO 镜像 |
| 客户机操作系统 | Linux |
| 虚拟机名称 | linux-template |
| CPU | 2 核 |
| 内存 | 4GB |
| 网络 | NAT |
| 磁盘控制器 | 默认 |
| 磁盘类型 | NVMe 或 SCSI |
| 磁盘大小 | 40GB |
| 磁盘存储 | 拆分为多个文件或单个文件均可 |

如果使用 SSD，虚拟磁盘选择单个文件性能略好；如果需要经常移动虚拟机，拆分为多个文件更方便。

### 5.2 安装系统

安装时建议：

- 选择最小化安装或 Server 安装。
- 创建普通用户，例如 `admin`。
- 勾选安装 OpenSSH Server。
- 时区选择 `Asia/Shanghai`。
- 磁盘分区使用默认自动分区即可。

---

## 6. 配置 Linux 基础环境

以下操作先在模板机 `linux-template` 中完成。

### 6.1 更新系统

Ubuntu 执行：

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y vim curl wget net-tools openssh-server
```

Rocky Linux / AlmaLinux 执行：

```bash
sudo dnf update -y
sudo dnf install -y vim curl wget net-tools openssh-server
```

### 6.2 启用 SSH

```bash
sudo systemctl enable ssh || sudo systemctl enable sshd
sudo systemctl start ssh || sudo systemctl start sshd
```

### 6.3 关闭模板机

基础环境配置完成后，关闭模板机：

```bash
sudo shutdown now
```

然后在 VMware 中给模板机创建一个快照，快照名称建议为：

```text
clean-template
```

---

## 7. 克隆生成三台虚拟机

在 VMware 中右键模板机：

```text
管理 -> 克隆
```

建议选择：

```text
完整克隆
```

分别克隆出三台虚拟机：

```text
linux-node1
linux-node2
linux-node3
```

完整克隆占用磁盘更多，但稳定性更好，三台虚拟机互不依赖，适合长期使用。

---

## 8. 配置固定 IP

以下示例以 Ubuntu Server 22.04 为例。

### 8.1 查看网卡名称

登录每台虚拟机后执行：

```bash
ip addr
```

常见网卡名称可能是：

```text
ens33
ens160
```

下面示例使用 `ens33`，实际操作时请替换为自己的网卡名称。

### 8.2 修改 netplan 配置

编辑配置文件：

```bash
sudo vim /etc/netplan/00-installer-config.yaml
```

节点 1 配置：

```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    ens33:
      dhcp4: false
      addresses:
        - 192.168.100.101/24
      routes:
        - to: default
          via: 192.168.100.2
      nameservers:
        addresses:
          - 8.8.8.8
          - 114.114.114.114
```

节点 2 只需要把 IP 改为：

```text
192.168.100.102
```

节点 3 只需要把 IP 改为：

```text
192.168.100.103
```

应用网络配置：

```bash
sudo netplan apply
```

### 8.3 Rocky Linux / AlmaLinux 固定 IP 示例

查看连接名称：

```bash
nmcli connection show
```

假设连接名称为 `ens33`，节点 1 执行：

```bash
sudo nmcli connection modify ens33 ipv4.addresses 192.168.100.101/24
sudo nmcli connection modify ens33 ipv4.gateway 192.168.100.2
sudo nmcli connection modify ens33 ipv4.dns "8.8.8.8 114.114.114.114"
sudo nmcli connection modify ens33 ipv4.method manual
sudo nmcli connection up ens33
```

节点 2 和节点 3 分别把 IP 改为 `192.168.100.102`、`192.168.100.103`。

---

## 9. 配置主机名和 hosts

### 9.1 设置主机名

节点 1：

```bash
sudo hostnamectl set-hostname linux-node1
```

节点 2：

```bash
sudo hostnamectl set-hostname linux-node2
```

节点 3：

```bash
sudo hostnamectl set-hostname linux-node3
```

### 9.2 配置 hosts

三台机器都编辑：

```bash
sudo vim /etc/hosts
```

追加：

```text
192.168.100.101 linux-node1
192.168.100.102 linux-node2
192.168.100.103 linux-node3
```

---

## 10. 配置 SSH 免密登录

如果后续要部署集群，建议在节点 1 配置到其他节点的免密登录。

在 `linux-node1` 执行：

```bash
ssh-keygen -t rsa -b 4096
```

一路回车即可。

复制公钥到三台机器：

```bash
ssh-copy-id admin@linux-node1
ssh-copy-id admin@linux-node2
ssh-copy-id admin@linux-node3
```

验证：

```bash
ssh admin@linux-node2
ssh admin@linux-node3
```

其中 `admin` 替换为你安装 Linux 时创建的用户名。

---

## 11. 稳定性优化

### 11.1 克隆后重新生成 machine-id

克隆虚拟机后，每台机器建议执行：

```bash
sudo rm -f /etc/machine-id
sudo systemd-machine-id-setup
sudo reboot
```

这样可以避免三台机器使用相同的系统机器 ID。

### 11.2 禁用 swap

如果后续要搭建 Kubernetes，建议关闭 swap。

临时关闭：

```bash
sudo swapoff -a
```

永久关闭：

```bash
sudo sed -i.bak '/ swap / s/^/#/' /etc/fstab
```

### 11.3 时间同步

Ubuntu：

```bash
sudo timedatectl set-timezone Asia/Shanghai
sudo timedatectl set-ntp true
```

Rocky Linux / AlmaLinux：

```bash
sudo timedatectl set-timezone Asia/Shanghai
sudo systemctl enable --now chronyd
```

### 11.4 VMware Tools

Ubuntu：

```bash
sudo apt install -y open-vm-tools
sudo systemctl enable --now open-vm-tools
```

Rocky Linux / AlmaLinux：

```bash
sudo dnf install -y open-vm-tools
sudo systemctl enable --now vmtoolsd
```

### 11.5 快照建议

建议至少创建以下快照：

| 快照名称 | 创建时机 |
|----------|----------|
| clean-template | 模板机基础系统安装完成后 |
| node-ready | 三台节点网络和主机名配置完成后 |
| before-cluster | 安装 Docker/Kubernetes/中间件之前 |

---

## 12. 验证检查

### 12.1 查看 IP

三台机器分别执行：

```bash
ip addr
```

确认 IP 分别为：

```text
192.168.100.101
192.168.100.102
192.168.100.103
```

### 12.2 验证主机名

```bash
hostname
```

### 12.3 验证互通

在 `linux-node1` 执行：

```bash
ping -c 4 linux-node2
ping -c 4 linux-node3
```

在 `linux-node2` 执行：

```bash
ping -c 4 linux-node1
ping -c 4 linux-node3
```

### 12.4 验证外网

```bash
ping -c 4 8.8.8.8
curl -I https://www.baidu.com
```

### 12.5 验证 SSH

在宿主机 PowerShell 中执行：

```powershell
ssh admin@192.168.100.101
ssh admin@192.168.100.102
ssh admin@192.168.100.103
```

---

## 13. 常见问题

### 13.1 虚拟机无法访问外网

检查项：

1. VMware 虚拟机网络是否选择 NAT。
2. VMware NAT 服务是否启动。
3. 虚拟机网关是否配置为 VMnet8 的网关，例如 `192.168.100.2`。
4. DNS 是否配置正确。

Windows 中可以检查服务：

```text
VMware NAT Service
VMware DHCP Service
```

### 13.2 宿主机无法 SSH 连接虚拟机

检查项：

1. 虚拟机 SSH 服务是否启动。
2. 虚拟机 IP 是否正确。
3. Windows 防火墙或 Linux 防火墙是否拦截。
4. 虚拟机是否使用 NAT 或 Host-only 网络。

Linux 中检查 SSH：

```bash
sudo systemctl status ssh || sudo systemctl status sshd
```

### 13.3 三台虚拟机 IP 冲突

常见原因是克隆后没有修改固定 IP。分别登录三台机器检查：

```bash
ip addr
```

确保三台机器 IP 不重复。

### 13.4 克隆后主机名相同

分别在三台机器执行：

```bash
hostname
```

如果相同，重新执行：

```bash
sudo hostnamectl set-hostname linux-nodeX
```

其中 `linux-nodeX` 替换为对应节点名。

### 13.5 虚拟机运行卡顿

建议：

- 降低每台虚拟机内存，例如从 4GB 调整为 2GB。
- 不要同时运行过多软件。
- 虚拟磁盘放到 SSD。
- 关闭不需要的图形界面，优先使用 Server 版本。
- 不要给虚拟机分配过多 CPU 核心。

---

## 最终环境清单

| 检查项 | 结果 |
|--------|------|
| 三台虚拟机已创建 | 是 |
| 三台虚拟机 IP 固定 | 是 |
| 三台虚拟机主机名不同 | 是 |
| 三台虚拟机 hosts 已配置 | 是 |
| 三台虚拟机可以互相 ping 通 | 是 |
| 宿主机可以 SSH 登录虚拟机 | 是 |
| 已创建稳定快照 | 是 |

完成以上步骤后，三台 Linux 虚拟机环境即可作为后续部署 Docker、Kubernetes、数据库集群或微服务项目的基础环境。
