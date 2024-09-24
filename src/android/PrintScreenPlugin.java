package cordova.plugin.printscreen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Base64;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class PrintScreenPlugin extends CordovaPlugin {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final String TAG = "PrintScreenPlugin";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("printScreen".equals(action)) {
            captureAndPrint(callbackContext);
            return true;
        }
        return false;
    }

    // Method to capture the screen and print it
    private void captureAndPrint(CallbackContext callbackContext) {
        final Activity activity = this.cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Capture the screen
                View view = activity.getWindow().getDecorView().getRootView();
                view.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
                view.setDrawingCacheEnabled(false);

                // Convert the bitmap to ESC/POS format
                byte[] printData = bitmapToEscPos(bitmap);

                // Print the data via Bluetooth
                if (printData != null) {
                    connectAndPrint(printData, callbackContext);
                } else {
                    callbackContext.error("Failed to capture screen.");
                }
            }
        });
    }

    // Convert Bitmap to ESC/POS image data
    private byte[] bitmapToEscPos(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Convert bitmap to monochrome (black-and-white)
        for (int y = 0; y < height; y += 24) {  // 24 dots per vertical slice
            baos.write(0x1B);  // ESC
            baos.write(0x2A);  // Start image print command
            baos.write(0x21);  // Image mode (32 dots)
            baos.write(width / 8);  // Image width in bytes
            baos.write(0x00);  // Image height

            for (int x = 0; x < width; x++) {
                for (int k = 0; k < 24; k++) {
                    int bit = 0;
                    if (y + k < height) {
                        int pixel = bitmap.getPixel(x, y + k);
                        int gray = (pixel & 0xFF0000) >> 16;  // Convert to grayscale
                        if (gray < 128) {  // Thresholding
                            bit = 1;
                        }
                    }
                    baos.write(bit);
                }
            }
            baos.write(0x0A);  // New line
        }

        return baos.toByteArray();
    }

    // Connect to the Bluetooth printer and send the ESC/POS image data
    private void connectAndPrint(byte[] printData, CallbackContext callbackContext) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            callbackContext.error("Bluetooth not supported on this device.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            callbackContext.error("Bluetooth is disabled. Please enable Bluetooth and try again.");
            return;
        }

        // Find the printer
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("XP-P800")) {  // Match printer by name
                    bluetoothDevice = device;
                    break;
                }
            }
        }

        if (bluetoothDevice == null) {
            callbackContext.error("No compatible Bluetooth printer found.");
            return;
        }

        try {
            // Connect to the printer
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(PRINTER_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();

            // Send the image data to the printer
            outputStream.write(printData);
            outputStream.flush();

            // Disconnect after printing
            outputStream.close();
            bluetoothSocket.close();

            callbackContext.success("Printed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "Error during Bluetooth connection/printing", e);
            callbackContext.error("Failed to print.");
        }
    }
}
