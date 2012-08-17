package eu.sofia.adk.sib.model.subscription;

import java.util.ArrayList;

import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;
import eu.sofia.adk.sib.util.EncodingFactory;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter.TypeAttribute;

public class Subscriber {

	private TypeAttribute originalEncoding;
	private String originalQuery;
	private SOFIAResultSet results;
	private ArrayList<SubscribedKP> subscribedKPs;
	
	public Subscriber(TypeAttribute originalEncoding, 
			String originalQuery, 
			SOFIAResultSet results) {
		this.originalEncoding = originalEncoding;
		this.originalQuery = originalQuery;
		this.results = results;
		this.subscribedKPs = new ArrayList<SubscribedKP>();
	}
	
	public boolean addSubscribedKP(SubscribedKP kp) {
		if (!subscribedKPs.contains(kp)) {
			return subscribedKPs.add(kp);
		}
		return false;
	}
	
	public boolean removeSubscribedKP(SubscribedKP kp) {
		return subscribedKPs.remove(kp);
	}

	public SubscribedKP getSubscribedKP(String kpId) {
		for (SubscribedKP kp : subscribedKPs) {
			if (kp.getSession().getNodeId().equals(kpId)) {
				return kp;
			}
		}
		return null;
	}
	
	/**
	 * Gets the list of subscribed nodes
	 * @return the list of subscribed nodes
	 */
	public ArrayList<SubscribedKP> listSubscribedKPs() {
		return subscribedKPs;
	}	

	public boolean hasSubscribedKPs() {
		return subscribedKPs.size() > 0;
	}

	public TypeAttribute getOriginalEncoding() {
		return originalEncoding;
	}
	
	public SOFIAResultSet getResults() {
		return results;
	}
	
	public String getFormattedResults() throws TransformationException, EncodingNotSupportedException {
		return EncodingFactory.getInstance().getTransformer(originalEncoding).toOriginalEncoding(originalQuery, results);
	}

	public String getOriginalQuery() {
		return originalQuery;
	}

	public void updateResultSet(SOFIAResultSet results) {
		this.results = results;
	}
}
