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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import name.schedenig.adbcontrol.Config;

public class MainActivity extends AppCompatActivity {
    private FsUtil fsUtil;
    private DialogAlter dialogAlter;
    private TextView textView, tvIp;
    public static final String TAG = "Droid adb tools";
    @SuppressLint("SdCardPath")
    final static String myPath = "/data/data/com.google.nartkolai.droidadbtools/files";
    private String myAdbCmd;
    private String useIpAdrDev = "Devices not selected";
    private int verPref;
    //public static String[] paht = {"PATH=$PATH:" + myPath};
    public static Config config;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialogAlter = new DialogAlter(this);
        verPref = 1;
        String fileName = "ipAdrDev";
        fsUtil = new FsUtil(this, fileName);
        textView = findViewById(R.id.out_text);
        tvIp = findViewById(R.id.txt_current_ip);
        tvIp.setText(useIpAdrDev);
        checkIfAlreadyhavePermission();

        /*
         * Adb binary file removed from android version more LOLLIPOP_MR1
         */
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            myAdbCmd = myPath + "/./adb";
            try {
                this.openFileInput("adb");
            } catch (FileNotFoundException e) {
                try {
                    String[] cmd;
                    CopyFiles.copyFile(this, (myPath + "/adb"));
                    cmd = Shell.exec("chmod 775 " + myPath + "/adb").split("\\n+");
                    txtSetter(cmd);
                } catch (IOException ignore) {
                } catch (Shell.ShellException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
        } else {
            myAdbCmd = "adb";
        }
        fsUtil.chkFile();// Check JSON files
        chkConfig();
    }


    public void onStartScreenActivity(View view) {
        if (checkIfAlreadyhavePermission() || Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startActivity(new Intent(this, ScreenActivity.class));
        } else {
            Toast.makeText(MainActivity.this, "Please give your permission.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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
        String[] cmd;
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
    public void setDelay(String delay){
            MyPrefHelper.putPref("screenshotDelay", Integer.valueOf(delay), MainActivity.this);
    }
    public void addIpDevices(String ip){
        textView.setText(ip);
        fsUtil.jsonHelper(ip);
    }
    public void actAdbCmd(String shell){
        Log.i(TAG, "from newShell  " + shell);
        try {
            // cmd1[0] = Shell.exec(String.valueOf(shell.getText())).split("\\n+");
            String[] cmd1 = Shell.exec(myAdbCmd + " " + shell).split("\\n+");
            txtSetter(cmd1);
            Shell.exec("ls");
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
        final String[][] cmd = {{""}};
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
                mySelector.setNeedInput(true);
                mySelector.setTilts("Delay Screen");
                mySelector.setText("" + MyPrefHelper.getPref("screenshotDelay", 1000, MainActivity.this));
                mySelector.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_CLASS_NUMBER);
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
                mySelector.setNeedInput(true);
                mySelector.setTilts("Add IP devices");
                mySelector.setText("192.168.");
                mySelector.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_VARIATION_NORMAL);
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
                mySelector.setNeedInput(false);
                mySelector.setTilts("Select IP devices");
                mySelector.setSubTilts("Delete select devices");
                mySelector.setItemList(list);
                dialogAlter.displayDialog(mySelector);
                return true;

            case R.id.get_list_con_dev:
                try {
                    // cmd[0] = Shell.exec("adb devices").split("\\n+");
                    cmd[0] = Shell.exec(myAdbCmd + " devices").split("\\n+");
                    Log.i(TAG, "cmd.length " + cmd[0].length);
                    txtSetter(cmd[0]);
                } catch (Shell.ShellException e) {
                    e.printStackTrace();
                }
                return true;

            case R.id.action_adb_kill:
                try {
                    cmd[0] = Shell.exec(myAdbCmd + " kill-server").split("\\n+");
                    txtSetter(cmd[0]);
                } catch (Shell.ShellException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.action_adb_start:
                try {
                    // cmd = Shell.sudo("ls").split("\\n+");
                    cmd[0] = Shell.exec(myAdbCmd + " start-server").split("\\n+");
                    txtSetter(cmd[0]);
                } catch (Shell.ShellException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.action_adb_shell:
                try {
                    myMethod = MainActivity.class.getMethod("actAdbCmd", parameterTypes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                mySelector = new MySelectorImpl(this, myMethod);
                mySelector.setNeedInput(true);
                mySelector.setTilts("adb command");
                mySelector.setText("pull /sdcard/adbcontrol_screenshot.png /sdcard/adbcontrol_screenshot.png");
                mySelector.setInputType(InputType.TYPE_CLASS_TEXT);
                dialogAlter.displayDialog(mySelector);
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
        Log.e(TAG, "cmd txtSetter " + txt.toString());
        textView.setText(txt.toString());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String chkPermissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, chkPermissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length <= 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Please give your permission.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkIfAlreadyhavePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
            return result == PackageManager.PERMISSION_GRANTED;
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
            MyPrefHelper.putPref("screenshotDelay", 1000, this);
            MyPrefHelper.putPref("localImageFilePath", "/sdcard/adbcontrol_screenshot.png", this);
            MyPrefHelper.putPref("phoneImageFilePath", "/sdcard/adbcontrol_screenshot.png", this);
        }
        config = new Config();
        config.setAdbCommand(MyPrefHelper.getPref("adbCommand", "adb", this));
        config.setLocalImageFilePath(MyPrefHelper.getPref("localImageFilePath", " ", this));
        config.setPhoneImageFilePath(MyPrefHelper.getPref("phoneImageFilePath", " ", this));
        config.setScreenshotDelay(MyPrefHelper.getPref("screenshotDelay", 1000, this));
    }

}
