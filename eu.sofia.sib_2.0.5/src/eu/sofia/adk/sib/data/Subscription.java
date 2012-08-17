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
 * This class represents a Subscription object
 *  
 * @author Cristina López, ESI
 */
public class Subscription {

	/** The subscription identifier */
	private Long subscriptionId;
	
	/** The KP identifier */
	private String nodeId;
	
	/** The query */
	private String query;
	
	/** The result obtained after executing the query in SPARQL-result format */
	private String result;

	private String date;
	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	
	/**
	 * Constructor
	 */
	public Subscription() {
		this.date = now();
	}
	
	/**
	 * Constructor with parameters 
	 * @param subscriptionId the subscription identifier
	 * @param nodeId the node identifier
	 * @param query the query
	 * @param result the result in sparql-results format
	 * @see <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format (W3C Recommendation)</a>
	 */
	public Subscription(Long subscriptionId, String nodeId, String query, String result){
		this.subscriptionId = subscriptionId;
		this.nodeId = nodeId;
		this.query = query;
		this.result = result;
		this.date = now();
	}

	/**
	 * Get method of the subscriptionId property
	 * @return a Long representing the subscription identifier
	 */
	public Long getSubscriptionId() {
		return subscriptionId;
	}

	/**
	 * Set method of the subscriptionId property
	 * @param subscriptionId a Long representing the subscription identifier
	 */
	public void setSubscriptionId(Long subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	/** 
	 * Get method of the nodeId property
	 * @return a String with the node identifier
	 */
	public String getNodeId() {
		return nodeId;
	}

	/**
	 * Set method of the nodeId property
	 * @param nodeId
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get method of the query property
	 * @return A String in SPARQL format
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Set method of the query property
	 * @param query a String with the SPARQL format
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Get method of the result property
	 * @return a result in sparql-results format
	 */
	public String getResult() {
		return result;
	}

	/** 
	 * Set method of the result property
	 * @param result a String in sparql-results format
	 */
	public void setResult(String result) {
		this.result = result;
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
	 * @param obj the object to be compared to the current subscription
	 * @return <code>true</code> if the session is the same 
	 */
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof Subscription) {
        	Subscription otherSubscription = (Subscription) obj;
        	return subscriptionId.equals(otherSubscription.subscriptionId);
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
