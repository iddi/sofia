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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation of the SAX error handler interface.
 * The parser will then report all errors and warnings through this interface.
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class SSAPMessageErrorHandler implements ErrorHandler {

	/**
	 * Receive notification of a warning.<br/>
	 * SAX parsers will use this method to report conditions that are not errors
	 * or fatal errors as defined by the XML recommendation. The default behaviour 
	 * is to take no action.<br/>
	 * The SAX parser must continue to provide normal parsing events after 
	 * invoking this method: it should still be possible for the application 
	 * to process the document through to the end.<br/>
	 * Filters may use this method to report other, non-XML warnings as well.
	 * 
	 * @param exception The warning information encapsulated in a SAX parse exception.
	 * @throws SAXException any SAX exception, possibly wrapping another exception
	 */
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SSAPMessage parse warning in line ").append(exception.getLineNumber());
		sb.append(",column ").append(exception.getColumnNumber()).append("\n");
		sb.append("Message:").append(exception.getMessage());
		
		System.err.println(sb.toString());
		exception.printStackTrace();
       
        throw new SAXException("SAX parsing warning");
	}

	/**
	 * Receive notification of a recoverable error.
	 * This corresponds to the definition of "error" in section 1.2 of the 
	 * W3C XML 1.0 Recommendation. For example, a validating parser would use 
	 * this callback to report the violation of a validity constraint. 
	 * The default behaviour is to take no action.<br/>
	 * The SAX parser must continue to provide normal parsing events after 
	 * invoking this method: it should still be possible for the application 
	 * to process the document through to the end. If the application cannot 
	 * do so, then the parser should report a fatal error even if the XML 
	 * recommendation does not require it to do so.<br/>
	 * Filters may use this method to report other, non-XML errors as well.
	 * 
	 * @param exception The warning information encapsulated in a SAX parse exception.
	 * @throws SAXException any SAX exception, possibly wrapping another exception
	 */
	@Override
	public void error(SAXParseException exception) throws SAXException {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SSAPMessage parse error in line ").append(exception.getLineNumber());
		sb.append(",column ").append(exception.getColumnNumber()).append("\n");
		sb.append("Message:").append(exception.getMessage());
		
		System.err.println(sb.toString());
		exception.printStackTrace();
       
        throw new SAXException("SAX parsing error", exception);
	}

	/**
	 * Receive notification of a non-recoverable error.
	 * <strong>There is an apparent contradiction between the documentation for this 
	 * method and the documentation for ContentHandler.endDocument(). Until 
	 * this ambiguity is resolved in a future major release, clients should 
	 * make no assumptions about whether endDocument() will or will not be 
	 * invoked when the parser has reported a fatalError() or thrown an 
	 * exception.</strong><br/>
	 * This corresponds to the definition of "fatal error" in section 1.2 of 
	 * the W3C XML 1.0 Recommendation. For example, a parser would use this 
	 * callback to report the violation of a well-formedness constraint.<br/>
	 * The application must assume that the document is unusable after the 
	 * parser has invoked this method, and should continue (if at all) only 
	 * for the sake of collecting additional error messages: in fact, SAX 
	 * parsers are free to stop reporting any other events once this method 
	 * has been invoked.
	 *  
	 * @param exception The warning information encapsulated in a SAX parse exception.
	 * @throws SAXException any SAX exception, possibly wrapping another exception
	 */
	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SSAPMessage parse fatal error in line ").append(exception.getLineNumber());
		sb.append(",column ").append(exception.getColumnNumber()).append("\n");
		sb.append("Message:").append(exception.getMessage());
		
		System.err.println(sb.toString());
		exception.printStackTrace();
       
        throw new SAXException("SAX parsing fatal error", exception);
	}

}
