package eu.sofia.adk.sib.util.transformer;

import java.util.HashMap;

import eu.sofia.adk.sib.exception.RDFM3ParseException;
import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;
import eu.sofia.adk.sib.model.query.SOFIAResultSetFormatter;
import eu.sofia.adk.sib.protocol.m3.M3Parser;
import eu.sofia.adk.sib.protocol.m3.RDFM3;
import eu.sofia.adk.sib.protocol.m3.RDFM3Triple;

public class RDFM3Transformer extends Transformer {

	private static final String CR = "\n";
	private static final String SPARQL_PREFIX = "PREFIX";
	private static final String SPARQL_SELECT = "SELECT";
	private static final String SPARQL_WHERE = "WHERE";	
	
	@Override
	public String toNTriples(String s) throws TransformationException {
		try {
			// Parses the input stream 
			M3Parser parser = new M3Parser();
			RDFM3 m3 = parser.parse(s);
			
			// Transforms the RDF-M3 structure into N-Triple format
			return this.formatAsNTriples(m3);

		} catch (RDFM3ParseException ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot parse due to " + ex.getMessage());
		} catch (TransformationException ex) {
			ex.printStackTrace();
			throw new TransformationException("Transformation from RDF-M3 into SPARQL was not successful");
		}
	}

	@Override
	public String toRDFXML(String s) throws TransformationException {
		return null;
		//throw new TransformationException("Cannot perform transform from RDF-M3 into RDF-XML format");
	}

	@Override
	public HashMap<String,String> toSPARQL(String s) throws TransformationException {
		try {
			// Parses the input stream 
			M3Parser parser = new M3Parser();
			RDFM3 m3 = parser.parse(s);
			
			// Transforms the RDF-M3 structure into SPARQL
			return this.formatAsSPARQL(m3);

		} catch (RDFM3ParseException ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot parse due to " + ex.getMessage());
		} catch (TransformationException ex) {
			ex.printStackTrace();
			throw new TransformationException("Transformation from RDF-M3 into SPARQL was not successful");
		}
	}
	
	@Override 
	public String toOriginalEncoding(String query, SOFIAResultSet results)
			throws TransformationException {
		try {
			// Parses the input stream 
			M3Parser parser = new M3Parser();
			RDFM3 m3 = parser.parse(query);
			
			// Transforms the RDF-M3 structure into SPARQL
			return SOFIAResultSetFormatter.asRDFM3(results, m3);

		} catch (RDFM3ParseException ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot parse due to " + ex.getMessage());
		} catch (TransformationException ex) {
			ex.printStackTrace();
			throw new TransformationException("Transformation from RDF-M3 into SPARQL was not successful");
		}
	}

	/**
	 * Formats a RDF-M3 as a SPARQL
	 * @return a string containing a SPARQL-query
	 * @throws TransformationException 
	 */
	private HashMap<String, String> formatAsSPARQL(RDFM3 m3) throws TransformationException {
		try {
			HashMap<String,String> sparqlQueries = new HashMap<String,String>();
			
			StringBuilder prefixes = new StringBuilder();

			if (m3.hasNamespaces()) {
				for(String prefix : m3.listNamespaces().keySet()) {
					prefixes.append(SPARQL_PREFIX).append(" ");
					prefixes.append(prefix).append(":");
					prefixes.append("<").append(m3.listNamespaces().get(prefix)).append(">");
					prefixes.append(CR);
				}
				prefixes.append(CR);
			}
			
			if (m3.hasTriples()) {
				for(RDFM3Triple queryTriple : m3.listTriples()) {

					StringBuilder sb = new StringBuilder();

					sb.append(prefixes.toString());
					
					// check wildcard constraints
					if (queryTriple.isSubjectWildcard() && 
							queryTriple.isPredicateWildcard() &&
							queryTriple.isObjectWildcard()) {
						sb.append(SPARQL_SELECT).append(" ");
						sb.append("?subject ?predicate ?object").append(CR);
						sb.append(SPARQL_WHERE).append(" { ");
						sb.append("?subject ?predicate ?object").append(" }").append(CR);
					} else if (!queryTriple.isSubjectWildcard() && 
							!queryTriple.isPredicateWildcard() &&
							!queryTriple.isObjectWildcard()) {
						// do nothing
					} else {
						StringBuilder selectVariables = new StringBuilder();
						StringBuilder whereVariables = new StringBuilder();
						selectVariables.append(SPARQL_SELECT).append(" ");
						whereVariables.append(SPARQL_WHERE).append(" {");
						
						// check subject: if wildcard, url or something
						if (queryTriple.isSubjectWildcard()) {
							selectVariables.append("?subject ");
							whereVariables.append("?subject ");
						} else {
							if (!queryTriple.isSubjectURL()) {
								whereVariables.append(queryTriple.getSubject());
							} else if (queryTriple.isSubjectURL()) {
								whereVariables.append("<");
								whereVariables.append(queryTriple.getSubject());
								whereVariables.append("> ");
							} else if (queryTriple.isSubjectNsPrefixFormat()) {
								whereVariables.append(queryTriple.getSubject());
							} else {
								whereVariables.append("<");
								whereVariables.append(queryTriple.getSubject());
								whereVariables.append("> ");
							}
						}
	
						// check predicate: if wildcard, url or something
						if (queryTriple.isPredicateWildcard()) {
							selectVariables.append("?predicate ");
							whereVariables.append("?predicate ");
						} else if (queryTriple.isPredicateNsPrefixFormat()) {
							String prefix = queryTriple.getPredicatePrefix();
							String namespace = m3.getNamespace(prefix);
							if (namespace != null) {
								whereVariables.append("<").append(namespace);
								if (!namespace.endsWith("#")) {
									whereVariables.append("#");
								}
								whereVariables.append(queryTriple.getPredicateElement()).append("> ");						
							} else {
								whereVariables.append("<");
								whereVariables.append(queryTriple.getPredicate());
								whereVariables.append("> ");
							}
						}
						
						// check predicate: if wildcard, url or something
						if (queryTriple.isObjectWildcard()) {
							selectVariables.append("?object ");
							whereVariables.append("?object ");
						} else {
							if(queryTriple.isObjectURL()) {
								whereVariables.append(" <");
								whereVariables.append(queryTriple.getObject());
								whereVariables.append("> ");
							} else if(queryTriple.isObjectLiteral()) {
								whereVariables.append(" \"");
								whereVariables.append(queryTriple.getObjectContent());
								whereVariables.append("\"");
							} else if (queryTriple.isObjectNsPrefixFormat()) {
								whereVariables.append(queryTriple.getObject());
							} else {
								whereVariables.append("<");
								whereVariables.append(queryTriple.getObject());
								whereVariables.append("> ");
							}
						}
						
						whereVariables.append("}");
						
						sb.append(selectVariables).append(whereVariables);
					}
					
					RDFM3 query = new RDFM3();
					query.addNamespaces(m3.listNamespaces());
					query.addTriple(queryTriple);
					sparqlQueries.put(sb.toString(),query.toString());
				}
			}
			
			return sparqlQueries;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot transform from RDF-M3 into SPARQL format");
		}
	}

	/**
	 * Formats a RDF-M3 as a N-Triples
	 * @return a string containing a set of N-Triples
	 * @throws TransformationException
	 */
	private String formatAsNTriples(RDFM3 m3) throws TransformationException {
		
		try {
			if (!m3.hasTriples()) { // check the existence of ntriples
				logger.error("Incoming parameter cannot be transformed in N-Triples format due to it has no triples");
				return "";
			}
			
			return m3.toNTriples();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot transform from RDF-M3 into N-Triple format");
		}
	}
}
