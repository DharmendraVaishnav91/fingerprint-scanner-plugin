var exec =require('cordova/exec');

var PLUGIN_NAME="FingerPrintScannerPlugin";

var FingerPrintScannerPlugin=function(){

}

FingerPrintScannerPlugin.echo=function(phrase,cb){
    exec(cb,null,PLUGIN_NAME,'echo',[phrase]);
  }

module.exports=FingerPrintScannerPlugin
