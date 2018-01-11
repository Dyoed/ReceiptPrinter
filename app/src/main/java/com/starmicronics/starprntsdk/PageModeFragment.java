package com.starmicronics.starprntsdk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.starmicronics.starprntsdk.functions.PrinterFunctions;
import com.starmicronics.starprntsdk.localizereceipts.ILocalizeReceipts;

import static com.starmicronics.starioextension.ICommandBuilder.BitmapConverterRotation;
import static com.starmicronics.starioextension.StarIoExt.Emulation;

public class PageModeFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {
    private static final int CAMERA_REQUEST_CODE  = 0x0001;
    private static final int LIBRARY_REQUEST_CODE = 0x0002;

    private ProgressDialog mProgressDialog;

    private int mLanguage;
    private int mPaperSize;

    private boolean mIsForeground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        Intent intent = getActivity().getIntent();
        mLanguage  = intent.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE,   PrinterSetting.LANGUAGE_ENGLISH);
        mPaperSize = intent.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

        PrinterSetting setting   = new PrinterSetting(getActivity());
        Emulation      emulation = setting.getEmulation();

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);
        String languageCode      = localizeReceipts.getLanguageCode();
        String paperSizeStr      = localizeReceipts.getPaperSizeStr();

        boolean canPrintTextReceipt     = emulation != Emulation.StarGraphic;

        addTitle("Like a StarIO-SDK Sample");
        addMenu(languageCode + " " + paperSizeStr      + " Text Label (Rotation0)",    canPrintTextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Text Label (Rotation90)",   canPrintTextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Text Label (Rotation180)",  canPrintTextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Text Label (Rotation270)",  canPrintTextReceipt);
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsForeground = true;
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);

        print(position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                // Canceled
                return;
            }

            print(8);
        }
        else if (requestCode == LIBRARY_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                // Canceled
                return;
            }

            if (data != null) {
                Uri uri = data.getData();

                if (uri != null) {
                    print(8);
                }
            }
        }
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // do nothing
    }

    private void print(int selectedIndex) {
        mProgressDialog.show();

        byte[] commands;

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);

        Rect                    rect;
        BitmapConverterRotation rotation;

        int width  = mPaperSize;
        int height = 30 * 8;        // 30mm

        switch (selectedIndex) {
            default:
            case 1:
                rect     = new Rect(0, 0, width, height);
                rotation = BitmapConverterRotation.Normal;
                break;
            case 2:
////            rect     = new Rect(0, 0, width, height);
//              rect     = new Rect(0, 0, height, width);
                //noinspection SuspiciousNameCombination
                rect     = new Rect(0, 0, width, width);
                rotation = BitmapConverterRotation.Right90;
                break;
            case 3:
                rect     = new Rect(0, 0, width, height);
                rotation = BitmapConverterRotation.Rotate180;
                break;
            case 4:
//              rect     = new Rect(0, 0, width, height);
                //noinspection SuspiciousNameCombination
                rect     = new Rect(0, 0, height, width);
                rotation = BitmapConverterRotation.Left90;
                break;
        }

        commands = PrinterFunctions.createTextPageModeData(emulation, localizeReceipts, rect, rotation, false);

        Communication.sendCommands(this, commands, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), mCallback);     // 10000mS!!!
    }

    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(boolean result, Communication.Result communicateResult) {
            if (!mIsForeground) {
                return;
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
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
}
