package com.badboy.codec;

import java.io.IOException;

import org.jboss.marshalling.ByteInput;

import io.netty.buffer.ByteBuf;

public class ChannelBufferByteInput implements ByteInput{

	private final ByteBuf buffer;
	
	public ChannelBufferByteInput(ByteBuf buffer) {
        this.buffer = buffer;
    }
	
	@Override
	public void close() throws IOException {
		
	}

	@Override
	public int available() throws IOException {
		return buffer.readableBytes();
	}

	@Override
	public int read() throws IOException {
		if(buffer.isReadable()) {
			return buffer.readByte() & 0xff;
		}
		return -1;
	}

	@Override
	public int read(byte[] arg0) throws IOException {
		return read(arg0, 0, arg0.length);
	}

	@Override
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		 int available = available();
		 if (available == 0) {
	          return -1;
	     }
		 arg2 = Math.min(available, arg2);
		 buffer.readBytes(arg0, arg1, arg2);
		 return arg2; 
	}

	@Override
	public long skip(long arg0) throws IOException {
		int readable = buffer.readableBytes();
		if (readable < arg0) {
			arg0 = readable;
        }
		buffer.readerIndex((int) (buffer.readerIndex() + arg0));
		return arg0;
	}

}
