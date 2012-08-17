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
 * The TransformationException captures all the exceptions related with the 
 * messages sent to the SIB.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, Tecnalia
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, Tecnalia
 */
@SuppressWarnings("serial")
public class TransformationException extends SIBException {
	
	/**
	 * The public constructor with a message param 
	 * @param message the message
	 */
	public TransformationException(String message) {
		super(message);
		this.status = SIBStatus.SSStatus_KP_Message_Syntax;
	}	
	
	/**
	 * The public constructor with a message param 
	 * @param message the message
	 */
	public TransformationException(String message, SIBStatus status) {
		super(message);
		this.status = status;
	}	
	
}
