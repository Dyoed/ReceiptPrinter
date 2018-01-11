package com.starmicronics.starprntsdk;


import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.starmicronics.cloudservices.AllReceipts;
import com.starmicronics.cloudservices.RequestCallback;
import com.starmicronics.cloudservices.RequestError;
import com.starmicronics.starioextension.IConnectionCallback;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExtManager;
import com.starmicronics.starioextension.StarIoExtManagerListener;
import com.starmicronics.starprntsdk.functions.AllReceiptsFunctions;
import com.starmicronics.starprntsdk.localizereceipts.ILocalizeReceipts;

import com.starmicronics.starprntsdk.Communication.Result;

public class AllReceiptsExtFragment extends Fragment implements IConnectionCallback, CommonAlertDialogFragment.Callback {

    private static final String OPEN_FAILURE_DIALOG = "OpenFailureDialog";

    private ProgressDialog mProgressDialog;

    private TextView mComment;

    private StarIoExtManager mStarIoExtManager;

    private int mSelectedIndex;
    private int mLanguage;
    private int mPaperSize;

    private boolean mIsFirst = true;
    private boolean mIsPrint;
    private boolean mIsForeground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        Intent intent = getActivity().getIntent();
        mLanguage = intent.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
        mPaperSize = intent.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);
        mSelectedIndex = intent.getIntExtra(CommonActivity.BUNDLE_KEY_SELECTED_INDEX, 0);

        PrinterSetting setting = new PrinterSetting(getActivity());

        mStarIoExtManager = new StarIoExtManager(StarIoExtManager.Type.Standard, setting.getPortName(), setting.getPortSettings(), 10000, getActivity());     // 10000mS!!!
        mStarIoExtManager.setCashDrawerOpenActiveHigh(setting.getCashDrawerOpenActiveHigh());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_all_receipts_ext, container, false);

        mComment = (TextView) rootView.findViewById(R.id.statusTextView);

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);

        mComment.startAnimation(animation);

        Button testButton = (Button) rootView.findViewById(R.id.testPrintButton);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                print();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mStarIoExtManager.setListener(mStarIoExtManagerListener);

        mProgressDialog.show();

        mStarIoExtManager.connect(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsForeground = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reloadIcon) {
            mProgressDialog.show();

            mStarIoExtManager.disconnect(new IConnectionCallback() {
                @Override
                public void onDisconnected() {
                    if (!mIsForeground) {
                        return;
                    }

                    mStarIoExtManager.connect(AllReceiptsExtFragment.this);
                }

                @Override
                public void onConnected(ConnectResult connectResult) {
                    // do nothing
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        mIsForeground = false;

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mStarIoExtManager.disconnect(this);
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        if (!mIsForeground) {
            return;
        }

        switch (tag) {
            case OPEN_FAILURE_DIALOG:
                mComment.setText("Check the device. (Power and Bluetooth pairing)\nThen touch up the Refresh button.");

                mComment.setTextColor(Color.RED);
                break;
        }
    }

    @Override
    public void onConnected(ConnectResult result) {
        if (!mIsForeground) {
            return;
        }

        if (result == ConnectResult.Success || result == ConnectResult.AlreadyConnected) {
            if (mIsFirst) {
                mIsFirst = false;

                print();
            }
            else if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
        else {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(OPEN_FAILURE_DIALOG);
            dialog.setTitle("Communication Result");
            dialog.setMessage("Fail to Open Port.");
            dialog.setPositiveButton("OK");
            dialog.show(getChildFragmentManager());
        }
    }

    @Override
    public void onDisconnected() {
        // do nothing
    }

    private void print() {
        mProgressDialog.show();

        byte[] commands;

        PrinterSetting setting = new PrinterSetting(getActivity());
        StarIoExt.Emulation emulation = setting.getEmulation();

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);

        int allReceiptSettings = setting.getAllReceiptSetting();

        boolean receipt = (allReceiptSettings & 0x00000001) != 0x00000000;
        boolean info    = (allReceiptSettings & 0x00000002) != 0x00000000;
        boolean qrCode  = (allReceiptSettings & 0x00000004) != 0x00000000;

        mIsPrint = receipt || info || qrCode;

        switch (mSelectedIndex) {
            case 1:
                commands = AllReceiptsFunctions.createTextReceiptData(getActivity(), emulation, localizeReceipts, mPaperSize, false, receipt, info, qrCode, mAllReceiptsCallback);
                break;
            case 2:
                commands = AllReceiptsFunctions.createTextReceiptData(getActivity(), emulation, localizeReceipts, mPaperSize, true, receipt, info, qrCode, mAllReceiptsCallback);
                break;
            case 3:
                commands = AllReceiptsFunctions.createRasterReceiptData(getActivity(), emulation, localizeReceipts, mPaperSize, receipt, info, qrCode, mAllReceiptsCallback);
                break;
            case 4:
                commands = AllReceiptsFunctions.createScaleRasterReceiptData(getActivity(), emulation, localizeReceipts, mPaperSize, true, receipt, info, qrCode, mAllReceiptsCallback);
                break;
//          case 5:
            default:
                commands = AllReceiptsFunctions.createScaleRasterReceiptData(getActivity(), emulation, localizeReceipts, mPaperSize, false, receipt, info, qrCode, mAllReceiptsCallback);
                break;
        }

        if (commands == null) {
            commands = new byte[0];
        }

        Communication.sendCommands(mStarIoExtManager, commands, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), mCallback);
    }

    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(boolean result, Result communicateResult) {
            if (!mIsForeground) {
                return;
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (!mIsPrint) {
                return;
            }

            String msg;

            switch (communicateResult) {
                case Success:
                    msg = "Success!";
                    break;
                case ErrorOpenPort:
                    msg = "Fail to openPort";
                    break;
                case ErrorBeginCheckedBlock:
                    msg = "Printer is offline (beginCheckedBlock)";
                    break;
                case ErrorEndCheckedBlock:
                    msg = "Printer is offline (endCheckedBlock)";
                    break;
                case ErrorReadPort:
                    msg = "Read port error (readPort)";
                    break;
                case ErrorWritePort:
                    msg = "Write port error (writePort)";
                    break;
                default:
                    msg = "Unknown error";
                    break;
            }

            CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
            dialog.setTitle("Communication Result");
            dialog.setMessage(msg);
            dialog.setPositiveButton("OK");
            dialog.show(getChildFragmentManager());
        }
    };

    private final RequestCallback mAllReceiptsCallback = new RequestCallback() {
        @Override
        public void onRequestResult(int statusCode, RequestError requestError) {
            if (!mIsForeground) {
                return;
            }

            String message;

            if (requestError != null) {
                message = requestError.getMessage();
            }
            else {
                message = "Status Code : " + statusCode;
            }

            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                 .show();
        }
    };

    private final StarIoExtManagerListener mStarIoExtManagerListener = new StarIoExtManagerListener() {
        @Override
        public void onPrinterImpossible() {
            mComment.setText("Printer Impossible.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onPrinterOnline() {
            mComment.setText("Printer Online.");

            mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onPrinterOffline() {
//          mComment.setText("Printer Offline.");
//
//          mComment.setTextColor(Color.RED);
        }

        @Override
        public void onPrinterPaperReady() {
//          mComment.setText("Printer Paper Ready.");
//
//          mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onPrinterPaperNearEmpty() {
            mComment.setText("Printer Paper Near Empty.");

            mComment.setTextColor(0xffffa500);     // Orange
        }

        @Override
        public void onPrinterPaperEmpty() {
            mComment.setText("Printer Paper Empty.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onPrinterCoverOpen() {
            mComment.setText("Printer Cover Open.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onPrinterCoverClose() {
//          mComment.setText("Printer Cover Close.");
//
//          mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onAccessoryConnectSuccess() {
            mComment.setText("Accessory Connect Success.");

            mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onAccessoryConnectFailure() {
            mComment.setText("Accessory Connect Failure.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onAccessoryDisconnect() {
            mComment.setText("Accessory Disconnect.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onStatusUpdate(String status) {
            AllReceipts.updateStatus(getContext(), status, new RequestCallback() {
                @Override
                public void onRequestResult(int statusCode, RequestError requestError) {
                    if (!mIsForeground) {
                        return;
                    }

                    String message;

                    if (requestError != null) {
                        message = requestError.getMessage();
                    }
                    else {
                        message = "Status Code : " + statusCode;
                    }

                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                         .show();
                }
            });
        }
    };
}
