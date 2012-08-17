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

package eu.sofia.adk.common.naming;

import java.util.StringTokenizer;

/**
 * The naming class is a utility class that allows to get the normalized
 * names in the different programming languages supported by this ADK.
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class Naming {

	/**
	 * Obtains a valid class name for a given class name
	 * @param className a class name
	 * @return a String with an normalized class name in java
	 */
	public static String getNormalizedJavaClassName(String className) {
		
		StringBuffer validClassName = new StringBuffer();
		
		StringTokenizer st = new StringTokenizer(className, ".-_");
	    
		while (st.hasMoreTokens()) {
	         String word = st.nextToken();
	         validClassName.append(getCapitalizedWord(word));
		}
	    
		return validClassName.toString();
	}
	
	/**
	 * Obtains a valid property name for a given class name
	 * @param propertyName a property name
	 * @return a String with an normalized property name in java
	 */
	public static String getNormalizedJavaPropertyName(String propertyName) {
		
		StringBuffer validPropertyName = new StringBuffer();
		
		StringTokenizer st = new StringTokenizer(propertyName, ".-");
	     while (st.hasMoreTokens()) {
	         String word = st.nextToken();
	         validPropertyName.append(word);
	     }
	     
	     return validPropertyName.toString();
	}	

	/**
	 * Transform the incoming word into a capitalized word
	 * @param word a word
	 * @return a string wiht capitalized word
	 */
	private static String getCapitalizedWord(String word) {
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}
}
