package com.starmicronics.starprntsdk;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public class PaperSizeSelectDialogFragment extends CommonAlertDialogFragment {

    private CommonAlertDialogFragment.Callback mCallbackTarget;

    public static PaperSizeSelectDialogFragment newInstance(String tag, int language) {
        PaperSizeSelectDialogFragment dialogFragment = new PaperSizeSelectDialogFragment();

        Bundle args = new Bundle();
        args.putString(DIALOG_TAG, tag);
        args.putBoolean(CANCEL, false);
        args.putBoolean(CALLBACK, true);
        args.putString(LABEL_NEGATIVE, "Cancel");
        args.putInt(CommonActivity.BUNDLE_KEY_LANGUAGE, language);

        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);

        return dialogFragment;
    }

    public static PaperSizeSelectDialogFragment newInstance(String tag) {
        return newInstance(tag, PrinterSetting.LANGUAGE_ENGLISH);
    }

    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Select paper size.");

        builder.setItems(new String[]{
                        "2\" (384dots)",
                        "3\" (576dots)",
                        "4\" (832dots)"},
                        mOnPaperSizeClickListener);

        setupNegativeButton(builder);

        mCallbackTarget = (CommonAlertDialogFragment.Callback) getParentFragment();

        return builder.create();
    }

    private DialogInterface.OnClickListener mOnPaperSizeClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            int selectedPaperSize = PrinterSetting.PAPER_SIZE_THREE_INCH;

            switch (which) {
                case 0: selectedPaperSize = PrinterSetting.PAPER_SIZE_TWO_INCH; break;
                case 1: selectedPaperSize = PrinterSetting.PAPER_SIZE_THREE_INCH; break;
                case 2: selectedPaperSize = PrinterSetting.PAPER_SIZE_FOUR_INCH; break;
            }

            Bundle args = getArguments();

            Intent intentForPassingData = new Intent();
            intentForPassingData.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, selectedPaperSize);
            intentForPassingData.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE,   args.getInt(CommonActivity.BUNDLE_KEY_LANGUAGE));

            mCallbackTarget.onDialogResult(getArguments().getString(DIALOG_TAG), intentForPassingData);

            dismiss();
        }
    };
}
