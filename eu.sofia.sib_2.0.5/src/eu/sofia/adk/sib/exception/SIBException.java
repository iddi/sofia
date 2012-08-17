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
 * The SIBException captures all the exceptions related with the SIB.
 * This includes the set of possible exceptions that can arise 
 * from the invalid or incomplete messages.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 */
@SuppressWarnings("serial")
public class SIBException extends Exception {

	/**
	 * Status of possible error messages that can be thrown from the SIB.
	 */
	public enum SIBStatus {
		SSStatus_Success					(0x0000),
		SSStatus_SIB_Notification_Reset		(0x0001),
		SSStatus_SIB_Notification_Closing	(0x0002),
		SSStatus_SIB_Error					(0x0003),
		SSStatus_SIB_Error_AccessDenied		(0x0004),
		SSStatus_SIB_Failure_OutOfResources	(0x0005),
		SSStatus_SIB_Failure_NotImplemented	(0x0006),
		SSStatus_KP_Error					(0x0007),
		SSStatus_KP_Request					(0x0008),
		SSAPMessage_MessageIncomplete		(0x0009),
		SSStatus_KP_Message_Syntax			(0x000A);
		
		private int status;
		
		SIBStatus(int status) {
			this.status = status;
		}
		
		public int getStatus() {
			return status;
		}
	}
	
	/** The SIBStatus associated to the thrown exception */
	protected SIBStatus status;
	
	/**
	 * The public constructor
	 */
	public SIBException() {
	}
	
	/**
	 * The public constructor with a message param 
	 * @param message the message
	 */
	public SIBException(String message) {
		super(message);
	}
	
	/**
	 * The public constructor with cause param
	 * @param cause a throwable
	 */
	public SIBException(Throwable cause) {
		super(cause);
	}
	
	
	/**
	 * The public constructor with message and cause param
	 * @param message the error message
	 * @param cause a throwable
	 */
	public SIBException(String message, Throwable cause) {
		super(message, cause);
	}	
	
	/**
	 * Gets the status of error associated to the exception
	 * @return the SIBStatus
	 */
	public SIBStatus getStatus() {
		return this.status;
	}	
}
