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

package eu.sofia.adk.sib.exception;

/**
 * The MalformedSSAPMessageException captures all the exceptions related with the 
 * messages sent to the SIB.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 */
@SuppressWarnings("serial")
public class MalformedSSAPMessageException extends SIBException {
	
	/**
	 * The public constructor with a message param 
	 * @param message the message
	 */
	public MalformedSSAPMessageException(String message) {
		super(message);
		this.status = SIBStatus.SSStatus_KP_Message_Syntax;
	}	
	
	/**
	 * The public constructor with a message param 
	 * @param message the message
	 */
	public MalformedSSAPMessageException(String message, SIBStatus status) {
		super(message);
		this.status = status;
	}	
	
}
