var exec = require('cordova/exec');
var PrintScreen = {
    printScreen: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "PrintScreenPlugin", "printScreen", []);
    }
};
module.exports = PrintScreen;
