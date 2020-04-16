package com.badboy.host;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.badboy.codec.NettyMessageDecoder;
import com.badboy.codec.NettyMessageEncoder;
import com.badboy.handshake.LoginAuthReqHandler;
import com.badboy.heartbeat.HeartBeatReqHandler;
import com.badboy.message.Header;
import com.badboy.message.NettyMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class NettyClient {
	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	
	public void connect(int port, String host) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel arg0) throws Exception {
					arg0.pipeline().addLast(new NettyMessageDecoder(1024*1024, 4, 4));
					arg0.pipeline().addLast(new NettyMessageEncoder());
					arg0.pipeline().addLast(new ReadTimeoutHandler(50));
					arg0.pipeline().addLast(new LoginAuthReqHandler());
					arg0.pipeline().addLast(new HeartBeatReqHandler());
				}
			});
			ChannelFuture f = b.connect(host, port).sync();
			f.channel().closeFuture().sync();
		} finally {
			executor.execute(new Runnable() {	
				@Override
				public void run() {
					try {
						TimeUnit.SECONDS.sleep(1);
						try {
							connect(8080, "127.0.0.1");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	private class NettyClientHandler extends ChannelHandlerAdapter{
		
		private int count;
		
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			NettyMessage message = null;
			for(int i=0;i<100;i++) {
				message = getMessage(i);
				ctx.write(message);
			}
	        ctx.flush();
		}
		
		public NettyMessage getMessage(int i) {
			NettyMessage nettyMessage = new NettyMessage();
			Header header = new Header();
			header.setLength(123);
			header.setSessionID(99999);
			header.setType((byte) 1);
			header.setPriority((byte) 7);
			Map<String, Object> attachment = new HashMap<String, Object>();
			attachment.put("Jerry --> " + i, "Tom " + i);
			header.setAttachment(attachment);
			nettyMessage.setHeader(header);
			nettyMessage.setBody("abcdefg-----------------------AAAAAA");
			return nettyMessage;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			NettyMessage data = (NettyMessage)msg;
			System.out.println("The message from server:" + ++count + "; Message: " + data + " ");
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			ctx.close();
		}
		
	}

	public static void main(String[] args) throws Exception{
		int port = 8080;
		new NettyClient().connect(port, "127.0.0.1");
	}

}
