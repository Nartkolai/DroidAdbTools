package com.google.nartkolai.droidadbtools;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.nartkolai.droidadbtools.Utils.AlterDialogHelper;
import com.google.nartkolai.droidadbtools.Utils.JSONUtil;
import com.google.nartkolai.droidadbtools.Utils.MyPrefHelper;
import com.google.nartkolai.droidadbtools.Utils.AlterDialogSelectorImpl;
import com.jjnford.android.util.Shell;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import name.schedenig.adbcontrol.Config;

public class MainActivity extends AppCompatActivity {
    static boolean debugUi = false;
    static int apiOs;
    private String[] cmd;
    private JSONUtil jsonUtil;
    private AlterDialogHelper alterDialogHelperBuilder;
    private AlertDialog dialogAlterShow;
    private TextView textView, tvIp;
    private boolean adbStatStop = true;
    private boolean initP;
    public static final String TAG = "Droid adb tools";
    @SuppressLint("SdCardPath")
    final static String myPath = "/data/data/com.google.nartkolai.droidadbtools/files";
    private String myAdbCmd;
    private String useIpAdrDev = "Devices not selected";
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
        if (savedInstanceState != null) {
            adbStatStop = savedInstanceState.getBoolean("adbStatStop");
            initP = savedInstanceState.getBoolean("initP");
            outText = savedInstanceState.getString("outText");
            useIpAdrDev = savedInstanceState.getString("useIpAdrDev");
        }
        setContentView(R.layout.activity_main);
        alterDialogHelperBuilder = new AlterDialogHelper(this);
        dialogAlterShow = new AlterDialogHelper(this);
        textView = findViewById(R.id.out_text);
        tvIp = findViewById(R.id.txt_current_ip);
        Button btnStartScreenActiv = findViewById(R.id.btn_screen_activity);
        textView.setText(outText);
        tvIp.setText(useIpAdrDev);
        String fileName = "ipAdrDev";
        jsonUtil = new JSONUtil(this, fileName);
        System.out.println("onCreate chkStartAdb " + adbStatStop);
        initParam();
        chkConfig();
        chkStartAdb();
        btnStartScreenActiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getApiOs() > 0) {
                    debugUi = false;
                    apiOs = getApiOs();
                    onStartScreenActivity();
                }
            }
        });
        btnStartScreenActiv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                apiOs = Build.VERSION_CODES.M;
                debugUi = true;
                onStartScreenActivity();
                return false;
            }
        });
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (dialogAlterShow !=null && dialogAlterShow.isShowing()){
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
    }

    /**
     * @return Initialization of the main parameters
     */
    boolean initParam() {
        if (!initP) {
            System.out.println("initParam() ");
            verPref = 2;
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
     * @return conditional status not running adb server
     */
    boolean chkStartAdb() {
        if (adbStatStop) {
            startAdbServer();
            adbStatStop = false;
            return false;
        }
        return false;
    }

    /**
     * Start Screen Activity
     */
     void onStartScreenActivity() {
        if ((checkIfAlreadyWritehavePermission() && checkIfAlreadyhavePermission() /*&& checkWriteSettingsPermission()*/) || Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startActivity(new Intent(this, ScreenActivity.class));
        } else {
            Toast.makeText(MainActivity.this, "Please give your permission.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Adb binary file removed from android version more LOLLIPOP_MR1
     */
    void selectAdbCmdAndCopyBinFiles() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            myAdbCmd = myPath + "/bin/./adb";
            myExportPath = new String[]{"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + myPath + "/lib"};
            try {
                new FileInputStream(myPath + "/bin/adb");
                new FileInputStream(myPath + "/lib/libcrypto.so");
            } catch (FileNotFoundException e) {
                try {
                    Shell.exec("mkdir " + myPath + "/lib");
                    Shell.exec("mkdir " + myPath + "/bin");
                    Shell.exec("chmod 775 " + myPath + "/bin/");
                    Shell.exec("chmod 775 " + myPath + "/lib/");
                    Shell.exec("cp -f /sdcard/adb/adb" + " " + myPath + "/bin/adb");
                    Shell.exec("chmod 755 " + myPath + "/bin/adb");
                    Shell.exec("cp -f /sdcard/adb/libcrypto.so" + " " + myPath + "/lib/libcrypto.so");
                    Shell.exec("chmod 755 " + myPath + "/lib/libcrypto.so");
                } catch (Shell.ShellException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        } else {
            myExportPath = null;
            myAdbCmd = "adb";
        }
    }


    /**
     * @param position  item from the list to connect
     * @param longClick long press on the selected item to delete it
     */
    @SuppressLint("SetTextI18n")
    public void selectDevice(String position, Boolean longClick) {
        if (longClick) {
            jsonUtil.jsonHelper(position, true);
        } else {
            useIpAdrDev = position;
            tvIp.setText("Ip " + useIpAdrDev + " selected");
            if (adbStatStop) {
                startAdbServer();
            }
            try {
                cmd = Shell.exec(myAdbCmd + " connect " + useIpAdrDev).split("\\n+");
                txtSetter(cmd);
            } catch (Shell.ShellException e) {
                e.printStackTrace();
            }
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
    }

    /**
     * @param shell send adb shell command
     */
    public void actAdbCmd(String shell) {
        if (adbStatStop) {
            startAdbServer();
        }
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
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
            startAdbServer();
        }
        try {
            cmd = null;
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
            startAdbServer();
        }
        try {
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
            startAdbServer();
        }
        try {
            cmd = Shell.exec(myAdbCmd + " shell su 0 setenforce 0").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }


    /*
     * Create menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String[] list = jsonUtil.jsonHelperGetItemArr();
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
            // Add devices
            case R.id.action_add_dev:
                try {
                    myMethod = MainActivity.class.getMethod("addIpDevices", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new AlterDialogSelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("Add IP devices", "192.168.", (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_NORMAL));
                dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
                dialogAlterShow.show();
                return true;
            // Select devices
            case R.id.action_select_dev:
                parameterTypes = new Class[2];
                parameterTypes[0] = String.class;
                parameterTypes[1] = Boolean.class;
                try {
                    myMethod = MainActivity.class.getMethod("selectDevice", String.class, Boolean.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new AlterDialogSelectorImpl(this, myMethod);
                mySelector.toAlterDialogListItem("Select IP devices", "Delete select devices", list);
                dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
                dialogAlterShow.show();
                return true;
            //Send adb shell command
            case R.id.action_adb_shell:
                try {
                    myMethod = MainActivity.class.getMethod("actAdbCmd", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new AlterDialogSelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("adb command", "shell input keyevent " + KeyEvent.KEYCODE_HOME, InputType.TYPE_CLASS_TEXT);
                dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
                dialogAlterShow.show();
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
                startAdbServer();
                return true;
            //Fix setenforce dev (Rooted)
            case R.id.fix_setenforce_dev:
                fixSuSetenforceDevice();
                return true;
            //Disconnect all adb devices
            case R.id.action_adb_disconnect:
                adbDisconnectAll();
                return true;
            //Exit
            case R.id.action_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * @param cmd result of a command sent for execution
     */
    @SuppressLint("Assert")
    void txtSetter(String[] cmd) {
        StringBuilder txt = new StringBuilder();
        assert cmd != null;
        for (String s : cmd) {
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
            int resultRead = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (resultRead != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
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
            MyPrefHelper.putPref("localImageFilePath", "/sdcard/adbcontrol_screenshot.png", this);
            MyPrefHelper.putPref("phoneImageFilePath", "/sdcard/adbcontrol_screenshot.png", this);
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
    private void startAdbServer() {
        try {
            Runtime.getRuntime().exec(myAdbCmd + " start-server", myExportPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Obtaining an OS version for further work with a screenshot. In API versions below 22, screenshots on ADB are taken without reference to screen orientation.
     * @return OS version API
     */
    int getApiOs(){
        int api = 0;
        try {
            cmd = Shell.exec(myAdbCmd + " shell getprop ro.build.version.sdk ").split("\\n+");
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
        try {
            api = Integer.valueOf(cmd[0]);
        }catch (Exception e){
            Log.e(TAG,"" + e);
            AlterDialogSelectorImpl mySelector;
            mySelector = new AlterDialogSelectorImpl(this, null);
            mySelector.toAlterDialogNoItem("Error", "Error selecting device. More than one device/emulator.");
            dialogAlterShow = alterDialogHelperBuilder.displayDialog(mySelector);
            dialogAlterShow.show();
        }
        return api;
    }

    @Override
    protected void onResume() {
        selectAdbCmdAndCopyBinFiles();
        super.onResume();
    }
}
