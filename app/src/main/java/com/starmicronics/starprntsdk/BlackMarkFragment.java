package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;

import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starprntsdk.functions.PrinterFunctions;
import com.starmicronics.starprntsdk.localizereceipts.ILocalizeReceipts;

import static com.starmicronics.starioextension.StarIoExt.Emulation;
import static com.starmicronics.starioextension.ICommandBuilder.BlackMarkType;

import java.util.ArrayList;
import java.util.List;

public class BlackMarkFragment extends Fragment implements ListView.OnItemClickListener, CommonAlertDialogFragment.Callback {

    private ProgressDialog mProgressDialog;

    private int mLanguage;
    private int mPaperSize;

    private boolean mIsForeground;

    private ArrayAdapter<ItemList> mAdapter;

    private Switch mDetectionSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        mAdapter = new ItemListAdapter(getActivity(), new ArrayList<ItemList>());

        Intent intent = getActivity().getIntent();
        mLanguage  = intent.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
        mPaperSize = intent.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);
        String            languageCode     = localizeReceipts.getLanguageCode();

        addTitle("Like a StarIO-SDK Sample");

        addMenu(languageCode + " Text Label");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_black_mark, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.blackMarkListView);

        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        listView.setDivider(null);

        mDetectionSwitch = (Switch) rootView.findViewById(R.id.blackMarkDetectionSwitch);

        PrinterSetting setting = new PrinterSetting(getActivity());
        StarIoExt.Emulation emulation = setting.getEmulation();

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        print();
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // do nothing
    }

    private void addTitle(String title) {
        List<TextInfo> textList = new ArrayList<>();

        textList.add(new TextInfo(title, R.id.menuTextView));

        mAdapter.add(new ItemList(R.layout.list_main_title_row, textList, false));
    }

    private void addMenu(String menu) {
        List<TextInfo> textList = new ArrayList<>();

        textList.add(new TextInfo(menu, R.id.menuTextView));

        mAdapter.add(new ItemList(R.layout.list_main_row, textList, true));
    }

    private void print() {
        mProgressDialog.show();

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        ILocalizeReceipts localizeReceipts = ILocalizeReceipts.createLocalizeReceipts(mLanguage, mPaperSize);

        BlackMarkType type;

        if (mDetectionSwitch.isChecked()) {
            type = BlackMarkType.ValidWithDetection;
        }
        else {
            type = BlackMarkType.Valid;
        }

        byte[] commands = PrinterFunctions.createTextBlackMarkData(emulation, localizeReceipts, type, false);

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
