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

package eu.sofia.adk.gateway;

import java.util.HashMap;
import java.util.Properties;

import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.gateway.service.IGatewayStateListener;
import eu.sofia.adk.gateway.session.impl.Session;
import eu.sofia.adk.sib.service.ISIB;

/**
 * This class represents a general gateway to connect to a SIB. A gateway is
 * a communication transport protocol listener which understands SSAP messages 
 * and sends them to the SIB. It also receives messages from SIB to send them
 * back to the KP.
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public abstract class AbstractGateway implements IGateway {

	/**
	 * Types of possible transactions with the SIB using the SSAP protocol.
	 */
	public enum GatewayStatus {  
		RUNNING		(1),
		STOPPED		(0);
		
		private final int status;
		
		GatewayStatus(int status) {
			this.status = status;
		}
		
		public int getStatus() {
			return status;
		}
		
		public boolean isRunning() {
			return (this == GatewayStatus.RUNNING);
		}
		
		public boolean isStopped() {
			return (this == GatewayStatus.STOPPED);
		}
	}	
	
	/** The generator id of the gateway */
	private static int idGenerator;
	
	/** A latch to avoid concurrency while creating a new id */
	private static Object uniqueId;
	
	/** The SIB reference */
	protected ISIB sib;
	
	/** The hash map containing all sessions of this gateway */
	protected HashMap<Long, Session> sessions;

	/** A support class to manage IGateStateListeners */
	private GatewayStateSupport stateSupport;
	
	/** The id of the gateway */
	protected int id;
	
	/** The name of the gateway */
	protected String name;
	
	/** The state of the gateway RUNNING or STOPPED */
	private GatewayStatus status;
	
	/** The type of the gateway */
	private String type;
	
	static {
		uniqueId = new Object();
	}
	
	/**
	 * The properties defining this gateway 
	 */
	protected Properties properties;
	
	/**
	 * Starts this gateway. 
	 * 
	 * @throws GatewayException
	 * @see GatewayException
	 */
	public abstract void startGateway() throws GatewayException ;

	/**
	 * Stops this gateway. 
	 * 
	 * @throws GatewayException
	 * @see GatewayException
	 */
	public abstract void stopGateway() throws GatewayException ;
	
	/**
	 * Constructs a new gateway.
	 * 
	 * @param name				the name for the new gateway.
	 * @param sib				the sib this gateway is attached to. 
	 * @param properties		the specific properties to create a concrete. 
	 * @throws GatewayException
	 * @see GatewayException
	 */
	public AbstractGateway(String name, String type, ISIB sib, Properties properties) {
		this.name = name;
		this.type = type;
		this.sib = sib;
		this.properties = properties;
		this.id = AbstractGateway.generateId(); 
		this.sessions = new HashMap<Long,Session>();
		this.stateSupport = new GatewayStateSupport();
		this.status = GatewayStatus.STOPPED;
	}
	
	/**
	 * Returns the SIB service.
	 * @return ISIB 
	 */
	public ISIB getSIB() {
		return this.sib;
	}
	
	/**
	 * Adds a listener to see gateway state changes (running, stopped).
	 */
	@Override
	public void addStateListener(IGatewayStateListener listener) {
		this.stateSupport.addListener(listener);
	}

	/**
	 * Removes a listener to see gateway state changes (running, stopped).
	 */
	@Override
	public void removeStateListener(IGatewayStateListener listener) {
		this.stateSupport.removeListener(listener);
	}
	
	
	/** 
	 * Generates a unique id per gateway
	 * @return a unique id
	 */
	private static int generateId() {
		synchronized(uniqueId) {
			return ++idGenerator;
		}
	}
	
	/**
	 * Retrieves the unique id of this gateway.
	 * @return the gateway id. 
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Establishes the reference to the SIB service.
	 * 
	 * @param sib the SIB service.
	 */
	public void setSIB(ISIB sib) {
		this.sib = sib;
	}
	
	/**
	 * Adds a session to the Session manager
	 *  
	 * @param session the session to be added
	 * @see Session
	 */
	public void addSession(Session session) {
		sessions.put(session.getId(), session);
	}
	
	/**
	 * Removes a session from the session
	 *  
	 * @param session 	the session to be removed
	 * @return the session removed or <code>null</code> if the session does not exist.
	 */
	public Session removeSession(Session session) {
		return sessions.remove(session.getId());
	}
	
	/**
	 * Gets the name of the gateway.
	 * @return the name of the gateway
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the properties of this gateway.
	 * @return The properties associated to the gateway
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Starts the gateway
	 * @throws GatewayException if the gateway can not be started.
	 * @see GatewayException
	 */
	public void start() throws GatewayException {
		startGateway();
		this.status = GatewayStatus.RUNNING;
		this.stateSupport.fireGatewayRunning(this);
	}

	/**
	 * Stops the gateway
	 * @throws GatewayException if the gateway can not be stopped.
	 * @see GatewayException
	 */
	public void stop() throws GatewayException {
		for(Session session : sessions.values()) {
			session.close();
		}

		stopGateway();
		this.status = GatewayStatus.STOPPED;
		this.stateSupport.fireGatewayStopped(this);
	}
	
	/**
	 * Gets the state of the gateway: RUNNING or STOPPED.
	 */
	public int getStatus() {
		return this.status.getStatus();
	}

	/**
	 * Gets if the gateway is running
	 * @return <code>true</code> if the gateway is running
	 */
	public boolean isRunning() {
		return this.status.isRunning();
	}	

	/**
	 * Gets if the gateway is stopped
	 * @return <code>true</code> if the gateway is stopped
	 */
	public boolean isStopped() {
		return this.status.isStopped();
	}		
	
	/**
	 * Gets the type of the gateway.
	 */
	public String getType() {
		return this.type;
	}
	
}
