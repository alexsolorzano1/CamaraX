package com.lab.Camara;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String TAG = "MainActivity";
    private static final int CAMERAID = 0;
    private SurfaceView mSurfaceView;
    private CameraDevice mCamera;
    private SurfaceHolder mSurfaceHolder;
    private ImageReader mImageReader;
    private CameraManager cameraManager;
    private String cameraId = null;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewCaptureRequest;
//Button btnTakePicture;
//ImageView imgView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        Button btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"foto tomada");
                takePicture();
            }
        });
        checkcameras();
        init();


    //    btnCamara = findViewById(R.id.btnTakePicture);
    //    imgView = findViewById(R.id.imageView);
    //    btnCamara.setOnClickListener(new View.OnClickListener() {
    //        @Override
    //        public void onClick(View view) {
    //            abrirCamara();
    //        }
    //    });

    }
    private void init() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView2);
       // mSurfaceView = (SurfaceView) findViewById(R.id.surface_view_camera2_activity);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(surfaceHolderCallback);
        mSurfaceHolder.setFixedSize(1080, 2000);

    }

    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startCameraCaptureSession();
            Log.d(TAG, "start camera session");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format,
                                   int width, int height) {
        }
    };

    private void startCameraCaptureSession() {
        int largestWidth = 1080;
        int largestHeight = 2000;

        mImageReader = ImageReader.newInstance(largestWidth, largestHeight, ImageFormat.JPEG, 1);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try (Image image = reader.acquireNextImage()) {
                    Image.Plane[] planes = image.getPlanes();
                    if (planes.length > 0) {
                        ByteBuffer buffer = planes[0].getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        saveImage(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = cameraManager.getCameraIdList()[CAMERAID];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            cameraManager.openCamera(cameraId, cameraDeviceCallback, null);
            Log.e(TAG, "camera is open");

        } catch (Exception e) {
            Log.e(TAG, "Unable to open the camera", e);
        }

    }

    private CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCamera = null;
            Log.e(TAG, "Camera Error: " + error);
        }
    };

    private void takePreview() {
        if (mCamera == null || mSurfaceHolder.isCreating()) {
            return;
        }

        try {

            Surface previewSurface = mSurfaceHolder.getSurface();

            mPreviewCaptureRequest = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);

            mPreviewCaptureRequest.addTarget(previewSurface);

            mCamera.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    captureSessionCallback,
                    null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera Access Exception", e);
        }

    }

    private void takePicture() {
        if (mCamera == null) return;
        try {
            CaptureRequest.Builder takePictureBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);

            takePictureBuilder.addTarget(mImageReader.getSurface());

            CaptureRequest mCaptureRequest = takePictureBuilder.build();
            mCaptureSession.capture(mCaptureRequest, null, null);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Error capturing the photo", e);
        }
    }

    private void saveImage(byte[] data) {
        // Save the image JPEG data to external storage
        FileOutputStream outStream = null;
        try {
            //String path = Environment.getExternalStorageState().toString();
            String path = getApplicationContext().getCacheDir().toString();
            File outputFile = new File(path, "test4.jpg");

            outStream = new FileOutputStream(outputFile);
            outStream.write(data);
            outStream.close();

            Log.d(TAG, "path:" + outputFile.getAbsolutePath());

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File Not Found", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        }
    }



    private CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCaptureSession = session;
            try {
                mCaptureSession.setRepeatingRequest(
                        mPreviewCaptureRequest.build(),
                        null, // optional CaptureCallback
                        null); // optional Handler
            } catch (CameraAccessException | IllegalStateException e) {
                Log.e(TAG, "Capture Session Exception", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    };

    public void checkcameras() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                Log.d(TAG, cameraId);
                CameraCharacteristics characteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    Log.d(TAG, "back camera");
                    // back camera
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    Log.d(TAG, "front camera");
                    // front camera
                } else {
                    Log.d(TAG, "external camera");
                    // external cameraCameraCharacteristics.LENS_FACING_EXTERNAL
                }

            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}