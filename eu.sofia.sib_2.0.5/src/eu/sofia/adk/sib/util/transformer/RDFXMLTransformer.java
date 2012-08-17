package eu.sofia.adk.sib.util.transformer;

import java.util.HashMap;

import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;
import eu.sofia.adk.sib.model.query.SOFIAResultSetFormatter;

public class RDFXMLTransformer extends Transformer {

	@Override
	public String toNTriples(String s) throws TransformationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toRDFXML(String s) throws TransformationException {
		return s;
	}

	@Override
	public HashMap<String, String> toSPARQL(String s) throws TransformationException {
		HashMap<String, String> sparqlQueries = new HashMap<String, String>();
		sparqlQueries.put(s,s);
		return sparqlQueries;
		
	}

	@Override
	public String toOriginalEncoding(String query, SOFIAResultSet results)
			throws TransformationException {
		try {
			// Transforms the RDF-M3 structure into SPARQL
			return SOFIAResultSetFormatter.asXMLString(results);

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot parse due to " + ex.getMessage());
		}
	}

}
