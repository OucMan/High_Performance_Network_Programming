# SoftRoCE

## 概述

RDMA是基于Infiniband技术的内存直接传送，无需内核参与，发送端硬件网卡直接操作应用空间buffer数据，进行协议封装后直接传送至接收端端网卡，解析报文后，将数据直接直接放到应用空间buffer。IB需要HPC领域的专用硬件，ROCE则是RDMA协议在普通以太网卡的实现，RoCEv1是在MAC上的二层封装，局域网内可以，要通过路由器则需要RoCEv2, 基于UDP的版本。鉴于目前本人没有支持RoCE的网卡，因此使用SoftRoCE，基于普通网卡用软件实现了硬件要做的事情。

## 环境准备

两台虚拟机，系统为CentOS 7.3，可相互通信

## 依赖安装

```
yum install epel-release -y  
yum install gcc gcc-c++ bc openssl-devel automake ncurses-devel libibverbs -y  
yum install libibverbs-devel libibverbs-utils librdmacm librdmacm-devel librdmacm-utils perl-Switch elfutils-libelf-devel  -y
```

## rxe-dev下载与安装

rxe-dev下载地址为 Github: https://github.com/SoftRoCE/rxe-dev.git

注意使用v18版本，即rxe-dev-rxe_submission_v18，后续操作均使用root权限执行

```
unzip rxe-dev-rxe_submission_v18.zip
cd rxe-dev-rxe_submission_v18/
cp /boot/config-3.10.0-514.el7.x86_64 .config
```

执行命令
```
make menuconfig
```
会出现选择界面（如果没出现，需要安装 ncurse-devel）

输入 "/" ，然后输入 rxe，按下 enter，会查找有关 rxe 的选择项。

输入数字 1，就会选择到“Software RDMA over Ethernet (ROCE) driver”的设置，输入 "M" ，选中 RDMA 的配置，如果 输不了 M，那就输入空格。

移动到保存按钮，回车，装保存到.config中，退出安装界面（exit）。

然后 vi .config 来确认 

```
CONFIG_RDMA_RXE = m
CONFIG_INFINIBAND_ADDR_TRANS = y
CONFIG_INFINIBAND_ADDR_TRANS_CONFIGFS = y
```

继续执行命令
```
make -j 2  # 因为我的虚拟机只有两个核，所以参数为2  
make modules_install
make install  
make headers_install INSTALL_HDR_PATH=/usr
```

等到不短的一段时间，安装成功，这时候虚拟机上多了一个4.7.0-rc3内核，以后在启动的时候，记得选择4.7.0-rc3内核。

## 关于librxe-dev的安装

我们没有安装librxe-dev，在安装librxe-dev的过程中会报错，错误信息如下
```
hecking for ibv_get_device_list in -libverbs... 
yes
checking infiniband/driver.h usability... no
checking infiniband/driver.h presence... no
checking for infiniband/driver.h... no
configure: error: <infiniband/driver.h> not found. librxe requires libibverbs.
```

问题不大，我们不用理睬就好，因为经过上述的操作我们已经有了rdma-core，它可以替代librxe-dev。

https://wangmingjun.com/2018/09/03/how-to-build-the-development-environment-of-software-rdma-over-converged-ethernet-roce/

## 重启操作系统

重启操作系统，在开机启动时，选择4.7.0-rc3内核

启动后，查看内核版本
```
uname -r
```

## 验证RDMA

```
[root@localhost jojo]# rxe_cfg start 
  Name        Link  Driver  Speed  NMTU  IPv4_addr      RDEV  RMTU  
  ens33       yes   e1000          1500  10.10.10.152               
  virbr0      no    bridge         1500  192.168.122.1              
  virbr0-nic  no    tun            1500                             
[root@localhost jojo]# rxe_cfg add ens33
[root@localhost jojo]# rxe_cfg status
  Name        Link  Driver  Speed  NMTU  IPv4_addr      RDEV  RMTU          
  ens33       yes   e1000          1500  10.10.10.152   rxe0  1024  (3)  
  virbr0      no    bridge         1500  192.168.122.1                      
  virbr0-nic  no    tun            1500                                     
[root@localhost jojo]# ibv_devices
    device          	   node GUID
    ------          	----------------
    rxe0            	020c29fffe7f1b7f
[root@localhost jojo]# ibv_devinfo rxe0
hca_id:	rxe0
	transport:			InfiniBand (0)
	fw_ver:				0.0.0
	node_guid:			020c:29ff:fe7f:1b7f
	sys_image_guid:			0000:0000:0000:0000
	vendor_id:			0x0000
	vendor_part_id:			0
	hw_ver:				0x0
	phys_port_cnt:			1
		port:	1
			state:			PORT_ACTIVE (4)
			max_mtu:		4096 (5)
			active_mtu:		1024 (3)
			sm_lid:			0
			port_lid:		0
			port_lmc:		0x00
			link_layer:		Ethernet
```
没有问题！

接下来再另一个虚拟机上执行上述的安装操作，这样我们就有两个支持RoCE的虚拟机了。

## softRoCE连通性测试

### 测试前准备

清空Iptable的规则
```
iptables -F
```

关闭selinux
```
vim /etc/selinux/config
SELINUX=disabled # selinux都禁用
```

### rping

已知服务器IP地址为10.10.10.152

在服务器上执行
```
rping -s -a 10.10.10.152 -v -C 10
```

在客户机上执行
```
rping -c -a 10.10.10.152 -v -C 10
```

结果为
```
ping data: rdma-ping-0: ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqr
ping data: rdma-ping-1: BCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrs
ping data: rdma-ping-2: CDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrst
ping data: rdma-ping-3: DEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstu
ping data: rdma-ping-4: EFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuv
ping data: rdma-ping-5: FGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvw
ping data: rdma-ping-6: GHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwx
ping data: rdma-ping-7: HIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxy
ping data: rdma-ping-8: IJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz
ping data: rdma-ping-9: JKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyzA
```

### ibv_rc_pingpong

在服务器上执行
```
ibv_rc_pingpong -d rxe0 -g 0
```

在客户机上执行
```
ibv_rc_pingpong -d rxe0 -g 0 10.10.10.152
```

结果为

服务器
```
[root@localhost jojo]# ibv_rc_pingpong -d rxe0 -g 0
  local address:  LID 0x0000, QPN 0x000011, PSN 0xad2f65, GID fe80::20c:29ff:fe7f:1b7f
  remote address: LID 0x0000, QPN 0x000011, PSN 0xfd5a98, GID fe80::20c:29ff:fe73:c400
```

客户端
```
[root@localhost jeff]# ibv_rc_pingpong -d rxe0 -g 0 10.10.10.152
  local address:  LID 0x0000, QPN 0x000011, PSN 0xfd5a98, GID fe80::20c:29ff:fe73:c400
  remote address: LID 0x0000, QPN 0x000011, PSN 0xad2f65, GID fe80::20c:29ff:fe7f:1b7f
```

没有问题！

## 总结

至此已完成SoftRoCE的安装，后续会基于次环境完成RoCE实例编程相关的工作。

