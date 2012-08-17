package eu.sofia.adk.sib.mgt;

import org.osgi.framework.ServiceRegistration;

import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.sib.service.ISIB;

public class SIBRegistration {

	private ISIB sib;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration SIBRegistration;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration viewerRegistration;
	
	public SIBRegistration() {
	}

	public ISIB getSIB() {
		return sib;
	}

	public void setSIB(ISIB sib) {
		this.sib = sib;
	}
	
	@SuppressWarnings("rawtypes")
	public ServiceRegistration getSIBRegistration() {
		return SIBRegistration;
	}

	@SuppressWarnings("rawtypes")
	public void setSIBRegistration(ServiceRegistration sIBRegistration) {
		SIBRegistration = sIBRegistration;
	}

	@SuppressWarnings("rawtypes")
	public ServiceRegistration getViewerRegistration() {
		return viewerRegistration;
	}

	@SuppressWarnings("rawtypes")
	public void setViewerRegistration(ServiceRegistration viewerRegistration) {
		this.viewerRegistration = viewerRegistration;
	}

	public void unregister() {
		if(this.SIBRegistration != null) {
			this.SIBRegistration.unregister();
		}
		if(this.viewerRegistration != null) {
			this.viewerRegistration.unregister();
		}
	}

	public void stop() throws SIBException {
		if(this.sib != null) {
			this.sib.stop();
		}
	}
	
}
