package com.starmicronics.starprntsdk;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.starmicronics.cloudservices.CloudServices;
import com.starmicronics.cloudservices.RequestCallback;
import com.starmicronics.cloudservices.RequestError;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.Emulation;
import com.starmicronics.starprntsdk.functions.AllReceiptsFunctions;
import com.starmicronics.starprntsdk.localizereceipts.ILocalizeReceipts;

import com.starmicronics.starprntsdk.Communication.Result;

import java.util.ArrayList;
import java.util.List;

public class AllReceiptsFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {

    private static final String RECEIPT_SWITCH_TAG = "receipt_switch_tag";
    private static final String INFO_SWITCH_TAG    = "info_switch_tag";
    private static final String QRCODE_SWITCH_TAG  = "qrcode_switch_tag";

    private static final String PREF_KEY_IS_FIRST_APP_LAUNCH = "pref_key_first_app_launch";

    private ProgressDialog mProgressDialog;

    private int mLanguage;
    private int mPaperSize;

    private boolean mIsPrint;
    private boolean mIsForeground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        Intent intent = getActivity().getIntent();
        mLanguage = intent.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
        mPaperSize = intent.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

        setInitialAllReceiptsSettings();

        updateList();
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

        if (1 <= position && position <= 5) {           // Tapped Like a StarIO-SDK Sample row
            print(position);
        }
        else if (7 <= position && position <= 11) {     // Tapped StarIoExtManager Sample row
            startTestPrintActivity(position - 6);
        }
        else if (position == 13) {                      // Tapped Procedure row
            CloudServices.showRegistrationView(getChildFragmentManager(), new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!mIsForeground) {
                        return;
                    }

                    updateList();
                }
            });
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String tag = (String) buttonView.getTag();

        if (tag == null) return;

        PrinterSetting setting = new PrinterSetting(getActivity());
        int allReceiptsSettings = setting.getAllReceiptSetting();

        if (isChecked) {
            switch (tag) {
                case RECEIPT_SWITCH_TAG:
                    allReceiptsSettings |= 0x00000001;
                    break;
                case INFO_SWITCH_TAG:
                    allReceiptsSettings |= 0x00000002;
                    break;
                case QRCODE_SWITCH_TAG:
                    allReceiptsSettings |= 0x00000004;
                    break;
            }
        }
        else {
            switch (tag) {
                case RECEIPT_SWITCH_TAG:
                    allReceiptsSettings &= 0x0000fffe;
                    break;
                case INFO_SWITCH_TAG:
                    allReceiptsSettings &= 0x0000fffd;
                    break;
                case QRCODE_SWITCH_TAG:
                    allReceiptsSettings &= 0x0000fffb;
                    break;
            }
        }

        setting.write(allReceiptsSettings);

        updateList();
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // do nothing
    }

    private void print(int selectedIndex) {
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

        switch (selectedIndex) {
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

        Communication.sendCommands(this, commands, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), mCallback);     // 10000mS!!!
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

    private void startTestPrintActivity(int position) {
        Intent intent = new Intent(getActivity(), CommonActivity.class);

        intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_all_receipts_ext);
        intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "AllReceipts Ext");
        intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
        intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);
        intent.putExtra(CommonActivity.BUNDLE_KEY_TEST_BUTTON_TEXT, "Print");
        intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE,         mLanguage);
        intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE,       mPaperSize);
        intent.putExtra(CommonActivity.BUNDLE_KEY_SELECTED_INDEX,   position);

        startActivity(intent);
    }

    private void setInitialAllReceiptsSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isFirstLaunch = prefs.getBoolean(PREF_KEY_IS_FIRST_APP_LAUNCH, true);

        if (isFirstLaunch) {
            PrinterSetting setting = new PrinterSetting(getActivity());
            int initialAllReceiptsSettings = 0x00000007;
            setting.write(initialAllReceiptsSettings);

            prefs.edit().putBoolean(PREF_KEY_IS_FIRST_APP_LAUNCH, false).apply();
        }
    }

    private void updateList() {
        adapter.clear();

        boolean isRegistered = CloudServices.isRegistered(getActivity());

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);
        String languageCode      = localizeReceipts.getLanguageCode();
        String paperSizeStr      = localizeReceipts.getPaperSizeStr();
        String scalePaperSizeStr = localizeReceipts.getScalePaperSizeStr();

        PrinterSetting setting = new PrinterSetting(getActivity());
        int allReceiptSettings = setting.getAllReceiptSetting();

        boolean receipt = (allReceiptSettings & 0x00000001) != 0x00000000;
        boolean info    = (allReceiptSettings & 0x00000002) != 0x00000000;
        boolean qrCode  = (allReceiptSettings & 0x00000004) != 0x00000000;

        Emulation emulation = setting.getEmulation();
        boolean canPrintTextReceipt     = emulation != Emulation.StarGraphic;
        boolean canPrintUtf8TextReceipt = emulation != Emulation.StarGraphic && emulation != Emulation.EscPos && emulation != Emulation.EscPosMobile;

        addTitle("Like a StarIO-SDK Sample");
        addMenu(languageCode + " " + paperSizeStr      + " Text Receipt",                isRegistered && canPrintTextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Text Receipt (UTF8)",         isRegistered && canPrintUtf8TextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Raster Receipt",              isRegistered);
        addMenu(languageCode + " " + scalePaperSizeStr + " Raster Receipt (Both Scale)", isRegistered);
        addMenu(languageCode + " " + scalePaperSizeStr + " Raster Receipt (Scale)",      isRegistered);

        addTitle("StarIoExtManager Sample");
        addMenu(languageCode + " " + paperSizeStr      + " Text Receipt",                isRegistered && canPrintTextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Text Receipt (UTF8)",         isRegistered && canPrintUtf8TextReceipt);
        addMenu(languageCode + " " + paperSizeStr      + " Raster Receipt",              isRegistered);
        addMenu(languageCode + " " + scalePaperSizeStr + " Raster Receipt (Both Scale)", isRegistered);
        addMenu(languageCode + " " + scalePaperSizeStr + " Raster Receipt (Scale)",      isRegistered);

        addTitle("Procedure");
        if (isRegistered) {
            addMenu("Registration", true, ContextCompat.getColor(getActivity(), R.color.aquamarine));
        }
        else {
            List<TextInfo> textList = new ArrayList<>();
            textList.add(new TextInfo("Unregistration State", R.id.menuTextView, R.anim.blink, Color.RED));

            adapter.add(new ItemList(R.layout.list_main_row, textList, ContextCompat.getColor(getActivity(), R.color.aquamarine), true));
        }

        addTitle("Print Setting");
        addSwitchMenu("Receipt",     receipt, ContextCompat.getColor(getActivity(), R.color.paleturquoise), RECEIPT_SWITCH_TAG, this);
        addSwitchMenu("Information", info,    ContextCompat.getColor(getActivity(), R.color.paleturquoise), INFO_SWITCH_TAG,    this);
        addSwitchMenu("QR Code",     qrCode,  ContextCompat.getColor(getActivity(), R.color.paleturquoise), QRCODE_SWITCH_TAG,  this);
    }
}
