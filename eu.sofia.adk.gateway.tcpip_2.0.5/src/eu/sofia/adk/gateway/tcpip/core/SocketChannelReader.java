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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;


/**
 * SocketChannelReader.java
 * 
 * Handles reading from all clients using a <code>Selector</code> and delegate them to the 
 * appropriate listeners.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia 
 */
public class SocketChannelReader extends Thread {

	/** Pending connections */
	private LinkedList<ClientConnection> newClients;

	/** Indicates if the thread is running */
	private boolean running;
	
	/** The selector, multiplexes access to client channels */
	private Selector selector;

	/** Read buffer */
	private ByteBuffer readBuffer = ByteBuffer.allocate(32768);

	/** Listener to inform that a new TCP/IP connection has been created. */
	private IConnectionListener connectionlistener;

	/** Time to sleep between acceptances */
	private long sleep = 30;
	
	/**
	 * Constructor.
	 */
	public SocketChannelReader(ThreadGroup tg, TCPIPGatewayConfiguration config) {
		super(tg, "SocketChannelReader");
		this.newClients = new LinkedList<ClientConnection>();
		this.running = false;
		this.readBuffer = ByteBuffer.allocate(TCPIPGatewayConfiguration.BUFFER_SIZE);
		this.sleep = config.getLong(TCPIPGatewayConfiguration.CHANNEL_READER_SLEEP);
	}

	public void setConnectionListener(IConnectionListener listener) {
		this.connectionlistener = listener;
	}
	
	/**
	 * Adds a new connection to the list of pending clients to process them in a synchronized way.
	 */
	public void addNewClient(ClientConnection clientConnection) {
		synchronized (newClients) {
			newClients.addLast(clientConnection);
		}
		// Force selector to return so that the new connection can get in the loop right away.
		selector.wakeup();
	}

	/**
	 * Loop forever, first doing our select() then check for new connections
	 */
	public void run() {
		running = true;
		try {
			System.out.println("SocketChannelReader started");
			selector = Selector.open();
			while (running) {
				try {
					select();
					if(!this.isInterrupted()) {
						checkNewConnections();
		
						try {
							//System.out.println("[" + this.getDateTime() + "] waiting?...");		
							Thread.sleep(sleep);
							//System.out.println("[" + this.getDateTime() + "] of course!");		
						} catch (InterruptedException e) {
						}
					}
				} catch(Exception e) {
					System.out.println("Error while reading info");
				}
			}
		} catch(IOException e) {
			System.out.println("Exception while opening Selector");
		} finally {
			System.out.println("SocketChannelReader finished");
		}
	}

	/**
	 * Check for new connections and register them with the selector
	 */
	private void checkNewConnections() {
		synchronized (newClients) {
			while (newClients.size() > 0) {
				try {
					ClientConnection client = (ClientConnection)newClients.removeFirst();
					connectionlistener.connectionCreated(client);
					client.registerAndConfigureChannel(selector);
				} catch (ClosedChannelException cce) {
					System.out.println("Channel closed");
				} catch (IOException ioe) {
					System.out.println("IOException on clientChannel");
				}
			}
		}
	}

	/**
	 * Do Select, and read from the channels. This method also checks if a connection has closed.
	 */
	private void select() {
		try {
			// This is a blocking select that will return when new data arrive.
			selector.select();
			Set<SelectionKey> readyKeys = selector.selectedKeys();

			Iterator<SelectionKey> i = readyKeys.iterator();
			while (i.hasNext()) {
				SelectionKey key = i.next();
				i.remove();
				SocketChannel channel = (SocketChannel) key.channel();
				ClientConnection client = (ClientConnection) key.attachment();
				readBuffer.clear();
				try {
					// Read from the channel.
					long readBytes = channel.read(readBuffer);
					
					// We detect end-of-stream when channel.read(...) return -1.
					if(readBytes == -1) {
						// We raise a connection closed event only when both input and output streams
						// are closed.
						if( channel.socket().isInputShutdown() && channel.socket().isOutputShutdown()) {
							System.out.println("[" + client.toString() + "] connection closed");
							client.fireConnectionClosed();
							connectionlistener.connectionDestroyed(client);
						}
					} else {
						// Add read bytes to the attachment.
						client.add(readBuffer);
					}
				} catch(IOException ioe) {
					// The client has closed abruptly.
					System.out.println("Connection [" + client.toString() + "] closed");
					client.fireConnectionClosed();
					connectionlistener.connectionDestroyed(client);
				}
			}
		} catch (Exception e) {
			System.out.println("Exception during select(): " + e.toString());
		}
	}
	
	public void shutdown() {
		try {
			running = false;
			this.interrupt();
			selector.close();
		} catch (Exception e) {
		}
	}

}