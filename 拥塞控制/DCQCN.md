# DCQCN


## 背景

本文开启了RDMA中的拥塞控制算法的研究。RDMA（Remote Direct Memory Access）技术是一种直接在内存和内存之间进行数据互传的技术，在数据传输的过程中完全实现了Kernel Bypass，CPU不需要参与操作，这也是RDMA在降低CPU消耗的同时，还能带来低时延的原因。最初RDMA的实现为IB网络，它需要一套新的网络基础设施和网卡来支持IB协议，这带来了成本问题。因此，为了将RDMA与现有的以太网基础设施融合，RoCE和iWarp被提出来。关于三种实现之间的对比这里不再赘述，只需要知道市场份额方面，Roce v2领先。Roce中，一个关键的问题就是如何是的现有的网络成为无损网络，目前采用的技术就是IEEE DCB中的PFC（优先级流控制）。但是部署在大规模网络上需要用到的PFC机制会产生一下两种局限性：

### Unfairness




### Victim flow









## 论文名

Congestion Control for Large-Scale RDMA Deployments
