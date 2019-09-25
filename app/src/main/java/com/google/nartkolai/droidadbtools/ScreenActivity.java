package com.google.nartkolai.droidadbtools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.nartkolai.droidadbtools.Utils.MyPrefHelper;
import com.jjnford.android.util.Shell;

import java.io.File;

import name.schedenig.adbcontrol.AdbHelper;
import name.schedenig.adbcontrol.AndroidKey;
import name.schedenig.adbcontrol.Config;

public class ScreenActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    private boolean debugUi = MainActivity.debugUi;
    private static String TAG = "ScreenActivity";
    private File file;
    private Config config;
    private Thread updateThread;
    private AdbHelper adbHelper;
    private static int screenWidth;
    private float finalHeight;
    private float finalWidth;
    private int screenDownX = 0;
    private int screenDownY = 0;
    private int xMove;
    private int xDown;
    private int yDown;
    private ImageView imageView;
    private Bitmap myBitmap;
    private int swipeZoneSize = 50; //dpi
    private View panelViewButton = null;
    private View panelViewSetSZ = null;
    private LinearLayout linearLayoutSwZonMain = null;
    private TextView textViewDebUiMode = null;
    private boolean showSwipeZone = true;
    private CheckBox checkBoxShowSZ;
    private Context context;
    private int apiOs = MainActivity.apiOs;

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_screen);
        context = this;
        config = MainActivity.config;
        file = new File(config.getLocalImageFilePath());
        adbHelper = new AdbHelper(config);
        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        imageView = findViewById(R.id.imageView);
        swipeZoneSize = MyPrefHelper.getPref("swipeZoneSize", 50, this);
        showSwipeZone = MyPrefHelper.getPref("showSwipeZone", true, this);
        hiddenNaniBar();
        startUpdateThread();
        forDebuggingUi();
        showSwipeZone(swipeZoneSize);
    }

    /**
     * Stop the thread of image acquisition
     */
    private void stopUpdateThread() {
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
    }

    /**
     * Start the thread of image acquisition
     */
    private void startUpdateThread() {
        if (updateThread == null) {
            updateThread = new Thread() {
                @Override
                public void run() {
                    while (!Thread.interrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeScreenshot();
                            }
                        });

                        try {
                            Thread.sleep(config.getScreenshotDelay());
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
            };
            updateThread.start();
        }
    }

    private void makeScreenshot() {
        if (!debugUi) {
            adbHelper.screenshot(file);
        }
        loadImage();
    }


    /**
     * Load image and get its size
     */
    private void loadImage() {
        myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        imageView = findViewById(R.id.imageView);
        if (apiOs < Build.VERSION_CODES.M) {
            switch (getAdbOrientation()) {
                case 1:
                    myBitmap = rotateBitmap(myBitmap, 270);
                    break;
                case 2:
                    myBitmap = rotateBitmap(myBitmap, 180);
                    break;
                case 3:
                    myBitmap = rotateBitmap(myBitmap, 90);
                    break;
            }
        }
        imageView.setImageBitmap(myBitmap);
        finalHeight = imageView.getMeasuredHeight();
        finalWidth = imageView.getMeasuredWidth();
        setOrientation(myBitmap.getWidth(), myBitmap.getHeight());
    }

    /**
     * @param source Input image
     * @param angle  Image rotation angle
     * @return Inverted image
     */
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Change the orientation of the control device
     *
     * @param width  Input image width
     * @param height Input image height
     */
    private void setOrientation(int width, int height) {
        if (width > height) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * @return Current orientation of the managed device
     */
    private int getAdbOrientation() {
        String[] cmd = null;
        try {
            cmd = Shell.exec(config.getAdbCommand() + " shell dumpsys input | grep SurfaceOrientation").split("\\n+");
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
        assert cmd != null;
        String i = cmd[0].substring(cmd[0].length() - 1);
        Log.i(TAG, "getAdbOrientation " + i);
        return Integer.valueOf(i);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float myBitmapWidth = myBitmap.getWidth();
        float myBitmapHeight = myBitmap.getHeight();
        float deltaX = myBitmapWidth / finalWidth;
        float deltaY = myBitmapHeight / finalHeight;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                xMove = (int) event.getX();
                break;
            case MotionEvent.ACTION_DOWN:
                xDown = (int) event.getX();
                yDown = (int) event.getY();
                screenDownX = (int) (event.getX() * deltaX);
                screenDownY = (int) (event.getY() * deltaY);
                break;
            case MotionEvent.ACTION_UP:
                int screenUpX = (int) (event.getX() * deltaX);
                int screenUpY = (int) (event.getY() * deltaY);
                int dx = Math.abs(xDown - (int) event.getX());
                int dy = Math.abs(yDown - (int) event.getY());
//                Log.i(TAG, "--------------------------------------------------------");
//                Log.i(TAG, "myBitmapWidth " + myBitmapWidth + " myBitmapHeight " + myBitmapHeight);
//                Log.i(TAG, "screenWidth " + screenWidth + " swipeZoneSize " + (int) convertDpToPixel(swipeZoneSize, this));
//                Log.i(TAG, "xMove " + xMove + " dx " + dx + " dy " + dy);

                if ((dy < dx) && dx > 50 && xMove != 0 && (xDown > (screenWidth - (int) convertDpToPixel(swipeZoneSize, this)))) {// Select the swipe zone
                    xMove = 0;
                    showButtonPanelView();
                } else {
                    touchHandler(screenDownX, screenDownY, screenUpX, screenUpY);
                }
                break;
        }
        return false;
    }

    /**
     * The handler will select the coordinates of the swipe from the touches, and transfer it for execution
     *
     * @param screenDownX coordinate reduced to the screen size of the managed device
     * @param screenDownY coordinate reduced to the screen size of the managed device
     * @param screenUpX   coordinate reduced to the screen size of the managed device
     * @param screenUpY   coordinate reduced to the screen size of the managed device
     */
    void touchHandler(int screenDownX, int screenDownY, int screenUpX, int screenUpY) {
        float screenWidth = myBitmap.getWidth();
        float screenHeight = myBitmap.getHeight();
        int dx = Math.abs(screenDownX - screenUpX);
        int dy = Math.abs(screenDownY - screenUpY);
        if (screenWidth >= screenUpX && screenHeight >= screenUpY) {
            Log.i(TAG, "move UpX " + screenUpX + ", UpY " + screenUpY);
            Log.i(TAG, "move DnX " + screenDownX + ", DnY " + screenDownY);
            if (dx < 5 && dy < 5) {
                if (debugUi) {
                    Toast.makeText(ScreenActivity.this, " UpX " + screenUpX + ",  UpY " + screenUpY, Toast.LENGTH_SHORT).show();
                } else {
                    adbHelper.sendClick(screenUpX, screenUpY);
                }
            } else {
                if (debugUi) {
                    Toast.makeText(ScreenActivity.this, "move UpX " + screenUpX + ", move DownX " + screenDownX, Toast.LENGTH_SHORT).show();
                } else {
                    adbHelper.sendSwipe(screenDownX, screenDownY, screenUpX, screenUpY);
                }
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void hiddenNaniBar() {

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopUpdateThread();
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     * {@link java.net.URL https://stackoverflow.com/questions/4605527/converting-pixels-to-dp}
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Show button bar/panel
     */
    @SuppressLint({"InflateParams"})
    void showButtonPanelView() {
        if (panelViewButton == null && panelViewSetSZ == null) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            LayoutInflater inflater = getLayoutInflater();
            panelViewButton = inflater.inflate(R.layout.button_layout, null);
            panelViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeButtonPanelView();
                }
            });
            addContentView(panelViewButton, layoutParams);
            Button button = findViewById(R.id.btn_set_swipe_zone);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setUpSwipeZonePanel();
                }
            });
        }
    }

    /**
     * Remove button bar/panel
     */
    void removeButtonPanelView() {
        if (panelViewButton != null) {
            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.removeView(panelViewButton);
            panelViewButton = null;
        }
    }

    /**
     * Show Swipe Zone
     *
     * @param swipeZoneSize size of the swipe zone of the button bar call
     */
    void showSwipeZone(float swipeZoneSize) {
        if (linearLayoutSwZonMain == null && showSwipeZone) {
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            linearLayoutSwZonMain = new LinearLayout(this);
            linearLayoutSwZonMain.setGravity(Gravity.END);
            linearLayoutSwZonMain.setOrientation(LinearLayout.HORIZONTAL);
            // Add Separator
            LinearLayout.LayoutParams llpViewSeparator = new LinearLayout.LayoutParams((int) convertDpToPixel(1f, this), LinearLayout.LayoutParams.MATCH_PARENT);
            LinearLayout linearLayoutSp = new LinearLayout(this);
            View viewSeparator = new View(this);
            viewSeparator.setClickable(false);
            viewSeparator.setBackgroundColor(Color.parseColor("#96FAF6F6"));
            viewSeparator.setLayoutParams(llpViewSeparator);
            linearLayoutSp.addView(viewSeparator);
            // Add Separator Zone
            LinearLayout.LayoutParams llpViewSeparatorZone = new LinearLayout.LayoutParams((int) convertDpToPixel(swipeZoneSize, this), LinearLayout.LayoutParams.MATCH_PARENT);
            LinearLayout linearLayoutSZ = new LinearLayout(this);
            View viewSeparatorZone = new View(this);
            viewSeparatorZone.setBackgroundColor(Color.parseColor("#26000000"));
            viewSeparatorZone.setClickable(false);
            viewSeparatorZone.setLayoutParams(llpViewSeparatorZone);
            linearLayoutSZ.addView(viewSeparatorZone);

            linearLayoutSwZonMain.addView(linearLayoutSp);
            linearLayoutSwZonMain.addView(linearLayoutSZ);
            addContentView(linearLayoutSwZonMain, rlp);
        }
    }

    /**
     * Remove Swipe Zone
     */
    void removeSwipeZone() {
        if (linearLayoutSwZonMain != null) {
            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.removeView(linearLayoutSwZonMain);
            linearLayoutSwZonMain = null;
        }
    }

    /**
     * Show setup swipe zone panel
     */
    @SuppressLint({"InflateParams", "SetTextI18n"})
    void setUpSwipeZonePanel() {
        if (panelViewSetSZ == null) {
            removeButtonPanelView();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            LayoutInflater inflater = getLayoutInflater();
            panelViewSetSZ = inflater.inflate(R.layout.set_swipe_zone_layout, null);
            panelViewSetSZ.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swipeZoneSize = MyPrefHelper.getPref("swipeZoneSize", swipeZoneSize, context);
                    resizeSwipeZone(swipeZoneSize);
                    removeSetUpSwipeZonePanel();
                }
            });
            addContentView(panelViewSetSZ, layoutParams);

            checkBoxShowSZ = findViewById(R.id.show_swipe_zone_check_box);
            Button btnSizeSzMinus = findViewById(R.id.btn_sz_size_minus);
            Button btnSizeSzPlus = findViewById(R.id.btn_sz_size_plus);
            Button btnOk = findViewById(R.id.btn_set_swipe_zone_ok);
            if (showSwipeZone) {
                checkBoxShowSZ.setChecked(true);
            } else {
                checkBoxShowSZ.setChecked(false);
            }
            checkBoxShowSZ.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        checkBoxShowSZ.setText("Show Swipe Zone");
                        showSwipeZone = true;
                        showSwipeZone(swipeZoneSize);
                    } else {
                        checkBoxShowSZ.setText("Hidden Swipe Zone");
                        showSwipeZone = false;
                        removeSwipeZone();
                    }
                }
            });
            btnSizeSzMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swipeZoneSize = resizeSwipeZone(swipeZoneSize - 5);
                }
            });
            btnSizeSzPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    swipeZoneSize = resizeSwipeZone(swipeZoneSize + 5);
                }
            });
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyPrefHelper.putPref("swipeZoneSize", swipeZoneSize, context);
                    MyPrefHelper.putPref("showSwipeZone", showSwipeZone, context);
                    removeSetUpSwipeZonePanel();
                }
            });
        }

    }

    /**
     * Resize Swipe Zone
     *
     * @param size new value swipe zone size
     * @return current value swipe zone size
     */
    int resizeSwipeZone(int size) {
        removeSwipeZone();
        showSwipeZone(size);
        return size;
    }

    /**
     * Remove setup swipe zone panel
     */
    void removeSetUpSwipeZonePanel() {
        if (panelViewSetSZ != null) {
            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.removeView(panelViewSetSZ);
            panelViewSetSZ = null;
        }

    }

    /**
     * Shows warning text about disabling adb commands
     */
    @SuppressLint("SetTextI18n")
    void forDebuggingUi() {
        if (debugUi && textViewDebUiMode == null) {
            RelativeLayout.LayoutParams rlpMain = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            LinearLayout linearLayoutText = new LinearLayout(this);
            linearLayoutText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            textViewDebUiMode = new TextView(this);
            textViewDebUiMode.setText("Debug UI Mode (adb command disabled)");
            textViewDebUiMode.setTextSize(20);
            textViewDebUiMode.setTextColor(Color.RED);
            linearLayoutText.addView(textViewDebUiMode);
            addContentView(linearLayoutText, rlpMain);
        }
    }

    public void keyeventBack(View view) {
        sendKeyevent(AndroidKey.BACK);
    }

    public void keyeventHome(View view) {
        sendKeyevent(AndroidKey.HOME);
    }

    public void keyeventMenu(View view) {
        sendKeyevent(AndroidKey.MENU);
    }

    public void onExit(View view) {
        removeButtonPanelView();
        finish();
    }

    void sendKeyevent(AndroidKey key) {
        if (debugUi) {
            Toast.makeText(ScreenActivity.this, "Key " + key, Toast.LENGTH_LONG).show();
            Log.i(TAG, "Key " + key.name());
        } else {
            adbHelper.sendKey(key);
        }
        removeButtonPanelView();
    }
}
