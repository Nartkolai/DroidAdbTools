package com.google.nartkolai.droidadbtools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

import name.schedenig.adbcontrol.AdbHelper;
import name.schedenig.adbcontrol.AndroidKey;
import name.schedenig.adbcontrol.Config;

public class ScreenActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    private boolean debugUi = false;
    private static String TAG = "ScreenActivity";
    private File file;
    private Config config;
    protected Thread updateThread;
    private AdbHelper adbHelper;
    private static int screenWidth;
    float finalHeight, finalWidth, deltaX, deltaY;
    int screenUpY = 0, screenDownX = 0, screenUpX = 0, screenDownY = 0, xMove, xDown;
    private ImageView imageView;
    private Bitmap myBitmap;
    private int swipeZone = 50;
    private View panelViewButton = null;

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
        config = MainActivity.config;
        file = new File(config.getLocalImageFilePath());
        adbHelper = new AdbHelper(config);
        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        imageView = findViewById(R.id.imageView);
        hiddenNaniBar();
        startUpdateThread();
    }


    /**
     * Stop the thread of image acquisition
     */
    protected void stopUpdateThread() {
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
    }

    /**
     * Start the thread of image acquisition
     */
    protected void startUpdateThread() {
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
        imageView = findViewById(R.id.imageView);
        myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        imageView.setImageBitmap(myBitmap);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                finalHeight = imageView.getMeasuredHeight();
                finalWidth = imageView.getMeasuredWidth();
//                if(finalHeight < finalWidth){
//                    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                    setOrientation(1);
//                }else {
//                    setOrientation(0);
//                    //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                }
                return true;
            }
        });
    }

//    public void setOrientation( int i)
//    {
//        Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, i);
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

                float myBitmapWidth = myBitmap.getWidth();
                float myBitmapHeight = myBitmap.getHeight();
                deltaX = myBitmapWidth / finalWidth;
                deltaY = myBitmapHeight / finalHeight;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        xMove = (int) event.getX();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        xDown = (int) event.getX();
                        screenDownX = (int) (event.getX() * deltaX);
                        screenDownY = (int) (event.getY() * deltaY);
                        break;
                    case MotionEvent.ACTION_UP:
                        screenUpX = (int) (event.getX() * deltaX);
                        screenUpY = (int) (event.getY() * deltaY);
                        int dy = Math.abs(screenDownY - screenUpY);
                        if ((dy <= 15) && (xMove != 0 ) && (xDown > (screenWidth - swipeZone))) {// Select the swipe zone
                            xMove = 0;
                            showButtonPanelView();
                        }else {
                                touchHandler(screenDownX, screenDownY, screenUpX, screenUpY);
                        }
                        break;
                }
                return false;
    }

    void touchHandler(int screenDownX, int screenDownY, int screenUpX, int screenUpY) {
        float screenWidth = myBitmap.getWidth();
        float screenHeight = myBitmap.getHeight();
        int dx = Math.abs(screenDownX - screenUpX);
        int dy = Math.abs(screenDownY - screenUpY);
        if (screenWidth >= screenUpX && screenHeight >= screenUpY) {
            if (dx < 5 && dy < 5) {
                if (debugUi) {
                    Toast.makeText(ScreenActivity.this, " UpX " + screenUpX + ",  UpY " + screenUpY, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "tap screenUpX " + screenUpX + ", screenUpY " + screenUpY);
                } else {
                    adbHelper.sendClick(screenUpX, screenUpY);
                }
            } else {
                if (debugUi) {
                    Toast.makeText(ScreenActivity.this, "move UpX " + screenUpX + ", move DownX " + screenDownX, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "move UpX " + screenUpX + ", UpY " + screenUpY);
                    Log.i(TAG, "move DnX " + screenDownX + ", DnY " + screenDownY);
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
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
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
     * Converts dip into its equivalent px
     * @param dpi dpi
     * @return pixel
     */
    int dpiToPx(int dpi){
        Resources r = getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dpi,
                r.getDisplayMetrics()
        );
        return (int) px;
    }

     @SuppressLint("InflateParams")
     void showButtonPanelView(){
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LayoutInflater inflater = getLayoutInflater();
         panelViewButton = inflater.inflate(R.layout.button_layout, null);
         panelViewButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 removeButtonPanelView();
             }
         });
        addContentView(panelViewButton,layoutParams);
    }

    void removeButtonPanelView(){
        if(panelViewButton != null) {
            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.removeView(panelViewButton);
            panelViewButton = null;
        }
    }


    public void keyeventPower(View view){
        sendKeyevent(AndroidKey.POWER);
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

    void sendKeyevent(AndroidKey key){
        if (debugUi) {
            Toast.makeText(ScreenActivity.this, "Key " + key, Toast.LENGTH_LONG).show();
            Log.i(TAG, "Key " + key.name());
        } else {
            adbHelper.sendKey(key);
        }
        removeButtonPanelView();
    }
}
