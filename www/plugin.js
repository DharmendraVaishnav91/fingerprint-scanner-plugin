var exec =require('cordova/exec');

var PLUGIN_NAME="FingerPrintScannerPlugin";

var FingerPrintScannerPlugin={
  echo:function(phrase,cb){
    exec(cb,null,PLUGIN_NAME,'echo',[phrase]);
  }
  
}

module.exports=FingerPrintScannerPlugin
