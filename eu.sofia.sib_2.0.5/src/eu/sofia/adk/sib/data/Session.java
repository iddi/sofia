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

package eu.sofia.adk.sib.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * This class represents a Session object
 *  
 * @author Cristina López, ESI
 */
public class Session {

	/** The KP identifier */
	private String nodeId;
	private String date;

	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * Constructor
	 */
	public Session() {
		this.date = now();
	}
	
	/**
	 * Constructor with parameters 
	 * @param nodeId
	 */
	public Session(String nodeId){
		this.nodeId = nodeId;
		this.date = now();
	}
	
	/**
	 * Get method of the nodeId property
	 * @return a String containing the KP identification
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Set method of the nodeId property
	 * @param nodeId the node identification
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Gets the current formatted date
	 * @return a String containing the formatted date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * Allows the equality comparision to another object
	 * @param obj the object to be compared to the current session
	 * @return <code>true</code> if the session is the same 
	 */
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof Session) {
        	Session otherSession = (Session) obj;
        	return nodeId.equals(otherSession.nodeId);
        } else {
        	return false;
        }
    }
	
	/**
	 * Gets the current formatted date
	 * @return a String with the formatted date
	 */
	private String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		return sdf.format(cal.getTime());
	}
}
