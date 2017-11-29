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


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;

import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.widget.Toast;


import java.io.IOException;
import java.nio.ByteBuffer;


import SecuGen.Driver.Constant;
import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGANSITemplateInfo;
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier;
import SecuGen.FDxSDKPro.SGDeviceInfoParam;
import SecuGen.FDxSDKPro.SGFDxConstant;
import SecuGen.FDxSDKPro.SGFDxDeviceName;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPresentEvent;
import SecuGen.FDxSDKPro.SGISOTemplateInfo;
import SecuGen.FDxSDKPro.SGImpressionType;
import SecuGen.FDxSDKPro.SGWSQLib;

import android.util.Log;

import java.util.Date;

public class FingerPrintScannerPlugin extends CordovaPlugin implements SGFingerPresentEvent {

    private static final String TAG = "FingerPrintScannerPlugin";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private int[] grayBuffer;
    private Bitmap grayBitmap;
    private IntentFilter filter; //2014-04-11
    private SGAutoOnEventNotifier autoOn;
    //  private boolean mLed;
    private boolean mAutoOnEnabled;
    // private int nCaptureModeN;
    private boolean bSecuGenDeviceOpened;
    private JSGFPLib sgfplib;
    private boolean usbPermissionRequested;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

    }
    @Override
    protected void pluginInitialize() {
      super.pluginInitialize();
      Context context = cordova.getActivity()
        .getApplicationContext();
      ContextWrapper contextWrapper = new ContextWrapper(context);
      mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
      filter = new IntentFilter(ACTION_USB_PERMISSION);
      //Uncomm
      contextWrapper.registerReceiver(mUsbReceiver, filter);
      sgfplib = new JSGFPLib((UsbManager) context.getSystemService(Context.USB_SERVICE));
      UsbDevice usbDevice = sgfplib.GetUsbDevice();


        // this.mToggleButtonSmartCapture.toggle();
        bSecuGenDeviceOpened = false;
        usbPermissionRequested = false;
        mAutoOnEnabled = false;
        //  autoOn = new SGAutoOnEventNotifier(sgfplib, context);

    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            byte[] capturedByteData=captureFingerPrint();
            final PluginResult result = new PluginResult(PluginResult.Status.OK, capturedByteData.toString());
            callbackContext.sendPluginResult(result);
        }
        return true;
    }

    public void SGFingerPresentCallback (){
    //Toast.makeText(JSGDActivity.this,"finger present callback is called",Toast.LENGTH_LONG).show();
      autoOn.stop();
    //fingerDetectedHandler.sendMessage(new Message());
    }
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                        }
                    }
                    //sLog.e(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
                }
            }
        }
    };

    public byte[] captureFingerPrint() {
      long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);
      UsbDevice usbDevice = sgfplib.GetUsbDevice();
      boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
      if (!hasPermission) {
        if (!usbPermissionRequested) {
          //Log.d(TAG, "Call GetUsbManager().requestPermission()");
          usbPermissionRequested = true;
          sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
        } else {
          //wait up to 20 seconds for the system to grant USB permission
          hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
          int i = 0;
          while ((hasPermission == false) && (i <= 40)) {
            ++i;
            hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            //Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
          }
        }
      }
        //this.mCheckBoxMatched.setChecked(false);
        int imageHeight, imageWidth, imageDPI;
        //byte[] buffer = new byte[imageWidth * imageHeight];
        int[] maxTemplateSize = new int[1];
        byte[] registerTemplate, registerImage;
        //long result = sgfplib.GetImage(buffer);
        // long result = sgfplib.GetImageEx(buffer, 10000, 50);
        //  mImageViewFingerprint.setImageBitmap(this.toGrayscale(buffer));
        // buffer = null;

        //ON resume has persmission code
        error = sgfplib.OpenDevice(0);
        if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
          bSecuGenDeviceOpened = true;
          SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
          error = sgfplib.GetDeviceInfo(deviceInfo);
          imageWidth = deviceInfo.imageWidth;
          imageHeight = deviceInfo.imageHeight;
          imageDPI = deviceInfo.imageDPI;
          sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
          sgfplib.GetMaxTemplateSize(maxTemplateSize);

          //Need to be setup up locally
          registerTemplate = new byte[maxTemplateSize[0]];
          //mVerifyTemplate = new byte[maxTemplateSize[0]];
          registerImage = new byte[imageWidth * imageHeight];
          long result = sgfplib.GetImage(registerImage);
          //sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte)0);
          result = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
          SGFingerInfo fpInfo = new SGFingerInfo();
          for (int i = 0; i < registerTemplate.length; ++i)
            registerTemplate[i] = 0;

          result = sgfplib.CreateTemplate(fpInfo, registerImage, registerTemplate);
          return registerTemplate;
        } else {
          return null;
        }
      }



}
