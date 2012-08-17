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


import java.util.Properties;

import eu.sofia.adk.gateway.AbstractGateway;
import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.session.impl.Session;
import eu.sofia.adk.sib.service.ISIB;


/**
 * TCPIPGateway.java
 * 
 * Handles reading from clients and hands off events to the consumer.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class TCPIPGateway extends AbstractGateway implements IConnectionListener {
	
	/**
	 * This class is responsible for accepting incoming client connections.
	 */
	private SocketChannelAcceptor channelAcceptor;
	/**
	 * This class is responsible for reading the information from the client channels.
	 */
	private SocketChannelReader channelReader;

	/**
	 * This class is responsible for writing the information to client channels.
	 */
	private SocketChannelWriter channelWriter;

	/**
	 * To read the messages that KP sends in order to discover SIBs.
	 */
	private MulticastReader multicastReader;
	/** 
	 * The ThreadGroup where all threads related with TCP/IP gateway will run.
	 */
	private ThreadGroup tg;

	/**
	 * The configuration of the tcp/ip gateway.
	 */
	private TCPIPGatewayConfiguration config;
	
	/**
	 * The logger
	 */
	
	private volatile boolean running;
	/**
	 * Constructor
	 */
	public TCPIPGateway(String name, String type, ISIB sib, Properties properties) {
		super(name, type, sib, properties);
		this.config = new TCPIPGatewayConfiguration();
		config.setProperties(properties);
		
		// Create the ThreadGroup
		tg = new ThreadGroup("TCP/IP Gateway");
	}

	/**
	 * This method starts the gateway. 
	 */
	public void startGateway() throws GatewayException {
		try {
			System.out.println("Starting the TCP/IP gateway...");
			channelWriter= new SocketChannelWriter(tg, config);
			channelWriter.start();
			
			channelReader = new SocketChannelReader(tg, config);
			channelReader.setConnectionListener(this);
			channelReader.start();
			
			channelAcceptor = new SocketChannelAcceptor(tg, channelReader, channelWriter, config);
			channelAcceptor.start();
			
			multicastReader = new MulticastReader(sib.getName(), config.getProperties());
			multicastReader .start();
			
			running = true;
		} catch(Exception e) {
			System.out.println("TCP/IP gateway failed");
			try {
				stop();
			} catch(Exception e2) {
			}
			running = false;
			throw new GatewayException(e.getMessage(), e);
		}
	}

	/**
	 * Shutdown the gateway.
	 */
	public void stopGateway() throws GatewayException {
		if(!running) {
			return;
		}
		
		System.out.println("Stopping TCP/IP gateway...");
		try {
			if(channelAcceptor != null) {
				channelAcceptor.shutdown();
				channelAcceptor = null;
			}
			if(channelReader != null) {
				channelReader.shutdown();
				channelReader = null;
			}
			if(channelWriter != null) {
				channelWriter.shutdown();
				channelWriter = null;
			}
			if(multicastReader != null) {
				multicastReader.shutdown();
				multicastReader = null;
			}
			
			System.out.println("TCP/IP gateway stopped");
		} catch(Exception e) {
			System.out.println("TCP/IP gateway failed while stopping");
			throw new GatewayException(e.getMessage(), e);
		}
	}

	/**
	 * @return The configuration of the TCP/IP server
	 */
	public TCPIPGatewayConfiguration getConfig() {
		return this.config;
	}

	@Override
	public void connectionCreated(ClientConnection connection) {
		Session session = new Session();
		session.setSIB(this.sib);
		session.setConnector(connection);
		this.addSession(session);
	}
	
	@Override
	public void connectionDestroyed(ClientConnection connection) {
		Session sessionToRemove = null;
		for(Session session : this.sessions.values()) {
			ClientConnection cc = (ClientConnection )session.getConnector(); 
			if(cc == connection) {
				sessionToRemove = session;
				break;
			}
		}
		if(sessionToRemove != null) {
			this.removeSession(sessionToRemove);
		}
	}
	
}
