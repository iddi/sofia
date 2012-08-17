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

import java.util.HashMap;

/**
 * The specialized parameter for bNodes
 * 
 * @author Fran Ruiz, ESI
 * @author Raul Otaolea, ESI
 *
 */
public class SSAPMessageBNodesParameter extends SSAPMessageParameter {
	
	public static int bNodeIndex = 1;
	
	/**
	 * Constructor of named parameter in a SSAP message
	 * @param status the response of the SIB
	 */
	public SSAPMessageBNodesParameter(HashMap<String,String> bNodes) {
		super(SSAPMessageParameter.NameAttribute.BNODES, getXMLBNodes(bNodes));
	}	
	
	private static String getXMLBNodes(HashMap<String,String> bNodes) {
		if (bNodes != null && bNodes.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("<urilist>\n");
			for (String bNodeIdentifier : bNodes.keySet()) {
				sb.append("\t<uri tag = \"").append(bNodeIdentifier).append("\">");
				sb.append(bNodes.get(bNodeIdentifier)).append("</uri>\n");
			}
			sb.append("</urilist>");
			return sb.toString();
		}
		return null;
	}


	/**
	 * Returns the content of the parameter
	 * @return A String with the content of the parameter
	 */
	@Override
	public String getContent() {
		StringBuilder sb = new StringBuilder();
		if (content == null) {
			sb.append("<urilist>");
			sb.append("</urilist>");
		} else {
			sb.append(super.getContent());
		}
		return sb.toString();
	}
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	@Override
	protected String contentXML() {
		return this.getContent();
	}	
}
