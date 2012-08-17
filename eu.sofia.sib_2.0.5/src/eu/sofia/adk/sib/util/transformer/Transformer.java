package eu.sofia.adk.sib.util.transformer;

import java.util.HashMap;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;

/**
 * This abstract class makes the transformations that
 * allows the parsing and transformation of data between
 * different languages that are available for querying and
 * data in RDF.
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 *
 */
public abstract class Transformer {
	/** The logger */
	protected static Logger logger;		
	
	public Transformer() {
    	logger = Logger.getLogger(Transformer.class);
	}
	
	/**
	 * Transforms the string in SPARQL
	 * @param s the input string to be transformed
	 * @return the SPARQL representation of the string
	 */
	public abstract HashMap<String,String> toSPARQL(String s) throws TransformationException;

	/**
	 * Transforms the string in N-Triples format
	 * @param s the input string to be transformed
	 * @return the N-Triples representation of the string
	 */
	public abstract String toNTriples(String s) throws TransformationException;
	
	/**
	 * Transforms the string in RDF-XML format
	 * @param s the input string to be transformed
	 * @return the RDF-XML representation of the string
	 */
	public abstract String toRDFXML(String s) throws TransformationException;

	/**
	 * Returns the results formatted into the original encoding
	 * @param query the original query
	 * @param results the obtained results in SOFIAResultSet format
	 * @return a string representation of the results in the original encoding
	 * @throws TransformationException if some error arise
	 */
	public abstract String toOriginalEncoding(String query, SOFIAResultSet results) throws TransformationException;
}
