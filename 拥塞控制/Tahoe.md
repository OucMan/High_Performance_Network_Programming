# TCP Tahoe

Tahoe是最早的应用在TCP中的拥塞控制机制，它的出现可以追溯到上世纪80年代，具体包括了慢开始、拥塞避免以及快重传三个机制。

## 算法详解

正式介绍Tahoe之前，先来认识一下需要用到的一些变量：

* cwnd：拥塞窗口
* ssthresh：慢开始门限

### 慢开始（Slow Start）

当TCP连接建立时，首先进入慢开始状态，在该状态，cwnd的大小初始化为一个MSS(maximum segment size)，同时把ssthresh设为64KB。此阶段，cwnd的值小于ssthresh，并且每收到一个ACK，cwnd的值便会加1，因此每经过一个RTT时间，window的值便会变成上个RTT时window值的2倍。也就是说，在这个阶段，cwnd的值会以指数（2的倍数）的方式增加。

### 拥塞避免（Congestion Avoidance）

当cwnd的值大于ssthresh，TCP便进入拥塞避免阶段。在这个阶段，cwnd的值以线性方式增长，大约每经过一个RTT，cwnd的值就会增加一个单位，以避免cwnd的值增加太快而发生数据报丢失。如果检测到数据报丢失或超时（timeout），则TCP的传送端会将ssthresh值设为发生拥塞时的cwnd值的一半，并重设cwnd的值为1，接着进入Slow Start阶段重传丢失的数据报。

### 快重传（Fast Retransmit）

Tahoe中，TCP发送者有两种方式发现数据报丢失。

* 发件人“超时”。 发送方对发送的每个数据包都设置了超时，当在超时时间内未收到对该数据包的确认，则认为数据包丢失，然后将重新发送该数据包并将拥塞窗口设置为1进入慢开始阶段。
* 接收者发回“重复ack”。 在TCP中，接收方仅确认按顺序发送的数据包，如果数据包发送顺序不正确，它将发出对最后看到的数据包的确认信息。 因此，如果接收方已经接收到1、2和3段，然后又接收到5段，则由于5乱序，它将再次确认3段。 在Tahoe中，如果发件人收到3个重复的ack，则认为该数据包丢失。 这被称为“**快重传**”，因为它不等待超时发生。

## 仿真实验

代码

## 总结
