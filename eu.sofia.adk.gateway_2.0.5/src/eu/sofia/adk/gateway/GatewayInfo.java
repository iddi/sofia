package eu.sofia.adk.gateway;

import org.osgi.framework.ServiceRegistration;

public class GatewayInfo {

	private AbstractGateway gateway;
	private ServiceRegistration service; 
	
	public GatewayInfo() {
	}

	public AbstractGateway getGateway() {
		return gateway;
	}

	public void setGateway(AbstractGateway gateway) {
		this.gateway = gateway;
	}

	public ServiceRegistration getService() {
		return service;
	}

	public void setService(ServiceRegistration service) {
		this.service = service;
	}
	
	
}
