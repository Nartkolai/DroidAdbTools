package com.nartkolai.droidadbtools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjnford.android.util.Shell;
import com.nartkolai.droidadbtools.Utils.AlterDialogHelper;
import com.nartkolai.droidadbtools.Utils.AlterDialogSelectorImpl;
import com.nartkolai.droidadbtools.Utils.JSONUtil;
import com.nartkolai.droidadbtools.Utils.MyPrefHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import name.schedenig.adbcontrol.Config;

public class MainActivity extends AppCompatActivity {
    static boolean debugUi = false;
    static int sdkOs;
    private String[] cmd;
    private JSONUtil jsonUtil;
    private AlterDialogHelper alterDialogHelperBuilder;
    private AlertDialog dialogAlterShow;
    private TextView textView;
    private TextView tvIp;
    private boolean adbStatStop = true;
    private boolean initP;
    public static final String TAG = MainActivity.class.getSimpleName();
    @SuppressLint("SdCardPath")
    final static String MY_PATH = "/data/data/com.nartkolai.droidadbtools/files";
    final static String ADB_VENDOR_KEYS_PATH = MY_PATH + "/adbkey";
    final static String ADB_LD_LIBRARY_PATH = MY_PATH + "/lib";
    final static String ADB_BIN_PATH = MY_PATH + "/bin";
//    final static String BIN_PATH = "/system/bin";
//    final static String LIB_PATH = "/system/lib";
    private String myAdbCmd;
    public static String useIpAdrDev;
    private String outText;
    private int verPref;
    public static String[] myExportPath;
    public static Config config;
    int REQUEST_CODE = 101;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verPref = 2;
        chkConfig();
        System.out.println("MY_PATH " + MY_PATH);
        if (savedInstanceState != null) {
            adbStatStop = savedInstanceState.getBoolean("adbStatStop");
            initP = savedInstanceState.getBoolean("initP");
            outText = savedInstanceState.getString("outText");
            useIpAdrDev = savedInstanceState.getString("useIpAdrDev");
            String s = savedInstanceState.getString("adbCommand");
            config.setAdbCommand(s);
        }
        setContentView(R.layout.activity_main);
        alterDialogHelperBuilder = new AlterDialogHelper(this);
        dialogAlterShow = new AlterDialogHelper(this);
        textView = findViewById(R.id.out_text);
        tvIp = findViewById(R.id.txt_current_ip);
        Button btnStartScreenActiv = findViewById(R.id.btn_screen_activity);
        textView.setText(outText);
        String str;
        if (useIpAdrDev == null) {
            str = "Devices not selected";
        } else {
            str = "Selected " + useIpAdrDev.substring(3) + " IP";
        }
        tvIp.setText(str);
        String fileName = "ipAdrDev";
        jsonUtil = new JSONUtil(this, fileName);
        initParam();
        chkStartAdb();
        btnStartScreenActiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSdkOs() > 0) {
                    debugUi = false;
                    sdkOs = getSdkOs();
                    onStartScreenActivity();
                }
            }
        });
        btnStartScreenActiv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startDbgUi();
                return false;
            }
        });
        if (MyPrefHelper.getPref("adbCommand", "adb", this) == null) {
            chkConfig();
        }
      //  startDbgUi();
    }

    void startDbgUi(){
        sdkOs = Build.VERSION_CODES.M;
        debugUi = true;
        onStartScreenActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogAlterShow != null && dialogAlterShow.isShowing()) {
            dialogAlterShow.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Store UI state to the savedInstanceState.
        savedInstanceState.putString("useIpAdrDev", useIpAdrDev);
        savedInstanceState.putString("outText", outText);
        savedInstanceState.putBoolean("adbStatStop", chkStartAdb());
        savedInstanceState.putBoolean("initP", initParam());
        savedInstanceState.putString("adbCommand", config.getAdbCommand());
    }

    /**
     * @return Initialization of the main parameters
     */
    boolean initParam() {
        if (!initP) {
            jsonUtil.chkFile();// Check JSON files
            checkIfAlreadyhavePermission();
            checkIfAlreadyWritehavePermission();
            selectAdbCmdAndCopyBinFiles();
            initP = true;
            return true;
        }
        return true;
    }


    /**
     * Conditional check of a not running adb server
     *
     * @return conditional status not running adb server
     */
    boolean chkStartAdb() {
        if (adbStatStop) {
            adbStartServer();
            adbStatStop = false;
            return false;
        }
        return false;
    }

    /**
     * Start Screen Activity
     */
    void onStartScreenActivity() {
        if ((checkIfAlreadyWritehavePermission() && checkIfAlreadyhavePermission() /*&& checkWriteSettingsPermission()*/) || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(this, ScreenActivity.class);
            intent.putExtra("sdkOs", sdkOs);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "Please give your permission.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Adb binary file removed from android version more LOLLIPOP_MR1
     */
    void selectAdbCmdAndCopyBinFiles() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1/* || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1*/) {
            myAdbCmd = ADB_BIN_PATH + "/./adb";
            String[] copyLibs = {"libcrypto.so"/*, "libc.so", "libdl.so", "libm.so", "libstdc++.so", "libc++.so"*/};

            MyPrefHelper.putPref("adbCommand", myAdbCmd, this);
            try {
                new FileInputStream(ADB_BIN_PATH + "/adb");
            } catch (FileNotFoundException e) {
                try {
                    myExportPath = null;
                    Shell.exec("mkdir " + ADB_BIN_PATH);
                    Shell.exec("chmod 775 " + ADB_BIN_PATH);
                    Shell.exec("cp -f /sdcard/adb/adb" + " " + ADB_BIN_PATH + "/adb");
                    Shell.exec("chmod 755 " + ADB_BIN_PATH + "/adb");
                } catch (Shell.ShellException e1) {
                    e1.printStackTrace();
                }
                Log.e(TAG, "adb bin no found " + e);
            }
            for (String copyLib : copyLibs) {
                try {
                    new FileInputStream(ADB_LD_LIBRARY_PATH + "/" + copyLib);
                } catch (FileNotFoundException e) {
                    try {
                        myExportPath = null;
                        Shell.exec("mkdir " + ADB_LD_LIBRARY_PATH);
                        Shell.exec("chmod 775 " + ADB_LD_LIBRARY_PATH);
                        Shell.exec("cp -f /sdcard/adb/" + copyLib + " " + ADB_LD_LIBRARY_PATH + "/" + copyLib);
                        Shell.exec("chmod 755 " + ADB_LD_LIBRARY_PATH + "/" + copyLib);
                    } catch (Shell.ShellException e1) {
                        e1.printStackTrace();
                    }
                    Log.e(TAG, "Lib " + copyLib + " no found " + e);
                }
            }
            try {
                new FileInputStream(ADB_VENDOR_KEYS_PATH + "/public");
            } catch (FileNotFoundException e) {
                try {
                    myExportPath = null;
                    Shell.exec("mkdir " + ADB_VENDOR_KEYS_PATH);
                    Shell.exec("chmod 775 " + ADB_VENDOR_KEYS_PATH);
                    myExportPath = new String[]{"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + ADB_LD_LIBRARY_PATH + "/",
                            "ADB_VENDOR_KEYS=$ADB_VENDOR_KEYS:" + ADB_VENDOR_KEYS_PATH + "/"};
                    adbActionKeygen();
                } catch (Shell.ShellException e1) {
                    e1.printStackTrace();
                }
                Log.e(TAG, "RSA key no found " + e);
            }
            myExportPath = new String[]{"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + ADB_LD_LIBRARY_PATH + "/",
                    "ADB_VENDOR_KEYS=$ADB_VENDOR_KEYS:" + ADB_VENDOR_KEYS_PATH + "/"};
        } else {
            myExportPath = null;
            myAdbCmd = "adb";
            MyPrefHelper.putPref("adbCommand", myAdbCmd, this);
        }
    }


    /**
     * Method called by alternative dialogue, device selection
     * @param position  item from the list to connect
     * @param longClick long press on the selected item to delete it
     */
    @SuppressLint("SetTextI18n")
    public void selectDevice(String position, Boolean longClick, Boolean addDev) {
        if (longClick && !addDev) {
            jsonUtil.jsonHelper(position, true);
        } else if (position != null && !addDev) {
            useIpAdrDev = " -s " + position + ":5555";
            tvIp.setText("Selected " + useIpAdrDev.substring(3) + " IP");
            if (adbStatStop) {
                adbStartServer();
            }
            try {
                myAdbCmd = MyPrefHelper.getPref("adbCommand", "adb", this);
                Shell.setOutputStream(Shell.OUTPUT.STDOUT);
                String s = myAdbCmd + " connect " + useIpAdrDev.substring(4);
                System.out.println("myAdbCmd " + myAdbCmd);
                cmd = Shell.exec(s).split("\\n+");
                myAdbCmd += useIpAdrDev;
                config.setAdbCommand(myAdbCmd);
                txtSetter(cmd);
            } catch (Shell.ShellException e) {
                e.printStackTrace();
            }
        }
        if (addDev) {
            AlterDialogSelectorImpl mySelector;
            Class[] parameterTypes = new Class[1];
            parameterTypes[0] = String.class;
            Method myMethod = null;
            try {
                myMethod = MainActivity.class.getMethod("addIpDevices", parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            mySelector = new AlterDialogSelectorImpl(this, myMethod);
            mySelector.toAlterDialogInputValues("Add IP devices", "192.168.", (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_NORMAL));
            mySelector.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
            dialogAlterShow.show();
        }
    }

    /**
     * @param delay setting the pause time between loading a screenshot
     */
    public void setDelay(String delay) {
        MyPrefHelper.putPref("screenshotDelay", Integer.valueOf(delay), MainActivity.this);
        config.setScreenshotDelay(Integer.valueOf(delay));
    }

    /**
     * @param ip add new IP address devices in list
     */
    public void addIpDevices(String ip) {
        outText = ip;
        textView.setText(ip);
        jsonUtil.jsonHelper(ip);
        actionSelectDevices();
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
            String[] cmd1 = Shell.exec(myAdbCmd + " " + shell).split("\\n+");
            txtSetter(cmd1);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command "devices", if send command "devices -l" unpredictable result possible
     */
    void getListConnectedDevices() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            cmd = null;
            System.out.println("myAdbCmd " + myAdbCmd);
            cmd = Shell.exec(myAdbCmd + " devices").split("\\n+");
            txtSetter(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command "kill-server"
     */
    void killAdbServer() {
        adbStatStop = true;
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(myAdbCmd + " kill-server").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send adb command  "disconnect"
     */
    void adbDisconnectAll() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(myAdbCmd + " disconnect").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }


    /**
     * Use for ROOT devices that do not respond to actions.
     */
    void fixSuSetenforceDevice() {
        if (adbStatStop) {
            adbStartServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(myAdbCmd + " shell su 0 setenforce 0").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    /**
     * Device Select Dialog
     */
    void actionSelectDevices() {
        final String[] list = jsonUtil.jsonHelperGetItemArr();
        AlterDialogSelectorImpl mySelector;
        Method myMethod = null;
        try {
            myMethod = MainActivity.class.getMethod("selectDevice", String.class, Boolean.class, Boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        mySelector = new AlterDialogSelectorImpl(this, myMethod);
        mySelector.toAlterDialogListItem("Select IP devices", "Delete select devices", list);
        dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
        dialogAlterShow.show();
    }

    void adbActionKeygen() {
        try {
            ///private
            Shell.setOutputStream(Shell.OUTPUT.STDERR);
            cmd = Shell.exec(myAdbCmd + " keygen " + ADB_VENDOR_KEYS_PATH + "/public").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }


    /**
     * Create menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            MenuItem item = menu.findItem(R.id.action_keygen);
            item.setVisible(false);
        }
        return true;
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        AlterDialogSelectorImpl mySelector;
        Class[] parameterTypes = new Class[1];
        parameterTypes[0] = String.class;
        Method myMethod = null;
        switch (id) {
            // Settings
            case R.id.action_settings:
                try {
                    myMethod = MainActivity.class.getMethod("setDelay", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new AlterDialogSelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("Delay Screen", String.valueOf(config.getScreenshotDelay()), (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER));
                dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
                dialogAlterShow.show();
                return true;
            // Select devices
            case R.id.action_select_dev:
                actionSelectDevices();
                return true;
            //Send adb shell command
            case R.id.action_adb_shell:
                actionAdbCommand();
                return true;
            // Get list connected devices
            case R.id.get_list_con_dev:
                getListConnectedDevices();
                return true;
            //Kill adb server
            case R.id.action_adb_kill:
                killAdbServer();
                return true;
            //Start adb server
            case R.id.action_adb_start:
                adbStartServer();
                return true;
            //Fix setenforce dev (Rooted)
            case R.id.fix_setenforce_dev:
                fixSuSetenforceDevice();
                return true;
            //Disconnect all adb devices
            case R.id.action_adb_disconnect:
                adbDisconnectAll();
                return true;
            //Generate adb public/private key
            case R.id.action_keygen:
                adbActionKeygen();
                return true;
            //Exit
            case R.id.action_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("SdCardPath")
    void actionAdbCommand() {
        Method myMethod = null;
        AlterDialogSelectorImpl mySelector;
        try {
            myMethod = MainActivity.class.getMethod("adbSendCmd", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        String s;
        mySelector = new AlterDialogSelectorImpl(this, myMethod);
//         s = "shell/sdcard/adb_screenshot.png";
        s = "shell input keyevent ";
        mySelector.toAlterDialogInputValues("adb command", s + KeyEvent.KEYCODE_POWER, InputType.TYPE_CLASS_TEXT);
        dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
        dialogAlterShow.show();
    }

    /**
     * @param cmd result of a command sent for execution
     */
    @SuppressLint("Assert")
    void txtSetter(String[] cmd) {
        StringBuilder txt = new StringBuilder();
        assert cmd != null;
        for (String s : cmd) {
            //WARNING: linker: Warning: unable to normalize "$LD_LIBRARY_PATH" (ignoring)
//            String s1 = "WARNING: linker: Warning: unable to normalize \"$LD_LIBRARY_PATH\"";
            String s1 = "WARNING: linker: Warning: unable to normalize \"$LD";
            if (s.length() >= 50 && s.substring(0, 50).equals(s1)) {
                return;
            }
            txt.append(s).append("\n");
            Log.e(TAG, " txtSetter " + s);
        }
        assert false;
        outText = txt.toString();
        textView.setText(txt.toString());
    }

    /**
     * @param grantResults the grant results for the corresponding permissions
     * @return result check all permissions
     */
    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (hasAllPermissionsGranted(grantResults)) {
            // all permissions granted
            Toast.makeText(MainActivity.this, "All permissions granted.", Toast.LENGTH_LONG).show();
        } else {
            checkIfAlreadyhavePermission();
            checkIfAlreadyWritehavePermission();
        }
    }

    /**
     * @return result check permissions READ EXTERNAL STORAGE
     */
    private boolean checkIfAlreadyhavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int resultRead = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (resultRead != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
            return resultRead == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * @return result check permissions WRITE EXTERNAL STORAGE
     */
    private boolean checkIfAlreadyWritehavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int resultRead = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (resultRead != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
            return resultRead == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Check preferences, and init config
     */
    @SuppressLint("SdCardPath")
    private void chkConfig() {
        int version = BuildConfig.VERSION_CODE;
        if (MyPrefHelper.getPref("version_pref", 0, this) < verPref) {
            MyPrefHelper.putPref("version_pref", verPref, this);
            MyPrefHelper.putPref("version", version, this);
            MyPrefHelper.putPref("adbCommand", myAdbCmd, this);
            MyPrefHelper.putPref("screenshotDelay", 3000, this);
            MyPrefHelper.putPref("localImageFilePath", getCacheDir() + "/screenshot.png", this);
            MyPrefHelper.putPref("phoneImageFilePath", "/sdcard/adb_screenshot.png", this);
        }
        config = new Config();
        config.setAdbCommand(MyPrefHelper.getPref("adbCommand", "adb", this));
        config.setLocalImageFilePath(MyPrefHelper.getPref("localImageFilePath", " ", this));
        config.setPhoneImageFilePath(MyPrefHelper.getPref("phoneImageFilePath", " ", this));
        config.setScreenshotDelay(MyPrefHelper.getPref("screenshotDelay", 3000, this));
    }


    /**
     * Start adb server implemented without waiting for a response from the process, as this leads to a hang of weak devices
     */
    private void adbStartServer() {
        try {
            Runtime.getRuntime().exec(myAdbCmd + " start-server", myExportPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Obtaining an OS version for further work with a screenshot. In SDK versions below 22, screenshots on ADB are taken without reference to screen orientation.
     *
     * @return OS version SDK
     */
    int getSdkOs() {
        int sdkv = 0;
        myAdbCmd = config.getAdbCommand();
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            System.out.println("getSdkOs " + myAdbCmd);
            cmd = Shell.exec(myAdbCmd + " shell getprop ro.build.version.sdk ").split("\\n+");
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
            AlterDialogSelectorImpl mySelector;
            mySelector = new AlterDialogSelectorImpl(this, null);
            mySelector.toAlterDialogNoItem("Error", "Error selecting device. More than one device/emulator.");
            dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
            dialogAlterShow.show();
        }
        return sdkv;
    }

    @Override
    protected void onResume() {
        selectAdbCmdAndCopyBinFiles();
        super.onResume();
    }
}
