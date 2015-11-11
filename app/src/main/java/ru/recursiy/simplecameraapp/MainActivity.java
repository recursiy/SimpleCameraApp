package ru.recursiy.simplecameraapp;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    final int BACK_CAMERA_ID = 0;
    final int FRONT_CAMERA_ID = 1;
    final boolean FULL_SCREEN = true;
    final String VIDEO_STARTED_TAG = "VIDEO_STARTED";
    final String CAMERA_ID_TAG = "CAMERA_ID";
    final String LOG_TAG = "SimpleCamera";

    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;
    boolean isVideoStarted = false;
    int cameraID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
        {
            isVideoStarted = savedInstanceState.getBoolean(VIDEO_STARTED_TAG);
            cameraID = savedInstanceState.getInt(CAMERA_ID_TAG);
        }

        if (!isVideoStarted) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId())
                    {
                        case R.id.startFrontButton:
                            cameraID = FRONT_CAMERA_ID;
                            break;
                        case R.id.startBackButton:
                            cameraID = BACK_CAMERA_ID;
                            break;
                        default:
                            Log.e(LOG_TAG, "Unknown button");
                    }
                    startCamera(cameraID);
                    findViewById(R.id.buttonField).setVisibility(View.INVISIBLE);
                    isVideoStarted = true;
                }
            };
            findViewById(R.id.startFrontButton).setOnClickListener(listener);
            findViewById(R.id.startBackButton).setOnClickListener(listener);
        }
        else
        {
            findViewById(R.id.buttonField).setVisibility(View.INVISIBLE);
        }

        sv = (SurfaceView) findViewById(R.id.surfaceView);

        holder = sv.getHolder();

        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);
    }

    void startCamera(int cameraId)
    {
        camera = Camera.open(cameraId);
        setPreviewSize(FULL_SCREEN);
        holderCallback.startCamera();
    }

    void stopCamera()
    {
        if (camera != null)
            camera.release();
        camera = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isVideoStarted)
            startCamera(cameraID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(VIDEO_STARTED_TAG, isVideoStarted);
        outState.putInt(CAMERA_ID_TAG, cameraID);
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (isVideoStarted)
            {
                startCamera();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            if (isVideoStarted) {
                camera.stopPreview();
                setCameraDisplayOrientation(cameraID);
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

        public void startCamera()
        {
            try {
                setCameraDisplayOrientation(cameraID);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void setPreviewSize(boolean fullScreen) {
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

        boolean widthIsMax = displaySize.x > displaySize.y;

        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        rectDisplay.set(0, 0, displaySize.x, displaySize.y);

        if (widthIsMax) {
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        if (!fullScreen) {
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        matrix.mapRect(rectPreview);

        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }
}