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

package eu.sofia.adk.common.sax;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** 
 * This class is the one that implements the ErrorHandler associated to 
 * the XML parsing.
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class SAXErrorHandler implements ErrorHandler {

	/**
	 * Shows the warning and throws the exception  
	 * @param e
	 * @throws SAXException
	 */
	public void warning(SAXParseException e) throws SAXException {
		show("Warning", e);
	    throw (e);
	}

	/**
	 * Shows the error and throws the exception  
	 * @param e
	 * @throws SAXException
	 */
	public void error(SAXParseException e) throws SAXException {
		show("Error", e);
		throw (e);
	}

	/**
	 * Shows the fatal error and throws the exception  
	 * @param e the excpetion
	 * @throws SAXException
	 */
	public void fatalError(SAXParseException e) throws SAXException {
		show("Fatal Error", e);
		throw (e);
	}

	/**
	 * Show the message of the exception
	 * @param type the type of message
	 * @param e the exception
	 */
	private void show(String type, SAXParseException e) {
		System.out.println(type + ": " + e.getMessage());
		System.out.println("Line " + e.getLineNumber() + " Column "
		        + e.getColumnNumber());
		System.out.println("System ID: " + e.getSystemId());
	}	
}
