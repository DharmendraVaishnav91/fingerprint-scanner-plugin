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
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import android.support.v4.content.LocalBroadcastManager;


public class FingerPrintScannerPlugin extends CordovaPlugin implements SGFingerPresentEvent {
    private int mImageWidth;
    private int mImageHeight;
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
    public Long startTime;
    public Long endTime;
    public Long deviceOpenError;

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
        autoOn = new SGAutoOnEventNotifier(sgfplib, this);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("toggleAutoOn")) {
            String status = args.getString(0);
            Long output;
            if(status=="true"){
//                sgfplib.SetLedOn(true);
                output = autoOn.start();
              // deviceOpenError = sgfplib.OpenDevice(0);
            }
            else{
//                sgfplib.SetLedOn(false);
                output = autoOn.stop();
                ///deviceOpenError = sgfplib.closeDevice(0);

            }
//            Long output = autoOn.start();
//            System.out.println(output);
            final PluginResult result = new PluginResult(PluginResult.Status.OK, output);
            callbackContext.sendPluginResult(result);
        }
        if (action.equals("checkAndOptPermission")) {
//            boolean checkPermission=false;
            JSONObject checkPermission = new JSONObject();
            try{
                checkPermission.put("result",checkAndOptPermission());
                checkPermission.put("status",0);
                checkPermission.put("error","");
               if(checkPermission.getBoolean("result"))
                 deviceOpenError = sgfplib.OpenDevice(0);

//                    autoOn.start();
                final PluginResult result = new PluginResult(PluginResult.Status.OK, checkPermission);
                callbackContext.sendPluginResult(result);
            }
            catch(Exception e){
                checkPermission.put("result","");
                checkPermission.put("status",1);
                checkPermission.put("error",e);
                final PluginResult result = new PluginResult(PluginResult.Status.ERROR, checkPermission);
                callbackContext.sendPluginResult(result);
            }
        }
        if (action.equals("scanBase64")) {
            String capturedByteData=captureFingerPrint();
            final PluginResult result = new PluginResult(PluginResult.Status.OK, capturedByteData);
            callbackContext.sendPluginResult(result);
        }
      return true;
    }

    public Handler fingerDetectedHandler = new Handler(){
        // @Override
        public void handleMessage(Message msg) {
            //Handle the message
          System.out.println("Time difference of callback invoke=  "+ (System.currentTimeMillis()-startTime) );
            Context context = cordova.getActivity()
                    .getApplicationContext();
            String capturedByteData=captureFingerPrint();
            endTime=System.currentTimeMillis();
            System.out.println("System capture end time: "+System.currentTimeMillis());
          System.out.println("Time difference=  "+ (endTime-startTime));
//            System.out.println("capturedByteData");
//            System.out.println(capturedByteData);
           /// if(capturedByteData.length()>=10000){
                final Intent intent = new Intent("getFingerPrintImage");
                Bundle b = new Bundle();
                b.putString( "data", capturedByteData );
                intent.putExtras( b);
                LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
           //     autoOn.start();
//            }else{
//                System.out.println("Blank image found");
//            }

        }
    };

    public void SGFingerPresentCallback (){
        startTime=System.currentTimeMillis();
        System.out.println("System capture start time: "+startTime);
        //Toast.makeText(JSGDActivity.this,"finger present callback is called",Toast.LENGTH_LONG).show();
        System.out.println("SGFingerPresentCallback is called");
         //autoOn.stop();
        fingerDetectedHandler.sendMessage(new Message());
       autoOn.stop();
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

    public <T> T captureFingerPrint() {
        int imageHeight, imageWidth;
        int[] maxTemplateSize = new int[1];
        byte[] registerTemplate, registerImage;

        //ON resume has persmission code

        if (deviceOpenError == SGFDxErrorCode.SGFDX_ERROR_NONE) {
            bSecuGenDeviceOpened = true;
            SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
            deviceOpenError = sgfplib.GetDeviceInfo(deviceInfo);
            mImageWidth = deviceInfo.imageWidth;
            mImageHeight = deviceInfo.imageHeight;
            //imageDPI = deviceInfo.imageDPI;
            //sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
           // sgfplib.GetMaxTemplateSize(maxTemplateSize);

            //Need to be setup up locally
           // registerTemplate = new byte[maxTemplateSize[0]];
            //mVerifyTemplate = new byte[maxTemplateSize[0]];
            registerImage = new byte[mImageWidth * mImageHeight];
            long result = sgfplib.GetImageEx(registerImage,5000,50);
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            this.toGrayscale(registerImage).compress(Bitmap.CompressFormat.JPEG, 100, bao);
            byte [] ba = bao.toByteArray();
            Long captureEndTime=System.currentTimeMillis();
            String imageString=Base64.encodeToString(ba, Base64.DEFAULT);
            return (T) imageString;
        }
        return null;
    }

    public boolean checkAndOptPermission(){
        long error = sgfplib.Init( SGFDxDeviceName.SG_DEV_AUTO);
        UsbDevice usbDevice = sgfplib.GetUsbDevice();
//        long deviceErrors = sgfplib.OpenDevice(0);
        if((usbDevice==null) || (error != SGFDxErrorCode.SGFDX_ERROR_NONE))
            return false;
//        if (usbDevice==null)

        boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
        SGDeviceInfoParam device_info;
      //  System.out.println(sgfplib.OpenDevice(0));


//        System.out.println(sgfplib.GetDeviceInfo(device_info));
        if (!hasPermission) {
            sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
            hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
            int i = 0;
            while ((hasPermission == false) && (i <= 40)) {
                ++i;
                hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                if(hasPermission)
                    return true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
                //Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
            }
        }
        return hasPermission;
    }
    public Bitmap toGrayscale(byte[] mImageBuffer) {

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



}
