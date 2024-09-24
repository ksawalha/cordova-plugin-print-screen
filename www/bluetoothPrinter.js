var exec = require('cordova/exec');

var BluetoothPrinter = {
    printHtml: function(htmlContent, deviceName, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "BluetoothPrinter", "printHtml", [htmlContent, deviceName]);
    }
};

module.exports = BluetoothPrinter;
