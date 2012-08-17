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
 * This class represents a Triple object
 *  
 * @author Cristina López, ESI
 */

public class Triple {

	/** Definition of the type Triple */
	private String subject;
	private String predicate;
	private String object;
	
	private String date;
	
	private String owner;	
	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	
	/**
	 * Constructor with parameters 
	 * @param subject a String representing the subject
	 * @param predicate a String representing the predicate
	 * @param object a String representing the object
	 */
	public Triple(String subject, String predicate, String object) {
		this.date = now();
		
		if (subject != null) {
			this.subject = subject;
		}
		
		if (predicate != null) {
			this.predicate = predicate;
		}
		
		if (object != null) {
			this.object = object;
		}
		
		this.owner = null;		
	}

	/**
	 * Get method of the subject property
	 * @return a String with the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Set method of the subject property
	 * @param subject a String representing the subject
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Get method of the predicate property
	 * @return a String with the predicate
	 */
	public String getPredicate() {
		return predicate;
	}

	/**
	 * Set method of the predicate property
	 * @param predicate a String representing the predicate
	 */
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	/**
	 * Get method of the object property
	 * @return String
	 */
	public String getObject() {
		return object;
	}

	/**
	 * Set method of the object property
	 * @param object
	 */
	public void setObject(String object) {
		this.object = object;
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
	 * @param obj the object to be compared to the current triple
	 * @return <code>true</code> if the triple is the same 
	 */
	@Override
	public boolean equals(Object obj) {
        if (obj instanceof Triple) {
        	Triple otherTriple = (Triple) obj;
        	return subject.equals(otherTriple.subject) &&
        		predicate.equals(otherTriple.predicate) &&
        		object.equals(otherTriple.object);
        } else {
        	return false;
        }
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(subject).append(" ").append(predicate).append(" ").append(object).append("\n");
		return sb.toString();
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


	/**
	 * @param owners the owner to add
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the owners
	 */
	public String getOwner() {
		return owner;
	}	
}
