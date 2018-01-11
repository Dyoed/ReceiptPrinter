package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.starioextension.StarIoExt;

import java.util.ArrayList;
import java.util.List;

public final class SearchPortFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {
    private ProgressDialog mProgressDialog;

    private static final String INTERFACE_SELECT_DIALOG          = "InterfaceSelectDialog";
    private static final String MODEL_CONFIRM_DIALOG             = "ModelConfirmDialog";
    private static final String MODEL_SELECT_DIALOG_0            = "ModelSelectDialog0";
    private static final String MODEL_SELECT_DIALOG_1            = "ModelSelectDialog1";
    private static final String DRAWER_OPEN_ACTIVE_SELECT_DIALOG = "DrawerOpenSelectDialog";
    private static final String PORT_NAME_INPUT_DIALOG           = "PortNameInputDialog";
    private static final String PORT_SETTINGS_INPUT_DIALOG       = "PortSettingsInputDialog";

    private String              mPortName;
    private String              mPortSettings;
    private String              mMacAddress;
    private String              mModelName;
    private StarIoExt.Emulation mEmulation;
    private Boolean             mDrawerOpenStatus;

    public SearchPortFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        updatePrinterList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reloadIcon) {
            updatePrinterList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View clickedItemView, int position, long id) {
        super.onItemClick(parent, clickedItemView, position, id);

        switchSelectedRow(position);

        TextView modelNameTextView = (TextView) clickedItemView.findViewById(R.id.modelNameTextView);
        String   modelName         = modelNameTextView.getText().toString();
        int      model             = ModelCapability.getModel(modelName);

        List<TextInfo> portInfoList = adapter.getItem(position).getTextList();

        for (TextInfo portInfo : portInfoList) {
            switch (portInfo.getTextResourceID()) {
                case R.id.modelNameTextView:
                    mModelName = portInfo.getText();
                    break;
                case R.id.portNameTextView:
                    mPortName = portInfo.getText();
                    break;
                case R.id.macAddressTextView:
                    mMacAddress = portInfo.getText();
                    if (mMacAddress.startsWith("(") && mMacAddress.endsWith(")")) {
                        mMacAddress = mMacAddress.substring(1, mMacAddress.length() - 1);
                    }
                    break;
            }
        }

        if (model == ModelCapability.NONE) {
            ModelSelectDialogFragment dialog = ModelSelectDialogFragment.newInstance(MODEL_SELECT_DIALOG_0);
            dialog.show(getChildFragmentManager());
        }
        else {
            ModelConfirmDialogFragment dialog = ModelConfirmDialogFragment.newInstance(MODEL_CONFIRM_DIALOG, model);
            dialog.show(getChildFragmentManager());
        }
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        switch (tag) {
            case INTERFACE_SELECT_DIALOG : {
                String[] selectedInterfaces = data.getStringArrayExtra(CommonActivity.BUNDLE_KEY_INTERFACE);
                boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (selectedInterfaces != null) {
                    addTitle("List");

                    if (selectedInterfaces.length <= 1 && selectedInterfaces[0].equals(PrinterSetting.IF_TYPE_MANUAL)) {
                        mMacAddress = "";

                        PrinterSetting         setting = new PrinterSetting(getActivity());
                        PortNameDialogFragment dialog  = PortNameDialogFragment.newInstance(PORT_NAME_INPUT_DIALOG, setting.getPortName());

                        dialog.show(getChildFragmentManager());
                    }
                    else {
                        for (String selectedInterface : selectedInterfaces) {
                            SearchTask searchTask = new SearchTask();
                            searchTask.execute(selectedInterface);
                        }

                        mProgressDialog.show();
                    }
                }
                else if (isCanceled) {
                    getActivity().finish();
                }
                break;
            }
            case PORT_NAME_INPUT_DIALOG: {
                String  portName   = data.getStringExtra(CommonActivity.BUNDLE_KEY_PORT_NAME);
                boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (isCanceled) {
                    getActivity().finish();
                    return;
                }

                if (portName.length() == 0) {
                    Toast.makeText(getActivity(), "Fill in the port name.", Toast.LENGTH_LONG)
                         .show();

                    PrinterSetting         setting = new PrinterSetting(getActivity());
                    PortNameDialogFragment dialog  = PortNameDialogFragment.newInstance(PORT_NAME_INPUT_DIALOG, setting.getPortName());

                    dialog.show(getChildFragmentManager());
                }
                else {
                    mPortName = portName;

                    PrinterSetting setting = new PrinterSetting(getActivity());
                    PortSettingsDialogFragment dialog = PortSettingsDialogFragment.newInstance(PORT_SETTINGS_INPUT_DIALOG, setting.getPortSettings());
                    dialog.show(getChildFragmentManager());
                }
                break;
            }
            case PORT_SETTINGS_INPUT_DIALOG: {
                String  portSettings = data.getStringExtra(CommonActivity.BUNDLE_KEY_PORT_SETTINGS);
                boolean isCanceled   = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (isCanceled) {
                    getActivity().finish();
                    return;
                }

                mPortSettings = portSettings;

                ModelSelectDialogFragment dialog = ModelSelectDialogFragment.newInstance(MODEL_SELECT_DIALOG_1);
                dialog.show(getChildFragmentManager());
                break;
            }
            case MODEL_CONFIRM_DIALOG: {
                boolean isPressedYes = data.hasExtra(CommonAlertDialogFragment.LABEL_POSITIVE);
                int selectedModel  = data.getIntExtra(CommonActivity.SELECTED_MODEL, ModelCapability.NONE);

                if (isPressedYes) {
                    mPortSettings = ModelCapability.getPortSettings(selectedModel);
                    mEmulation    = ModelCapability.getEmulation(selectedModel);

                    if (ModelCapability.getDrawerOpenStatus(selectedModel)) {
                        DrawerOpenActiveSelectDialogFragment dialog = DrawerOpenActiveSelectDialogFragment.newInstance(DRAWER_OPEN_ACTIVE_SELECT_DIALOG);
                        dialog.show(getChildFragmentManager());
                    }
                    else {
                        mDrawerOpenStatus = true;
                        registerPrinter();

                        getActivity().finish();
                    }
                }
                else {
                    ModelSelectDialogFragment dialog = ModelSelectDialogFragment.newInstance(MODEL_SELECT_DIALOG_0);
                    dialog.show(getChildFragmentManager());
                }
                break;
            }
            case MODEL_SELECT_DIALOG_0: {
                int selectedModel = data.getIntExtra(CommonActivity.SELECTED_MODEL, ModelCapability.NONE);

                boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (isCanceled) {
                    return;
                }

                mPortSettings = ModelCapability.getPortSettings(selectedModel);
                mEmulation    = ModelCapability.getEmulation(selectedModel);

                if (ModelCapability.getDrawerOpenStatus(selectedModel)) {
                    DrawerOpenActiveSelectDialogFragment dialog = DrawerOpenActiveSelectDialogFragment.newInstance(DRAWER_OPEN_ACTIVE_SELECT_DIALOG);
                    dialog.show(getChildFragmentManager());
                }
                else {
                    mDrawerOpenStatus = true;
                    registerPrinter();

                    getActivity().finish();
                }
                break;
            }
            case MODEL_SELECT_DIALOG_1: {
                int selectedModel = data.getIntExtra(CommonActivity.SELECTED_MODEL, ModelCapability.NONE);

                boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (isCanceled) {
                    return;
                }

                mModelName = ModelCapability.getModelTitle(selectedModel);
                mEmulation = ModelCapability.getEmulation(selectedModel);

                if (ModelCapability.getDrawerOpenStatus(selectedModel)) {
                    DrawerOpenActiveSelectDialogFragment dialog = DrawerOpenActiveSelectDialogFragment.newInstance(DRAWER_OPEN_ACTIVE_SELECT_DIALOG);
                    dialog.show(getChildFragmentManager());
                }
                else {
                    mDrawerOpenStatus = true;
                    registerPrinter();

                    getActivity().finish();
                }
                break;
            }
            case DRAWER_OPEN_ACTIVE_SELECT_DIALOG: {
                boolean openStatus = data.getBooleanExtra(CommonActivity.BUNDLE_KEY_DRAWER_OPEN_STATUS, false);
                boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

                if (isCanceled) {
                    return;
                }

                mDrawerOpenStatus = openStatus;
                registerPrinter();

                getActivity().finish();
                break;
            }
        }
    }

    private void updatePrinterList() {
        adapter.clear();

        InterfaceSelectDialogFragment dialog = InterfaceSelectDialogFragment.newInstance(INTERFACE_SELECT_DIALOG);
        dialog.show(getChildFragmentManager());
    }

    /**
     * Register printer information to SharedPreference.
     */
    private void registerPrinter() {
        PrinterSetting setting = new PrinterSetting(getActivity());
        setting.write(mModelName, mPortName, mMacAddress, mPortSettings, mEmulation, mDrawerOpenStatus);
    }

    private void switchSelectedRow(int index) {
        for (int i = 0; i < adapter.getCount(); i++) {
            ItemList itemList = adapter.getItem(i);

            if (itemList.getImgList() == null) {
                continue;
            }

            List<ImgInfo> imgList = new ArrayList<>();

            int imageId;

            if (i == index) {
                imageId = R.drawable.checked_icon;
            }
            else {
                imageId = R.drawable.unchecked_icon;
            }

            imgList.add(new ImgInfo(imageId, R.id.checkedIconImageView));

            itemList.setImgList(imgList);

            adapter.remove(itemList);
            adapter.insert(itemList, i);
        }
    }

    /**
     * Printer search task.
     */
    private class SearchTask extends AsyncTask<String, Void, Void> {
        private List<PortInfo> mPortList;

        SearchTask() {
            super();
        }

        @Override
        protected Void doInBackground(String... interfaceType) {
            try {
                mPortList = StarIOPort.searchPrinter(interfaceType[0], getActivity());
            }
            catch (StarIOPortException e) {
                mPortList = new ArrayList<>();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void doNotUse) {
            for (PortInfo info : mPortList) {
                addItem(info);
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
    }

    private void addItem(PortInfo info) {
        List<TextInfo> textList = new ArrayList<>();
        List<ImgInfo>  imgList  = new ArrayList<>();

        String modelName;
        String portName;
        String macAddress;

        // --- Bluetooth ---
        // It can communication used device name(Ex.BT:Star Micronics) at bluetooth.
        // If android device has paired two same name device, can't choose destination target.
        // If used Mac Address(Ex. BT:00:12:3f:XX:XX:XX) at Bluetooth, can choose destination target.
        if (info.getPortName().startsWith(PrinterSetting.IF_TYPE_BLUETOOTH)) {
            modelName  = info.getPortName().substring(PrinterSetting.IF_TYPE_BLUETOOTH.length());
            portName   = PrinterSetting.IF_TYPE_BLUETOOTH + info.getMacAddress();
            macAddress = info.getMacAddress();
        }
        else {
            modelName  = info.getModelName();
            portName   = info.getPortName();
            macAddress = info.getMacAddress();
        }

        textList.add(new TextInfo(modelName,  R.id.modelNameTextView));
        textList.add(new TextInfo(portName,   R.id.portNameTextView));

        if (   info.getPortName().startsWith(PrinterSetting.IF_TYPE_ETHERNET)
            || info.getPortName().startsWith(PrinterSetting.IF_TYPE_BLUETOOTH)) {
            textList.add(new TextInfo("(" + macAddress + ")", R.id.macAddressTextView));
        }

        PrinterSetting setting = new PrinterSetting(getActivity());

        if (setting.getPortName().equals(portName)) {
            imgList.add(new ImgInfo(R.drawable.checked_icon, R.id.checkedIconImageView));
        }
        else {
            imgList.add(new ImgInfo(R.drawable.unchecked_icon, R.id.checkedIconImageView));
        }

        adapter.add(new ItemList(R.layout.list_printer_info_row, textList, imgList));
    }
}
