package com.gunsoft.facerecognitionopencv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gunsoft.facerecognitionopencv.service.BlockingService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class LockActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static String app = "";

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    private Tutorial3View   mOpenCvCameraView;
    com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer faceRecognizer;

    private Mat mRgba;
    private Mat mGray;
    private CascadeClassifier mJavaDetector;

    PersonRecognizer fr;

    private int mAbsoluteFaceSize   = 0;
    private float mRelativeFaceSize   = 0.2f;
    private int mDetectorType       = JAVA_DETECTOR;
    private File                   mCascadeFile;
    private String[]               mDetectorName;

    private int mLikely=999;

    int countImages=0;

    Bitmap mBitmap;

    String mPath="";

    static final long MAXIMG = 10;

    public LockActivity()
    {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //   System.loadLibrary("detection_based_tracker");



                    fr=new PersonRecognizer(mPath);
                    String s = getResources().getString(R.string.Straininig);
                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    fr.load();

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        //                 mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    if(mOpenCvCameraView.isFrontCameraAvailable())
                    {
                        // use front camera first run
                        mOpenCvCameraView.setCameraIndex(org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT );
                    }
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;


            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mPath=getFilesDir()+"/facerecogOCV/";
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lock, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void unlock(View view)
    {
        BlockingService.getMyService().shutdownService();
        app = "com.bbm";
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BlockingService.getMyService().startService();
        mOpenCvCameraView.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //preventing default implementation previous to android.os.Build.VERSION_CODES.ECLAIR
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Mat rotImage = Imgproc.getRotationMatrix2D(new Point(mRgba.cols() / 2,
                mRgba.rows() / 2), 90, 1.0);

        Imgproc.warpAffine(mRgba, mRgba, rotImage, mRgba.size());

        Imgproc.warpAffine(mGray, mGray, rotImage, mRgba.size());

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            //  mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
//            if (mNativeDetector != null)
//                mNativeDetector.detect(mGray, faces);
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();

        if ((facesArray.length>0)&&(countImages<MAXIMG))
        {


            Mat m=new Mat();
            Rect r=facesArray[0];


            m=mRgba.submat(r);
            mBitmap = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);


            Utils.matToBitmap(m, mBitmap);
            // SaveBmp(mBitmap,"/sdcard/db/I("+countTrain+")"+countImages+".jpg");


            fr.predict(m);
            mLikely=fr.getProb();

            if (mLikely < 0);
            else if (mLikely < 50)
            {
                BlockingService.getMyService().shutdownService();
                app = "com.bbm";
                finish();
            }
            else if (mLikely < 80);
            else;


        }
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        return mRgba;
    }
}
