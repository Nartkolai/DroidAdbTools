package com.google.nartkolai.droidadbtools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;

import name.schedenig.adbcontrol.AdbHelper;
import name.schedenig.adbcontrol.Config;

import static android.view.Gravity.LEFT;
import static android.view.Gravity.TOP;

public class ScreenActivity extends AppCompatActivity {
    private File file;
    private Config config;
    protected Thread updateThread;
    private AdbHelper adbHelper;
    private int currentApiVersion;
    private static int screenWidth;
    private static int screenHeight;
    float finalHeight, finalWidth, deltaX, deltaY;
    int screenUpY = 0, screenDownX = 0, screenUpX = 0, screenDownY = 0;
    private ImageView imageView;
    private Bitmap myBitmap;

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
        currentApiVersion = android.os.Build.VERSION.SDK_INT;
        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        screenHeight = this.getResources().getDisplayMetrics().heightPixels;
        imageView = findViewById(R.id.imageView);
        initLayoutParams();
        hiddenNaniBar();
        startUpdateThread();
    }


    protected void stopUpdateThread(){
        if(updateThread != null){
            updateThread.interrupt();
            updateThread = null;
        }
    }

    protected void startUpdateThread(){
        if(updateThread == null){
            updateThread = new Thread(){
                @Override
                public void run(){
                    while(!Thread.interrupted()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeScreenshot();
                            }
                            });

                        try{
                            Thread.sleep(config.getScreenshotDelay());
                        }
                        catch(InterruptedException ex){
                            break;
                        }
                    }
                }
            };
            updateThread.start();
        }
    }

    private void makeScreenshot(){
        adbHelper.screenshot(file);
        loadImage();
    }

    private void loadImage(){
        imageView = findViewById(R.id.imageView);
        myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        imageView.setImageBitmap(myBitmap);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                finalHeight = imageView.getMeasuredHeight();
                finalWidth = imageView.getMeasuredWidth();
                return true;
            }
        });
    }



    @SuppressLint("RtlHardcoded")
    public void initLayoutParams() {
        WindowManager.LayoutParams mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0,
                0,
                PixelFormat.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = TOP | LEFT;
        mLayoutParams.x = screenWidth / 2;
        mLayoutParams.y = screenHeight / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float screenWidth = myBitmap.getWidth();
        float screenHeight = myBitmap.getHeight();
        deltaX = screenWidth / finalWidth;
        deltaY = screenHeight / finalHeight;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                screenDownX = (int) (event.getX() * deltaX);
                screenDownY = (int) (event.getY() * deltaY);
                break;
            case MotionEvent.ACTION_UP:
                screenUpX = (int) (event.getX() * deltaX);
                screenUpY = (int) (event.getY() * deltaY);
                int dx = Math.abs(screenDownX - screenUpX);
                int dy = Math.abs(screenDownY - screenUpY);
                if (screenWidth >= screenUpX && screenHeight >= screenUpY) {
                    if (dx < 5 && dy < 5) {
                        adbHelper.sendClick(screenUpX, screenUpY);
                    } else {
                         adbHelper.sendSwipe(screenDownX, screenDownY, screenUpX, screenUpY);
                    }
                }

                break;
        }
        return false;
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
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
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
        stopUpdateThread();
    }

}
