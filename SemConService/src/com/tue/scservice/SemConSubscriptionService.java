package com.tue.scservice;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.app.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import javax.xml.datatype.*;

import sofia_kp.KPICore;
import sofia_kp.SSAP_XMLTools;
import sofia_kp.iKPIC_subscribeHandler;


public class SemConSubscriptionService extends Service implements iKPIC_subscribeHandler {
	
public static String tag = "SemConSubscriptionService";
	
	static KPICore kp = null;
	static SSAP_XMLTools xmlTools=null; //SSAP object
	String xml="";
	boolean ack=false;
	
	static final String URI     = "uri";
	static final String LITERAL = "literal";
	static final String deviceID = "phone1";
    
    //Default configuration parameters
    //SIB connection parameters
	//static String HOST = "131.155.176.117";
	static String HOST = "192.168.1.4";
	static String PORT = "23000"; 
	static String SSNM = "test"; 
	
	static final String ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#";
	static final String sc = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#";
	static final String semint = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticInteraction.owl#";
	static final String sofia = "http://www.sofia-project.eu/ontologies/core/2010/01/19/sofia.owl#";
	
	
	String subID_1="";
	Vector<Vector<String>> triples = null;
	
	void registerDevice() {
		triples = new Vector<Vector<String>>();
		//On connect to smart space, writes the following to the SIB:
		String u1 = ns + deviceID;
		
		triples.add( xmlTools.newTriple( u1, "rdf:type", sofia + "SmartObject", URI, URI) );
		//triples.add( xmlTools.newTriple( u1, sc + "acceptsMediaType", sc + "Audio", URI, URI) ); //change to values for the dimmable light

		String u2 = ns + "id" + UUID.randomUUID().toString();
		
		triples.add( xmlTools.newTriple( u1, sofia + "hasIdentification", u2 , URI, URI) );
		triples.add( xmlTools.newTriple( u2, semint + "ofIDType", semint + "RFID_Mifare" , URI, URI) );		
		triples.add( xmlTools.newTriple( u2, semint + "idValue", "\"440086305B\"" , URI, LITERAL) );
		
		triples.add( xmlTools.newTriple( u1, ns + "functionalitySource", ns + "Alarm" , URI, URI) );
		triples.add( xmlTools.newTriple( u1, ns + "functionalitySource", ns + "Music" , URI, URI) );
    
	    xml=kp.insert(triples);
	    ack=xmlTools.isInsertConfirmed(xml);
	    Log.d(tag,"Insert confirmed:"+(ack?"YES":"NO"));	    
	    if(!ack){Log.d(tag,"Can not insert data into the SIB)"); return ;}
	}
	
	
	@Override
	public void onCreate() {		  
	
		if(HOST==null||PORT==null||SSNM==null)
		{
			System.out.println("Some parameters are null!");
		   	System.exit(1);
		}
		   
		//make all needed components
		kp = new KPICore(HOST,Integer.parseInt(PORT),SSNM);
		xmlTools = new SSAP_XMLTools(null,null,null);
		
		kp.setEventHandler(this);
		
		System.out.println("Joining the SIB...");		
		xml=kp.join();
		ack=xmlTools.isJoinConfirmed(xml);
		Log.d(tag,"Join confirmed:"+(ack?"YES":"NO")+"\n");
		if(!ack){Log.d(tag,"ERROR"+"Can not JOIN the SIB"); return ;}
		
		registerDevice();
		
		if(xml==null || xml.length()==0){Log.d(tag,"Subscription message NOT valid!\n"); return;}
		Log.d(tag,"Subscribe confirmed:"+(this.xmlTools.isSubscriptionConfirmed(xml)?"YES":"NO")+"\n");
		
//		if(!this.xmlTools.isSubscriptionConfirmed(xml)){return;}
//		subID_1=this.xmlTools.getSubscriptionID(xml);
//		Log.d(tag,"RDF Subscribe initial result:"+xml.replace("\n", "")+"\n");
		
		Log.d(tag,"Subscribe to connectedTo as source\n"); 
		xml=kp.subscribeRDF( ns + deviceID , sc + "connectedTo",null, URI);
		
		if(xml==null || xml.length()==0){Log.d(tag,"Subscription message NOT valid!\n"); return;}
		Log.d(tag,"Subscribe confirmed:"+(this.xmlTools.isSubscriptionConfirmed(xml)?"YES":"NO")+"\n");
		
	}

	  @Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
	      Log.d(tag, "service starting");
	      
	      // If we get killed, after returning from here, restart
	      return START_STICKY;
	  }

	  @Override
	  public IBinder onBind(Intent intent) {
	      // We don't provide binding, so return null
	      return null;
	  }
	  
	  @Override
	  public void onDestroy() {
		super.onDestroy();
		//Leaving the Smart Space...
	    xml=kp.leave();
	    ack=xmlTools.isLeaveConfirmed(xml);
	    System.out.println("Leave confirmed:"+(ack?"YES":"NO"));	
	    Log.d(tag, "service done"); 
	  }
	  
	  /**
		 * Handle the events in case a triple we have subscribed to changes
		 */
	  @Override
	  public void kpic_SIBEventHandler(String xml) {
			String subject = "", object = "", predicate = "";
			
			Log.d(tag,"Subscription notification!");
			//Get new triples as triples are added or updated in the SIB
			Vector<Vector<String>> triples = xmlTools.getNewResultEventTriple(xml);
			
			if(triples!=null){
				for(int i=0; i<triples.size() ; i++ ){ 
					Vector<String> t=triples.get(i);
					subject=xmlTools.triple_getSubject(t);
					object=xmlTools.triple_getObject(t);
					predicate=xmlTools.triple_getPredicate(t);
				}
				if(triples.size()==0){
		
					triples = xmlTools.getObsoleteResultEventTriple(xml);
					
					if(triples != null) {
						Log.d(tag,"Obsolete Triples:" + triples);
						for(int i=0; i<triples.size() ; i++ ){ 
							Vector<String> t=triples.get(i);
							subject=xmlTools.triple_getSubject(t);
							predicate=xmlTools.triple_getPredicate(t);
							object=xmlTools.triple_getObject(t);
						}
						if(predicate.contains("connectedTo") && subject.contains(deviceID)) {
							Log.d(tag,"Setting to third of max volume");
				        	AudioManager  mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
				            int set_volume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/3;
				        	mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,set_volume, 0);

						}


					}
					
					return;
				}
			}
			
			
			Log.d(tag,"Triple:" + triples);
			
			if(predicate.contains("connectedTo") && subject.contains(deviceID)) {
				//This device is now a source
				Log.d(tag,"Now a source for " + object);
		    	Intent broadcastIntent = new Intent("com.tue.scservice.NEW_CONNECTION");		    	
		    	//broadcastIntent.putExtra("message", "Hello world");
		    	this.sendBroadcast(broadcastIntent);
			}
			
			
			if(subject.contains("event")){
				
				xml=kp.queryRDF( subject , "rdf:type", null, URI, URI);
				Vector<Vector<String>> result = xmlTools.getQueryTriple(xml);
				
				for(int i=0;i<result.size();i++) {
					String eventType = result.get(i).get(2);	
				
					if(eventType.contains("AlarmEndEvent")){
						Log.d(tag,"AlarmEndEvent");
						Intent intent = new Intent("com.android.deskclock.ALARM_DISMISS");
						sendBroadcast(intent); //Deskclock should pick this up and end alarm
					}
				}
			}
			
		}
	}



//