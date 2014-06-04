/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
/**
 *  Portions of this code are copyright (c) 2009 Harald Mueller and Sofia Lemons.
 * 
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 */
package org.span.manager;

import org.span.service.ManetHelper;
import org.span.service.ManetObserver;
import org.span.service.core.ManetService.AdhocStateEnum;
import org.span.service.system.ManetConfig;
import org.span.service.vanetsex.VANETPrefs;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class ManetManagerApp extends Application implements ManetObserver {

	public static final String TAG = "ManetManagerApp";
	
	// preferences
	public SharedPreferences prefs = null;
	public SharedPreferences.Editor prefEditor = null;
	
	public VANETPrefs vanetPrefs = null;
	
	private int lastByteOfIpAddress = 0;
	private int generatedIdOffset = 0;
	
	// Unique ID of messages in whole network is generated by using last byte of
	// IP address as most significant byte in an int. Rest part of bits in the int
	// are used as auto incremented unique ID starting from 0 and ending with
	// ((2^23)-1).
	// Why 2^23? : Int is 4 bytes. Subtract most significant bit from an integer
	// and subtract the last byte of IP address, gives the 23 bits.
	private final int MAX_ID = (int) Math.pow(2, 23) -1;
	
	// MANET helper
	public ManetHelper manet = null;
	
	// MANET config
	public ManetConfig manetcfg = null;
	
	// adhoc state
	public AdhocStateEnum adhocState = null;
	
	// singleton
	private static ManetManagerApp instance = null;
	
	public static ManetManagerApp getInstance() {
		return instance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d(TAG, "onCreate()");
		
		// singleton
		instance = this;
        
        // preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
        // preference editor
        prefEditor = prefs.edit();
		
		// VANET prefs
        vanetPrefs = VANETPrefs.create(this);
		
        // init MANET helper
		manet = new ManetHelper(this);
		manet.registerObserver(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		
		Log.d(TAG, "onTerminate()");
		
    	// manet.stopAdhoc();
    	manet.disconnectFromService();
	}
    
	/*
	Handler displayMessageHandler = new Handler(){
        public void handleMessage(Message msg) {
       		if (msg.obj != null) {
       			ManetManagerApp.this.displayToastMessage((String)msg.obj);
       		}
        	super.handleMessage(msg);
        }
    };
    */
    
	public void displayToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
	
	public void focusAndshowKeyboard(final View v) {
		v.requestFocus();
		v.postDelayed(new Runnable() {
              @Override
              public void run() {
                  InputMethodManager keyboard = (InputMethodManager)
                  getSystemService(getBaseContext().INPUT_METHOD_SERVICE);
                  keyboard.showSoftInput(v, 0);
              }
          },100);
	}
    
    public int getVersionNumber() {
    	int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Package name not found", e);
        }
        return version;
    }
    
    public String getVersionName() {
    	String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (Exception e) {
            Log.e(TAG, "Package name not found", e);
        }
        return version;
    }
    
    public boolean isServiceRunning(Class serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    public void setLastByteOfIpAddress(int lastByteOfIpAddress) {
        this.lastByteOfIpAddress = lastByteOfIpAddress;
        generatedIdOffset = (int)(lastByteOfIpAddress * Math.pow(2, 23));
    }
    
    public int generateNextVANETMessageID() {
        int generatedId = vanetPrefs.get_message_id_generator_value();
        if(generatedId >= MAX_ID) {
            vanetPrefs.edit().put_message_id_generator_value(generatedId + 1).commit();
        } else {
            vanetPrefs.edit().put_message_id_generator_value(0).commit();
        }
        
        return generatedIdOffset + generatedId;
    }
    
    public int generateNextVANETEventID() {
        int generatedId = vanetPrefs.get_event_id_generator_value();
        if(generatedId >= MAX_ID) {
            vanetPrefs.edit().put_event_id_generator_value(generatedId + 1).commit();
        } else {
            vanetPrefs.edit().put_event_id_generator_value(0).commit();
        }
        
        return generatedIdOffset + generatedId;
    }
    
  
	// MANET callback methods
  	
	@Override
	public void onServiceConnected() {
		Log.d(TAG, "onServiceConnected()"); // DEBUG
	}

	@Override
	public void onServiceDisconnected() {
		Log.d(TAG, "onServiceDisconnected()"); // DEBUG
	}

	@Override
	public void onServiceStarted() {
		Log.d(TAG, "onServiceStarted()"); // DEBUG
	}

	@Override
	public void onServiceStopped() {
		Log.d(TAG, "onServiceStopped()"); // DEBUG
	}

	@Override
	public void onAdhocStateUpdated(AdhocStateEnum state, String info) {
		Log.d(TAG, "onAdhocStateUpdated()"); // DEBUG
		adhocState = state;
	}
	
	@Override
	public void onConfigUpdated(ManetConfig manetcfg) {
		// Log.d(TAG, "onConfigUpdated()"); // DEBUG
		this.manetcfg = manetcfg;
		
		String device = manetcfg.getDeviceType();
		Log.d(TAG, "device: " + device); // DEBUG
	}
	
	@Override
	public void onError(String error) {
		// Log.d(TAG, "onError()"); // DEBUG
	}
}
