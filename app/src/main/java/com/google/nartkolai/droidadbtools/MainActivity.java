package com.google.nartkolai.droidadbtools;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.nartkolai.droidadbtools.Utils.CopyFiles;
import com.google.nartkolai.droidadbtools.Utils.DialogAlter;
import com.google.nartkolai.droidadbtools.Utils.FsUtil;
import com.google.nartkolai.droidadbtools.Utils.MyPrefHelper;
import com.google.nartkolai.droidadbtools.Utils.MySelectorImpl;
import com.jjnford.android.util.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import name.schedenig.adbcontrol.Config;

public class MainActivity extends AppCompatActivity {
    String[] cmd;
    private FsUtil fsUtil;
    private DialogAlter dialogAlter;
    private TextView textView, tvIp;
    public static final String TAG = "Droid adb tools";
    @SuppressLint("SdCardPath")
    final static String myPath = "/data/data/com.google.nartkolai.droidadbtools/files";
    private String myAdbCmd;
    private String useIpAdrDev = "Devices not selected";
    private int verPref;
    public static String[] myExportPaht = {"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + myPath + "/lib"};
    public static Config config;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialogAlter = new DialogAlter(this);
        verPref = 2;
        String fileName = "ipAdrDev";
        fsUtil = new FsUtil(this, fileName);
        textView = findViewById(R.id.out_text);
        tvIp = findViewById(R.id.txt_current_ip);
        tvIp.setText(useIpAdrDev);
        checkIfAlreadyhavePermission();
        checkIfAlreadyWritehavePermission();
        selectAdbCmdAndCopyBinFiles();
        fsUtil.chkFile();// Check JSON files
        chkConfig();
    }

    @Override
    protected void onResume() {
        selectAdbCmdAndCopyBinFiles();
        super.onResume();
    }


    public void onStartScreenActivity(View view) {
        if ((checkIfAlreadyWritehavePermission() && checkIfAlreadyhavePermission()) || Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startActivity(new Intent(this, ScreenActivity.class));
        } else {
            Toast.makeText(MainActivity.this, "Please give your permission.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    /**
     * @param position item from the list to connect
     * @param longClick long press on the selected item to delete it
     */
    public void selectDevice(String position, Boolean longClick) {
        if(longClick) {
            fsUtil.jsonHelper(position, true);
        }else {
        useIpAdrDev = position;
        tvIp.setText(useIpAdrDev);
       // String[] cmd;
        try {
            cmd = Shell.exec(myAdbCmd + " connect " + useIpAdrDev).split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
        }
    }

    void selectAdbCmdAndCopyBinFiles(){
        /*
         * Adb binary file removed from android version more LOLLIPOP_MR1
         */
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            myAdbCmd = myPath + "/bin/./adb";
            try {
                new FileInputStream(myPath + "/bin/adb");
                new FileInputStream(myPath + "/lib/libcrypto.so");
            } catch (FileNotFoundException e) {
                try {
                    Shell.exec("mkdir " + myPath + "/lib");
                    Shell.exec("mkdir " + myPath + "/bin");
                    Shell.exec("chmod 775 " + myPath + "/bin/");
                    Shell.exec("chmod 775 " + myPath + "/lib/");
                    Shell.exec("cp -f /sdcard/adb/adb" + " " +  myPath + "/bin/adb");
                    Shell.exec("chmod 755 " + myPath + "/bin/adb");
                    Shell.exec("cp -f /sdcard/adb/libcrypto.so" + " " +  myPath + "/lib/libcrypto.so");
                    Shell.exec("chmod 755 " + myPath + "/lib/libcrypto.so");

                } catch (Shell.ShellException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        } else {
            myAdbCmd = "adb";
        }
    }

    /**
     * @param delay setting the pause time between loading a screenshot
     */
    public void setDelay(String delay){
            MyPrefHelper.putPref("screenshotDelay", Integer.valueOf(delay), MainActivity.this);
            config.setScreenshotDelay(Integer.valueOf(delay));
    }
    public void addIpDevices(String ip){
        textView.setText(ip);
        fsUtil.jsonHelper(ip);
    }
    public void actAdbCmd(String shell){
        Log.i(TAG, "from newShell  " + shell);
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            // cmd1[0] = Shell.exec(String.valueOf(shell.getText())).split("\\n+");
            String[] cmd1 = Shell.exec(myAdbCmd + " " + shell).split("\\n+");
            txtSetter(cmd1);
          //  Shell.exec("ls");
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    void getListConnectedDevices(){
        try {
            cmd = null;
            cmd = Shell.exec(myAdbCmd + " devices").split("\\n+");
            txtSetter(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void killAdbServer(){
        try {
            cmd = Shell.exec(myAdbCmd + " kill-server").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    void startAdbServer(){
        try {
            cmd = Shell.exec(myAdbCmd + " start-server").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }

    void adbDisconnectAll(){
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
    void fixSuSetenforceDevice(){
        try {
            cmd = Shell.exec(myAdbCmd + " shell su 0 setenforce 0").split("\\n+");
            txtSetter(cmd);
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
    }


    /*
     * Add menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint({"Assert", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MySelectorImpl mySelector;
        final String[] list = fsUtil.jsonHelperGetItemArr();
        int id = item.getItemId();
        Class[] parameterTypes = new Class[1];
        parameterTypes[0] = String.class;
        Method myMethod = null;
        switch (id) {
            // settings
            case R.id.action_settings:
                try {
                    myMethod = MainActivity.class.getMethod("setDelay", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new MySelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("Delay Screen", String.valueOf(config.getScreenshotDelay()), (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER));
                dialogAlter.displayDialog(mySelector);
                return true;
            // add dev
            case R.id.action_add_dev:
                try {
                    myMethod = MainActivity.class.getMethod("addIpDevices", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new MySelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("Add IP devices", "192.168.", (InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_NORMAL));
                dialogAlter.displayDialog(mySelector);
                return true;
            // Select dev
            case R.id.action_select_dev:
                parameterTypes = new Class[2];
                parameterTypes[0] = String.class;
                parameterTypes[1] = Boolean.class;
                try {
                    myMethod = MainActivity.class.getMethod("selectDevice", String.class, Boolean.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new MySelectorImpl(this, myMethod);
                mySelector.toAlterDialogListItem("Select IP devices","Delete select devices", list);
                dialogAlter.displayDialog(mySelector);
                return true;

            case R.id.action_adb_shell:
                try {
                    myMethod = MainActivity.class.getMethod("actAdbCmd", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new MySelectorImpl(this, myMethod);
                mySelector.toAlterDialogInputValues("adb command","shell input keyevent " + KeyEvent.KEYCODE_HOME, InputType.TYPE_CLASS_TEXT);
                dialogAlter.displayDialog(mySelector);
                return true;

            case R.id.get_list_con_dev:
                getListConnectedDevices();
                return true;

            case R.id.action_adb_kill:
                killAdbServer();
                return true;

            case R.id.action_adb_start:
                startAdbServer();
                return true;

            case R.id.fix_setenforce_dev:
                fixSuSetenforceDevice();
                return true;

            case R.id.action_adb_disconnect:
                adbDisconnectAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @SuppressLint("Assert")
    void txtSetter(String[] cmd) {
        StringBuilder txt = new StringBuilder();
        assert cmd != null;
        for (String s : cmd) {
            txt.append(s).append("\n");
            Log.e(TAG, " txt.append(s) " + s);
        }
        assert false;
        textView.setText(txt.toString());
    }

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
        if(hasAllPermissionsGranted(grantResults)){
            // all permissions granted
            Toast.makeText(MainActivity.this, "All permissions granted.", Toast.LENGTH_LONG).show();
        }else {
            // some permission are denied.
            checkIfAlreadyhavePermission();
            checkIfAlreadyWritehavePermission();
        }
        }

    private boolean checkIfAlreadyhavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int resultRead = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (resultRead != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
            return resultRead == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private boolean checkIfAlreadyWritehavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int resultRead = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (resultRead != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            return resultRead == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

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

}
