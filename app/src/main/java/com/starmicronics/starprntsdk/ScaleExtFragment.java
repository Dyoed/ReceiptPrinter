package com.starmicronics.starprntsdk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.starioextension.IPeripheralConnectParser;
import com.starmicronics.starioextension.IScaleWeightParser;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.ScaleModel;
import com.starmicronics.starprntsdk.functions.ScaleFunctions;

public class ScaleExtFragment extends Fragment implements CommonAlertDialogFragment.Callback {
    private enum PeripheralStatus {
        Invalid,
        Impossible,
        Connect,
        Disconnect,
    }

    private ProgressDialog mProgressDialog;

    private TextView mComment;

    private boolean mIsForeground;

    private PeripheralStatus mScaleStatus = PeripheralStatus.Invalid;

    private boolean          mTryConnect = false;
    private ScaleWatchThread mThread = null;

    public ScaleExtFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scale_ext, container, false);

        mComment = (TextView) rootView.findViewById(R.id.statusTextView);

        setHasOptionsMenu(true);

        Button button;
        button = (Button) rootView.findViewById(R.id.zeroClearButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScaleStatus == PeripheralStatus.Connect) {
                    mProgressDialog.show();
                    Communication.sendCommandsDoNotCheckCondition(ScaleExtFragment.class, ScaleFunctions.createZeroClear(), mThread.getPort(), mCallback);
                }
                else {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }

                    CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                    dialog.setTitle("Communication Result");
                    dialog.setMessage("Scale Disconnect");
                    dialog.setPositiveButton("OK");
                    dialog.show(getChildFragmentManager());
                }
            }
        });

        button = (Button) rootView.findViewById(R.id.unitChangeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScaleStatus == PeripheralStatus.Connect) {
                    mProgressDialog.show();
                    Communication.sendCommandsDoNotCheckCondition(ScaleExtFragment.class, ScaleFunctions.createUnitChange(), mThread.getPort(), mCallback);
                }
                else {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }

                    CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                    dialog.setTitle("Communication Result");
                    dialog.setMessage("Scale Disconnect");
                    dialog.setPositiveButton("OK");
                    dialog.show(getChildFragmentManager());
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsForeground = true;

        connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reloadIcon) {

            disconnect();
            connect();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        mIsForeground = false;

        if (mProgressDialog != null && !mTryConnect) {
            mProgressDialog.dismiss();
        }

        disconnect();
    }

    @Override
    public void onDialogResult(String tag, Intent data) {
        // Do nothing
    }


    void connect() {
        AsyncTask<Void, Void, StarIOPort> task = new AsyncTask<Void, Void, StarIOPort>() {
            @Override
            protected void onPreExecute() {
                mTryConnect = true;
                mProgressDialog.show();
            }

            @Override
            protected StarIOPort doInBackground(Void... voids) {
                if (mThread != null) {
                    if (mThread.getWaitDisconnect()) {
                        try {
                            mThread.join();
                            mThread = null;
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                    }
                    else {
                        return null;
                    }
                }

                StarIOPort port = null;

                PrinterSetting setting = new PrinterSetting(getActivity());
                try {
                    synchronized (ScaleExtFragment.class) {
                        port = StarIOPort.getPort(setting.getPortName(), setting.getPortSettings(), 10000, getActivity());
                    }
                } catch (StarIOPortException e) {
                    // Do Nothing
                }
                return port;
            }

            @Override
            protected void onPostExecute(StarIOPort port) {
                mTryConnect = false;

                if (!mIsForeground) {
                    mThread = new ScaleWatchThread(port);
                    mThread.disconnect();
                    mThread.start();
                    return;
                }

                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }

                if (port != null) {
                    mThread = new ScaleWatchThread(port);
                    mThread.start();

                    mScaleStatus = PeripheralStatus.Invalid;
                    mComment.clearAnimation();
                    mComment.setTextColor(Color.DKGRAY);
                    mComment.setText("Printer connected.");

                } else {
                    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
                    mComment.startAnimation(animation);
                    mComment.setTextColor(Color.RED);
                    mComment.setText("Check the device. (Power and Bluetooth pairing)\nThen touch up the Refresh button.");

                    CommonAlertDialogFragment dialog = CommonAlertDialogFragment.newInstance("CommResultDialog");
                    dialog.setTitle("Communication Result");
                    dialog.setMessage("Fail to openPort");
                    dialog.setPositiveButton("OK");
                    dialog.show(getChildFragmentManager());
                }
            }
        };

        if (!mTryConnect) {
            task.execute();
        }
    }

    void disconnect(){
        if (mThread != null) {
            mThread.disconnect();
        }
    }

    final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(boolean result, Communication.Result communicateResult) {
            if (!mIsForeground) {
                return;
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            String msg;

            if (communicateResult != Communication.Result.Success) {

                switch (communicateResult) {
//                  case Success:
//                      msg = "Success";
//                      break;
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
    };

    private class ScaleWatchThread extends Thread {
        private boolean    mIsStop       = false;
        private StarIOPort mPort         = null;
        private boolean    mWaitCallback = false;
        private boolean    mReleasePort  = false;

        ScaleWatchThread(StarIOPort port) {
            mPort = port;
        }

        void disconnect(){
            mIsStop = true;
        }

        boolean getWaitDisconnect() {
            return mIsStop;
        }

        StarIOPort getPort() {
            return mPort;
        }

        @Override
        public void run() {

            while(!mIsStop) {
                if (mPort != null) {
                    mWaitCallback = true;

                    final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS10);
//                  final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS12);
//                  final IScaleWeightParser parser = StarIoExt.createScaleWeightParser(ScaleModel.APS20);

                    ScaleCommunication.parseDoNotCheckCondition(ScaleExtFragment.class, parser, mPort, new Communication.SendCallback() {
                        @Override
                        public void onStatus(boolean result, Communication.Result communicateResult) {
                            if (!mIsForeground) {
                                mWaitCallback = false;
                                return;
                            }

                            if (result) {
                                mScaleStatus = PeripheralStatus.Connect;
                                switch (parser.getStatus()) {
                                    case Zero:
                                        mComment.setTextColor(Color.GREEN);
                                        break;
                                    case Motion:
                                        mComment.setTextColor(Color.RED);
                                        break;
                                    case NotInMotion:
                                        mComment.setTextColor(Color.BLUE);
                                        break;
                                }

                                mComment.clearAnimation();
                                mComment.setText(parser.getWeight());
                                mWaitCallback = false;
                            }
                            else {
                                final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS10);
//                              final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS12);
//                              final IPeripheralConnectParser parser = StarIoExt.createScaleConnectParser(ScaleModel.APS20);

                                Communication.parseDoNotCheckCondition(ScaleExtFragment.class, parser, mPort, new Communication.SendCallback() {
                                    @Override
                                    public void onStatus(boolean result, Communication.Result communicateResult) {
                                        if (!mIsForeground) {
                                            mWaitCallback = false;
                                            return;
                                        }

                                        if (result) {
                                            //noinspection StatementWithEmptyBody
                                            if (parser.isConnected()) {
                                                // Because the scale doesn't sometimes react.
                                            }
                                            else {
                                                if (mScaleStatus != PeripheralStatus.Disconnect) {
                                                    mScaleStatus = PeripheralStatus.Disconnect;
                                                    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
                                                    mComment.startAnimation(animation);
                                                    mComment.setTextColor(Color.RED);
                                                    mComment.setText("Scale Disconnect");
                                                }
                                            }
                                        }
                                        else {
                                            if (mScaleStatus != PeripheralStatus.Impossible) {
                                                mScaleStatus = PeripheralStatus.Impossible;
                                                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
                                                mComment.startAnimation(animation);
                                                mComment.setTextColor(Color.RED);
                                                mComment.setText("Printer Impossible");
                                            }
                                            mReleasePort = true;
                                        }

                                        mWaitCallback = false;
                                    }
                                });
                            }
                        }
                    });

                    while (mWaitCallback) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                    }

                    if (mReleasePort) {
                        break;
                    }
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }

            if (mPort != null) {
                try {
                    synchronized (ScaleExtFragment.class) {
                        mScaleStatus = PeripheralStatus.Invalid;
                        StarIOPort.releasePort(mPort);
                        mPort = null;
                    }
                } catch (StarIOPortException e) {
                    // Do nothing
                }
            }
        }
    }
}
