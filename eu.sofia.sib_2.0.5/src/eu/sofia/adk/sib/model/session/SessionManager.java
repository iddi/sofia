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

package eu.sofia.adk.sib.model.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.data.Session;

/**
 * The Session Manager controls the joined smart applications (aka nodes)
 * to the SIB. The active sessions allows to control if one of the nodes
 * can perform action operations (insert, remove, update, subscribe, unsubscribe,
 * query and leave) with the model. 
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public class SessionManager {

	/** The logger */
	private static Logger logger;

	/** The hashtable where the node_ids are saved */
	protected Hashtable<String, SIBSession> joinedNodes;
	
	/**
	 * Constructor of the Semantic Information Broker
	 */
	public SessionManager() {
		joinedNodes = new Hashtable<String, SIBSession>();		
		logger = Logger.getLogger(SessionManager.class);
	}

	/**
	 * Adds a node (Smart Application) to the SIB
	 * @param nodeId The node identification
	 * @param session The session that joins
	 * @return <code>true</code> if the node has successfully joined
	 */
	public boolean addSession(String nodeId, SIBSession session) {
		
		if (!joinedNodes.containsKey(nodeId)) {
			try {
				joinedNodes.put(nodeId, session);
			} catch (Exception ex) {
				logger.error("Cannot join node " + nodeId + " to the SIB");
				logger.error("Reason: " + ex.toString());
			}
			return true;
		} else { 
			logger.error("Cannot join node " + nodeId + " to the SIB");
			logger.error("Reason: Node already joined");
			return false;
		}
	} 
	
	/**
	 * Removes the node from the SIB joined nodes
	 * @param nodeId The node identification
	 * @return The object removed (a SIBSession)
	 */
	public SIBSession removeNode(String nodeId) {
		
		SIBSession session = null; 
		if (joinedNodes.containsKey(nodeId)) {
			session = joinedNodes.remove(nodeId);
			if (session == null) {
				logger.error("The node " + nodeId + " has no session");
			}
		} else {
			logger.error("The node " + nodeId + " was not joined to this SIB");
		}
		
		return session;
		
	}
	
	/**
	 * Gets the SIBSession for a joined node
	 * @param nodeId The node identifier
	 * @return The SIBSession for the node. null if not present
	 */
	public SIBSession getSession(String nodeId) {
		
		SIBSession session = null; 
		if (joinedNodes.containsKey(nodeId)) {
			session = joinedNodes.get(nodeId);
			if (session == null) {
				logger.error("The node " + nodeId + " has no session");
			}
		} else {
			logger.error("The node " + nodeId + " was not joined to this SIB");
		}
		return session;		
	}
	
	/**
	 * Get the sessions that are currently established to the Semantic Information Broker
	 * @return a hastable containing all the current sessions
	 */
	public Collection<Session> getAllSessions(){
		
		ArrayList<Session> sessions = new ArrayList<Session>();
		
		for (String nodeId : joinedNodes.keySet()) {
			sessions.add(new Session(nodeId));
		}
		return sessions;
	}
	
	/**
	 * Checks if a node is joined to the SIB
	 * @param nodeId The node identification
	 * @return <code>true</code> if the node is joined
	 */
	public boolean isJoinedNode(String nodeId) {
		return joinedNodes.containsKey(nodeId);
	}
}
