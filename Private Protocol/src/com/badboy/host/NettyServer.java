package com.badboy.host;

import com.badboy.codec.NettyMessageDecoder;
import com.badboy.codec.NettyMessageEncoder;
import com.badboy.handshake.LoginAuthRespHandler;
import com.badboy.heartbeat.HeartBeatRespHandler;
import com.badboy.message.NettyMessage;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

public class NettyServer {
	
	public void bind(int port) throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
			 .option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChildChannelHandler());
			ChannelFuture f = b.bind(port).sync();
			f.channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel arg0) throws Exception {
			arg0.pipeline().addLast(new NettyMessageDecoder(1024*1024, 4, 4));
			arg0.pipeline().addLast(new NettyMessageEncoder());
			arg0.pipeline().addLast(new ReadTimeoutHandler(50));
			arg0.pipeline().addLast(new LoginAuthRespHandler());
			arg0.pipeline().addLast(new HeartBeatRespHandler());
		}
	}
	
	private class NettyServerHandler extends ChannelHandlerAdapter{
		
		private int counter;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("The netty server reveives: " + ++counter + " message: " + msg + "");
			ctx.writeAndFlush((NettyMessage)msg);
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
		new NettyServer().bind(port);
	}

}
