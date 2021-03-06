package com.nartkolai.droidadbtools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjnford.android.util.Shell;
import com.nartkolai.droidadbtools.Utils.MakeStubBitmap;
import com.nartkolai.droidadbtools.Utils.MyPrefHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import name.schedenig.adbcontrol.AdbHelper;
import name.schedenig.adbcontrol.AndroidKey;
import name.schedenig.adbcontrol.Config;

public class ScreenActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    private boolean debugUi = MainActivity.debugUi;
    private static String TAG = "ScreenActivity";
    private File file;
    private File tmpFile;
    private Config config;
    private Handler updateHandler;
    private Runnable runnable;
    private AdbHelper adbHelper;
    private static int screenWidth;
    //    private static int screenHeight;
    private float finalHeight;
    private float finalWidth;
    private int screenDownX = 0;
    private int screenDownY = 0;
    private int xMove;
    private int xDown;
    private int yDown;
    private float deltaX;
    private float deltaY;
    private ImageView imageView;
    private Bitmap myBitmap;
    private float myBitmapWidth;
    private float myBitmapHeight;
    private int swipeZoneSize = 50; //dpi
    private View panelViewButton = null;
    private View panelViewSetSZ = null;
    private LinearLayout linearLayoutSwZonMain = null;
    private TextView textViewDebUiMode = null;
    private boolean showSwipeZone = true;
    private CheckBox checkBoxShowSZ;
    private Context context;
    private int sdkOs/* = MainActivity.sdkOs*/;
    private ScreenAsyncTask screenAsyncTask;
    private boolean updateOrientation = true;
    private ProgressBar progressBar;
    private TextView progressBarText;
    @SuppressLint("SimpleDateFormat")
    final DateFormat DATE_FORMAT = new SimpleDateFormat("mm:ss.SSS");

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate()");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_screen);
        context = this;
        config = MainActivity.config;
        file = new File(config.getLocalImageFilePath());
        tmpFile = new File(config.getLocalImageFilePath() + ".tmp");
        adbHelper = new AdbHelper(config);
        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
//        screenHeight = this.getResources().getDisplayMetrics().heightPixels;
        imageView = findViewById(R.id.imageView);
        swipeZoneSize = MyPrefHelper.getPref("swipeZoneSize", 50, this);
        showSwipeZone = MyPrefHelper.getPref("showSwipeZone", true, this);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        progressBarText = findViewById(R.id.progressBar_text);
//        progressBarText.setVisibility(View.INVISIBLE);
        Intent intent = getIntent();
        sdkOs = intent.getIntExtra("sdkOs", 0);
        hiddenNaniBar();
        startUpdateRunnable();
        forDebuggingUi();
        showSwipeZone(swipeZoneSize);
        myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    /**
     * Stop the thread of image acquisition
     */
    private void stopUpdateRunnable() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(runnable);
            updateHandler = null;
        }
    }

    /**
     * Start the thread of image acquisition
     */
    private void startUpdateRunnable() {
        runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("_____________________________ " + DATE_FORMAT.format(Calendar.getInstance().getTime()) + " _________________________________________");
                System.out.println("UpdateRunnable");
                makeScreenshot();
                updateHandler.postDelayed(this, config.getScreenshotDelay());
            }
        };
        if (updateHandler == null) {
            updateHandler = new Handler();
            updateHandler.postDelayed(runnable, config.getScreenshotDelay());
        }
    }

    private void makeScreenshot() {
        if (!debugUi) {
            if (updateOrientation) {
                screenAsyncTask = new ScreenAsyncTask(this);
                screenAsyncTask.execute(tmpFile);
            }
        }
        loadImage();
    }


    /**
     * Load image and get its size
     */
    private void loadImage() {
        System.out.println("pre loadImage " + DATE_FORMAT.format(Calendar.getInstance().getTime()));
        if (myBitmap == null) {
            String str = "Loading Image";
            if (debugUi) str = "Stub image";
            myBitmap = new MakeStubBitmap(str, this).getBitmap();
        }
        if (updateOrientation || debugUi) {
            if (sdkOs < Build.VERSION_CODES.M) {
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
            myBitmapWidth = myBitmap.getWidth();
            myBitmapHeight = myBitmap.getHeight();
            deltaX = myBitmapWidth / finalWidth;
            deltaY = myBitmapHeight / finalHeight;
            setOrientation(myBitmap.getWidth(), myBitmap.getHeight());
            updateOrientation = false;
        }
        imageView.setImageBitmap(myBitmap);
        finalHeight = imageView.getMeasuredHeight();
        finalWidth = imageView.getMeasuredWidth();
        System.out.println("pos loadImage " + DATE_FORMAT.format(Calendar.getInstance().getTime()));
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
        View relativeLayoutScreenView = findViewById(R.id.relative_layout_screen_view);
//        View screenSeparatorViewHorizontal = findViewById(R.id.separator_horizontal);
//        View screenSeparatorViewVertical = findViewById(R.id.separator_vertical);
//        System.out.println("getWidth() " + relativeLayoutScreenView.getWidth());
//        System.out.println("  width    " + width);
//        System.out.println("getHeight() " + relativeLayoutScreenView.getHeight());
//        System.out.println("  height    " + height);
        double ratioScreenView = relativeLayoutScreenView.getWidth() / relativeLayoutScreenView.getHeight();
        double ratioScreenImg = (double) width / (double) height;
        double ratioWidth;
        double ratioHeight;

        if (ratioScreenView > ratioScreenImg) {
            ratioWidth = relativeLayoutScreenView.getWidth() / (double) width;
            ratioHeight = relativeLayoutScreenView.getHeight() / (double) height;
//            System.out.println("in >");
        } else {
            ratioWidth = (double) width / relativeLayoutScreenView.getWidth();
            ratioHeight = (double) height / relativeLayoutScreenView.getHeight();
//            System.out.println("in <");
        }
        double deltaWidth = ratioWidth / ratioHeight;
        double deltaHeight = ratioHeight / ratioWidth;
//        System.out.println("coefWidth " + coefWidth);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(
                (int) (relativeLayoutScreenView.getWidth() * deltaWidth),
                (int) (relativeLayoutScreenView.getHeight() * deltaHeight)
        ));
        if (width < height) {
//            screenSeparatorViewHorizontal.setVisibility(View.GONE);
//            screenSeparatorViewHorizontal.setVisibility(View.INVISIBLE);
            System.out.println("SCREEN_ORIENTATION_PORTRAIT");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
//            screenSeparatorViewHorizontal.setVisibility(View.GONE);
//            screenSeparatorViewHorizontal.setVisibility(View.INVISIBLE);
            System.out.println("SCREEN_ORIENTATION_LANDSCAPE");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * @return Current orientation of the managed device
     */
    private int getAdbOrientation() {
        String[] cmd = null;
        try {
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            cmd = Shell.exec(config.getAdbCommand() + " shell dumpsys input | grep SurfaceOrientation").split("\\n+");
        } catch (Shell.ShellException e) {
            e.printStackTrace();
        }
        assert cmd != null;
        String i = cmd[0].substring(cmd[0].length() - 1);
        System.out.println("getAdbOrientation " + DATE_FORMAT.format(Calendar.getInstance().getTime()));
        return Integer.valueOf(i);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

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
                if ((dy < dx) && dx > 50 && xMove != 0 && (xDown > (screenWidth - (int) convertDpToPixel(swipeZoneSize)))) {// Select the swipe zone
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
        int dx = Math.abs(screenDownX - screenUpX);
        int dy = Math.abs(screenDownY - screenUpY);
        if (myBitmapWidth >= screenUpX && myBitmapHeight >= screenUpY) {
            Log.i(TAG, "DnX " + screenDownX + ", DnY " + screenDownY);
            Log.i(TAG, "UpX " + screenUpX + ", UpY " + screenUpY);
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
        removeButtonPanelView();
        stopScreenAsyncTask();
        stopUpdateRunnable();
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     * {@link java.net.URL https://stackoverflow.com/questions/4605527/converting-pixels-to-dp}
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public float convertDpToPixel(float dp) {
        return dp * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
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
            LinearLayout.LayoutParams llpViewSeparator = new LinearLayout.LayoutParams((int) convertDpToPixel(1f), LinearLayout.LayoutParams.MATCH_PARENT);
            LinearLayout linearLayoutSp = new LinearLayout(this);
            View viewSeparator = new View(this);
            viewSeparator.setClickable(false);
            viewSeparator.setBackgroundColor(Color.parseColor("#96FAF6F6"));
            viewSeparator.setLayoutParams(llpViewSeparator);
            linearLayoutSp.addView(viewSeparator);
            // Add Separator Zone
            LinearLayout.LayoutParams llpViewSeparatorZone = new LinearLayout.LayoutParams((int) convertDpToPixel(swipeZoneSize), LinearLayout.LayoutParams.MATCH_PARENT);
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
        stopScreenAsyncTask();
        stopUpdateRunnable();
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

    void stopScreenAsyncTask() {
        if (screenAsyncTask != null) {
            screenAsyncTask.cancel(true);
            screenAsyncTask = null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ScreenAsyncTask extends AsyncTask<File, Integer, File> {//Todo
        Context context;

        private WeakReference<ScreenActivity> activityReference;

        // only retain a weak reference to the activity
        ScreenAsyncTask(ScreenActivity context) {
            this.context = context;
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarText.setText("0");
        }

        @Override
        protected File doInBackground(File... file) {
            InputStream input = null;
            OutputStream output = null;
            int fileLength = alterAdbScreenCap();

            try {
                alterAdbScreenPull(file[0]);
                input = new FileInputStream(file[0]);
                output = new FileOutputStream(config.getLocalImageFilePath());
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) { // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
            }

//            adbHelper.screenshot(file[0]);
            return file[0];
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setIndeterminate(false);
            progressBar.setProgress(values[0]);
            progressBarText.setText(values[0].toString());
//            System.out.println("Screen size " + values[0]);
        }

        @Override
        protected void onPostExecute(File newFile) {
            super.onPostExecute(newFile);
            // get a reference to the activity if it is still there
            ScreenActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // modify the activity's UI
//            if (newFile.renameTo(file)) {
//                System.out.println("Rename files");
//            }
            progressBar.setProgress(0);
            myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            updateOrientation = true;
            // access Activity member variables
        }
    }


    int alterAdbScreenCap() {
        String[] cmd;
        try {
            String myAdbCmd = config.getAdbCommand();
            Shell.exec(myAdbCmd + " shell screencap -p " + config.getPhoneImageFilePath());
            Shell.setOutputStream(Shell.OUTPUT.STDOUT);
            System.out.println("alterAdbScreenCap " + DATE_FORMAT.format(Calendar.getInstance().getTime()));
            cmd = Shell.exec(myAdbCmd + " shell ls -l " + config.getPhoneImageFilePath()).split("\\s+");
            for (int i = 0; i < cmd.length; i++) {
                if (i == 3) {
//                    System.out.println("Screen size " + cmd[i]);
                    return Integer.valueOf(cmd[i]);
                }
            }
        } catch (Shell.ShellException e) {
            Log.e(TAG, "Error adb shell: " + e);
            e.printStackTrace();
        }
        return 0;
    }

    void alterAdbScreenPull(File file) {
        try {
            String myAdbCmd = config.getAdbCommand();
            Shell.exec(myAdbCmd + " pull " + config.getPhoneImageFilePath()
                    + " " + file.getAbsolutePath());
            System.out.println("alterAdbScreenPull " + DATE_FORMAT.format(Calendar.getInstance().getTime()));
        } catch (Shell.ShellException e) {
            Log.e(TAG, "Error pull image: " + e);
            e.printStackTrace();
        }
    }
}


