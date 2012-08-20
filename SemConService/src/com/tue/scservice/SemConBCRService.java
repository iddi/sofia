package com.tue.scservice;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.content.Context;

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

/*
 * Uses IntentService as the base class
 * to make this work on a separate thread.
 */
public class SemConBCRService 
extends ALongRunningNonStickyBroadcastService //implements iKPIC_subscribeHandler
{
	public static String tag = "SemConBCRService";
	
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
	
	static String ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#";
	static String sc = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#";  
	
	int prev_volume = 50;
	
	private Context context;
	
	//Required by IntentService
	public SemConBCRService() {
		super("com.ai.android.service.SemConBCRService");
	}
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		context = getApplicationContext();
		
		 if(HOST==null||PORT==null||SSNM==null)
         {
        	System.out.println("Some parameters are null!");
        	System.exit(1);
         }
        
		//make all needed components
		kp = new KPICore(HOST,Integer.parseInt(PORT),SSNM);
		xmlTools = new SSAP_XMLTools(null,null,null);

		
		System.out.println("Joining the SIB...");		
		xml=kp.join();
		ack=xmlTools.isJoinConfirmed(xml);
		Log.d(tag,"Join confirmed:"+(ack?"YES":"NO")+"\n");
		if(!ack){Log.d(tag,"ERROR"+"Can not JOIN the SIB"); return ;}
		
		//Sync time with SIB
		
/*Android does not implement correct permissions, see Issue: http://code.google.com/p/android/issues/detail?id=4581		
 * 		xml=kp.queryRDF( ns + deviceID , ns + "currentDateTime", null, URI, URI);
		Vector<Vector<String>> result = xmlTools.getQueryTriple(xml);
		
		for(int i=0;i<result.size();i++) {
			String dateTime = result.get(i).get(2);	
			Log.d(tag,"Date/time on SIB: " +dateTime);
			//2012-02-09T10:47:40.925+01:00
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	        Date date;
			try {
				date = (Date)formatter.parse(dateTime);
				Log.d(tag, "Parsed date: " + date);

		        android.os.SystemClock.setCurrentTimeMillis(date.getTime());

			} catch (ParseException e) {
				Log.d(tag,"Couldn't parse " + dateTime);
				e.printStackTrace();
			}

		}*/
		
		
		
	
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Leaving the Smart Space...
	    xml=kp.leave();
	    ack=xmlTools.isLeaveConfirmed(xml);
	    Log.d(tag,"Leave confirmed:"+(ack?"YES":"NO"));	
	}
	
	private void addEvent(String eventType) {
	    Vector<Vector<String>> triples = null;
		triples = new Vector<Vector<String>>();
		String event = "event"+UUID.randomUUID().toString();
		triples.add( xmlTools.newTriple( ns+event, "rdf:type", ns+eventType, URI, URI) );
		triples.add( xmlTools.newTriple( ns+event, ns+"generatedBy", ns+deviceID, URI, URI) );
		
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String dateTime = formatter.format(today);
		triples.add( xmlTools.newTriple( ns+event, ns+"inXSDDateTime", dateTime , URI, LITERAL) );
		
	    xml=kp.insert(triples);
	    ack=xmlTools.isInsertConfirmed(xml);
	    Log.d(tag,"Insert confirmed for "+ deviceID +":"+(ack?"YES":"NO"));
	    if(!ack){Log.d(tag,"ERROR: Can not insert data into the SIB");}
	}
	
	@TargetApi(9)
	private void addEvent(String eventType, String dataValue) {
	    Vector<Vector<String>> triples = null;
		triples = new Vector<Vector<String>>();
		String event = "event"+UUID.randomUUID().toString();
		triples.add( xmlTools.newTriple( ns+event, "rdf:type", ns+eventType, URI, URI) );
		triples.add( xmlTools.newTriple( ns+event, ns+"generatedBy", ns+deviceID, URI, URI) );
		
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String dateTime = formatter.format(today);
		triples.add( xmlTools.newTriple( ns+event, ns+"inXSDDateTime", dateTime , URI, LITERAL) );
		
		if(!dataValue.isEmpty()) {
			Log.d(tag,"Data value: " + dataValue);
			triples.add( xmlTools.newTriple( ns+event, ns+"dataValue", dataValue , URI, LITERAL) );
		}
		
	    xml=kp.insert(triples);
	    ack=xmlTools.isInsertConfirmed(xml);
	    Log.d(tag,"Insert confirmed for "+ deviceID +":"+(ack?"YES":"NO"));	    
	    if(!ack){Log.d(tag,"ERROR: Can not insert data into the SIB");}
	}
	
	/*
	 * Perform long running operations in this method.
	 * This is executed in a separate thread. 
	 */
	@TargetApi(9)
	@Override
	protected void handleBroadcastIntent(Intent broadcastIntent) 
	{
	        Log.d(tag, "intent=" + broadcastIntent);	       
	        String action = broadcastIntent.getAction();
	        Log.d(tag, "action= " + action);
	        
        	String cmd = broadcastIntent.getStringExtra("command");        	
        	if(cmd != null) {
        		Log.d(tag, "command=" + cmd);
	        }
        	
        	if(action.equals("com.tue.scservice.NEW_CONNECTION")) {
        		Log.d(tag,"NEW CONNECTION");
        	}
	        
	        Boolean alarmSet = broadcastIntent.getBooleanExtra("alarmSet",false);
	        Log.d(tag, "alarmSet: " + alarmSet);
	        String nextAlarm = Settings.System.getString(this.getContentResolver(),android.provider.Settings.System.NEXT_ALARM_FORMATTED);
	        Log.d(tag, "next alarm: " + nextAlarm);
	        
	        // The user shouldn't be required to set the time, this should be done automatically
	        if (Intent.ACTION_TIME_CHANGED.equals(action)) {
	        	//String currentTime = System.currentTimeMillis()*0.001));
	        	Calendar newDate = Calendar.getInstance();
	        	
	        	//Log.d(tag, "time changed="+currentTime);
		        //SimpleDateFormat xmlformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		        //String newTime = xmlformatter.format(System.currentTimeMillis());
	        	
		        SimpleDateFormat xmlformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		        String currentTime = xmlformatter.format(newDate.getTime());
		        Log.d(tag,"New TimeSetEvent: " + currentTime );			        
		        addEvent("TimeSetEvent",currentTime);	        	
	        }
	        
	        if(action.equals("android.intent.action.ALARM_CHANGED") && (nextAlarm != null)) {
		        DateFormat formatter = new SimpleDateFormat("E h:mm a");
		        DateFormat formatter24 = new SimpleDateFormat("E H:mm");
		        Date date;
		        Calendar newDate = Calendar.getInstance();
				
		        if(nextAlarm.isEmpty()) {
			        nextAlarm = broadcastIntent.getStringExtra("time");
			        Log.d(tag, "next alarm (from intent extra): " + nextAlarm);
			        if(nextAlarm == null) {
			        	return;
			        }
		        }
		        
		        
				try {
					date = (Date)formatter.parse(nextAlarm);
					Log.d(tag, "Parsed date: " + date);
				} catch (ParseException e) {
					Log.d(tag,"Couldn't parse " + nextAlarm + " as AM. Trying 24 hour format.");					
					try {
						date = (Date)formatter24.parse(nextAlarm);
					}catch(ParseException error) {
						error.printStackTrace();
						return;
					}
				}  	
	
				//newDate.setFirstDayOfWeek(Calendar.SUNDAY); //Date and Calendar seem to differ
		        int diff = date.getDay() - (newDate.get(Calendar.DAY_OF_WEEK)-1);
		        Log.d(tag,"Diff: " + diff);
		        if (!(diff >= 0)) {
		            diff += 7;
		        }
		        Log.d(tag,"Hours date: " + date.getHours());
		        newDate.add(Calendar.DAY_OF_MONTH, diff);
		        newDate.set(Calendar.HOUR_OF_DAY, date.getHours());
		        newDate.set(Calendar.MINUTE, date.getMinutes());
		        newDate.set(Calendar.SECOND, date.getSeconds());

		        SimpleDateFormat xmlformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		        	
		        
		        if(alarmSet == true) {
		        	Log.d(tag,"New AlarmSetEvent: " + xmlformatter.format(newDate.getTime()) );	
		        	addEvent("AlarmSetEvent", xmlformatter.format(newDate.getTime()) );	
		        }
		        if(alarmSet == false) {
		        	Log.d(tag,"New AlarmRemoveEvent: " + xmlformatter.format(newDate.getTime()) );	
		        	addEvent("AlarmRemoveEvent", xmlformatter.format(newDate.getTime()) );	
		        }
		        	
	        }
	        
	        if(action.equals("com.android.deskclock.ALARM_DISMISS") || action.equals("com.tue.scservice.ALARM_END")) {
	        	Log.d(tag,"ALARM KILLED");
	        	addEvent("AlarmEndEvent");
	        }
	        
	        
	        
	        if(action.equals("com.android.deskclock.ALARM_ALERT")) {
		        Log.d(tag,"New AlarmAlertEvent");			        
		        addEvent("AlarmAlertEvent" );	
		        
			    Intent i = new Intent(this, AlarmActivity.class);
			    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    context.startActivity(i);
	        }
	        
	    	
	    	
	        
	        if(action.equals("com.android.music.playstatechanged") || action.equals("com.android.music.metachanged")){
	        	Log.d(tag, "MUSIC WOOP WOOP");
	        	
	        	//Mute media player
	        	AudioManager  mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
	            prev_volume =mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	            int set_volume=0;
	            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,set_volume, 0);
	        	
	        	String artist = broadcastIntent.getStringExtra("artist");
		    	String album = broadcastIntent.getStringExtra("album");
		    	String track = broadcastIntent.getStringExtra("track");
		    	Log.d(tag,artist+":"+album+":"+track);
		    	
		    	AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
		    	if(manager.isMusicActive())
		    	 {
		    		track = track.replace("&", "%20");
		    		addEvent("PlayEvent", artist + "/" + album + "/" + track );
		    	 }else{
		    		 addEvent("PauseEvent"); 
		    	 }
	        }
	        
	        
/*	        if (Intent.ACTION_MEDIA_BUTTON.equals(broadcastIntent.getAction())) {
	            Log.d(tag, "Media button pressed");
	            KeyEvent event = (KeyEvent)broadcastIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	            if (KeyEvent.KEYCODE_VOLUME_UP == event.getKeyCode()) {
	                Log.d(tag,"WOOPWOOP");
	            }
	        }*/
	        

	        
	}
	
}
