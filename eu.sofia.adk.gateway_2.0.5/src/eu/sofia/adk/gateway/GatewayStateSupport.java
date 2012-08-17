package eu.sofia.adk.gateway;

import java.util.ArrayList;
import java.util.List;

import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.gateway.service.IGatewayStateListener;

public class GatewayStateSupport {

	protected List<IGatewayStateListener> listeners = null;
	
	public GatewayStateSupport() {
		listeners = new ArrayList<IGatewayStateListener>( );
	}
  
    public void addListener(IGatewayStateListener l) {
        if(l == null) {
            throw new IllegalArgumentException("The listener can not be null");
        }
        synchronized(listeners) {
        	listeners.add(l);
        }
    }

    public void removeListener(IGatewayStateListener l) {
    	synchronized(listeners) {
    		listeners.remove(l);
    	}
	}
    
    public void fireGatewayStopped(IGateway gateway) {
    	synchronized(listeners) {
    		for(IGatewayStateListener listener : listeners) {
    			listener.gatewayStopped(gateway);
    		}
    	}
    }

    public void fireGatewayRunning(IGateway gateway) {
    	synchronized(listeners) {
    		for(IGatewayStateListener listener : listeners) {
    			listener.gatewayRunning(gateway);
    		}
    	}
    }
}
