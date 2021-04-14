# SoftRoCE

## 概述

在SoftRoCE的安装-1一文中，介绍了在CentOS7中安装SoftRocE的过程，本文则介绍在Ubuntu 20.04.2 LTS上安装Soft-RoCE的过程，更加简单。

## 环境准备

一个虚拟子网上的两台Ubuntu虚拟机，两台虚拟机上运行Soft-RoCE。因此我们创建两个虚拟机，设置其网络为仅主机模式VMnet1。

![仅主机网络模式](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/host-only.png)

注：刚开始可以将网络模式设置为NAT，因为需要安装一些软件，比如我这个虚拟机安装完成后，都没有ifconfig，因此得安装net-tools。

## 安装

在我们使用的Ubuntu 20.04.2 LTS系统版本上，内核已经打开了Infiniband和RXE（即Soft-RoCE功能的软件实体）的相关选项，可以通过如下命令确认
```
cat /boot/config-$(uname -r) | grep RXE
```
如果CONFIG_RDMA_RXE的值为y或者m，表示当前的操作系统可以使用RXE。如果该选项值为n或者搜索不到RXE，那么很遗憾你可能需要重新编译内核。

这里，我们可以很顺利地看到CONFIG_RDMA_RXE的值为m。

![内核加载RXE情况](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/rxe-core.png)

接着来安装用户态动态链接库，也就是rdma-core，运行下面的命令即可
```
sudo apt-get install libibverbs1 ibverbs-utils librdmacm1 libibumad3 ibverbs-providers rdma-core
```

安装完上述软件之后，可以执行ibv_devices看看有没有报错：

![RDMA设备列表](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/ibv-devices.png)

这是个基于verbs接口编写的小程序，用来获取并打印出当前系统中的RDMA设备列表（现在当然是空的，因为我们还没有添加Soft-RoCE设备）。

自此，Soft-RoCE的安装就完成了。后面，我们使用相同的步骤安装另一个虚拟机，然后将两个虚拟机的网络配置为仅主机模式。

我们看一下宿主机VMnet1网卡，以及两个虚拟机的ip地址:

![宿主机IP](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/host-ip.png)
![虚拟机A IP](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/vma-ip.png)
![虚拟机B IP](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/vmb-ip.png)

宿主机VMnet1网卡的ip地址为192.168.10.1，虚拟机A的ip地址为192.168.10.130，虚拟机B的ip地址为192.168.10.131，可见这三个网卡都处于192.168.10.x网段。

接下来我们来测试一下安装好的Soft-RoCE

首先加载内核驱动，modprobe会自动加载依赖的其他驱动。
```
sudo modprobe rdma_rxe
```

然后进行用户态配置，增加一个RDMA设备
```
sudo rdma link add rxe_0 type rxe netdev ens33
```
该命令创建了一个名为rxe_0的RDMA设备，ens33为Soft-RoCE设备所绑定的网络设备名，也就是刚才ifconfig看到的本机网卡名。

接着用命令rdma link来查看是否添加成功

![RDMA LINK](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/rdma-link.png)

也可以跑下前文提到的ibv_devices程序了，可以看到已经在设备列表里了：

![RDMA设备列表](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/ibv-devices-one.png)

可以看下这个虚拟RDMA设备的信息：

![RDMA设备信息](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/device-info.png)

注意上面的命令需要在两个虚拟机上都运行。

接下来执行perftest测试，首先我们将虚拟机B（192.168.10.131）设置为服务器，虚拟机A（192.168.10.130）设置为客户端。

在虚拟机B执行命令
```
ib_send_bw -d rxe_0
```

在虚拟机A上执行命令
```
ib_send_bw -d rxe_0 192.168.10.131
```

然后得到结果

虚拟机A

![客户端](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/client.png)

虚拟机B

![服务器](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/server.png)

然后我们可以在宿主机上使用wireshark抓包看看，打开Wireshark，选择宿主机和两台虚拟机处于同一个子网的虚拟网卡VMnet1，然后再次执行上述的ib_send_bw，结果就可以看到RRoCE（Routable RoCE，即可以被路由的RoCE，RoCE v2）报文。

![Wireshark报文](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/wireshark.png)

随便选中一个条目就可以看到每一层报文的内容了，从上到下分别是：物理层-->以太网链路层-->IPv4网络层-->UDP传输层-->IB传输层（BTH头和iCRC校验）-->数据。

![Roce v2报文](https://github.com/OucMan/High_Performance_Network_Programming/blob/master/RDMA/pic/wireshark2.png)



## 其它安装

除了Soft-RoCE软件栈之外，还有两个工具要安装：iproute2和perftest
```
sudo apt-get install iproute2
sudo apt-get install perftest
```
iproute2是用来替代net-tools软件包的，是一组开源的网络工具集合，比如用更强大ip命令替换了以前常用的ifconfig。我们需要其中的rdma工具来对RXE进行配置。

perftest是一个基于Verbs接口开发的开源RDMA性能测试工具，可以对支持RDMA技术的节点进行带宽和时延测试。

