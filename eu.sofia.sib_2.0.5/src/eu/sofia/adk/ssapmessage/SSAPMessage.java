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

package eu.sofia.adk.ssapmessage;

import java.util.ArrayList;
import java.util.Collection;

import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter.NameAttribute;

/**
 * This abstract class implements the SSAPMessage protocol
 * 
 * @author Raul Otaolea, ESI
 */
public abstract class SSAPMessage {

	/**
	 * Types of possible transactions with the SIB using the SSAP protocol.
	 */
	public enum TransactionType {  
		JOIN		("JOIN"),
		LEAVE		("LEAVE"),
		INSERT		("INSERT"),
		REMOVE		("REMOVE"),
		UPDATE		("UPDATE"),
		SUBSCRIBE	("SUBSCRIBE"),
		UNSUBSCRIBE	("UNSUBSCRIBE"),
		QUERY		("QUERY");
		
		private final String type;
		
		TransactionType(String type) {
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
	}
	
	/**
	 * Types of possible message types using the SSAP protocol.
	 */
	public enum MessageType {
		REQUEST		("REQUEST"),
		CONFIRM		("CONFIRM"),
		INDICATION	("INDICATION");
		
		private final String type;
		
		MessageType(String type) {
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
	}
	
	/** The transaction identifier */
	protected long transactionId;
	
	/** The node (aka smart application) identifier */
	private String nodeId;

	/** The smart space (aka SIB) identifier */
	private String spaceId;

	/** The transaction type */
	private TransactionType tranType;

	/** The message type */
	private MessageType msgType;
	
	/** The set of parameters */
	private ArrayList<SSAPMessageParameter> parameters;
	
	/**
	 * Public constructor
	 */
	public SSAPMessage() {
		parameters = new ArrayList<SSAPMessageParameter>();
	}

	/**
	 * Sets the node identifier
	 * @param nodeId a string indicating the node identifier
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
		
	/**
	 * Gets the node identifier
	 * @return a string with the node identifier
	 */
	public String getNodeId() {
		return this.nodeId;
	}
	
	/**
	 * Sets the smart space identifier
	 * @param spaceId a string with the smart space identifier
	 */
	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}
	
	/**
	 * Gets the smart space identifier
	 * @return a string with the smart space identifier
	 */
	public String getSpaceId() {
		return this.spaceId;
	}
	
	/**
	 * Sets the transaction identifier
	 * @param transactionId a long with the transaction identifier
	 */
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	
	/**
	 * Gets the transaction identifier
	 * @return a long with the transaction identifier
	 */
	public long getTransactionId() {
		return transactionId;
	}
	
	/**
	 * Sets the transaction type
	 * @param type the transaction type
	 */
	public void setTransactionType(TransactionType type) {
		this.tranType = type;
	}
	
	/**
	 * Gets the transaction type
	 * @return the transaction type
	 */
	public TransactionType getTransactionType() {
		return tranType;
	}
	
	/**
	 * Sets the message type
	 * @param msgType the message type
	 */
	public void setMessageType(MessageType msgType) {
		this.msgType = msgType;
	}
	
	/**
	 * Gets the message type
	 * @return the message type
	 */
	public MessageType getMessageType() {
		return msgType;
	}
	
	/**
	 * Adds a parameter to the parameter collection
	 * @param param a SSAPMessageParameter
	 * @see eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter
	 */
	public void addParameter(SSAPMessageParameter param) {
		if (param != null) {
			parameters.add(param);
		}
	}
	
	/**
	 * Adds a list of parameters to the parameter collection
	 * @param params a collection of SSAPMessageParameter
	 */
	public void addParameters(Collection<SSAPMessageParameter> params) {
		if (params != null && params.size() != 0) {
			this.parameters.addAll(params);
		}
	}
	
	/**
	 * Gets the count of parameters whose attribute <code>name</code> has 
	 * the value specified as parameter
	 * @param name the parameter name
	 * @return the quantity of parameters with the attribute name set to paramName
	 */
	public int getParameterSize(NameAttribute name) {
		int paramSize = 0;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(name)) {
				paramSize++;
			}
		}
		return paramSize;
	}	
	
	/**
	 * Gets the count of parameters
	 * @return the quantity of parameters
	 */
	public int getParameterSize() {
		return parameters.size();
	}	
	
	/**
	 * Gets the set of parameters for a parameter name
	 * @param name the value of the parameter
	 * @return the param list
	 */
	public Collection<SSAPMessageParameter> listParameters(NameAttribute name) {
		ArrayList<SSAPMessageParameter> paramList = new ArrayList<SSAPMessageParameter>();
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(name)) {
				paramList.add(parameter);
			}
		}
		return paramList;
	}	
	
	/**
	 * Gets the set of parameters
	 * @return the param list
	 */
	public Collection<SSAPMessageParameter> listParameters() {
		return parameters;
	}	
	
	/**
	 * Gets the parameters corresponding to a parameter name
	 * @param name the value of the parameter
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getParameter(NameAttribute name) {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(name)) {
				param = parameter;
			}
		}
		return param;
	}
	
	/**
	 * Gets the parameters corresponding to the credentials
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getCredentialsParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.CREDENTIALS)) {
				param = parameter;
			}
		}
		return param;
	}	
	
	/**
	 * Gets the parameters corresponding to the status
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageStatusParameter getStatusParameter() {
		SSAPMessageStatusParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName() != null) {
				if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.STATUS)) {
					param = (SSAPMessageStatusParameter) parameter;
				}
			}
		}
		return param;
	}	
	
	/**
	 * Gets the parameters corresponding to the inserted triples
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getInsertGraphParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.INSERTGRAPH)) {
				param = parameter;
			}
		}
		return param;
	}	
	
	public SSAPMessageParameter getEncodingParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.TYPE) ||
					parameter.getName().equals(SSAPMessageParameter.NameAttribute.ENCODING) ||
					parameter.getName().equals(SSAPMessageParameter.NameAttribute.FORMAT)) {
				param = parameter;
			}
		}
		return param;
	}	
	
	/**
	 * Gets the parameters corresponding to the removed triples
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getRemoveGraphParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {		
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.REMOVEGRAPH)) {
				param = parameter;
			}
		}
		return param;
	}		

	/**
	 * Gets the parameters corresponding to the confirm
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getConfirmParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.CONFIRM)) {
				param = parameter;
			}
		}
		return param;
	}		
	

	/**
	 * Gets the parameters corresponding to the bNodes
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getBNodesParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.BNODES)) {
				param = parameter;
			}
		}
		return param;
	}	
	

	/**
	 * Gets the parameters corresponding to the query
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getQueryParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.QUERY)) {
				param = parameter;
			}
		}
		return param;
	}
	

	/**
	 * Gets the parameters corresponding to the Subscription ID
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getSubscriptionIdParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.SUBSCRIPTIONID)) {
				param = parameter;
			}
		}
		return param;
	}
	
	/**
	 * Gets the parameters corresponding to the results
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getResultsParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.RESULTS)) {
				param = parameter;
			}
		}
		return param;
	}	

	/**
	 * Gets the parameters corresponding to the new results
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getNewResultsParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.NEWRESULTS)) {
				param = parameter;
			}
		}
		return param;
	}	

	/**
	 * Gets the parameters corresponding to the obsolete results
	 * @return a SSAPMessageParameter
	 */
	public SSAPMessageParameter getObsoleteResultsParameter() {
		SSAPMessageParameter param = null;
		for (SSAPMessageParameter parameter : parameters) {
			if (parameter.getName().equals(SSAPMessageParameter.NameAttribute.OBSOLETERESULTS)) {
				param = parameter;
			}
		}
		return param;
	}	
	
	/**
	 * Gets the string representation of this SSAPMessage
	 * @return a string representation of the SSAPMessage
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(openXML());
		sb.append(contentXML());
		sb.append(closeXML());
		return sb.toString();
	}
	
	/**
	 * Returns the opening tag of a SSAPMessage
	 * @return a string with the opening tag of a XML representation of SSAPMessage
	 */
	protected String openXML() {
		return "<SSAP_message>\n";
	}
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	protected String contentXML() {
		StringBuilder sb = new StringBuilder();
		sb.append("  <node_id>");
		sb.append(nodeId);
		sb.append("</node_id>\n");
		sb.append("  <space_id>");
		sb.append(spaceId);
		sb.append("</space_id>\n");
		sb.append("  <transaction_id>");
		sb.append(transactionId);
		sb.append("</transaction_id>\n");
		sb.append("  <transaction_type>");
		sb.append(tranType);
		sb.append("</transaction_type>\n");
		sb.append("  <message_type>");
		sb.append(msgType);
		sb.append("</message_type>\n");
		for(SSAPMessageParameter parameter : parameters) {
			sb.append(parameter.toString());
		}
		return sb.toString();
	}
	
	/**
	 * Returns the opening tag of a SSAPMessage
	 * @return a string with the closing tag of a XML representation of SSAPMessage
	 */
	protected String closeXML() {
		return "</SSAP_message>";
	}
	
	/**
	 * Gets the byte[] representation of the SSAPMessage
	 * @return a byte[] array
	 */
	public byte[] toByteArray() {
		return toString().getBytes();
	}
}
