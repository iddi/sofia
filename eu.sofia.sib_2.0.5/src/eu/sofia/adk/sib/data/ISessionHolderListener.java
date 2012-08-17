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

import java.util.Collection;

/**
 * This interface determines the set of methods needed to be implemented by
 * the classes responsible of the sessions
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public interface ISessionHolderListener {

	/**
	 * Performs an initialization of the set of joined sessions to the SIB.
	 * @param sessions the collection of sessions 
	 */
	public void initSessions(Collection<Session> sessions);

	/**
	 * Adds a new session to the set of sessions
	 * @param addedSession the added session
	 */
	public void addSession(Session addedSession);
	
	/**
	 * Remove a session from the set of sessions
	 * @param removedSession the removed session
	 */
	public void removeSession(Session removedSession);

	/**
	 * Resets the content of the SIB
	 */
	public void reset();		
}
