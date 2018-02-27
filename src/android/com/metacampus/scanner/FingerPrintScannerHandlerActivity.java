/*
 * Copyright (C) 2017 Metacube Software Pvt. Ltd.
 *
 */

package com.metacampus.scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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

import com.metacampus.scanner.R;

public class FingerPrintScannerHandlerActivity extends Activity implements SGFingerPresentEvent {

    private static final String TAG = "SecuGen USB";
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

    //This broadcast receiver is necessary to get user permissions to access the attached USB device
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if (ACTION_USB_PERMISSION.equals(action)) {
    			synchronized (this) {
    				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
    					if(device != null){
    					}
    				}
    					//sLog.e(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
    			}
    		}
    	}
    };

    //This message handler is used to access local resources not
    //accessible by SGFingerPresentCallback() because it is called by
    //a separate thread.
    public Handler fingerDetectedHandler = new Handler(){
		// @Override
	    public void handleMessage(Message msg) {
	       CaptureFingerPrint();
	    }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

        //USB Permissions
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
       	filter = new IntentFilter(ACTION_USB_PERMISSION);
		//Uncomm
		registerReceiver(mUsbReceiver, filter);
        sgfplib = new JSGFPLib((UsbManager)getSystemService(Context.USB_SERVICE));
       // this.mToggleButtonSmartCapture.toggle();
        bSecuGenDeviceOpened = false;
        usbPermissionRequested = false;
		mAutoOnEnabled = false;
		autoOn = new SGAutoOnEventNotifier (sgfplib, this);
    }


    @Override
    public void onPause() {
    	if (bSecuGenDeviceOpened)
    	{   autoOn.stop();
    		sgfplib.CloseDevice();
            bSecuGenDeviceOpened = false;
    	}
    	unregisterReceiver(mUsbReceiver);
        super.onPause();

    }

    @Override
    public void onResume(){
    	//Log.d(TAG, "onResume()");

        super.onResume();

       	registerReceiver(mUsbReceiver, filter);
        long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE){
        	AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        	if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
        		dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
        	else
        		dlgAlert.setMessage("Fingerprint device initialization failed!");
        	dlgAlert.setTitle("SecuGen Fingerprint SDK");
        	dlgAlert.setPositiveButton("OK",
        			new DialogInterface.OnClickListener() {
        		      public void onClick(DialogInterface dialog,int whichButton){
        		        	//finish();
        		        	return;
        		      }
        			}
        	);
        	dlgAlert.setCancelable(false);
        	dlgAlert.create().show();
        }
        else {
	        UsbDevice usbDevice = sgfplib.GetUsbDevice();
	        if (usbDevice == null){
	        	AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
	        	dlgAlert.setMessage("SecuGen fingerprint sensor not found!");
	        	dlgAlert.setTitle("SecuGen Fingerprint SDK");
	        	dlgAlert.setPositiveButton("OK",
	        			new DialogInterface.OnClickListener() {
	        		      public void onClick(DialogInterface dialog,int whichButton){
	        		        //	finish();
	        		        	return;
	        		      }
	        			}
	        	);
	        	dlgAlert.setCancelable(false);
	        	dlgAlert.create().show();
	        }
	        else {
	        	boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
		        if (!hasPermission) {
			        if (!usbPermissionRequested) {
			        	usbPermissionRequested = true;
			        	sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
			        }
			        else {
			        	hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
			        	int i=0;
				        while ((hasPermission == false) && (i <= 40))
				        {
				        	++i;
				            hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
				        	try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
				        }
			        }
		        }
//		        if (hasPermission) {
//					//Toast.makeText(JSGDActivity.this,"has permisson start is called",Toast.LENGTH_LONG).show();
//			        error = sgfplib.OpenDevice(0);
//					if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
//				        bSecuGenDeviceOpened = true;
////						SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
////				        error = sgfplib.GetDeviceInfo(deviceInfo);
////				    	mImageWidth = deviceInfo.imageWidth;
////				    	mImageHeight= deviceInfo.imageHeight;
////				    	mImageDPI = deviceInfo.imageDPI;
////				        sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
////						sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
//////
////                        //Need to be setup up locally
////				        mRegisterTemplate = new byte[mMaxTemplateSize[0]];
////				        mVerifyTemplate = new byte[mMaxTemplateSize[0]];
//
//                        sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte)0);
//				        if (mAutoOnEnabled){
//				        	autoOn.start();
//				        }
//			        }
//			        else {
//						//debugMessage("Waiting for USB Permission\n");
//			        }
//		        }
	        }
        }
    }

    @Override
    public void onDestroy() {
		sgfplib.CloseDevice();
    	sgfplib.Close();
//    	unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer, int width, int height)
    {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
                        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                        Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(byte[] mImageBuffer)
    {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
                        Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                        Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        //Bitmap bm contains the fingerprint img
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    //Converts image to grayscale (NEW)
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y=0; y< height; ++y) {
            for (int x=0; x< width; ++x){
            	int color = bmpOriginal.getPixel(x, y);
            	int r = (color >> 16) & 0xFF;
            	int g = (color >> 8) & 0xFF;
            	int b = color & 0xFF;
            	int gray = (r+g+b)/3;
            	color = Color.rgb(gray, gray, gray);
            	//color = Color.rgb(r/3, g/3, b/3);
            	bmpGrayscale.setPixel(x, y, color);
            }
        }
        return bmpGrayscale;
    }

    /**
     * This method compare the existingTemplate and verifyTemplate for their matching
     * @param existingTemplate
     * @param verifyTemplate
     * @return boolean array consisting true if mathched successfully otherwise false
     */
	public boolean[] verifyFingerprintData(byte[] existingTemplate,byte[] verifyTemplate){
        boolean[] matched = new boolean[1];
        try {
            long result = sgfplib.MatchTemplate(existingTemplate, verifyTemplate, SGFDxSecurityLevel.SL_NORMAL, matched);
          return matched;
        }catch (Exception e){
            Toast.makeText(FingerPrintScannerHandlerActivity.this,e.toString()+" verify fingerprint data",Toast.LENGTH_LONG).show();
        }

	}

    public void SGFingerPresentCallback (){
		//Toast.makeText(JSGDActivity.this,"finger present callback is called",Toast.LENGTH_LONG).show();
		autoOn.stop();
		//fingerDetectedHandler.sendMessage(new Message());
    }

    /**
     * This method capture the fingerprint of user and store in temporary variable buffer which is byte []
      */
      public byte[] captureFingerPrint() {
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
          long error = sgfplib.OpenDevice(0);
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
