package com.starmicronics.starprntsdk;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;


public class SerialNumberDialogFragment extends CommonAlertDialogFragment {

    private TextView mSerialNumberTextView;

    private ProgressDialog mProgressDialog;

    private boolean mIsFirst = true;
    private boolean mIsForeground;

    public static SerialNumberDialogFragment newInstance(String tag) {
        SerialNumberDialogFragment dialogFragment = new SerialNumberDialogFragment();

        Bundle args = new Bundle();
        args.putString(DIALOG_TAG, tag);
        args.putBoolean(CANCEL, false);
        args.putBoolean(CALLBACK, false);

        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);

        dialogFragment.setPositiveButton("OK");

        return dialogFragment;
    }

    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState) {
        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View dialogView = View.inflate(getActivity(), R.layout.dialog_serial_number, null);

        builder.setTitle("Serial Number");

        builder.setView(dialogView);

        mSerialNumberTextView = (TextView) dialogView.findViewById(R.id.serialNumberTextView);

        setupPositiveButton(builder);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mIsFirst) {
            mProgressDialog.show();

            PrinterSetting setting = new PrinterSetting(getActivity());

            Communication.getSerialNumber(SerialNumberDialogFragment.class, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), mCallback);
        }
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

    final Communication.SerialNumberCallback mCallback = new Communication.SerialNumberCallback() {
        @Override
        public void onSerialNumber(Communication.Result communicateResult, String serialNumber) {
            if (!mIsForeground) {
                return;
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            mIsFirst = false;

            String msg;

            switch (communicateResult) {
                case Success:
                    msg = serialNumber;
                    break;
                case ErrorOpenPort:
                    msg = "Fail to Open Port.";
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

            mSerialNumberTextView.setText(msg);
        }
    };
}
