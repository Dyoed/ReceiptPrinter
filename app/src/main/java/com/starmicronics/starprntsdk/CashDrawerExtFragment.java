package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.IConnectionCallback;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExtManager;
import com.starmicronics.starioextension.StarIoExtManagerListener;
import com.starmicronics.starprntsdk.functions.CashDrawerFunctions;

public class CashDrawerExtFragment extends Fragment implements IConnectionCallback, CommonAlertDialogFragment.Callback {

    private static final String OPEN_FAILURE_DIALOG = "OpenFailureDialog";

    private ProgressDialog mProgressDialog;

    private TextView mComment;

    private StarIoExtManager mStarIoExtManager;

    private int mSelectedIndex;

    private boolean mIsFirst = true;
    private boolean mIsForeground;

    public CashDrawerExtFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);

        Intent intent = getActivity().getIntent();
        mSelectedIndex = intent.getIntExtra(CommonActivity.BUNDLE_KEY_SELECTED_INDEX, 0);

        PrinterSetting setting = new PrinterSetting(getActivity());

        mStarIoExtManager = new StarIoExtManager(StarIoExtManager.Type.Standard, setting.getPortName(), setting.getPortSettings(), 10000, getActivity());     // 10000mS!!!
        mStarIoExtManager.setCashDrawerOpenActiveHigh(setting.getCashDrawerOpenActiveHigh());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cash_drawer_ext, container, false);

        mComment = (TextView) rootView.findViewById(R.id.statusTextView);

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);

        mComment.startAnimation(animation);

        Button testButton = (Button) rootView.findViewById(R.id.testOpenButton);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDrawer();
            }
        });

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mStarIoExtManager.setListener(mStarIoExtManagerListener);

        mProgressDialog.show();

        mStarIoExtManager.connect(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsForeground = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reloadIcon) {
            mProgressDialog.show();

            mStarIoExtManager.disconnect(new IConnectionCallback() {
                @Override
                public void onDisconnected() {
                    if (!mIsForeground) {
                        return;
                    }

                    mStarIoExtManager.connect(CashDrawerExtFragment.this);
                }

                @Override
                public void onConnected(ConnectResult connectResult) {
                    // do nothing
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
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
    public void onStop() {
        super.onStop();

        mStarIoExtManager.disconnect(this);
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        if (!mIsForeground) {
            return;
        }

        switch (tag) {
            case OPEN_FAILURE_DIALOG:
                mComment.setText("Check the device. (Power and Bluetooth pairing)\nThen touch up the Refresh button.");

                mComment.setTextColor(Color.RED);
                break;
        }
    }

    @Override
    public void onConnected(ConnectResult result) {
        if (!mIsForeground) {
            return;
        }

        if (result == ConnectResult.Success || result == ConnectResult.AlreadyConnected) {
            if (mIsFirst) {
                mIsFirst = false;

                openDrawer();
            }
            else if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }
        else {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance(OPEN_FAILURE_DIALOG);
            dialog.setTitle("Communication Result");
            dialog.setMessage("Fail to Open Port.");
            dialog.setPositiveButton("OK");
            dialog.show(getChildFragmentManager());
        }
    }

    @Override
    public void onDisconnected() {
        // do nothing
    }

    private void openDrawer() {
        mProgressDialog.show();

        byte[] data;

        PrinterSetting setting = new PrinterSetting(getActivity());
        StarIoExt.Emulation emulation = setting.getEmulation();

        boolean doCheckCondition = mSelectedIndex == 1 || mSelectedIndex == 3;

        switch (mSelectedIndex) {
            case 1:
            case 2:
            default:
                data = CashDrawerFunctions.createData(emulation, ICommandBuilder.PeripheralChannel.No1);
                break;
            case 3:
            case 4:
                data = CashDrawerFunctions.createData(emulation, ICommandBuilder.PeripheralChannel.No2);
                break;
        }

        if (doCheckCondition) {
            Communication.sendCommands(mStarIoExtManager, data, mStarIoExtManager.getPort(), mCallback);
        }
        else {
            Communication.sendCommandsDoNotCheckCondition(mStarIoExtManager, data, mStarIoExtManager.getPort(), mCallback);
        }
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

    private final StarIoExtManagerListener mStarIoExtManagerListener = new StarIoExtManagerListener() {
        @Override
        public void onPrinterImpossible() {
            mComment.setText("Printer Impossible.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onCashDrawerOpen() {
            mComment.setText("Cash Drawer Open.");

            mComment.setTextColor(Color.MAGENTA);
        }

        @Override
        public void onCashDrawerClose() {
            mComment.setText("Cash Drawer Close.");

            mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onAccessoryConnectSuccess() {
            mComment.setText("Accessory Connect Success.");

            mComment.setTextColor(Color.BLUE);
        }

        @Override
        public void onAccessoryConnectFailure() {
            mComment.setText("Accessory Connect Failure.");

            mComment.setTextColor(Color.RED);
        }

        @Override
        public void onAccessoryDisconnect() {
            mComment.setText("Accessory Disconnect.");

            mComment.setTextColor(Color.RED);
        }
    };
}
