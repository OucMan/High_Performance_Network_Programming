package com.badboy.codec;

import java.io.IOException;

import org.jboss.marshalling.ByteOutput;

import io.netty.buffer.ByteBuf;

public class ChannelBufferByteOutput implements ByteOutput{
	
	private final ByteBuf buffer;
	
	public ChannelBufferByteOutput(ByteBuf buffer) {
        this.buffer = buffer;
    }

	@Override
	public void close() throws IOException {
		
	}

	@Override
	public void flush() throws IOException {
		
	}

	@Override
	public void write(int arg0) throws IOException {
		buffer.writeByte(arg0);
	}

	@Override
	public void write(byte[] arg0) throws IOException {
		buffer.writeBytes(arg0);
	}

	@Override
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		buffer.writeBytes(arg0, arg1, arg2);
	}
	
	ByteBuf getBuffer() {
        return buffer;
    }
}
