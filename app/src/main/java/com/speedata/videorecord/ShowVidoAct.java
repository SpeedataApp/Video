package com.speedata.videorecord;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ShowVidoAct extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "ShowVidoAct";
    private SurfaceView mSurfaceview;
    private ImageButton mBtnStartStop;
    private boolean mStartedFlg = false;
    private MediaRecorder mRecorder;
    private SurfaceHolder mSurfaceHolder;
    private Camera myCamera;
    private Camera.Parameters myParameters;
    private Camera.AutoFocusCallback mAutoFocusCallback = null;
    private boolean isView = false;
    private TextView tvTime, tvGPS;
    private Timer timer;
    private GPSUtil gpsUtil;
    private BufferedWriter CtrlFile;
    private static final String DEVFILE_PATH = "/sys/class/misc/mtgpio/pin";
    private Handler handler;
    private boolean isflag = true;
    private SurfaceHolder holder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        setContentView(R.layout.activity_main);

        //重写AutoFocusCallback接口
        mAutoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    Log.i(TAG, "AutoFocus: success...");
                } else {
                    Log.i(TAG, "AutoFocus: failure...");
                }
            }
        };
        tvTime = (TextView) findViewById(R.id.capture_textview_information);
        tvGPS = (TextView) findViewById(R.id.tv_gps);
        mSurfaceview = (SurfaceView) findViewById(R.id.capture_surfaceview);
        mBtnStartStop = (ImageButton) findViewById(R.id.ib_stop);


    }

    @Override
    protected void onResume() {
        super.onResume();
        gpsUtil = new GPSUtil();
        gpsUtil.openGPS(getContentResolver(),this);
        gpsUtil.registerGPS();
        // 取得holder
        holder = mSurfaceview.getHolder();

        holder.addCallback(this); // holder加入回调接口

        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        StartTimer();
        tvGPS.setText(gpsUtil.gpsInfo());
        handler=new Handler();
        File DeviceName = new File(DEVFILE_PATH);
        try {
            CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        handler.postDelayed(runnable, 0);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isflag) {
                isflag = false;
                try {
                    CtrlFile.write("-wdout71 1");
                    CtrlFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                isflag = true;
                try {
                    CtrlFile.write("-wdout71 0");
                    CtrlFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            handler.postDelayed(runnable, 500);
        }
    };

    //初始化屏幕设置
    public void initScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        // 设置横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
    }


    //初始化Camera设置
    public void initCamera() {
        if (myCamera == null ) {
            myCamera = Camera.open();
            Log.i(TAG, "camera.open");
        }
        if (myCamera != null  ) {
            try {
                myParameters = myCamera.getParameters();
                myParameters.setPreviewSize(1920, 1080);
                //设置对焦模式
                myParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                myCamera.setParameters(myParameters);
                myCamera.setPreviewDisplay(mSurfaceHolder);
                myCamera.startPreview();
                isView = true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ShowVidoAct.this, "初始化相机错误",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        mSurfaceHolder = holder;
        initCamera();
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        initCamera();
//        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
//
//            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//            @Override
//            public void onClick(View v) {
        // TODO Auto-generated method stub
//            }
        if (!mStartedFlg) {
            // Start
            if (mRecorder == null) {
                mRecorder = new MediaRecorder(); // Create MediaRecorder
            }
            try {
                myCamera.unlock();
                mRecorder.setCamera(myCamera);
                // Set audio and video source and encoder
                // 这两项需要放在setOutputFormat之前
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
                mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

                // Set output file path
                String path = getSDPath();
                if (path != null) {
                    mStartedFlg = true;
                    File dir = new File(path + "/Video");
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    path = dir + "/" + getDate() + ".mp4";
                    mRecorder.setOutputFile(path);
                    Log.d(TAG, "bf mRecorder.prepare()");
                    mRecorder.prepare();
                    Log.d(TAG, "af mRecorder.prepare()");
                    Log.d(TAG, "bf mRecorder.start()");
                    mRecorder.start();   // Recording is now started
                    Log.d(TAG, "af mRecorder.start()");

                    mBtnStartStop.setBackground(getDrawable(R.drawable.rec_stop));
                }
            } catch (Exception e) {
                mStartedFlg = true;
                e.printStackTrace();
            }
        } else {
            // stop
            if (mStartedFlg) {
                try {
                    mRecorder.stop();
                    mRecorder.reset();
                    mBtnStartStop.setBackground(getDrawable(R.drawable.rec_start));
                } catch (Exception e) {
                    mStartedFlg = false;
                    e.printStackTrace();
                }
            }
            mStartedFlg = false;
        }
//        });
    }

    private void StartTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTime.setText(getDate());
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // surfaceDestroyed的时候同时对象设置为null
        if (myCamera!=null){
            myCamera.setPreviewCallback(null);
            myCamera.stopPreview();
            myCamera.release();
            myCamera = null;
            isView=true;
        }
        Log.i(TAG, "surfaceDestroyed: ");
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (timer != null) {
            timer.cancel();
        }
        gpsUtil.fnish();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!isTaskRoot()) {
            finish();
            return;
        }
    }

    /**
     * 获取系统时间，保存文件以系统时间戳命名
     */
    public static String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        int hour = ca.get(Calendar.HOUR);           // 小时
        int second = ca.get(Calendar.SECOND);       // 秒

        String date = year + "-" + (month + 1) + "-" + day + "-" + hour + "-" + minute + "-" + second;
        Log.d(TAG, "date:" + date);

        return date;
    }

    /**
     * 获取SD path
     */
    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
            return sdDir.toString();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            CtrlFile.write("-wdout71 0");
            CtrlFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (gpsUtil!=null){
            gpsUtil.closeGPS(getContentResolver());
        }
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }

    }

}

