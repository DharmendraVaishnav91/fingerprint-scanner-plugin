var exec =require('cordova/exec');

var PLUGIN_NAME="FingerPrintScannerPlugin";

var FingerPrintScannerPlugin=function(){

}
/**
**@successCb is the success callback get called when plugin action is successful
**@failureCb is the failure callback get called when plugin action is failed
**/
FingerPrintScannerPlugin.scanBase64=function(successCb,failureCb){
    exec(successCb,failureCb,PLUGIN_NAME,'scanBase64',[]);
}
FingerPrintScannerPlugin.scanImage=function(successCb,failureCb){
    exec(successCb,failureCb,PLUGIN_NAME,'scanImage',[]);
}
FingerPrintScannerPlugin.checkAndOptPermission=function(successCb,failureCb){
    exec(successCb,failureCb,PLUGIN_NAME,'',[]);
}

module.exports=FingerPrintScannerPlugin
