package de.appplant.cordova.plugin.printer;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import androidx.annotation.NonNull;
import androidx.core.print.PrintHelper;

/**
 * Simple delegate class to have access to the onFinish method.
 */
class PrintProxy extends PrintDocumentAdapter {
    private final @NonNull PrintDocumentAdapter delegate;
    private final @NonNull PrintHelper.OnPrintFinishCallback callback;

    PrintProxy(@NonNull PrintDocumentAdapter adapter,
                @NonNull PrintHelper.OnPrintFinishCallback callback) {
        this.delegate = adapter;
        this.callback = callback;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle bundle) {
        delegate.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, bundle);
    }

    @Override
    public void onWrite(PageRange[] range,
                        ParcelFileDescriptor dest,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {
        delegate.onWrite(range, dest, cancellationSignal, callback);
    }

    @Override
    public void onFinish() {
        super.onFinish();
        callback.onFinish();
    }
}
