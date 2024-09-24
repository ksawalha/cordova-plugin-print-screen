package cordova.plugin.printscreen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class PrintScreenPlugin extends CordovaPlugin {

    private static final String TAG = "PrintScreenPlugin";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("printScreen".equals(action)) {
            captureAndPrint(callbackContext);
            return true;
        }
        return false;
    }

    private void captureAndPrint(CallbackContext callbackContext) {
        final Activity activity = this.cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Capture the screen
                    View view = activity.getWindow().getDecorView().getRootView();
                    Bitmap bitmap = getBitmapFromView(view);

                    if (bitmap != null) {
                        Log.d(TAG, "Screen capture successful");
                        byte[] printData = bitmapToEscPos(bitmap);

                        // Print the data via Bluetooth
                        if (printData != null) {
                            connectAndPrint(printData, callbackContext);
                        } else {
                            callbackContext.error("Failed to convert screen to ESC/POS format.");
                        }
                    } else {
                        callbackContext.error("Failed to capture screen.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error capturing screen", e);
                    callbackContext.error("Error capturing screen: " + e.getMessage());
                }
            }
        });
    }

    // Capture the current view as a Bitmap
    private Bitmap getBitmapFromView(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    // Convert Bitmap to ESC/POS image data
    private byte[] bitmapToEscPos(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int widthBytes = (width + 7) / 8;  // Ensure width is a multiple of 8

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // ESC/POS command to set line spacing
        baos.write(0x1B);  // ESC
        baos.write(0x33);  // Set line spacing
        baos.write(24);    // 24-dot line height

        for (int y = 0; y < height; y += 24) {
            baos.write(0x1B);  // ESC
            baos.write(0x2A);  // Image print mode
            baos.write(0x21);  // 24-dot double-density
            baos.write(widthBytes);  // Width in bytes
            baos.write(0x00);  // LSB

            for (int x = 0; x < width; x++) {
                for (int k = 0; k < 24; k++) {
                    int bit = 0;
                    if (y + k < height) {
                        int pixel = bitmap.getPixel(x, y + k);
                        int gray = (pixel & 0xFF);  // Get grayscale value
                        if (gray < 128) {  // Thresholding
                            bit |= (1 << (7 - k % 8));
                        }
                    }
                    baos.write(bit);
                }
            }
            baos.write(0x0A);  // New line
        }

        return baos.toByteArray();
    }

    // Connect to Bluetooth printer and print ESC/POS image data
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

        // Discover paired devices
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
            Log.d(TAG, "Bluetooth connection established");

            outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(printData);
            outputStream.flush();
            Log.d(TAG, "Data sent to printer");

            // Close the connection
            outputStream.close();
            bluetoothSocket.close();
            callbackContext.success("Printed successfully!");

        } catch (Exception e) {
            Log.e(TAG, "Error during Bluetooth connection/printing", e);
            callbackContext.error("Failed to print. Error: " + e.getMessage());
        }
    }
}
