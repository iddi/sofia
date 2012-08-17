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

package eu.sofia.adk.common.xsd;

import org.apache.commons.lang.ArrayUtils;

/**
 * A helper class that returns the corresponding datatype mapping.
 * This mapping is based on the defined mapping for the JAXB (see
 * <url>http://java.sun.com/webservices/docs/2.0/tutorial/doc/JAXBWorks4.html</url>
 *  
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class JavaDatatype {
	
	/** The Java defined datatypes */
	private static String[] integerDatatype = {"byte", "short", "int"};
	private static String[] longDatatype = {"long"};
	private static String[] floatDatatype = {"float"};
	private static String[] doubleDatatype = {"double"};
	private static String[] booleanDatatype = {"boolean"};
	
	/** the shared instance */
	static private JavaDatatype _instance = null;
	
	/**
	 * Gets the shared instance
	 * @return the shared instance of ADK Model
	 */
	static public JavaDatatype getInstance() {
		if(null == _instance) {
			_instance = new JavaDatatype();
		}
		return _instance;
	}
	
	/**
	 * Gets the initialization value for a given datatype
	 * @param datatype the string representation of the datatype
	 * @return a string representation of the initialization value
	 */
	public static String getInizializationValue(String datatype) {
		if (isIntegerDatatype(datatype)) {
			return "0";
		} else if (isLongDatatype(datatype)) {
			return "0L";
		} else if (isFloatDatatype(datatype)) {
			return "0.0f";
		} else if (isDoubleDatatype(datatype)) {
			return "0.0d";
		} else if (isBooleanDatatype(datatype)) {
			return "false";
		} else {
			return "null";
		}
	}
	
	/**
	 * Determines if the datatype is an integer datatype 
	 * @param datatype a string representation of the datatype
	 * @return <code>true</code> if the datatype is an integer type
	 */
	private static boolean isIntegerDatatype(String datatype) {
		return ArrayUtils.contains(integerDatatype, datatype);
	}

	/**
	 * Determines if the datatype is a long datatype 
	 * @param datatype a string representation of the datatype
	 * @return <code>true</code> if the datatype is a long type
	 */
	private static boolean isLongDatatype(String datatype) {
		return ArrayUtils.contains(longDatatype, datatype);
	}

	/**
	 * Determines if the datatype is a float datatype 
	 * @param datatype a string representation of the datatype
	 * @return <code>true</code> if the datatype is a float type
	 */
	private static boolean isFloatDatatype(String datatype) {
		return ArrayUtils.contains(floatDatatype, datatype);
	}

	/**
	 * Determines if the datatype is a double datatype 
	 * @param datatype a string representation of the datatype
	 * @return <code>true</code> if the datatype is a double type
	 */
	private static boolean isDoubleDatatype(String datatype) {
		return ArrayUtils.contains(doubleDatatype, datatype);
	}

	/**
	 * Determines if the datatype is a boolean datatype 
	 * @param datatype a string representation of the datatype
	 * @return <code>true</code> if the datatype is a boolean type
	 */
	private static boolean isBooleanDatatype(String datatype) {
		return ArrayUtils.contains(booleanDatatype, datatype);
	}
}
