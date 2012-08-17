package com.tue.scservice;

import com.tue.scservice.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;



public class AlarmActivity extends Activity {
	
	public static final String tag="AlarmActivity";
	private Context context;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        context = this;
        
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        
        alertDialog.setTitle("Dismiss alarm");
        alertDialog.setMessage("Brough to you by a semantic connection");
        
        alertDialog.setCancelable(false);
        
        alertDialog.setButton("Snooze", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
       
                Intent i = new Intent("com.android.deskclock.ALARM_SNOOZE");
                context.sendBroadcast(i);
                finish();
          } });
        
        alertDialog.setButton2("Dismiss", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
       
                Intent i = new Intent("com.android.deskclock.ALARM_DISMISS");
                context.sendBroadcast(i);
                finish();
       
          } });
        
        alertDialog.show();
        
        /* WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.copyFrom(alertDialog.getWindow().getAttributes());
        lp.width = 300;
        lp.height = 500;
        lp.x=-200;
        lp.y=100;
        alertDialog.getWindow().setAttributes(lp);*/
        
        /*final Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Perform action on clicks
                TextView tv = getTextView();
                tv.setText("boom");
                Intent i = new Intent("com.android.deskclock.ALARM_DISMISS");
                context.sendBroadcast(i);
            }
        });*/
        

    }
    
    
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();

    }
    
    private TextView getTextView(){
        return (TextView)this.findViewById(R.id.text1);
    }
    

	
	

}
