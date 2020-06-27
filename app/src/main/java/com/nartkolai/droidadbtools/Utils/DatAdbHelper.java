package com.nartkolai.droidadbtools.Utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;

import com.jjnford.android.util.Shell;
import com.nartkolai.droidadbtools.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import name.schedenig.adbcontrol.Config;

public class DatAdbHelper implements DroidAdbHelper {
    private static final String TAG = DatAdbHelper.class.getSimpleName();

    private boolean adbStatStop = true;
    private Context context;
    private String[] cmd;
    private JSONUtil jsonUtil;
    private static String useIpAdrDev;
    private AlterDialogHelper alterDialogHelperBuilder;
    private AlertDialog dialogAlterShow;


    @SuppressLint("SdCardPath")
    private final static String MY_PATH = "/data/data/com.nartkolai.droidadbtools/files";
    private final static String ADB_VENDOR_KEYS_PATH = MY_PATH + "/adbkey";
    private final static String ADB_LD_LIBRARY_PATH = MY_PATH + "/lib";
    private final static String ADB_BIN_PATH = MY_PATH + "/bin";
    private final static String SOURCE_ADB_PATH = "/sdcard/adb";

    private String adbCmd;
    private String[] exportPath;
    private Config config = MainActivity.config;

    public DatAdbHelper(Context context){
        this.context = context;
        jsonUtil = new JSONUtil(context, "ipAdrDev");
        alterDialogHelperBuilder = new AlterDialogHelper(context);
        dialogAlterShow = new AlterDialogHelper(context);
    }

    /**
     * Adb binary file removed from android version more LOLLIPOP_MR1
     */
    public AdbCfg selectAdbCmdAndCopyBinFiles() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            adbCmd = ADB_BIN_PATH + "/./adb";
            String[] copyLibs = {"libcrypto.so", "libc.so", "libdl.so", "libm.so", "libstdc++.so"};
            exportPath = new String[]{"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + ADB_LD_LIBRARY_PATH + "/",
                    "ADB_VENDOR_KEYS=$ADB_VENDOR_KEYS:" + ADB_VENDOR_KEYS_PATH + "/adbkey:" + ADB_VENDOR_KEYS_PATH + "/adbkey.pub"};
            MyPrefHelper.putPref("adbCommand", adbCmd, context);
            try {
                new FileInputStream(ADB_BIN_PATH + "/adb");
            } catch (FileNotFoundException e) {
                try {
                    if (!new File(ADB_BIN_PATH).mkdirs()) {
                        Log.i(TAG, "Dir " + ADB_BIN_PATH + " not created");
                    }
                    File adbBin = new File(ADB_BIN_PATH + "/adb");
                    exportPath = null;
                    FsUtil.copyFile(new File(SOURCE_ADB_PATH + "/adb"), adbBin);
                    FsUtil.chmodFile(adbBin);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Log.e(TAG, "adb bin no found " + e);
            }
            for (String copyLib : copyLibs) {
                try {
                    new FileInputStream(ADB_LD_LIBRARY_PATH + "/" + copyLib);
                } catch (FileNotFoundException e) {
                    try {
                        if (new File(ADB_LD_LIBRARY_PATH).mkdirs()) {
                            Log.e(TAG, "Dir " + ADB_LD_LIBRARY_PATH + " not created");
                        }
                        FsUtil.copyFile(new File(SOURCE_ADB_PATH + "/" + copyLib),
                                new File(ADB_LD_LIBRARY_PATH + "/" + copyLib));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    Log.e(TAG, "Lib " + copyLib + " no found " + e);
                }
            }
            try {
                new FileInputStream(ADB_VENDOR_KEYS_PATH + "/public");
            } catch (FileNotFoundException e) {
                adbActionKeygen();
                Log.e(TAG, "RSA key no found " + e);
            }
        } else {
            exportPath = new String[]{"PATH=$PATH:/system/bin",
                    "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/system/lib",
                    "ADB_VENDOR_KEYS=$ADB_VENDOR_KEYS:" + ADB_VENDOR_KEYS_PATH + "/:" + ADB_VENDOR_KEYS_PATH + "/public:" + ADB_VENDOR_KEYS_PATH + "/public.pub"};
            adbCmd = "adb";
            MyPrefHelper.putPref("adbCommand", adbCmd, context);
        }
        return new AdbCfg(adbCmd, exportPath);
    }


    /**
     * Conditional check of a not running adb server
     *
     * @return conditional status not running adb server
     */
    public boolean chkStartAdb() {
        if (adbStatStop) {
            adbStartServer();
            adbStatStop = false;
            return false;
        }
        return false;
    }

    /**
     * Start adb server implemented without waiting for a response from the process, as this leads to a hang of weak devices
     */
    public void adbStartServer() {
        try {
            Runtime.getRuntime().exec(adbCmd + " start-server", exportPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method called by alternative dialogue, device selection
     *
     * @param position  item from the list to connect
     * @param longClick long press on the selected item to delete it
     */
    @SuppressLint("SetTextI18n")
    public void selectDevice(String position, Boolean longClick, Boolean addDev) {
        if (longClick && !addDev) {
            jsonUtil.jsonHelper(position, true);
        } else if (position != null && !addDev) {
            useIpAdrDev = " -s " + position + ":5555";
            //tvIp.setText("Selected " + useIpAdrDev.substring(3) + " IP");
            if (adbStatStop) {
                adbStartServer();
            }
            try {
                adbCmd = MyPrefHelper.getPref("adbCommand", "adb", context);
                Shell.setOutputStream(Shell.OUTPUT.STDOUT);
                String s = adbCmd + " connect " + useIpAdrDev.substring(4);
                System.out.println("myAdbCmd " + adbCmd);
                cmd = Shell.exec(s).split("\\n+");
                adbCmd += useIpAdrDev;
                config.setAdbCommand(adbCmd);
                txtSetter(cmd);
            } catch (Shell.ShellException e) {
                e.printStackTrace();
            }
        }
        if (addDev) {
            AlterDialogSelector mySelector;
            Class[] parameterTypes = new Class[1];
            parameterTypes[0] = String.class;
            Method myMethod = null;
            try {
                myMethod = MainActivity.class.getMethod("addIpDevices", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            mySelector = new MySelectorHelper(this, myMethod);
            mySelector.toAlterDialogInputValues("Add IP devices", "192.168.", (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_NORMAL));
            mySelector.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
            dialogAlterShow.show();
        }
    }

    /**
     * @param shell send adb shell command
     */
    public void adbSendCmd(String shell) {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            String[] cmd1 = Shell.exec(cmd + " " + shell).split("\\n+");
            txtSetter(cmd1);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command "devices", if send command "devices -l" unpredictable result possible
     */
    public void getListConnectedDevices() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            cmd = null;
            System.out.println("myAdbCmd " + adbCmd);
            cmd = Shell.exec(adbCmd + " devices").split("\\n+");
            txtSetter(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command "kill-server"
     */
    public void killAdbServer() {
        adbStatStop = true;
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(adbCmd + " kill-server").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command  "disconnect"
     */
    public void adbDisconnectAll() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(adbCmd + " disconnect").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }


    /**
     * Use for ROOT devices that do not respond to actions.
     */
    public void fixSuSetenforceDevice() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(adbCmd + " shell su 0 setenforce 0").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    public void adbActionKeygen() {
        if (new File(ADB_VENDOR_KEYS_PATH).mkdirs()) {
            Log.e(TAG, "Dir " + ADB_VENDOR_KEYS_PATH + " not created");
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(adbCmd + " keygen " + ADB_VENDOR_KEYS_PATH + "/public").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtaining an OS version for further work with a screenshot. In SDK versions below 22, screenshots on ADB are taken without reference to screen orientation.
     *
     * @return OS version SDK
     */
    public int getSdkOs() {
        int sdkv = 0;
        adbCmd = config.getAdbCommand();
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            System.out.println("getSdkOs " + adbCmd);
            cmd = Shell.exec(adbCmd + " shell getprop ro.build.version.sdk ").split("\\n+");
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
        try {
            for (String s : cmd) {
                System.out.println("sdkv " + s);
                sdkv = Integer.valueOf(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e);
            AlterDialogSelector mySelector;
            mySelector = new MySelectorHelper(this, null);
            mySelector.toAlterDialogNoItem("Error", "Error selecting device. More than one device/emulator.");
            dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
            dialogAlterShow.show();
        }
        return sdkv;
    }

    @Override
    public String[] txtSetter(String[] s) {
        return s;
    }
}
