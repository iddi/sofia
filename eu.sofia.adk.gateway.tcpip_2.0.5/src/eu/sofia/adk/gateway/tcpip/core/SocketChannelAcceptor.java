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
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import eu.sofia.adk.gateway.tcpip.parser.SSAPMessageParser;

/**
 * SocketChannelAcceptor.java
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class SocketChannelAcceptor extends Thread {

	
	/**  
	 * Indicates if this server is running
	 */
	private boolean running;

	/** Selector for managing ServerSocketChannels. We use a Selector in case we will 
	 * listen to another port for administrative issues. */
	private Selector selector;

	/** 
	 * ServerSocketChannel vector for accepting client connections 
	 */
	private Vector<ServerSocketChannel> sscs;
	
	/**
	 * This class is responsible for reading the information from the client channels.
	 */
	private SocketChannelReader channelReader;

	/**
	 * This class is responsible for writing the information to client channels.
	 */
	private SocketChannelWriter channelWriter;
	
	/**
	 * Properties of the TCP/IP gateway. 
	 */
	private TCPIPGatewayConfiguration config;
	
	/**
	 * Constructor.
	 * @param threadGroup
	 * @param channelReader
	 * @param channelWriter
	 * @param properties
	 */
	
	public SocketChannelAcceptor(ThreadGroup tg, SocketChannelReader channelReader,
			SocketChannelWriter channelWriter, TCPIPGatewayConfiguration config) {
		super(tg, "SocketChannelAcceptor");
		this.channelReader = channelReader;
		this.channelWriter = channelWriter;
		this.config = config;
		this.sscs = new Vector<ServerSocketChannel>();
	}
	
	/**
	 * TCPIPServer specific initialization, bind to the server port, setup the
	 * Selector, etc.
	 */
	private boolean initServerSocket() {
		try {
			
			// Get addresses to bind to.
			ArrayList<String> ips = config.getAddresses();
			if(ips == null || ips.size() == 0) {
				System.out.println("No IPs to bind to !");
				return false;
			}

			// Bind to port.
			int port = config.getInt(TCPIPGatewayConfiguration.PORT);
			
			// Open a selector.
			selector = Selector.open();

			// For each ip.
			for( String ip : ips) {
				// Open a non-blocking server socket channel.
				ServerSocketChannel ssc = ServerSocketChannel.open();
				ssc.configureBlocking(false);
				
				// Bind the server socket to the ip.
				InetAddress addr = InetAddress.getByName(ip);
				System.out.println("binding to address: " + addr.getHostAddress() + ", port: " + port);
				ssc.socket().bind(new InetSocketAddress(addr, port));

				// Get the selector and register the channel with it to handle new connections (accept).
				ssc.register(selector, SelectionKey.OP_ACCEPT);
				
				// Add to the ssc list.
				sscs.add(ssc);
			}
			
			return true;
		} catch (BindException be) {
			System.out.println("Error initializing ServerSocket. Is another instance running?");
			return false;
		} catch (Exception e) {
			System.out.println("Error initializing ServerSocket");
			return false;
		}
	}
	
	/**
	 * Accept socket connections and pass them to SocketChannelReader.
	 */
	public void run() {
		System.out.println("SocketChannelAcceptor started");
		running = initServerSocket();
		
		while (running) {
			try {
				// Accept new connections. It is a blocking operation that will return when we get a new connection.
				selector.select();

				// Fetch the keys and iterate through them to process.
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> i = readyKeys.iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					i.remove();

					ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
					SocketChannel clientChannel = ssChannel.accept();
					
					if(clientChannel != null) {
						// Add to the connections list.
						ClientConnection clientConnection = new ClientConnection(clientChannel, new SSAPMessageParser());
						clientConnection.setChannelWriter(channelWriter);
						
						// Add new connections to SocketChannelReader for processing.
						channelReader.addNewClient(clientConnection);
						System.out.println("[" + clientConnection + "] new connection");
					}
				}
			} catch (IOException ioe) {
				System.out.println("Error during serverSocket select(): " + ioe.getMessage());
			} catch (Exception e) {
				System.out.println("Exception in run()");
			}
		}
		
		// Destroy the ServerSocket.
		try {
			selector.close();
			for(ServerSocketChannel ssc : sscs) {
				ssc.close();
			}
		} catch(Exception e) {
		}
		System.out.println("SocketChannelAcceptor finished");
	}
	
	/**
	 * Shutdown the acceptor thread.
	 */
	public void shutdown() {
		running = false;
		if(selector != null) {
			selector.wakeup();
		}
	}

}
