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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import eu.sofia.adk.gateway.connector.impl.Connector;
import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.ssapmessage.SSAPMessage;

/**
 * ClientConnection.java
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class ClientConnection extends Connector {
	
	private static long nextId;
	
	/** Unique id for the client connection. */
	private long id;
	/** The channel associated with the client connection. */
	private SocketChannel channel;
	/** The class responsible to save and parse received bytes to detect new messages */
	private ClientConnectionAttachment attachment;
	/** The class to write messages to the client */
	private SocketChannelWriter channelWriter;
	
	public ClientConnection(SocketChannel channel, ClientConnectionAttachment attachment) {
		id = ++nextId;
		this.channel = channel;
		this.attachment = attachment;
	}
	
	public void setChannelWriter(SocketChannelWriter channelWriter) {
		this.channelWriter = channelWriter;
	}
	
	public long getId() {
		return id;
	}
	
	public SocketChannel getChannel() {
		return channel;
	}

	public ClientConnectionAttachment getAttachment() {
		return attachment;
	}
	
	public void add(ByteBuffer bb) throws MessageParseException {
		attachment.add(bb);
		if(attachment.checkForMessages()) {
			Iterator<ClientMessage> msgList = attachment.getMessages();
			while(msgList.hasNext()) {
				ClientMessage message = msgList.next();
				this.fireMessageReceived(message.getPayload());
			}
		}
	}
	
	public void sendMessage(ClientMessage message) {
		channelWriter.sendMessage(message, channel);
	}
	
	public void registerAndConfigureChannel(Selector selector) throws IOException {
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ, this);
	}
	
	public void close() {
		if(channel != null) {
			try {
//				channel.socket().close();
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
				GatewayException ex = new GatewayException("Error while trying to close the channel",e); 
				this.fireConnectionError(ex);
			} catch (Exception e) {
				GatewayException ex = new GatewayException("Error while trying to close the channel",e); 
				this.fireConnectionError(ex);
			}
		}
	}
	
	/**
	 * Retrieves a description of the connection.
	 * @return the inet address of the connection.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id=");
		sb.append(id);
		sb.append(", ip=");
		if(channel != null) {
			sb.append(channel.socket().getInetAddress());
		}
		return sb.toString();
	}

	public void sendToKP(byte[] ba) {
		ClientMessage msg = new ClientMessage(ba);
		sendMessage(msg);
	}
	
	public void messageReceivedFromSIB(SSAPMessage sibMsg) {
		byte[] payload = sibMsg.toByteArray();
		sendToKP(payload);
	}
	
	/**
	 * Fires a connection timeout when the connection is closed
	 */
	@Override
	public void fireConnectionClosed() {
		close();
		super.fireConnectionClosed();
	}	
}
