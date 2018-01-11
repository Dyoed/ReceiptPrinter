package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.starmicronics.starioextension.IPeripheralConnectParser;
import com.starmicronics.starioextension.IScaleWeightParser;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.ScaleModel;
import com.starmicronics.starprntsdk.functions.ScaleFunctions;

public class ScaleFragment extends ItemListFragment implements CommonAlertDialogFragment.Callback {
    private ProgressDialog mProgressDialog;

    private boolean mIsForeground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        addTitle("Contents");
        addMenu("Check Status");
        addMenu("Displayed Weight");
        addMenu("Zero Clear");
        addMenu("Unit Change");

        addTitle("Like a StarIoExtManager Sample");
        addMenu("Sample");
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
    public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
        super.onItemClick(parent, view, position, id);

        if (1 <= position && position <= 4) {
            mProgressDialog.show();

            PrinterSetting setting = new PrinterSetting(getActivity());

            final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS10);
//          final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS12);
//          final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS20);

            Communication.parseDoNotCheckCondition(DisplayFragment.class, parser, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), new Communication.SendCallback() {
                @Override
                public void onStatus(boolean result, Communication.Result communicateResult) {
                    if (!mIsForeground) {
                        return;
                    }

                    if (result) {
                        String message;

                        if (parser.isConnected()) {
                            if (position == 1) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.dismiss();
                                }

                                message = "Scale Connect";

                                CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                                dialog.setTitle("Check Status");
                                dialog.setMessage(message);
                                dialog.setPositiveButton("OK");
                                dialog.show(getChildFragmentManager());
                            }
                            else if (position == 2) {
                                PrinterSetting setting = new PrinterSetting(getActivity());

                                final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS10);
//                              final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS12);
//                              final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS20);

                                ScaleCommunication.parseDoNotCheckCondition(ScaleFragment.class, parser, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), new Communication.SendCallback() {
                                    @Override
                                    public void onStatus(boolean result, Communication.Result communicateResult) {
                                        if (!mIsForeground) {
                                            return;
                                        }

                                        if (mProgressDialog != null) {
                                            mProgressDialog.dismiss();
                                        }

                                        if (result) {
                                            String title;

                                            switch (parser.getStatus()) {
                                                default:
                                                case Zero:
                                                    title = "Success  [ Zero ]";
                                                    break;
                                                case NotInMotion:
                                                    title = "Success  [ Not in motion ]";
                                                    break;
                                                case Motion:
                                                    title = "Success  [ Motion ]";
                                                    break;
                                            }

                                            CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                                            dialog.setTitle(title);
                                            dialog.setMessage(parser.getWeight());
                                            dialog.setPositiveButton("OK");
                                            dialog.show(getChildFragmentManager());
                                        }
                                        else {
                                            String msg;

                                            switch (communicateResult) {
                                                case Success:
                                                    msg = "Success (Not communicate scale)";
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
                                    }
                                });
                            }
                            else {
                                byte[] data;
                                if (position == 3){
                                    data = ScaleFunctions.createZeroClear();
                                }
                                else {
                                    data = ScaleFunctions.createUnitChange();
                                }

                                PrinterSetting setting = new PrinterSetting(getActivity());

                                Communication.sendCommandsDoNotCheckCondition(ScaleFragment.class, data, setting.getPortName(), setting.getPortSettings(), 10000, getActivity(), new Communication.SendCallback() {
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
                                });
                            }
                        }
                        else {
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }

                            message = "Scale Disconnect";
                            CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                            dialog.setTitle("Check Status");
                            dialog.setMessage(message);
                            dialog.setPositiveButton("OK");
                            dialog.show(getChildFragmentManager());
                        }
                    }
                    else {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }

                        CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                        dialog.setTitle("Communication Result");
                        dialog.setMessage("Printer Impossible");
                        dialog.setPositiveButton("OK");
                        dialog.show(getChildFragmentManager());
                    }
                }
            });
        }
        else if (6 <= position && position <= 6) {
            Intent intent = new Intent(getActivity(), CommonActivity.class);
            intent.putExtra(CommonActivity.BUNDLE_KEY_ACTIVITY_LAYOUT, R.layout.activity_scale_ext);
            intent.putExtra(CommonActivity.BUNDLE_KEY_TOOLBAR_TITLE, "Scale Ext");
            intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_HOME_BUTTON, true);
            intent.putExtra(CommonActivity.BUNDLE_KEY_SHOW_RELOAD_BUTTON, true);

            startActivity(intent);
        }
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // do nothing
    }
}
