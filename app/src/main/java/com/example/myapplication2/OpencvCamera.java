package com.example.myapplication2;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import  org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Date;

public class OpencvCamera extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private ImageView ic_baseline_videocam_24;
    private ImageView take_picture;
    private MediaRecorder recorder;
    private int take_video = 0;
    private int take_image = 0;
    private int video_photo = 0;
    Mat mRGBA;
    Mat mRGBAT;
    Mat mROI;

    CameraBridgeViewBase cameraBridgeViewBase;
    File cascFile;
    CascadeClassifier faceDetector;


    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "onManagerConnected: Opencv loaded");
                    //cameraBridgeViewBase.enableView();

                    //**********************
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    cascFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");

                    FileOutputStream fos = new FileOutputStream(cascFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    is.close();
                    fos.close();

                    faceDetector = new CascadeClassifier(cascFile.getAbsolutePath());

                    if (faceDetector.empty()) {
                        faceDetector = null;
                    } else {
                        cascadeDir.delete();
                    }
                    cameraBridgeViewBase.enableView();
                }
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(OpencvCamera.this, new String[]{Manifest.permission.CAMERA}, 1);
        ActivityCompat.requestPermissions(OpencvCamera.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(OpencvCamera.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        setContentView(R.layout.activity_opencv_camera);
        if (ContextCompat.checkSelfPermission(OpencvCamera.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(OpencvCamera.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_surface);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        recorder = new MediaRecorder();
        ic_baseline_videocam_24 = findViewById(R.id.baseline_videocam_24);
        take_picture = findViewById(R.id.baseline_motion_photos_on_24);
        ic_baseline_videocam_24.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ic_baseline_videocam_24.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ic_baseline_videocam_24.setColorFilter(Color.WHITE);
                    if (video_photo == 0) {
                        take_picture.setImageResource(R.drawable.ic_baseline_motion_photos_on_24);
                        take_picture.setColorFilter(Color.WHITE);
                        video_photo = 1;
                    } else {
                        take_picture.setImageResource(R.drawable.ic_baseline_motion_photos_on_24);
                        video_photo = 0;
                    }
                    return true;
                }
                return false;
            }
        });

        take_picture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (video_photo == 0) {
                        if (take_image == 0) {
                            take_picture.setColorFilter(Color.DKGRAY);
                        }
                    }
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (video_photo == 1) {
                        if (take_video == 0) {
                            try {
//começar a gravar
//criar pasta
                                File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/My_Application2");
//checar se existe a pasta, ou criar uma nova
                                boolean success = true;
                                if (!folder.exists()) {
                                    success = folder.mkdir();
                                }
                                take_picture.setImageResource(R.drawable.ic_baseline_motion_photos_on_24);
                                take_picture.setColorFilter(Color.RED);
                                //recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                                //CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                                //recorder.setProfile(camcorderProfile);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                                String current_Date_and_time = sdf.format(new Date());
                                String filename = Environment.getExternalStorageDirectory().getPath() + "/My_Application2/" + current_Date_and_time + ".mp4";
                                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                                recorder.setVideoSize(480, 720);
                                //recorder.setVideoFrameRate(15);
                                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
                                recorder.setOutputFile(filename);
                                recorder.prepare();
                                cameraBridgeViewBase.setRecorder(recorder);
                                recorder.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            take_video = 1;
                        } else {
                            take_picture.setImageResource(R.drawable.ic_baseline_motion_photos_on_24);
                            take_picture.setColorFilter(Color.WHITE);
                            cameraBridgeViewBase.setRecorder(null);
                            recorder.stop();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            take_video = 0;
                        }
                    } else {
                        take_picture.setColorFilter(Color.WHITE);
                        if (take_image == 0) {
                            take_image = 1;
                        } else {
                            take_image = 0;
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if request is denied, this will return an empty array
        switch(requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    cameraBridgeViewBase.setCameraPermissionGranted();
                }
                else{
                    //permisiion denied
                }
                return;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
//            if load success
            Log.d(TAG, "onResume: Opencv initialized");
            try {
                baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            Log.d(TAG, "onResume: Opencv not initialized");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat();
        mRGBAT = new Mat(height, width, CvType.CV_8UC1);

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBAT = inputFrame.gray();
        Imgproc.cvtColor(mRGBA, mRGBA, Imgproc.COLOR_RGBA2BGRA);
        List<Mat> channels = new ArrayList<Mat>();
        Mat mInter = new Mat(mRGBA.width(), mRGBA.height(), CvType.CV_8UC4);
        Mat buffer = new Mat();
        Mat frame = new Mat() ;
        Mat frame_blue = new Mat() ;
        Mat frame_red = new Mat() ;

        Core.split(mRGBA, channels);
        frame.put(mInter);
        //detectando cara
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(mRGBA,faceDetections);
        int size = frame.size();

        for (Rect rect: faceDetections.toArray()){

            Imgproc.rectangle(mRGBA, new Point(rect.x, rect.y),//rect.x, rect.y
                    new Point(rect.x + 200, rect.y + 65),//rect.width, rect.y +rect.height
                    new Scalar(0,0,255), 3);//azul,verde,vermelho

            mROI = mRGBA.submat((rect.y),(rect.y + 65),rect.x,rect.x + 200);

           Core.absdiff(mROI,frame, buffer);

            frame_blue.add(channels.get(0).clone());
            frame_red.add(channels.get(2).clone());

            Log.i(TAG, String.valueOf(frame_red));
            frame.add(mROI.clone());
        }
       Log.i(TAG, Integer.toString(frame.size()));

        return mRGBA;
    }

}
//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