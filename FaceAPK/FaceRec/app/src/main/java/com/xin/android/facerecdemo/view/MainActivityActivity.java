package com.xin.android.facerecdemo.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.grg.idcard.IDCardMsg;
import com.grg.idcard.IDCardRecognition;
import com.grg.idcard.IDCardRecognition.IDCardRecListener;
import com.xin.android.facerecdemo.R;
import com.xin.android.facerecdemo.bean.FaceInfo;
import com.xin.android.facerecdemo.bean.ImageData;
import com.xin.android.facerecdemo.bean.Rect;
import com.xin.android.facerecdemo.util.Constant;
import com.xin.android.facerecdemo.util.JniLoader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivityActivity extends Activity implements CvCameraViewListener2 {

    private final String TAG = getClass().getSimpleName();
    private final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private CameraBridgeViewBase mOpenCvCameraView;

    private IDCardRecognition mIDCardRecognition;
    private IDCardMsg lastCardMsg = null;

    private static final String LOCK_BITMAP = "lock_bitmap";

    private Mat mRgba;
    private Mat mGray;
    private Mat mFlipRgba;
    private Mat mSelectedRgb;
    private Mat mSelectedGray;

    private boolean updateImage = true;
    private boolean isFaceComparing = false;

    private Bitmap mSelectedBitmap;
    private Bitmap mIdBitmap;
    private ImageView mCurrentImage;
    private ImageView mIdImage;
    private TextView mMultFaceMessage;

    private TextView mIdName;
    private TextView mIdSex;
    private TextView mIdNation;
    private TextView mIdBirthday;
    private TextView mIdNumber;
    private TextView mIdAddress;
    private TextView mIdSignOffice;
    private TextView mIdUsefuDate;

    private String gPath;
    private int mHeight = 0;
    private int mWidth = 0;

    private boolean isFrontCamera = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
//                    System.loadLibrary("detection_based_tracker");
//                    System.loadLibrary("native-lib");

                    mOpenCvCameraView.enableView();

                    mIDCardRecognition = new IDCardRecognition(MainActivityActivity.this, myHandler,
                            mIDCardRecListener);
                    mIDCardRecognition.start();

                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Camera.getCameraInfo(camIdx, cameraInfo);
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            Log.d(TAG, "device has back camera!");
//                            break;
                        }

                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            Log.d(TAG, "device has front camera!");
                            isFrontCamera = true;
                            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                            break;
                        }
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_activity);
        findViews();

        gPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/FaceRec/";
        int result;
        result = JniLoader.getInstance().callInitFaceRec(1, gPath);
        Log.d(TAG, "callInitFaceRec result == " + result);
        if (result != 0) {
            JniLoader.getInstance().callInitFaceRec(1, gPath);
        }
    }

    private void findViews() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cv_camera_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mCurrentImage = (ImageView) findViewById(R.id.current_image);
        mIdImage = (ImageView) findViewById(R.id.id_image);
        mMultFaceMessage = (TextView) findViewById(R.id.mult_face_message);

        mIdName = (TextView) findViewById(R.id.id_name_info);
        mIdSex = (TextView) findViewById(R.id.id_sex_info);
        mIdNation = (TextView) findViewById(R.id.id_nation_info);
        mIdBirthday = (TextView) findViewById(R.id.id_birthday_info);
        mIdNumber = (TextView) findViewById(R.id.id_number_info);
        mIdAddress = (TextView) findViewById(R.id.id_address_info);
        mIdSignOffice = (TextView) findViewById(R.id.id_sign_office_info);
        mIdUsefuDate = (TextView) findViewById(R.id.id_useful_data_info);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        if (mIDCardRecognition != null) {
            mIDCardRecognition.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mFlipRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mFlipRgba.release();
        if (mSelectedRgb != null) {
            mSelectedRgb.release();
            mSelectedGray.release();
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mHeight == 0 || mWidth == 0) {
            mHeight = mOpenCvCameraView.getHeight();
            mWidth = mOpenCvCameraView.getWidth();
        }

//        Log.d(TAG, "start faceDetection");

        if (!isFaceComparing) {
            ArrayList<FaceInfo> faceList = faceDetection();
//        Log.d(TAG, "end faceDetection");

            if (faceList.size() == 1 && updateImage) {
                Rect rect = faceList.get(0).getBbox();

                Log.d(TAG,"faceInfo rect == " + rect.toString());
                int faceCenterX = rect.getX() + rect.getWidth()/2;
                int faceCenterY = rect.getY() + rect.getHeight()/2;

                if ( mWidth/2 - 50 < faceCenterX && faceCenterX< mWidth/2 + 50 &&
                        mHeight/2 -50 < faceCenterY && faceCenterY <mHeight/2 + 50) {
                    updateImage = false;
//            synchronized (LOCK_BITMAP) {
                    mSelectedBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap
                            .Config.RGB_565);
                    mSelectedRgb = mRgba.clone();
                    mSelectedGray = mGray.clone();

                    Log.d(TAG, "mRgba dataAddr == " + mRgba.dataAddr());
                    Log.d(TAG, "mSelectedRgb dataAddr == " + mSelectedRgb.dataAddr());

                    Utils.matToBitmap(mSelectedRgb, mSelectedBitmap, false);
//            }
                    myHandler.sendEmptyMessage(Constant.FIND_ONE_FACE_MESSAGE);
                }



            } else if (faceList.size() > 1) {
                myHandler.sendEmptyMessage(Constant.FIND_MULT_FACE_MESSAGE);
            }/* else {
            myHandler.sendEmptyMessage(Constant.FIND_NULL_FACE_MESSAGE);
        }*/

            for (int i = 0; i < faceList.size(); i++) {
                Rect faceRect = faceList.get(i).getBbox();
//            Log.d(TAG, "face rect == " + faceRect.toString());
                Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), FACE_RECT_COLOR, 3);
            }
        }

        if (isFrontCamera) {
            Core.flip(mRgba, mFlipRgba, 1);
            return mFlipRgba;
        } else {
            return mRgba;
        }
    }

    /**
     * 保存方法
     */
    public void saveBitmap(Bitmap bitmap, String fileName) {
        Log.e(TAG, "保存图片");
        File f = new File(gPath, fileName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Log.i(TAG, "已经保存");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void faceCompare() {
        if (mSelectedBitmap == null || mIdBitmap == null) {
            myHandler.sendEmptyMessageDelayed(Constant.START_FACE_COMPARE_MESSAGE,
                    2000);
            return;
        }

        mSelectedRgb.release();
        mSelectedGray.release();

        isFaceComparing = true;

        saveBitmap(mSelectedBitmap, "selected.jpg");
        saveBitmap(mIdBitmap, "id.jpg");



/*        for (int i = 0; i < selectedFeatures.length; i++) {
            Log.d(TAG, "idFeatures[" + i + "] == " + selectedFeatures[i]);
        }*/

        Mat idColorMat = Imgcodecs.imread(gPath + "/id.jpg");
//        Mat idTmpMat = new Mat();
        Mat idGrayMat = new Mat();
//        Imgproc.cvtColor(idColorMat, idTmpMat, Imgproc.COLOR_RGB2BGR);
//        Imgproc.cvtColor(idTmpMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(idColorMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);

        ImageData idColorSrc = getImageData(idColorMat);
        ImageData idGraySrc = getImageData(idGrayMat);

        Log.d(TAG, "idColorSrc == " + idColorSrc.toString());
        Log.d(TAG, "idGraySrc == " + idGraySrc.toString());

        float idFeatures[] = JniLoader.getInstance().callFaceRecExtract(idColorSrc,
                idGraySrc);

        /*Mat idColorMat1 = Imgcodecs.imread(gPath + "/selected.jpg");
//        Mat idTmpMat = new Mat();
        Mat idGrayMat1 = new Mat();
//        Imgproc.cvtColor(idColorMat, idTmpMat, Imgproc.COLOR_RGB2BGR);
//        Imgproc.cvtColor(idTmpMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(idColorMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);

        ImageData idColorSrc1 = getImageData(idColorMat);
        ImageData idGraySrc1 = getImageData(idGrayMat);

        Log.d(TAG, "idColorSrc == " + idColorSrc.toString());
        Log.d(TAG, "idGraySrc == " + idGraySrc.toString());

        float idFeatures1[] = JniLoader.getInstance().callFaceRecExtract(idColorSrc1,
                idGraySrc1);*/

        Mat selectedColorMat = Imgcodecs.imread(gPath + "/selected.jpg");

//        Mat selectedTmpMat = new Mat();
        Mat selectedGrayMat = new Mat();
//        Imgproc.cvtColor(selectedColorMat, selectedTmpMat, Imgproc.COLOR_RGB2BGR);
//        Imgproc.cvtColor(selectedTmpMat, selectedGrayMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(selectedColorMat, selectedGrayMat, Imgproc.COLOR_BGR2GRAY);

//        ImageData selectedColor = getImageData(selectedTmpMat);
        ImageData selectedColor = getImageData(selectedColorMat);
        ImageData selectedGray = getImageData(selectedGrayMat);

        Log.d(TAG, "selectedColor == " + selectedColor.toString());
        Log.d(TAG, "selectedGray == " + selectedGray.toString());

        float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract(selectedColor,
                selectedGray);

/*        for (int i = 0; i < idFeatures.length; i++) {
            Log.d(TAG, "idFeatures[" + i + "] == " + idFeatures[i]);
        }*/

        float similarity = JniLoader.getInstance().callFaceCampare(idFeatures, selectedFeatures);
        Log.d(TAG, "Finish the compare, similarity == " + similarity);
        if (Float.isNaN(similarity)) {
            Log.d(TAG, "Float.NaN");

        }
        updateImage = true;
        isFaceComparing = false;
    }

    private void faceCompareFromMat() {
        if (mSelectedBitmap == null || mIdBitmap == null) {
            myHandler.sendEmptyMessageDelayed(Constant.START_FACE_COMPARE_MESSAGE,
                    2000);
            return;
        }

        isFaceComparing = true;

        saveBitmap(mSelectedBitmap, "selected.jpg");
        saveBitmap(mIdBitmap, "id.jpg");

        Mat selectedColorMat = new Mat();
        Utils.bitmapToMat(mSelectedBitmap, selectedColorMat, false);

        Mat selectedGrayMat = new Mat();
        Mat selectedTmpMat = new Mat();
        Imgproc.cvtColor(mSelectedRgb, selectedTmpMat, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(selectedTmpMat, selectedGrayMat, Imgproc.COLOR_BGR2GRAY);

        ImageData selectedColor = getImageData(selectedTmpMat);
        ImageData selectedGray = getImageData(selectedGrayMat);

        Log.d(TAG, "selectedColor == " + selectedColor.toString());
        Log.d(TAG, "selectedGray == " + selectedGray.toString());


        float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract(selectedColor,
                selectedGray);



        Mat idColorMat = new Mat();
        Utils.bitmapToMat(mIdBitmap, idColorMat, false);

        Mat idGrayMat = new Mat();
        Imgproc.cvtColor(idColorMat, idGrayMat, Imgproc.COLOR_RGB2GRAY);

        ImageData idColorSrc = getImageData(idColorMat);
        ImageData idGraySrc = getImageData(idGrayMat);


        Log.d(TAG, "idColorSrc == " + idColorSrc.toString());
        Log.d(TAG, "idGraySrc == " + idGraySrc.toString());

        float idFeatures[] = JniLoader.getInstance().callFaceRecExtract(idColorSrc,
                idGraySrc);

        float similarity = JniLoader.getInstance().callFaceCampare(selectedFeatures, idFeatures);
        Log.d(TAG, "Finish the compare, similarity == " + similarity);
        if (Float.isNaN(similarity)) {
            Log.d(TAG, "Float.NaN");
        }
        updateImage = true;
        isFaceComparing = false;

    }

    private ArrayList<FaceInfo> faceDetection() {
        ImageData imgColor = getImageData(mRgba);
        ImageData imgGray = getImageData(mGray);

        return JniLoader.getInstance().callFaceRecDetect(0, imgColor, imgGray);
    }

    private ImageData getImageData(Mat mat) {

/*        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap, false);
        Mat tmpMat = new Mat();
        Utils.bitmapToMat(bitmap, tmpMat, false);


        ImageData imageData = new ImageData();
        imageData.setDataPtr(tmpMat.dataAddr());
        imageData.setWidth(tmpMat.cols());
        imageData.setHeight(tmpMat.rows());
        imageData.setNumChannels(tmpMat.channels());*/

        ImageData imageData = new ImageData();
        imageData.setDataPtr(mat.dataAddr());
        imageData.setWidth(mat.cols());
        imageData.setHeight(mat.rows());
        imageData.setNumChannels(mat.channels());

        return imageData;
    }

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.FIND_NULL_FACE_MESSAGE:
                    mMultFaceMessage.setVisibility(View.GONE);
                    mCurrentImage.setImageBitmap(null);
                    break;
                case Constant.FIND_ONE_FACE_MESSAGE:
                    mMultFaceMessage.setVisibility(View.GONE);
                    mCurrentImage.setImageBitmap(mSelectedBitmap);
                    break;
                case Constant.FIND_MULT_FACE_MESSAGE:
                    mMultFaceMessage.setVisibility(View.VISIBLE);
                    mCurrentImage.setImageBitmap(null);
                    break;
                case Constant.FIND_ID_CARD_MESSAGE:
                    mIDCardRecListener.onResp((IDCardMsg) msg.obj);
                    break;
                case Constant.LOST_ID_CARD_MESSAGE:
                    mIDCardRecListener.onResp(null);
                    break;
                case Constant.START_FACE_COMPARE_MESSAGE:
                    if (mSelectedBitmap == null || mIdBitmap == null) {
                        myHandler.sendEmptyMessageDelayed(Constant.START_FACE_COMPARE_MESSAGE,
                                2000);
                        return;
                    }
                    faceCompare();
                    break;
                case Constant.CLEAR_ID_INFO_MESSAGE:
                    clearIdInfo();
                    break;
                default:
                    break;
            }
        }
    };

    private void clearIdInfo() {
        if (isFaceComparing) {
            myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 5000);
            return;
        }
        mIDCardRecListener.onResp(null);
    }

    private IDCardRecListener mIDCardRecListener = new IDCardRecListener() {
        @Override
        public void onResp(IDCardMsg info) {
            Log.d(TAG, "IDCardRecListener  onResp");

            if (info == null) {
                mIdImage.setImageBitmap(null);
                lastCardMsg = null;

                mIdName.setText("");
                mIdSex.setText("");
                mIdNation.setText("");
                mIdBirthday.setText("");
                mIdNumber.setText("");
                mIdAddress.setText("");
                mIdSignOffice.setText("");
                mIdUsefuDate.setText("");
                return;
            }

            if (lastCardMsg != null && lastCardMsg.equals(info)) {
                return;
            }

            lastCardMsg = info;
            String text = info.getName() + "\n"
                    + info.getSexStr() + "\n"
                    + info.getNationStr() + "族" + "\n"
                    + info.getBirthDate() + "\n"
                    + info.getIdCardNum() + "\n"
                    + info.getAddress() + "\n"
                    + info.getUsefulEndDate() + "--" + info.getUsefulStartDate() + "\n"
                    + info.getSignOffice() + "\n";
//            mIdTextView.setText(text);
            Log.d(TAG, "text == " + text);
            mIdName.setText(info.getName());
            mIdSex.setText(info.getSexStr());
            mIdNation.setText(info.getNationStr());
            mIdBirthday.setText(info.getBirthDate().toString());
            mIdNumber.setText(info.getIdCardNum());
            mIdAddress.setText(info.getAddress());
            mIdSignOffice.setText(info.getSignOffice());
            mIdUsefuDate.setText(info.getUsefulStartDate() + " 至 " + info.getUsefulEndDate());
            mIdBitmap = BitmapFactory.decodeByteArray(info.getPortrait(), 0, info.getPortrait().length);
            mIdImage.setImageBitmap(mIdBitmap);

            myHandler.sendEmptyMessage(Constant.START_FACE_COMPARE_MESSAGE);
            myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 5000);
        }
    };
}