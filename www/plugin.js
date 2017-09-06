var exec =require('cordova/exec');

var PLUGIN_NAME="FingerPrintScannerPlugin";

var FingerPrintScannerPlugin=function(){

}
/**
**@successCb is the success callback get called when plugin action is successful
**@failureCb is the failure callback get called when plugin action is failed
**/
FingerPrintScannerPlugin.scan=function(successCb,failureCb){
    exec(successCb,failureCb,PLUGIN_NAME,'scan',[]);
}

module.exports=FingerPrintScannerPlugin
