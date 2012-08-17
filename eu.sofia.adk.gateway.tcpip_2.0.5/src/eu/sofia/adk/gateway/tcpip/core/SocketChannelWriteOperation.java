/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Raul Otaolea (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *    Fran Ruiz (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *******************************************************************************/ 

package eu.sofia.adk.gateway.tcpip.core;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

/**
 * SocketChannelWriteOperation.java
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia 
 */
public class SocketChannelWriteOperation implements Runnable {
	
	private SocketChannel channel;
	private ClientMessage message;
	
	private ByteBuffer buffer;
	private long channelWriteSleep = 10;
	
	public SocketChannelWriteOperation(SocketChannel channel, ClientMessage message) {
		this.channel = channel;
		this.message = message;
		buffer = ByteBuffer.allocateDirect(message.getPayloadSize());
	}
	
	public void setChannelWriteSleep(long l) {
		this.channelWriteSleep = l;
	}
	
	public SocketChannel getChannel() {
		return channel;
	}
	
	public ClientMessage getMessage() {
		return message;
	}
	
	public void run() {
		// Cehck if the channel is ready.		
		if (channel == null || !channel.isConnected()) {
		    System.err.println("writeEvent: client channel null or not connected");
		    return;
		}
		
		buffer.clear();
		buffer.put(message.getPayload());
		// prepare for a channel.write
		buffer.flip();
		
		channelWrite(channel, buffer);
	}
	
	private void channelWrite(SocketChannel channel, ByteBuffer writeBuffer) {
		long nbytes = 0;
		long toWrite = writeBuffer.remaining();

		// loop on the channel.write() call since it will not necessarily
		// write all bytes in one shot
		try {
			while (nbytes != toWrite) {
				nbytes += channel.write(writeBuffer);

				try {
					Thread.sleep(channelWriteSleep);
				} catch (InterruptedException e) {
				}
			}
		} catch (ClosedChannelException cce) {
		} catch (Exception e) {
		}

		writeBuffer = null;

	}
}
