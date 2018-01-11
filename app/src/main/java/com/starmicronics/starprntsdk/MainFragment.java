package com.starmicronics.starprntsdk;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;

import com.starmicronics.cloudservices.CloudServices;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.starioextension.StarIoExt;

import static com.starmicronics.starioextension.StarIoExt.Emulation;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {

    private static final int PRINTER_SET_REQUEST_CODE = 1;

    private static final String PRINTER_LANG_SELECT_DIALOG                  = "PrinterLanguageSelectDialog";
    private static final String PRINTER_LANG_SELECT_PAGE_MODE_DIALOG        = "PrinterLanguageSelectPageModeDialog";
    private static final String PRINTER_PAPER_SIZE_SELECT_DIALOG            = "PrinterPaperSizeSelectDialog";
    private static final String PRINTER_PAPER_SIZE_SELECT_PAGE_MODE_DIALOG  = "PrinterPaperSizeSelectPageModeDialog";

    private static final String BLACK_MARK_LANG_SELECT_DIALOG               = "BlackMarkLanguageSelectDialog";
    private static final String BLACK_MARK_PASTE_LANG_SELECT_DIALOG         = "BlackMarkPasteLanguageSelectDialog";

    private static final String MPOP_BARCODE_READER_CONFIRM_DIALOG          = "mPOPBarcodeReaderConfirmDialog";
    private static final String MPOP_DISPLAY_CONFIRM_DIALOG                 = "mPOPDisplayConfirmDialog";
    private static final String MPOP_SCALE_CONFIRM_DIALOG                   = "mPOPScaleConfirmDialog";

    private static final String MPOP_COMBINATION_CONFIRM_DIALOG             = "mPOPCombinationConfirmDialog";
    private static final String MPOP_COMBINATION_LANG_SELECT_DIALOG         = "mPOPCombinationLanguageSelectDialog";

    private static final String API_PAPER_SIZE_SELECT_DIALOG                = "ApiPaperSizeSelectDialog";

    private static final String ALL_RECEIPT_LANG_SELECT_DIALOG              = "AllReceiptLanguageSelectDialog";
    private static final String ALL_RECEIPT_PAPER_SIZE_SELECT_DIALOG        = "AllReceiptPaperSizeSelectDialog";

    private static final String SERIAL_NUMBER_CONFIRM_DIALOG                = "SerialNumberConfirmDialog";
    private static final String SERIAL_NUMBER_DIALOG                        = "SerialNumberDialog";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PRINTER_SET_REQUEST_CODE) {
            updateList();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);

        Intent intent = new Intent(getActivity(), CommonActivity.class);

        switch (position) {
            case 1: {   // Tapped Destination Device row
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_printer_search);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Search Port");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);

                startActivityForResult(intent, PRINTER_SET_REQUEST_CODE);
                break;
            }
            case 3: {   // Tapped Printer row (Sample)
                LanguageSelectDialogFragment dialog = LanguageSelectDialogFragment.newInstance(PRINTER_LANG_SELECT_DIALOG);
                dialog.show(getChildFragmentManager());
                break;

            }
            case 4: {   // Tapped Printer row (Black Mark)
                BlackMarkLanguageSelectDialogFragment dialog = BlackMarkLanguageSelectDialogFragment.newInstance(BLACK_MARK_LANG_SELECT_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 5: {   // Tapped Printer row (Black Mark Paste)
                BlackMarkLanguageSelectDialogFragment dialog = BlackMarkLanguageSelectDialogFragment.newInstance(BLACK_MARK_PASTE_LANG_SELECT_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 6: {   // Tapped Printer row (Page Mode)
                LessLanguageSelectDialogFragment dialog = LessLanguageSelectDialogFragment.newInstance(PRINTER_LANG_SELECT_PAGE_MODE_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 8: {   // Tapped CashDrawer row
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_cash_drawer);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Cash Drawer");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);
                break;
            }
            case 10: {   // Tapped Barcode Reader row
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(MPOP_BARCODE_READER_CONFIRM_DIALOG);
                dialog.setTitle("This menu is for mPOP.");
                dialog.setPositiveButton("Continue");
                dialog.setNegativeButton("Cancel");
                dialog.setCancelable(false);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 12: {   // Tapped Display row
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(MPOP_DISPLAY_CONFIRM_DIALOG);
                dialog.setTitle("This menu is for mPOP or TSP100IIIU.");
                dialog.setPositiveButton("Continue");
                dialog.setNegativeButton("Cancel");
                dialog.setCancelable(false);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 14: {   // Tapped Scale row
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(MPOP_SCALE_CONFIRM_DIALOG);
                dialog.setTitle("This menu is for mPOP.");
                dialog.setPositiveButton("Continue");
                dialog.setNegativeButton("Cancel");
                dialog.setCancelable(false);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 16: {   // Tapped Combination row
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(MPOP_COMBINATION_CONFIRM_DIALOG);
                dialog.setTitle("This menu is for mPOP.");
                dialog.setPositiveButton("Continue");
                dialog.setNegativeButton("Cancel");
                dialog.setCancelable(false);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 18: {   // Tapped API row
                PrinterSetting setting = new PrinterSetting(getActivity());
                Emulation emulation = setting.getEmulation();

                if (emulation == Emulation.EscPos || emulation == Emulation.StarDotImpact) {
                    intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_api);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "API");
                    intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, emulation == Emulation.EscPos? PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH: PrinterSetting.PAPER_SIZE_DOT_THREE_INCH);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                    startActivity(intent);
                }
                else {
                    PaperSizeSelectDialogFragment dialog = PaperSizeSelectDialogFragment.newInstance(API_PAPER_SIZE_SELECT_DIALOG);
                    dialog.show(getChildFragmentManager());
                }

                break;
            }
            case 20: {   // Tapped AllReceipts row
                AllReceiptLanguageSelectDialogFragment dialog = AllReceiptLanguageSelectDialogFragment.newInstance(ALL_RECEIPT_LANG_SELECT_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 22: {   // Tapped Device Status row (Device Status/Firmware Information)
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_device_status);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Device Status");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);

                startActivity(intent);
                break;
            }
            case 23: {   // Tapped Device Status row (Serial Number)
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(SERIAL_NUMBER_CONFIRM_DIALOG);
                dialog.setTitle("This menu is for mPOP or TSP100III.");
                dialog.setPositiveButton("Continue");
                dialog.setNegativeButton("Cancel");
                dialog.setCancelable(false);
                dialog.show(getChildFragmentManager());
                break;
            }
            case 25: {   // Tapped Bluetooth Setting row
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_bluetooth_setting);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Bluetooth Setting");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);

                startActivity(intent);
                break;
            }
            case 27: {   // Tapped Library Version row
                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("");
                dialog.setTitle("Library Version");
                dialog.setMessage(
                        "StarIOPort3.1 version " + StarIOPort.getStarIOVersion() + "\n" +
                        StarIoExt.getDescription() + "\n" +
                        CloudServices.getDescription());
                dialog.setPositiveButton("OK");
                dialog.show(getChildFragmentManager());
                break;
            }
        }
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        boolean isCanceled = data.hasExtra(CommonAlertDialogFragment.LABEL_NEGATIVE);

        if (isCanceled) return;

        switch (tag) {
            case PRINTER_LANG_SELECT_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                PrinterSetting setting = new PrinterSetting(getActivity());
                Emulation emulation = setting.getEmulation();

                if (emulation == Emulation.EscPos || emulation == Emulation.StarDotImpact) {
                    Intent intent = new Intent(getActivity(), CommonActivity.class);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_printer);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Printer");
                    intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, emulation == Emulation.EscPos? PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH: PrinterSetting.PAPER_SIZE_DOT_THREE_INCH);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                    startActivity(intent);
                }
                else {
                    PaperSizeSelectDialogFragment dialog = PaperSizeSelectDialogFragment.newInstance(PRINTER_PAPER_SIZE_SELECT_DIALOG, language);
                    dialog.show(getChildFragmentManager());
                }

                break;
            }
            case PRINTER_PAPER_SIZE_SELECT_DIALOG: {
                int language  = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
                int paperSize = data.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_printer);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Printer");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, paperSize);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case BLACK_MARK_LANG_SELECT_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_blackmark);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Black Mark");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case BLACK_MARK_PASTE_LANG_SELECT_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_blackmark_paste);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Black Mark Paste");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case PRINTER_LANG_SELECT_PAGE_MODE_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                PaperSizeSelectDialogFragment dialog = PaperSizeSelectDialogFragment.newInstance(PRINTER_PAPER_SIZE_SELECT_PAGE_MODE_DIALOG, language);
                dialog.show(getChildFragmentManager());
                break;
            }
            case PRINTER_PAPER_SIZE_SELECT_PAGE_MODE_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
                int paperSize = data.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_page_mode);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Page Mode");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, paperSize);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case MPOP_BARCODE_READER_CONFIRM_DIALOG: {
                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_barcode_reader_ext);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Barcode Reader Ext");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);

                startActivity(intent);

                break;
            }
            case MPOP_DISPLAY_CONFIRM_DIALOG: {
                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_display);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Display");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);
                break;
            }
            case MPOP_SCALE_CONFIRM_DIALOG: {
                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_scale);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Scale");
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);
                break;
            }
            case MPOP_COMBINATION_CONFIRM_DIALOG: {
                LanguageSelectDialogFragment dialog = LanguageSelectDialogFragment.newInstance(MPOP_COMBINATION_LANG_SELECT_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
            case MPOP_COMBINATION_LANG_SELECT_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_combination);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Combination");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case API_PAPER_SIZE_SELECT_DIALOG: {
                int paperSize = data.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, -1);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_api);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "API");
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, paperSize);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case ALL_RECEIPT_LANG_SELECT_DIALOG: {
                int language = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);

                PrinterSetting setting = new PrinterSetting(getActivity());
                Emulation emulation = setting.getEmulation();

                if (emulation == Emulation.EscPos) {
                    Intent intent = new Intent(getActivity(), CommonActivity.class);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_all_receipts);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "AllReceipts");
                    intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_ESCPOS_THREE_INCH);
                    intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                    startActivity(intent);
                }
                else {
                    PaperSizeSelectDialogFragment dialog = PaperSizeSelectDialogFragment.newInstance(ALL_RECEIPT_PAPER_SIZE_SELECT_DIALOG, language);
                    dialog.show(getChildFragmentManager());
                }

                break;
            }
            case ALL_RECEIPT_PAPER_SIZE_SELECT_DIALOG: {
                int language  = data.getIntExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, PrinterSetting.LANGUAGE_ENGLISH);
                int paperSize = data.getIntExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, PrinterSetting.PAPER_SIZE_THREE_INCH);

                Intent intent = new Intent(getActivity(), CommonActivity.class);
                intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_all_receipts);
                intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "AllReceipts");
                intent.putExtra(CommonActivity.BUNDLE_KEY_LANGUAGE, language);
                intent.putExtra(CommonActivity.BUNDLE_KEY_PAPER_SIZE, paperSize);
                intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);

                startActivity(intent);

                break;
            }
            case SERIAL_NUMBER_CONFIRM_DIALOG: {
                SerialNumberDialogFragment dialog = SerialNumberDialogFragment.newInstance(SERIAL_NUMBER_DIALOG);
                dialog.show(getChildFragmentManager());
                break;
            }
        }
    }

    private void updateList() {
        adapter.clear();

        PrinterSetting setting = new PrinterSetting(getActivity());
        Emulation emulation = setting.getEmulation();

        boolean isDeviceSelected     = !setting.getModelName().isEmpty();

        boolean isBluetoothInterface = setting.getPortName().toUpperCase().startsWith("BT:");

        boolean canUseBlackMark       = emulation != Emulation.StarGraphic;
        boolean canUsePageMode        = emulation != Emulation.StarGraphic && emulation != Emulation.StarDotImpact;
        boolean canUseCashDrawer      = emulation != Emulation.EscPosMobile;
        boolean canUseBarcodeReader   = emulation == Emulation.StarPRNT;
        boolean canUseCustomerDisplay = emulation == Emulation.StarPRNT || setting.getModelName().matches("Star TSP1..IIIU");
        boolean canGetSerialNumber    = emulation == Emulation.StarPRNT || emulation == Emulation.StarGraphic;
        boolean canUseAllReceipt      = emulation != Emulation.StarDotImpact;

        addTitle("Destination Device");
        addPrinterInfo(setting);

        addTitle("Printer");
        addMenu("Sample",                     isDeviceSelected);
        addMenu("Black Mark Sample",          isDeviceSelected && canUseBlackMark);
        addMenu("Black Mark Sample (Paste)",  isDeviceSelected && canUseBlackMark);
        addMenu("Page Mode Sample",           isDeviceSelected && canUsePageMode);

        addTitle("Cash Drawer");
        addMenu("Sample",                     isDeviceSelected && canUseCashDrawer);

        addTitle("Barcode Reader (for mPOP)");
        addMenu("StarIoExtManager Sample",    isDeviceSelected && canUseBarcodeReader);

        addTitle("Display (for mPOP or TSP100IIIU)");
        addMenu("Sample",                     isDeviceSelected && canUseCustomerDisplay);

        addTitle("Scale (for mPOP)");
        addMenu("Sample",                     isDeviceSelected && canUseBarcodeReader);

        addTitle("Combination (for mPOP)");
        addMenu("StarIoExtManager Sample",    isDeviceSelected && canUseBarcodeReader);

        addTitle("API");
        addMenu("Sample",                     isDeviceSelected,                        ContextCompat.getColor(getActivity(), R.color.pink));

        addTitle("AllReceipts");
        addMenu("Sample",                     isDeviceSelected && canUseAllReceipt,    ContextCompat.getColor(getActivity(), R.color.aquamarine));

        addTitle("Device Status");
        addMenu("Sample",                     isDeviceSelected);
        addMenu("Serial Number",              canGetSerialNumber);

        addTitle("Bluetooth");
        addMenu("Bluetooth Setting",          isDeviceSelected && isBluetoothInterface);

        addTitle("Appendix");
        addMenu("Library Version");
    }

    private void addPrinterInfo(PrinterSetting setting) {
        String modelName = setting.getModelName();

        List<TextInfo> textList = new ArrayList<>();

        if (modelName.isEmpty()) {
            textList.add(new TextInfo("Unselected State", R.id.menuTextView, R.anim.blink, Color.RED));

            adapter.add(new ItemList(R.layout.list_main_row, textList, ContextCompat.getColor(getActivity(), R.color.lightskyblue), true));
        }
        else {
            String portName   = setting.getPortName();
            String macAddress = setting.getMacAddress();

            if (portName.startsWith(PrinterSetting.IF_TYPE_ETHERNET)) {
                textList.add(new TextInfo(modelName,                          R.id.deviceTextView));
                if (macAddress.isEmpty()) {
                    textList.add(new TextInfo(portName                          , R.id.deviceDetailTextView));
                }
                else {
                    textList.add(new TextInfo(portName + " (" + macAddress + ")", R.id.deviceDetailTextView));
                }
            }
            else if (portName.startsWith(PrinterSetting.IF_TYPE_BLUETOOTH)) {
                textList.add(new TextInfo(modelName, R.id.deviceTextView));
                if (macAddress.isEmpty()) {
                    textList.add(new TextInfo(portName                          , R.id.deviceDetailTextView));
                }
                else {
                    textList.add(new TextInfo(portName + " (" + macAddress + ")", R.id.deviceDetailTextView));
                }
            }
            else {  // USB
                textList.add(new TextInfo(modelName, R.id.deviceTextView));
                textList.add(new TextInfo(portName,  R.id.deviceDetailTextView));
            }

            adapter.add(new ItemList(R.layout.list_destination_device_row, textList, ContextCompat.getColor(getActivity(), R.color.lightskyblue), true));
        }
    }
}
