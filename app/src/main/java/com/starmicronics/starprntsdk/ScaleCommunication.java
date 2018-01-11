package com.starmicronics.starprntsdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.IPeripheralCommandParser;
import com.starmicronics.starioextension.IPeripheralCommandParser.ParseResult;

import static com.starmicronics.starioextension.IPeripheralCommandParser.ParseResult.Failure;
import static com.starmicronics.starioextension.IPeripheralCommandParser.ParseResult.Success;

@SuppressWarnings("WeakerAccess")
public class ScaleCommunication extends Communication {

    public static void parseDoNotCheckCondition(Object lock, IPeripheralCommandParser parser, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        ParseWeightDoNotCheckConditionThread thread = new ParseWeightDoNotCheckConditionThread(lock, parser, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void parseDoNotCheckCondition(Object lock, IPeripheralCommandParser parser, StarIOPort port, SendCallback callback) {
        ParseWeightDoNotCheckConditionThread thread = new ParseWeightDoNotCheckConditionThread(lock, parser, port, callback);
        thread.start();
    }
}

class ParseWeightDoNotCheckConditionThread extends Thread {
    private final Object               mLock;
    private IPeripheralCommandParser   mParser;
    private StarIOPort                 mPort = null;
    private Communication.SendCallback mCallback;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    ParseWeightDoNotCheckConditionThread(Object lock, IPeripheralCommandParser function, StarIOPort port, Communication.SendCallback callback) {
        mLock     = lock;
        mParser   = function;
        mPort     = port;
        mCallback = callback;
    }

    ParseWeightDoNotCheckConditionThread(Object lock, IPeripheralCommandParser function, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mLock         = lock;
        mParser       = function;
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
    }

    @Override
    public void run() {
        Communication.Result communicateResult = Communication.Result.ErrorOpenPort;
        boolean result = false;

        synchronized (mLock) {
            try {
                if (mPort == null) {

                    if (mPortName == null) {
                        resultSendCallback(false, communicateResult, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              port.writePort(dummy, 0, dummy.length);

                communicateResult = Communication.Result.ErrorWritePort;

                StarPrinterStatus status = mPort.retreiveStatus();

                if (status.rawLength == 0) {
                    throw new StarIOPortException("Unable to communicate with printer.");
                }

                long start = System.currentTimeMillis();

                if (mPort.getPortName().toLowerCase().startsWith("usb")) {                  // USB Interface: Temporarily Disable ASB/NSB.

                    byte[] buffer = new byte[1024];
                    byte[] disableAsbNsbCmd = {0x1b, 0x1e, 'a', 0};
                    mPort.writePort(disableAsbNsbCmd, 0, disableAsbNsbCmd.length);

                    long lastReceiveTime = System.currentTimeMillis();
                    while (true) {

                        if (mPort.readPort(buffer, 0, buffer.length) != 0) {
                            try {
                                Thread.sleep(10);                                           // Wait 10ms
                            } catch (InterruptedException e) {
                                // do nothing
                            }
                            lastReceiveTime = System.currentTimeMillis();
                        }

                        if (100 < (System.currentTimeMillis() - lastReceiveTime)) {
                            break;
                        }

                        if (5000 < (System.currentTimeMillis() - start)) {
                            communicateResult = Communication.Result.ErrorReadPort;
                            throw new StarIOPortException("Error");
                        }
                    }
                }

                byte[] requestWeightToScaleCommand     = mParser.createSendCommands();
                byte[] receiveWeightFromPrinterCommand = mParser.createReceiveCommands();

                start = System.currentTimeMillis();

                boolean isScaleDataReceived = false;

                communicateResult = Communication.Result.ErrorWritePort;

                while(!isScaleDataReceived) {
                    mPort.writePort(requestWeightToScaleCommand, 0, requestWeightToScaleCommand.length);    // Write the command requesting weight to the scale via the printer.

                    long startRequestWeight = System.currentTimeMillis();

                    while (!isScaleDataReceived) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }

                        byte[] receiveBuffer = new byte[1024];

                        mPort.writePort(receiveWeightFromPrinterCommand, 0, receiveWeightFromPrinterCommand.length);    // Write the command to the printer to receive the weight data in the buffer of the printer.

                        long startReceiveWeight = System.currentTimeMillis();

                        int amount = 0;

                        ParseResult parseResult;

                        while (true) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                // Nothing
                            }

                            int receiveSize = mPort.readPort(receiveBuffer, amount, receiveBuffer.length - amount);

                            if (0 < receiveSize) {
                                amount += receiveSize;
                            }

                            parseResult = mParser.parse(receiveBuffer, amount);

                            if (parseResult == Success) {
                                result            = true;
                                communicateResult = Communication.Result.Success;

                                isScaleDataReceived = true;

                                break;
                            }
                            else if (parseResult == Failure) {
                                break;
                            }

                            if (250 < (System.currentTimeMillis() - startReceiveWeight)) {
                                break;
                            }
                        }

                        if (250 < (System.currentTimeMillis() - startRequestWeight)) {
                            // Resend Scale Command
                            // Because the scale doesn't sometimes react.
                            break;
                        }
                    }

                    if (1000 < (System.currentTimeMillis() - start)) {
                        communicateResult = Communication.Result.ErrorReadPort;
                        break;
                    }
                }

            } catch (StarIOPortException e) {
                // Nothing
            }

            if (mPort != null && mPortName != null) {
                try {
                    StarIOPort.releasePort(mPort);
                } catch (StarIOPortException e) {
                    // Nothing
                }
                mPort = null;
            }

            resultSendCallback(result, communicateResult, mCallback);
        }
}

    private static void resultSendCallback(final boolean result, final Communication.Result communicationResult, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(result, communicationResult);
                }
            });
        }
    }
}