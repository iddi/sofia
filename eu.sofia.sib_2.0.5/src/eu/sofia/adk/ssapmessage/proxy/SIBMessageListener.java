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

package eu.sofia.adk.ssapmessage.proxy;

import eu.sofia.adk.ssapmessage.SSAPMessage;

/**
 * This interface will be used as listener of the SIB messages that are
 * sent to the corresponding nodes. 
 * The SSAPMessage must be always an SSAPMessageResponse. 
 * 
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public interface SIBMessageListener {

	/**
	 * Receives a message from the SIB with the response to a SSAPMessageRequest
	 * or a subscription notification. This Response can be either a notification
	 * (SSAPMessageIndication)
	 * or a 
	 * @param message the response received from the SIB (subscription indication or response)
	 */
	public void messageReceived(SSAPMessage message);
}
