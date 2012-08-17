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

package eu.sofia.adk.gateway.parser;

/**
 * The class <code>SSAPMessageParseException</code> is a form of <code>Throwable</code>
 * that indicates conditions that a reasonable application might want to catch.
 *   
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 *
 */
@SuppressWarnings("serial")
public class SSAPMessageParseException extends Exception {

	/**
	 * Constructs a new exception with <code>null</code> as its detail message. 
	 * The cause is not initialized, and may subsequently be initialized by a 
	 * call to {@link Throwable.initCause(java.lang.Throwable)}.
	 * @param message the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public SSAPMessageParseException() {
		super();
	}

	
	/**
	 * Constructs a new exception with the specified detail message. 
	 * The cause is not initialized, and may subsequently be initialized by a 
	 * call to {@link Throwable.initCause(java.lang.Throwable)}.
	 * @param message the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public SSAPMessageParseException(String message) {
		super(message);
	}
	
	/**
	 * Constructs a new exception with the specified detail message and cause.<br/>
	 * Note that the detail message associated with cause is not automatically incorporated in this exception's detail message. 
	 * @param message the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 * @param cause
	 */
	public SSAPMessageParseException(String message, Throwable cause) {
		super(message,cause);
	}
}
