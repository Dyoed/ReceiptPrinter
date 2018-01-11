package com.starmicronics.starprntsdk.extensions;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.view.View;
import android.webkit.WebView;

/**
 * Created by joedsantiago on 11/01/2018.
 */

@SuppressWarnings("ALL")
//TODO use RxJava
public class HtmlReceiptConverter implements WebView.PictureListener {

    private WebView webView;
    private Bitmap receipt = null;
    private OnReceiptBitmapCaptured onReceiptBitmapCaptured;

    public HtmlReceiptConverter(WebView webView, OnReceiptBitmapCaptured onReceiptBitmapCaptured) {
        this.onReceiptBitmapCaptured = onReceiptBitmapCaptured;
        this.webView = webView;
        initWebView();
    }

    private void initWebView() {
        webView.setPictureListener(this);
    }

    public void getReceipt(String url) {
        if (receipt != null) {
            onReceiptBitmapCaptured.onReceiptBitmapCaptured(receipt);
            return;
        }
        webView.loadUrl(url);
    }

    public void getReceiptFromStringHtml(String html) {
        if (receipt != null) {
            onReceiptBitmapCaptured.onReceiptBitmapCaptured(receipt);
            return;
        }
        webView.loadData(html, "text/html", "UTF-8");
    }


    private Bitmap getReceiptAsBitmap(WebView v) {
        v.measure(View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

        //TODO move to BG thread
        Bitmap image = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        v.draw(canvas);
        return image;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        receipt.recycle();
    }

    @Override
    public void onNewPicture(WebView view, Picture picture) {
        if (receipt == null) {
            webView.setPictureListener(null);
            receipt = getReceiptAsBitmap(view);
            onReceiptBitmapCaptured.onReceiptBitmapCaptured(receipt);
            webView.setPictureListener(this);
        }
    }

    public interface OnReceiptBitmapCaptured {
        void onReceiptBitmapCaptured(Bitmap bitmap);
    }
}
