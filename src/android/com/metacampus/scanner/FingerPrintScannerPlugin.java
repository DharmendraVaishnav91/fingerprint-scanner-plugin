package com.metacampus.scanner;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

import java.util.Date;
public class FingerPrintScannerPlugin extends CordovaPlugin{
  private static final String TAG="FingerPrintScannerPlugins";


  public void initialize(CordovaInterface cordova, CordovaWebView webView){
    initialize(cordova,webView);
    Log.d(TAG,"Initializing FingerPrintScannerPlugin");
  }

  public boolean execute(String action, JSONArray args, final CallbackContext CallbackContext) throws JSONException{
    if(action.equals("echo")) {
      String phrase = args.getString(0);
      // Echo back the first argument
      Log.d(TAG, phrase);
    }
    return true;
  }
}
