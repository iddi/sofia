package eu.sofia.adk.sib.util;

import java.util.ArrayList;
import java.util.List;

import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.sib.service.ISIBStateListener;

public class SIBStateSupport {

	protected List<ISIBStateListener> listeners = null;
	
	public SIBStateSupport() {
		listeners = new ArrayList<ISIBStateListener>( );
	}
  
    public void addListener(ISIBStateListener l) {
        if(l == null) {
            throw new IllegalArgumentException("The listener can not be null");
        }
        synchronized(listeners) {
        	listeners.add(l);
        }
        
    }

    public void removeListener(ISIBStateListener l) {
    	synchronized(listeners) {
    		listeners.remove(l);
    	}
	}
    
    public void fireSIBStopped(ISIB sib) {
    	synchronized(listeners) {
    		for(ISIBStateListener listener : listeners) {
    			listener.SIBStopped(sib);
    		}
    	}
    }

    public void fireSIBRunning(ISIB sib) {
    	synchronized(listeners) {
    		for(ISIBStateListener listener : listeners) {
    			listener.SIBRunning(sib);
    		}
    	}
    }
    
}
