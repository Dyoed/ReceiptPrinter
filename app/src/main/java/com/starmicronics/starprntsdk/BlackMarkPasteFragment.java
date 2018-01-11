package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.starmicronics.starprntsdk.functions.PrinterFunctions;
import com.starmicronics.starprntsdk.localizereceipts.ILocalizeReceipts;

import static com.starmicronics.starioextension.StarIoExt.Emulation;
import static com.starmicronics.starioextension.ICommandBuilder.BlackMarkType;

public class BlackMarkPasteFragment extends Fragment implements CommonAlertDialogFragment.Callback {

    private ProgressDialog mProgressDialog;

    private int mLanguage;
    private int mPaperSize;

    private boolean mIsForeground;

    private EditText mEditTextView;
    private Switch   mDoubleHeightSwitch;
    private Switch   mDetectionSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        Intent intent = getActivity().getIntent();
        mLanguage  = intent.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
        mPaperSize = intent.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_black_mark_paste, container, false);

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);

        mEditTextView = (EditText) rootView.findViewById(R.id.blackMarkEditTextView);

        mEditTextView.setText(localizeReceipts.createPasteTextLabelString());

        mDoubleHeightSwitch = (Switch) rootView.findViewById(R.id.blackMarkHeightX2Switch);

        mDetectionSwitch = (Switch) rootView.findViewById(R.id.blackMarkDetectionSwitch);

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        if (emulation == Emulation.StarLine ||
            emulation == Emulation.StarDotImpact ||
            emulation == Emulation.EscPos) {
            mDetectionSwitch.setEnabled(true);
            mDetectionSwitch.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
        }
        else {
            mDetectionSwitch.setEnabled(false);
            mDetectionSwitch.setTextColor(ContextCompat.getColor(getActivity(), R.color.lightgrey));
        }

        Button clearButton = (Button) rootView.findViewById(R.id.blackMarkPasteClearButton);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditTextView.setText("");
            }
        });

        Button printButton = (Button) rootView.findViewById(R.id.blackMarkPastePrintButton);

        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print();
            }
        });

        return rootView;
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
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // do nothing
    }

    private void print() {
        mProgressDialog.show();

        String  pasteText      = mEditTextView.getText().toString();
        boolean isDoubleHeight = mDoubleHeightSwitch.isChecked();
        boolean isDetection    = mDetectionSwitch.isChecked();

        byte[] commands;

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);

        BlackMarkType type;

        if (isDetection) {
            type = BlackMarkType.ValidWithDetection;
        }
        else {
            type = BlackMarkType.Valid;
        }

        commands = PrinterFunctions.createPasteTextBlackMarkData(emulation, localizeReceipts, pasteText, isDoubleHeight, type, false);

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
