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

package eu.sofia.adk.ssapmessage.parameter;

/**
 * The specialized parameter for status
 * 
 * @author Fran Ruiz, ESI
 * @author Raul Otaolea, ESI
 *
 */
public class SSAPMessageStatusParameter extends SSAPMessageParameter {
	
	/**
	 * The SIB Status
	 */
	public enum SIBStatus {
//		STATUS_OK						("sib:STATUS_OK"),
//		STATUS_ERROR					("sib:STATUS_ERROR"),
//		STATUS_ACCESS_DENIED			("sib:STATUS_ERROR_AccessDenied"),
//		STATUS_OUT_OF_RESOURCES			("sib:STATUS_ERROR_OutOfResources"),
//		STATUS_NOT_IMPLEMENTED			("sib:STATUS_ERROR_NotImplemented"),
//		STATUS_KP_ERROR					("sib:STATUS_KP_ERROR"),
//		STATUS_KP_REQUEST				("sib:STATUS_KP_Request"),
//		STATUS_KP_JOINED				("sib:STATUS_KP_JOINED"),
//		STATUS_KP_MESSAGE_INCOMPLETE	("sib:STATUS_KP_Message_Incomplete"),
//		STATUS_KP_MESSAGE_SYNTAX		("sib:STATUS_KP_Message_Syntax");

//		As defined in Section 4.5 in D5.21 
		STATUS_OK						("m3:Success"),
		STATUS_RESET					("m3:SIB.Notification.Reset"),
		STATUS_CLOSING					("m3:SIB.Notification.Closing"),
		STATUS_ERROR					("m3:SIB.Error"),
		STATUS_ACCESS_DENIED			("m3:SIB.Error.AccessDenied"),
		STATUS_OUT_OF_RESOURCES			("m3:SIB.Failure.OutOfResources"),
		STATUS_NOT_IMPLEMENTED			("m3:SIB.Failure.NotImplemented"),
		STATUS_KP_ERROR					("m3:KP.Error"),
		STATUS_KP_REQUEST				("m3:KP.Error.Request"),
		STATUS_KP_MESSAGE_INCOMPLETE	("m3:KP:Error.Message.Incomplete"),
		STATUS_KP_MESSAGE_SYNTAX		("m3:KP:Error.Message.Syntax");

		private final String value;
		
		SIBStatus(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public static SIBStatus find(String s) {
			for(SIBStatus status : SIBStatus.values()) {
				if(status.getValue().equalsIgnoreCase(s)) {
					return status;
				}
			}
			return null;
		}
	}
	
	private SIBStatus sibStatus;
	
	/**
	 * Constructor of named parameter in a SSAP message
	 * @param status the response of the SIB
	 */
	public SSAPMessageStatusParameter(SIBStatus status) {
		super(SSAPMessageParameter.NameAttribute.STATUS,null);
		this.sibStatus = status;
	}	
	
	/**
	 * Returns the content of the parameter
	 * @return A String with the content of the parameter
	 */
	public SIBStatus getStatus() {
		return sibStatus;
	}

	/**
	 * Returns the content of the parameter
	 * @return A String with the content of the parameter
	 */
	@Override
	public String getContent() {
		return sibStatus.toString();
	}
	
	/**
	 * Sets the status
	 * @param status the status param
	 */
	public void setStatus(SIBStatus status) {
		this.sibStatus = status;
	}		
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	@Override
	protected String contentXML() {
		return sibStatus.getValue();
	}	
}
