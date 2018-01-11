package com.starmicronics.starprntsdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.IPeripheralCommandParser;
import com.starmicronics.starioextension.IPeripheralCommandParser.ParseResult;

import java.util.Map;

@SuppressWarnings({"UnusedParameters", "UnusedAssignment", "WeakerAccess"})
public class Communication {
    @SuppressWarnings("unused")
    public enum Result {
        Success,
        ErrorUnknown,
        ErrorOpenPort,
        ErrorBeginCheckedBlock,
        ErrorEndCheckedBlock,
        ErrorWritePort,
        ErrorReadPort,
    }

    interface StatusCallback {
        void onStatus(StarPrinterStatus result);
    }

    interface FirmwareInformationCallback {
        void onFirmwareInformation(Map<String, String> firmwareInformationMap);
    }

    interface SerialNumberCallback {
        void onSerialNumber(Communication.Result communicateResult, String serialNumber);
    }

    interface SendCallback {
        void onStatus(boolean result, Communication.Result communicateResult);
    }


    public static void sendCommands(Object lock, byte[] commands, StarIOPort port, SendCallback callback) {
        SendCommandThread thread = new SendCommandThread(lock, commands, port, callback);
        thread.start();
    }

    public static void sendCommandsDoNotCheckCondition(Object lock, byte[] commands, StarIOPort port, SendCallback callback) {
        SendCommandDoNotCheckConditionThread thread = new SendCommandDoNotCheckConditionThread(lock, commands, port, callback);
        thread.start();
    }

    public static void sendCommands(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        SendCommandThread thread = new SendCommandThread(lock, commands, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void sendCommandsDoNotCheckCondition(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        SendCommandDoNotCheckConditionThread thread = new SendCommandDoNotCheckConditionThread(lock, commands, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void retrieveStatus(Object lock, String portName, String portSettings, int timeout, Context context, StatusCallback callback) {
        RetrieveStatusThread thread = new RetrieveStatusThread(lock, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void getFirmwareInformation(Object lock, String portName, String portSettings, int timeout, Context context, FirmwareInformationCallback callback) {
        GetFirmwareInformationThread thread = new GetFirmwareInformationThread(lock, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void getSerialNumber(Object lock, String portName, String portSettings, int timeout, Context context, SerialNumberCallback callback) {
        GetSerialNumberThread thread = new GetSerialNumberThread(lock, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void parseDoNotCheckCondition(Object lock, IPeripheralCommandParser parser, String portName, String portSettings, int timeout, Context context, SendCallback callback) {
        ParseConnectionDoNotCheckConditionThread thread = new ParseConnectionDoNotCheckConditionThread(lock, parser, portName, portSettings, timeout, context, callback);
        thread.start();
    }

    public static void parseDoNotCheckCondition(Object lock, IPeripheralCommandParser parser, StarIOPort port, SendCallback callback) {
        ParseConnectionDoNotCheckConditionThread thread = new ParseConnectionDoNotCheckConditionThread(lock, parser, port, callback);
        thread.start();
    }
}

class SendCommandThread extends Thread {
    private final Object mLock;
    private Communication.SendCallback mCallback;
    private byte[] mCommands;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    SendCommandThread(Object lock, byte[] commands, StarIOPort port, Communication.SendCallback callback) {
        mCommands = commands;
        mPort     = port;
        mCallback = callback;
        mLock     = lock;
    }

    SendCommandThread(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mCommands     = commands;
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
        mLock         = lock;
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
                if (mPort == null) {
                    communicateResult = Communication.Result.ErrorOpenPort;
                    resultSendCallback(false, communicateResult, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                StarPrinterStatus status;

                communicateResult = Communication.Result.ErrorBeginCheckedBlock;

                status = mPort.beginCheckedBlock();

                if (status.offline) {
                    throw new StarIOPortException("A printer is offline.");
                }

                communicateResult = Communication.Result.ErrorWritePort;

                mPort.writePort(mCommands, 0, mCommands.length);

                communicateResult = Communication.Result.ErrorEndCheckedBlock;

                mPort.setEndCheckedBlockTimeoutMillis(30000);     // 30000mS!!!

                status = mPort.endCheckedBlock();

                if (status.coverOpen) {
                    throw new StarIOPortException("Printer cover is open");
                } else if (status.receiptPaperEmpty) {
                    throw new StarIOPortException("Receipt paper is empty");
                } else if (status.offline) {
                    throw new StarIOPortException("Printer is offline");
                }

                result = true;
                communicateResult = Communication.Result.Success;
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

    private static void resultSendCallback(final boolean result, final Communication.Result communicateResult, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(result, communicateResult);
                }
            });
        }
    }
}

class SendCommandDoNotCheckConditionThread extends Thread {
    private final Object mLock;
    private Communication.SendCallback mCallback;
    private byte[] mCommands;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    SendCommandDoNotCheckConditionThread(Object lock, byte[] commands, StarIOPort port, Communication.SendCallback callback) {
        mLock     = lock;
        mCommands = commands;
        mPort     = port;
        mCallback = callback;
    }

    SendCommandDoNotCheckConditionThread(Object lock, byte[] commands, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mLock         = lock;
        mCommands     = commands;
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
                if (mPort == null) {
                    communicateResult = Communication.Result.ErrorOpenPort;
                    resultSendCallback(false, communicateResult, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                StarPrinterStatus status;

                communicateResult = Communication.Result.ErrorWritePort;

                status = mPort.retreiveStatus();

                if (status.rawLength == 0) {
                    throw new StarIOPortException("Unable to communicate with printer.");
                }

                communicateResult = Communication.Result.ErrorWritePort;

                mPort.writePort(mCommands, 0, mCommands.length);

//                if (status.coverOpen) {
//                    throw new StarIOPortException("Printer cover is open");
//                } else if (status.receiptPaperEmpty) {
//                    throw new StarIOPortException("Receipt paper is empty");
//                } else if (status.offline) {
//                    throw new StarIOPortException("Printer is offline");
//                }

                communicateResult = Communication.Result.Success;
                result = true;
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

    private static void resultSendCallback(final boolean result, final Communication.Result communicateResult, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(result, communicateResult);
                }
            });
        }
    }
}

class ParseConnectionDoNotCheckConditionThread extends Thread {
    private final Object               mLock;
    private IPeripheralCommandParser   mParser;
    private StarIOPort                 mPort;
    private Communication.SendCallback mCallback;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    ParseConnectionDoNotCheckConditionThread(Object lock, IPeripheralCommandParser parser, StarIOPort port, Communication.SendCallback callback) {
        mLock     = lock;
        mParser   = parser;
        mPort     = port;
        mCallback = callback;
    }

    ParseConnectionDoNotCheckConditionThread(Object lock, IPeripheralCommandParser parser, String portName, String portSettings, int timeout, Context context, Communication.SendCallback callback) {
        mLock         = lock;
        mParser       = parser;
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
//              mPort.writePort(dummy, 0, dummy.length);

                communicateResult = Communication.Result.ErrorWritePort;

                StarPrinterStatus status = mPort.retreiveStatus();

                if (status.rawLength == 0) {
                    throw new StarIOPortException("Unable to communicate with printer.");
                }

                communicateResult = Communication.Result.ErrorWritePort;
                byte[] sendData = mParser.createSendCommands();
                mPort.writePort(sendData, 0, sendData.length);

                byte[] data = new byte[1024];
                int amount = 0;

                long start = System.currentTimeMillis();

                while (true) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }

                    int receiveSize = mPort.readPort(data, amount, data.length - amount);

                    if (0 < receiveSize) {
                        amount += receiveSize;
                    }

                    byte[] receiveData = new byte[amount];
                    System.arraycopy(data, 0, receiveData, 0, amount);

                    if (mParser.parse(receiveData, amount) == ParseResult.Success) {
                        result = true;
                        communicateResult = Communication.Result.Success;
                        break;
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

    private static void resultSendCallback(final boolean result, final Communication.Result communicateResult, final Communication.SendCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(result, communicateResult);
                }
            });
        }
    }
}

class RetrieveStatusThread extends Thread {
    private final Object mLock;
    private Communication.StatusCallback mCallback;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    @SuppressWarnings("unused")
    RetrieveStatusThread(Object lock, StarIOPort port, Communication.StatusCallback callback) {
        mPort     = port;
        mCallback = callback;
        mLock     = lock;
    }

    RetrieveStatusThread(Object lock, String portName, String portSettings, int timeout, Context context, Communication.StatusCallback callback) {
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
        mLock         = lock;
    }

    @Override
    public void run() {

        synchronized (mLock) {
            StarPrinterStatus status = null;

            try {
                if (mPort == null) {

                    if (mPortName == null) {
                        resultSendCallback(null, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    resultSendCallback(null, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                status = mPort.retreiveStatus();

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

            resultSendCallback(status, mCallback);
        }
    }

    private static void resultSendCallback(final StarPrinterStatus status, final Communication.StatusCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatus(status);
                }
            });
        }
    }
}

class GetFirmwareInformationThread extends Thread {
    private final Object mLock;
    private Communication.FirmwareInformationCallback mCallback;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    GetFirmwareInformationThread(Object lock, String portName, String portSettings, int timeout, Context context, Communication.FirmwareInformationCallback callback) {
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
        mLock         = lock;
    }

    @Override
    public void run() {

        synchronized (mLock) {
            Map<String, String> firmwareInformationMap = null;

            try {
                if (mPort == null) {
                    if (mPortName == null) {
                        resultSendCallback(null, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    resultSendCallback(null, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                firmwareInformationMap = mPort.getFirmwareInformation();

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

            resultSendCallback(firmwareInformationMap, mCallback);
        }
    }

    private static void resultSendCallback(final Map<String, String> firmwareInformationMap, final Communication.FirmwareInformationCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onFirmwareInformation(firmwareInformationMap);
                }
            });
        }
    }
}

class GetSerialNumberThread extends Thread {
    private final Object mLock;
    private Communication.SerialNumberCallback mCallback;

    private StarIOPort mPort;

    private String  mPortName = null;
    private String  mPortSettings;
    private int     mTimeout;
    private Context mContext;

    GetSerialNumberThread(Object lock, String portName, String portSettings, int timeout, Context context, Communication.SerialNumberCallback callback) {
        mPortName     = portName;
        mPortSettings = portSettings;
        mTimeout      = timeout;
        mContext      = context;
        mCallback     = callback;
        mLock         = lock;
    }

    @Override
    public void run() {
        Communication.Result communicateResult = Communication.Result.ErrorOpenPort;

        String serialNumber = null;

        synchronized (mLock) {
            try {
                if (mPort == null) {
                    if (mPortName == null) {
                        resultSendCallback(communicateResult, null, mCallback);
                        return;
                    } else {
                        mPort = StarIOPort.getPort(mPortName, mPortSettings, mTimeout, mContext);
                    }
                }
                if (mPort == null) {
                    resultSendCallback(communicateResult, null, mCallback);
                    return;
                }

//              // When using USB interface with mPOP(F/W Ver 1.0.1), you need to send the following data.
//              byte[] dummy = {0x00};
//              mPort.writePort(dummy, 0, dummy.length);

                communicateResult = Communication.Result.ErrorWritePort;

                StarPrinterStatus status = mPort.retreiveStatus();

                if (status.rawLength == 0) {
                    throw new StarIOPortException("Unable to communicate with printer.");
                }

                byte[] getInformationCommand = new byte[]{ 0x1b, 0x1d, 0x29, 0x49, 0x01, 0x00, 49 };  // ESC GS ) I pL pH fn (Transmit printer information command)

                mPort.writePort(getInformationCommand, 0, getInformationCommand.length);

                byte[] data = new byte[1024];

                int amount = 0;

                long start = System.currentTimeMillis();

                String information = "";

                while (true) {
                    if (3000 < (System.currentTimeMillis() - start)) {
                        communicateResult = Communication.Result.ErrorReadPort;

                        throw new StarIOPortException("Timeout");
                    }

                    int receiveSize = mPort.readPort(data, amount, data.length - amount);

                    if (0 < receiveSize) {
                        amount += receiveSize;
                    }
                    else {
                        continue;
                    }

                    byte[] receiveData = new byte[amount];
                    System.arraycopy(data, 0, receiveData, 0, amount);

                    boolean receiveResponse = false;

                    if (2 <= amount) {
                        for (int i = 0; i < amount; i++) {
                            if (receiveData[i]     == 0x0a &&       // Check the footer of the command.
                                receiveData[i + 1] == 0x00) {
                                for (int j = 0; j < amount - 9; j++) {
                                    if (receiveData[j]     == 0x1b &&     // Check the header of the command.
                                        receiveData[j + 1] == 0x1d &&
                                        receiveData[j + 2] == 0x29 &&
                                        receiveData[j + 3] == 0x49 &&
                                        receiveData[j + 4] == 0x01 &&
                                        receiveData[j + 5] == 0x00 &&
                                        receiveData[j + 6] == 49) {
                                        for (int k = j + 7; k < amount - 2; k++) {
                                            String text = String.format("%c", receiveData[k]);

                                            information += text;
                                        }

                                        receiveResponse = true;
                                        break;
                                    }
                                }

                                break;
                            }
                        }
                    }

                    if (receiveResponse) {
                        break;
                    }
                }

                int startIndex = information.indexOf("PrSrN=");

                if (startIndex != -1) {
                    String temp = information.substring(startIndex);

                    int endIndex = temp.indexOf(",");

                    if (endIndex != -1) {
                        serialNumber = temp.substring("PrSrN=".length(), endIndex);
                        communicateResult = Communication.Result.Success;
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

            resultSendCallback(communicateResult, serialNumber, mCallback);
        }
    }

    private static void resultSendCallback(final Communication.Result communicateResult, final String serialNumber, final Communication.SerialNumberCallback callback) {
        if (callback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSerialNumber(communicateResult, serialNumber);
                }
            });
        }
    }
}
