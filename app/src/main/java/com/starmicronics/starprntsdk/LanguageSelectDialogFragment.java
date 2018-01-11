package com.starmicronics.starprntsdk;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public class LanguageSelectDialogFragment extends CommonAlertDialogFragment {

    private Callback mCallbackTarget;

    public static LanguageSelectDialogFragment newInstance(String tag) {
        LanguageSelectDialogFragment dialogFragment = new LanguageSelectDialogFragment();

        Bundle args = new Bundle();
        args.putString(DIALOG_TAG, tag);
        args.putBoolean(CANCEL, false);
        args.putBoolean(CALLBACK, true);
        args.putString(LABEL_NEGATIVE, "Cancel");

        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);

        return dialogFragment;
    }

    @Override
    public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Select language.");

        builder.setItems(new String[]{
                        "English",
                        "Japanese",
                        "French",
                        "Portuguese",
                        "Spanish",
                        "German",
                        "Russian",
                        "Simplified Chinese",
                        "Traditional Chinese"},
                        mOnLanguageClickListener);

        setupNegativeButton(builder);

        mCallbackTarget = (CommonAlertDialogFragment.Callback) getParentFragment();

        return builder.create();
    }

    private DialogInterface.OnClickListener mOnLanguageClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            int selectedLanguage = PrinterSetting.LANGUAGE_ENGLISH;

            switch (which) {
                case 0: selectedLanguage = PrinterSetting.LANGUAGE_ENGLISH; break;
                case 1: selectedLanguage = PrinterSetting.LANGUAGE_JAPANESE; break;
                case 2: selectedLanguage = PrinterSetting.LANGUAGE_FRENCH; break;
                case 3: selectedLanguage = PrinterSetting.LANGUAGE_PORTUGUESE; break;
                case 4: selectedLanguage = PrinterSetting.LANGUAGE_SPANISH; break;
                case 5: selectedLanguage = PrinterSetting.LANGUAGE_GERMAN; break;
                case 6: selectedLanguage = PrinterSetting.LANGUAGE_RUSSIAN; break;
                case 7: selectedLanguage = PrinterSetting.LANGUAGE_SIMPLIFIED_CHINESE; break;
                case 8: selectedLanguage = PrinterSetting.LANGUAGE_TRADITIONAL_CHINESE; break;
            }

            Intent intentForPassingData = new Intent();
            intentForPassingData.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, selectedLanguage);

            mCallbackTarget.onDialogResult(getArguments().getString(DIALOG_TAG), intentForPassingData);

            dismiss();
        }
    };
}
