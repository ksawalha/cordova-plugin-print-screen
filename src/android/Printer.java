package de.appplant.cordova.plugin.printer;

import android.app.Activity;
import android.webkit.WebView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Plugin to print HTML documents. Creates an invisible web view
 * that loads the markup data. Once the page has been fully rendered it takes
 * the print adapter of that web view and initializes a print job.
 */
public final class Printer extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callback) {
        boolean valid = true;

        if (action.equalsIgnoreCase("check")) {
            check(args.optString(0), callback);
        } else if (action.equalsIgnoreCase("types")) {
            types(callback);
        } else if (action.equalsIgnoreCase("print")) {
            print(args.optString(0), args.optJSONObject(1), callback);
        } else {
            valid = false;
        }

        return valid;
    }

    private void check(@Nullable String item, CallbackContext callback) {
        cordova.getThreadPool().execute(() -> {
            PrintManager pm = new PrintManager(cordova.getContext());
            boolean printable = pm.canPrintItem(item);
            sendPluginResult(callback, printable);
        });
    }

    private void types(CallbackContext callback) {
        cordova.getThreadPool().execute(() -> {
            JSONArray utis = PrintManager.getPrintableTypes();
            PluginResult res = new PluginResult(Status.OK, utis);
            callback.sendPluginResult(res);
        });
    }

    private void print(@Nullable String content, JSONObject settings, CallbackContext callback) {
        cordova.getThreadPool().execute(() -> {
            PrintManager pm = new PrintManager(cordova.getContext());
            WebView view = (WebView) webView.getView();
            pm.print(content, settings, view, (boolean completed) -> sendPluginResult(callback, completed));
        });
    }

    private void sendPluginResult(@NonNull CallbackContext callback, boolean value) {
        PluginResult result = new PluginResult(Status.OK, value);
        callback.sendPluginResult(result);
    }
}
