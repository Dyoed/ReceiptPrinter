package com.starmicronics.starprntsdk;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.starmicronics.starprntsdk.functions.ApiFunctions;

import static com.starmicronics.starioextension.StarIoExt.Emulation;
import static com.starmicronics.starioextension.ICommandBuilder.BlackMarkType;

public class ApiFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {

    private static final String BLACK_MARK_TYPE_SELECT_DIALOG = "BlackMarkTypeSelectDialog";
    private static final int    BLACK_MARK_MENU_INDEX         = 23;

    private ProgressDialog mProgressDialog;

    private BlackMarkType mBlackMarkType;

    private boolean mIsForeground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        addTitle("Sample");
        addMenu("Generic");
        addMenu("Font Style");
        addMenu("Initialization");
        addMenu("Code Page");
        addMenu("International");
        addMenu("Feed");
        addMenu("Character Space");
        addMenu("Line Space");
        addMenu("Emphasis");
        addMenu("Invert");
        addMenu("Under Line");
        addMenu("Multiple");
        addMenu("Absolute Position");
        addMenu("Alignment");
        addMenu("Logo");
        addMenu("Cut Paper");
        addMenu("Peripheral");
        addMenu("Sound");
        addMenu("Bitmap");
        addMenu("Barcode");
        addMenu("PDF417");
        addMenu("QR Code");
        addMenu("Black Mark");
        addMenu("Page Mode");
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

        if (position == BLACK_MARK_MENU_INDEX) {    // Black Mark
            BlackMarkTypeSelectDialogFragment dialog = BlackMarkTypeSelectDialogFragment.newInstance(BLACK_MARK_TYPE_SELECT_DIALOG);
            dialog.show(getChildFragmentManager());
        }
        else {                                      // Others
            print(position);
        }
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

        if (isCanceled) return;

        if (tag.equals(BLACK_MARK_TYPE_SELECT_DIALOG)) {
            int index = data.getIntExtra(CommonActivity.BUNDLE_KEY_BLACK_MARK_TYPE_INDEX, 0);

            switch (index) {
                default:
                case 0: mBlackMarkType = BlackMarkType.Invalid;            break;
                case 1: mBlackMarkType = BlackMarkType.Valid;              break;
                case 2: mBlackMarkType = BlackMarkType.ValidWithDetection; break;
            }

            print(BLACK_MARK_MENU_INDEX);   // Black Mark
        }
    }

    private void print(int selectedIndex) {
        mProgressDialog.show();

        byte[] data;

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        int paperSize = getActivity().getIntent().getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, -1);

        switch (selectedIndex) {
            case 1:  data = ApiFunctions.createGenericData(emulation);                            break;
            case 2:  data = ApiFunctions.createFontStyleData(emulation);                          break;
            case 3:  data = ApiFunctions.createInitializationData(emulation);                     break;
            case 4:  data = ApiFunctions.createCodePageData(emulation);                           break;
            case 5:  data = ApiFunctions.createInternationalData(emulation);                      break;
            case 6:  data = ApiFunctions.createFeedData(emulation);                               break;
            case 7:  data = ApiFunctions.createCharacterSpaceData(emulation);                     break;
            case 8:  data = ApiFunctions.createLineSpaceData(emulation);                          break;
            case 9:  data = ApiFunctions.createEmphasisData(emulation);                           break;
            case 10: data = ApiFunctions.createInvertData(emulation);                             break;
            case 11: data = ApiFunctions.createUnderLineData(emulation);                          break;
            case 12: data = ApiFunctions.createMultipleData(emulation);                           break;
            case 13: data = ApiFunctions.createAbsolutePositionData(emulation);                   break;
            case 14: data = ApiFunctions.createAlignmentData(emulation);                          break;
            case 15: data = ApiFunctions.createLogoData(emulation);                               break;
            case 16: data = ApiFunctions.createCutPaperData(emulation);                           break;
            case 17: data = ApiFunctions.createPeripheralData(emulation);                         break;
            case 18: data = ApiFunctions.createSoundData(emulation);                              break;
            case 19: data = ApiFunctions.createBitmapData(emulation, paperSize, getActivity());   break;
            case 20: data = ApiFunctions.createBarcodeData(emulation);                            break;
            case 21: data = ApiFunctions.createPdf417Data(emulation);                             break;
            case 22: data = ApiFunctions.createQrCodeData(emulation);                             break;
            case 23: data = ApiFunctions.createBlackMarkData(emulation, mBlackMarkType);          break;
            case 24: data = ApiFunctions.createPageModeData(emulation, paperSize, getActivity()); break;
            default: data = ApiFunctions.createGenericData(emulation);                            break;
        }

        Communication.sendCommands(this, data, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), mCallback);     // 10000mS!!!
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
                case Success :
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
