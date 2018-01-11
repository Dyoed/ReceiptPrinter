package com.starmicronics.starprntsdk;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starprntsdk.extensions.HtmlReceiptConverter;
import com.starmicronics.starprntsdk.functions.PrinterFunctions;

/**
 * Created by joedsantiago on 11/01/2018.
 */

public class ReceiptActivity extends AppCompatActivity {

    private HtmlReceiptConverter htmlReceiptConverter;


    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        setContentView(R.layout.activity_receipt);

        final PrinterSetting setting = new PrinterSetting(ReceiptActivity.this);
        final StarIoExt.Emulation emulation = setting.getEmulation();
        final int paperSize = PrinterSetting.PAPER_SIZE_THREE_INCH;

        final WebView webView = (WebView) findViewById(R.id.webView);
        htmlReceiptConverter = new HtmlReceiptConverter(webView, new HtmlReceiptConverter.OnReceiptBitmapCaptured() {
            @Override
            public void onReceiptBitmapCaptured(Bitmap bitmap) {
                byte[] commands = PrinterFunctions.createRasterData(
                        emulation, bitmap, paperSize, true);
                print(setting, commands);
            }
        });

        htmlReceiptConverter.getReceipt("file:///android_asset/snackchat_receipt_long.html");
    }

    private void print(PrinterSetting setting, byte[] commands) {
        Communication.sendCommands(this, commands, setting.getPortName(), setting.getPortSettings(), 10000,
                this, new Communication.SendCallback() {
                    @Override
                    public void onStatus(boolean result, Communication.Result communicateResult) {

                    }
                });
    }


}
