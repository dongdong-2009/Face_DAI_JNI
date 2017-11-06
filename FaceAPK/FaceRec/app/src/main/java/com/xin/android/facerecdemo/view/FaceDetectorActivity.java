package com.xin.android.facerecdemo.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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
import com.xin.android.facerecdemo.util.EncryptUtils;
import com.xin.android.facerecdemo.util.JniLoader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class FaceDetectorActivity extends Activity implements CvCameraViewListener2 {

    private final String TAG = getClass().getSimpleName();
    private final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private CameraBridgeViewBase mOpenCvCameraView;

    private IDCardRecognition mIDCardRecognition;
    private IDCardMsg lastCardMsg = null;

    private static final String LOCK_BITMAP = "lock_bitmap";

    private String SERVER_IP;
    private int SERVER_PORT = 5958;
    private int THRESHOLD_VALUE = 52;

    private Mat mRgba;
    private Mat mGray;
    private Mat mSelectedRgb;
    private Mat mSelectedGray;

    private volatile boolean updateImage = true;
    private volatile boolean isFaceComparing = false;
    private volatile boolean hasCompared = false;
    private volatile boolean hasIdInfo = false;

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
    private TextView mIdUsefulDate;
    private TextView mCompareResult;
    private TextView mCompareValue;
    private TextView mNearToDisplay;

    private String gPath;
    private int mHeight = 0;
    private int mWidth = 0;

    private int mIdWidth;
    private int mIdHeight;

    private int mSimilarity  = 60;

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

                    mIDCardRecognition = new IDCardRecognition(FaceDetectorActivity.this, myHandler,
                            mIDCardRecListener);
                    mIDCardRecognition.start();
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
                .LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_activity);
        findViews();

        gPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/FaceRec/";
        Log.d(TAG, "gptah result == " + gPath);
        int result;
        result = JniLoader.getInstance().callInitFaceRec(1, gPath);
        Log.d(TAG, "callInitFaceRec result == " + result);
        if (result != 0) {
            JniLoader.getInstance().callInitFaceRec(1, gPath);
        }


        String server = Constant.readServerInfo().trim();
        if (!TextUtils.isEmpty(server)) {
            String info[] = server.split(":");

            if (info.length > 0) {
                THRESHOLD_VALUE = Integer.parseInt(info[0]);
                if (info.length == 3) {
                    SERVER_IP = info[1];
                    SERVER_PORT = Integer.parseInt(info[2]);
                }
            }

        }

        Log.d(TAG, SERVER_IP + ":" + SERVER_PORT + ":" + THRESHOLD_VALUE);
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
        mIdUsefulDate = (TextView) findViewById(R.id.id_useful_data_info);
        mCompareResult = (TextView) findViewById(R.id.compare_result_text);
        mCompareValue = (TextView) findViewById(R.id.compare_result_value);
        mNearToDisplay = (TextView) findViewById(R.id.near_to_display);
    }

    @Override
    public void onPause() {
        super.onPause();

        myHandler.removeCallbacksAndMessages(null);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        if (mIDCardRecognition != null) {
            mIDCardRecognition.close();
        }
        mSoundPool.release();

    }

    @Override
    public void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        prepareVoice();

    }

    public void onDestroy()
    {
        super.onDestroy();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
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

        if (!isFaceComparing && hasIdInfo && !hasCompared) {
            synchronized (Constant.LOCK) {
                ArrayList<FaceInfo> faceList = faceDetection();

                for (int j = 0; j< faceList.size(); j++) {
                    Log.d(TAG, "face score == " + faceList.get(j).getScore());
                }


                if (faceList.size() >0 && updateImage) {
                    Rect rect = faceList.get(0).getBbox();

                    Log.d(TAG, "faceInfo rect == " + rect.toString());
                    int faceCenterX = rect.getX() + rect.getWidth() / 2;
                    int faceCenterY = rect.getY() + rect.getHeight() / 2;

                    if (rect.getHeight() > 50 && rect.getWidth() > 50 &&
                            ((mWidth / 2 - 250) < faceCenterX) &&
                            (faceCenterX < (mWidth / 2 + 250)) &&
                            ((mHeight / 2 - 250) < faceCenterY) &&
                            (faceCenterY < (mHeight / 2 + 250))) {
                        updateImage = false;
//                        mSelectedBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap
//                                .Config.RGB_565);


                        Bitmap originalBitmap;
//                        originalBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap
//                                .Config.RGB_565);
                        originalBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap
                                .Config.RGB_565);
                        Utils.matToBitmap(mRgba, originalBitmap, false);
                        saveBitmap(originalBitmap, "original.jpg");
                        originalBitmap.recycle();

                        mSelectedRgb = mRgba.clone();
                        mSelectedGray = mGray.clone();

                        org.opencv.core.Rect faceRect = new org.opencv.core.Rect(rect.getX(),
                                rect.getY(), rect.getWidth(), rect.getHeight());

                        Mat faceMat = new Mat(mSelectedRgb, faceRect);
                        double rate = (1.0*rect.getWidth()/mIdWidth > 1.0*rect.getHeight()
                                /mIdHeight)? 1.0*mIdHeight/rect.getHeight():1.0*mIdWidth/rect
                                .getWidth();

                        Mat reszieMat = new Mat();
                        Imgproc.resize(faceMat, reszieMat, new Size(rate *rect.getWidth(), rate *
                                rect.getHeight()));

                        Log.d(TAG, "resizeMat  == " + reszieMat.toString());

                        mSelectedBitmap = Bitmap.createBitmap(reszieMat.cols(), reszieMat.rows(), Bitmap
                                .Config.RGB_565);
//                        Utils.matToBitmap(mSelectedRgb, mSelectedBitmap, false);
//                        Utils.matToBitmap(faceMat, mSelectedBitmap, false);
                        Utils.matToBitmap(reszieMat, mSelectedBitmap, false);
                        faceMat.release();
                        reszieMat.release();
                        myHandler.sendEmptyMessage(Constant.FIND_ONE_FACE_MESSAGE);

                        mSelectedRgb.release();
                        mSelectedGray.release();
                    }

                        Rect faceRect = faceList.get(0).getBbox();
                        Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), FACE_RECT_COLOR, 3);
                        if (updateImage) {
                            myHandler.sendEmptyMessage(Constant.NEAR_TO_DISPLAY_MESSAGE);
                        }
                    }

            }
        }
        return mRgba;
    }

    /**
     * 保存图片
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

    private void startFaceCompare() {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                faceCompare();
                faceCompareMat();
            }
        }).start();
    }

    private void faceCompareMat() {
        synchronized (Constant.LOCK) {

            if (!hasIdInfo) {
                myHandler.sendEmptyMessage(Constant.CLEAR_LOCAL_IMAGE_MESSAGE);
                return;
            }

            myHandler.removeMessages(Constant.CLEAR_ID_INFO_MESSAGE);
            myHandler.sendEmptyMessage(Constant.COMPARING_MESSAGE);

            Log.d(TAG, "faceCompare start");
//
//            Mat originalColorMat = Imgcodecs.imread(gPath + "/original.jpg");
//            Mat originalGrayMat = new Mat();
//            Imgproc.cvtColor(originalColorMat, originalGrayMat, Imgproc.COLOR_BGR2GRAY);
//            JniLoader.getInstance().callFaceRecDetect2(originalColorMat.getNativeObjAddr(),
//                    originalGrayMat.getNativeObjAddr());
//

            isFaceComparing = true;


            saveBitmap(mSelectedBitmap, "selected.jpg");
            saveBitmap(mIdBitmap, "id.jpg");

            Mat idColorMat = Imgcodecs.imread(gPath + "/id.jpg");
            Mat idGrayMat = new Mat();
            Imgproc.cvtColor(idColorMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);
            Log.d(TAG, "callFaceRecExtract 111 start");
            float idFeatures[] = JniLoader.getInstance().callFaceRecExtract2(idColorMat.getNativeObjAddr(),
                    idGrayMat.getNativeObjAddr());
            Log.d(TAG, "callFaceRecExtract 111 end");


            Mat selectedColorMat = Imgcodecs.imread(gPath + "/selected.jpg");
            Mat selectedGrayMat = new Mat();
            Imgproc.cvtColor(selectedColorMat, selectedGrayMat, Imgproc.COLOR_BGR2GRAY);
            Log.d(TAG, "callFaceRecExtract 222 start");
            float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract2
                    (selectedColorMat.getNativeObjAddr(), selectedGrayMat.getNativeObjAddr());
            Log.d(TAG, "callFaceRecExtract 222 start");
            int faceExtractResult = JniLoader.getInstance().faceExtractResult();

            float similarity;
            if (faceExtractResult != 0) {
                similarity = 0.0f;
            } else {
                similarity = JniLoader.getInstance().callFaceCampare(idFeatures, selectedFeatures);
            }
            Log.d(TAG, "Finish the compare, similarity == " + similarity);
            if (Float.isNaN(similarity)) {
                Log.e(TAG, "Float.NaN");
            } else {
                mSimilarity = Math.round(similarity * 100);

                if (connectWithTcp(SERVER_IP, SERVER_PORT)) {
                    Log.d(TAG, "Finished uploading the data to server");
                } else {
                    Log.d(TAG, "Error when Uploading the data to server");
                }

                Message msg = new Message();
                msg.what = Constant.SHOW_FACE_COMPARE_RESULT_MESSAGE;
                Bundle bundle = new Bundle();
                bundle.putFloat("similarity", mSimilarity);
                msg.setData(bundle);
                myHandler.sendMessage(msg);
            }

            myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 3 * 1000);

            Log.d(TAG, "faceCompare end");
//            hasCompared = true;
//            updateImage = true;
            isFaceComparing = false;
        }
    }

    private void faceCompare() {
        synchronized (Constant.LOCK) {

            if (!hasIdInfo) {
                myHandler.sendEmptyMessage(Constant.CLEAR_LOCAL_IMAGE_MESSAGE);
                return;
            }

            myHandler.removeMessages(Constant.CLEAR_ID_INFO_MESSAGE);
            myHandler.sendEmptyMessage(Constant.COMPARING_MESSAGE);

            isFaceComparing = true;

            Log.d(TAG, "faceCompare start");

            saveBitmap(mSelectedBitmap, "selected.jpg");
            saveBitmap(mIdBitmap, "id.jpg");

            Mat idColorMat = Imgcodecs.imread(gPath + "/id.jpg");
            Mat idGrayMat = new Mat();
            Imgproc.cvtColor(idColorMat, idGrayMat, Imgproc.COLOR_BGR2GRAY);

            ImageData idColorSrc = getImageData(idColorMat);
            ImageData idGraySrc = getImageData(idGrayMat);

            Log.d(TAG, "callFaceRecExtract 111 start");

//            float idFeatures[] = JniLoader.getInstance().callFaceRecExtract(idColorSrc,
//                    idGraySrc);
            float idFeatures[] = JniLoader.getInstance().callFaceRecExtract2(idColorMat.getNativeObjAddr(),
                    idGrayMat.getNativeObjAddr());
//            float idFeatures[] = JniLoader.getInstance().callFaceRecExtract(idGraySrc,
//                    idGraySrc);
            Log.d(TAG, "callFaceRecExtract 111 end");

            Mat selectedColorMat = Imgcodecs.imread(gPath + "/selected.jpg");
            Mat selectedGrayMat = new Mat();
            Imgproc.cvtColor(selectedColorMat, selectedGrayMat, Imgproc.COLOR_BGR2GRAY);

            ImageData selectedColor = getImageData(selectedColorMat);
            ImageData selectedGray = getImageData(selectedGrayMat);

            Log.d(TAG, "callFaceRecExtract 222 start");
//            float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract(selectedColor,
//                    selectedGray);
            float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract2
                    (selectedColorMat.getNativeObjAddr(), selectedGrayMat.getNativeObjAddr());
            Log.d(TAG, "callFaceRecExtract 222 start");

//            float selectedFeatures[] = JniLoader.getInstance().callFaceRecExtract(selectedGray,
//                    selectedGray);

            float similarity = JniLoader.getInstance().callFaceCampare(idFeatures, selectedFeatures);
            Log.d(TAG, "Finish the compare, similarity == " + similarity);
            if (Float.isNaN(similarity)) {
                Log.e(TAG, "Float.NaN");
            } else {

                mSimilarity = Math.round(similarity * 100);

                if (connectWithTcp(SERVER_IP, SERVER_PORT)) {
                    Log.d(TAG, "Finished uploading the data to server");
                } else {
                    Log.d(TAG, "Error when Uploading the data to server");
                }

                Message msg = new Message();
                msg.what = Constant.SHOW_FACE_COMPARE_RESULT_MESSAGE;
                Bundle bundle = new Bundle();
                bundle.putFloat("similarity", mSimilarity);
                msg.setData(bundle);
                myHandler.sendMessage(msg);
            }

            myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 3 * 1000);

            Log.d(TAG, "faceCompare end");
//            hasCompared = true;
//            updateImage = true;
            isFaceComparing = false;
        }
    }



    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> soundID = new HashMap<Integer, Integer>();

    private void prepareVoice() {
        SoundPool.Builder builder = new SoundPool.Builder();
        //传入音频数量
        builder.setMaxStreams(2);
        //AudioAttributes是一个封装音频各种属性的方法
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        //设置音频流的合适的属性
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        //加载一个AudioAttributes
        builder.setAudioAttributes(attrBuilder.build());
        mSoundPool = builder.build();

        soundID.put(1, mSoundPool.load(this, R.raw.verify_success, 1));
        soundID.put(2, mSoundPool.load(this, R.raw.verify_fail, 1));
//        soundID.put(2, mSoundPool.load(getAssets().openFd("assets.mp3"), 1));  //需要捕获IO异常
    }

    private ArrayList<FaceInfo> faceDetection() {
        ImageData imgColor = getImageData(mRgba);
        ImageData imgGray = getImageData(mGray);

        return JniLoader.getInstance().callFaceRecDetect2(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr());
    }

    private ImageData getImageData(Mat mat) {

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
                    mNearToDisplay.setVisibility(View.GONE);
                    mCurrentImage.setImageBitmap(mSelectedBitmap);
                    startFaceCompare();
                    break;
                case Constant.FIND_ID_CARD_MESSAGE:
                    mIDCardRecListener.onResp((IDCardMsg) msg.obj);
                    break;
                case Constant.LOST_ID_CARD_MESSAGE:
                    mIDCardRecListener.onResp(null);
                    break;
                case Constant.CLEAR_ID_INFO_MESSAGE:
                    clearIdInfo();
                    break;
                case Constant.CLEAR_LOCAL_IMAGE_MESSAGE:
                    mCurrentImage.setImageBitmap(null);
                    if (mSelectedBitmap != null) {
                        synchronized (Constant.LOCK) {
                            mSelectedBitmap.recycle();
                            mSelectedBitmap = null;
                        }
                    }
                    updateImage = true;

                    break;
                case Constant.COMPARING_MESSAGE:
                    mCompareResult.setVisibility(View.VISIBLE);
                    mCompareResult.setTextColor(getResources().getColor(R.color.green));
                    mCompareResult.setText("正在验证中...");
                    break;

                case Constant.SHOW_FACE_COMPARE_RESULT_MESSAGE:
                    float similarity = msg.getData().getFloat("similarity");
//                    Toast.makeText(FaceDetectorActivity.this, "" + similarity, Toast.LENGTH_LONG).show();
                    if (similarity > THRESHOLD_VALUE) {
                        Log.d(TAG, "soundID == " + soundID.size());
                        mSoundPool.play(soundID.get(1), 1, 1, 0, 0, 1);
                        mCompareValue.setTextColor(getResources().getColor(R.color.green));
                        mCompareValue.setVisibility(View.VISIBLE);
                        mCompareValue.setText("" + similarity);

                        mCompareResult.setTextColor(getResources().getColor(R.color.green));
                        mCompareResult.setVisibility(View.VISIBLE);
                        mCompareResult.setText("验证成功");

                    } else {
                        Log.d(TAG, "soundID == " + soundID.size());
                        mSoundPool.play(soundID.get(2), 1, 1, 0, 0, 1);
                        mCompareValue.setTextColor(getResources().getColor(R.color.red));
                        mCompareValue.setVisibility(View.VISIBLE);
                        mCompareValue.setText("" + similarity);

                        mCompareResult.setTextColor(getResources().getColor(R.color.red));
                        mCompareResult.setVisibility(View.VISIBLE);
                        mCompareResult.setText("验证失败");
                    }

                    break;
                case Constant.NEAR_TO_DISPLAY_MESSAGE:
                    mNearToDisplay.setVisibility(View.VISIBLE);
                    break;
                case Constant.GET_ID_INFORMATION_MESSAGE:
                    hasIdInfo = true;
//                    startFaceCompare();
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
        mCurrentImage.setImageBitmap(null);
        if (mSelectedBitmap != null) {
            synchronized (Constant.LOCK) {
                mSelectedBitmap.recycle();
                mSelectedBitmap = null;
            }
        }
        hasCompared = false;
        updateImage = true;
        hasIdInfo = false;
        mCompareValue.setVisibility(View.INVISIBLE);
        mCompareResult.setVisibility(View.INVISIBLE);
    }

    private IDCardRecListener mIDCardRecListener = new IDCardRecognition.IDCardRecListener() {
        @Override
        public void onResp(IDCardMsg info) {
            Log.d(TAG, "IDCardRecListener onResp");

            if (info == null) {
                mIdImage.setImageBitmap(null);
                lastCardMsg = null;

                if (mIdBitmap != null) {
                    mIdBitmap.recycle();
                    mIdBitmap = null;
                }

                mIdName.setText("");
                mIdSex.setText("");
                mIdNation.setText("");
                mIdBirthday.setText("");
                mIdNumber.setText("");
                mIdAddress.setText("");
                mIdSignOffice.setText("");
                mIdUsefulDate.setText("");
                return;
            }

            if (lastCardMsg != null && lastCardMsg.equals(info)) {
                myHandler.removeMessages(Constant.CLEAR_ID_INFO_MESSAGE);
                myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 5*1000);
                return;
            }

            lastCardMsg = info;
//            String text = info.getName() + "\n"
//                    + info.getSexStr() + "\n"
//                    + info.getNationStr() + "\n"
//                    + info.getBirthDate() + "\n"
//                    + info.getIdCardNum() + "\n"
//                    + info.getAddress() + "\n"
//                    + info.getUsefulEndDate() + "--" + info.getUsefulStartDate() + "\n"
//                    + info.getSignOffice() + "\n";
//            mIdTextView.setText(text);
//            Log.d(TAG, "text == " + text);
            mIdName.setText(info.getName());
            mIdSex.setText(info.getSexStr());
            mIdNation.setText(info.getNationStr());
            mIdBirthday.setText(info.getBirthDate().toString());
            mIdNumber.setText(info.getIdCardNum());
            mIdAddress.setText(info.getAddress());
            mIdSignOffice.setText(info.getSignOffice());
            mIdUsefulDate.setText(info.getUsefulStartDate() + " 至 " + info.getUsefulEndDate());
            mIdBitmap = BitmapFactory.decodeByteArray(info.getPortrait(), 0, info.getPortrait().length);
            mIdImage.setImageBitmap(mIdBitmap);
            Log.d(TAG, "mIdBitmap w&h == " + mIdBitmap.getWidth() + "*" + mIdBitmap.getHeight());

            mIdWidth = mIdBitmap.getWidth();
            mIdHeight = mIdBitmap.getHeight();

            myHandler.removeMessages(Constant.CLEAR_ID_INFO_MESSAGE);
            myHandler.sendEmptyMessage(Constant.GET_ID_INFORMATION_MESSAGE);
            myHandler.sendEmptyMessageDelayed(Constant.CLEAR_ID_INFO_MESSAGE, 5*1000);

        }
    };

    public byte[] getBytesFromInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[0xFFFF];

            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);

            os.flush();

            return os.toByteArray();
        }
    }

    private byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private byte[] int2Bytes(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)};
    }

    public boolean connectWithTcp(String ip, int port) {
        Socket socket;
        boolean updated = false;

        if (TextUtils.isEmpty(ip)) {
            return false;
        }



        try {
            socket = new Socket(ip, port);

            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

            byte[] data = new byte[552];

            byte[] header = "test".getBytes(); 	//"test"是测试厂商码，等测试通过后联系云辰，会分配发布厂商码
            System.arraycopy(header, 0, data, 0, header.length);    //8

//            System.arraycopy(int2Bytes(80+256*60), 0, data, 8, 4);   //4
            System.arraycopy(int2Bytes(mSimilarity + 256 * THRESHOLD_VALUE), 0, data, 8, 4);   //4

//            byte[] szName = "李明".getBytes("GB2312");
            byte[] szName = lastCardMsg.getName().getBytes("GB2312");
            System.arraycopy(szName, 0, data, 12, szName.length); //64

//            byte[] szCertifiCode = "131002199011142256".getBytes("GB2312");
            byte[] szCertifiCode = lastCardMsg.getIdCardNum().getBytes("GB2312");
            System.arraycopy(szCertifiCode, 0, data, 76, szCertifiCode.length); //20

//            byte[] szNation = "汉族".getBytes("GB2312");
            byte[] szNation = lastCardMsg.getNationStr().getBytes("GB2312");
            System.arraycopy(szNation, 0, data, 96, szNation.length); //20

//            byte[] szExpiredDate = "2016082420360824".getBytes("GB2312");
            String useDate = lastCardMsg.getUsefulStartDate().toNumString() + lastCardMsg
                    .getUsefulEndDate().toNumString();
            byte[] szExpiredDate = useDate.getBytes("GB2312");
            System.arraycopy(szExpiredDate, 0, data, 116, szExpiredDate.length); //20

            int sex = lastCardMsg.getSex();
            System.arraycopy(int2Bytes(sex), 0, data, 136, 4);   //4

//            byte[] szBirthday = "20160824".getBytes("GB2312");
            byte[] szBirthday = lastCardMsg.getBirthDate().toNumString().getBytes("GB2312");
            System.arraycopy(szBirthday, 0, data, 140, szBirthday.length); //20

//            byte[] szAddress = "上海市南京西路19号".getBytes("GB2312");
            byte[] szAddress = lastCardMsg.getAddress().getBytes("GB2312");
            System.arraycopy(szAddress, 0, data, 160, szAddress.length); //128

//            byte[] szOrgan = "上海市公安市局黄浦分局".getBytes("GB2312");
            byte[] szOrgan = lastCardMsg.getSignOffice().getBytes("GB2312");
            System.arraycopy(szOrgan, 0, data, 288, szOrgan.length); //128

            byte[] szBackup = "Sam111111-222222".getBytes("GB2312");
            System.arraycopy(szBackup, 0, data, 416, szBackup.length); //128

            InputStream stream1 = null;
            InputStream stream2 = null;
            stream1 = new FileInputStream(new File(gPath + "/id.jpg"));

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            Bitmap bitmap1 = BitmapFactory.decodeStream(stream1, null, opts);

            stream2 = new FileInputStream(new File(gPath + "/selected.jpg"));
            opts.inSampleSize = 4;
            Bitmap bitmap2 = BitmapFactory.decodeStream(stream2, null, opts);

//            byte[] imgByte1 = getBytesFromInputStream(stream1);
//            byte[] imgByte2 = getBytesFromInputStream(stream2);

            byte[] imgByte1 = bitmap2Bytes(bitmap1);
            byte[] imgByte2 = bitmap2Bytes(bitmap2);


            System.arraycopy(int2Bytes(imgByte1.length), 0, data, 544, 4);   //4
            System.arraycopy(int2Bytes(imgByte2.length), 0, data, 548, 4);   //4

//            System.arraycopy(int2Bytes(mIdBitmap.getByteCount()), 0, data, 544, 4);   //4
//            System.arraycopy(int2Bytes(mSelectedBitmap.getByteCount()), 0, data, 548, 4);   //4

            dataOutputStream.write(EncryptUtils.encryptDES(data, "testtest".getBytes()));	//"testtest"是测试加密密钥，等测试通过后联系云辰，会分配发布加密密钥

            dataOutputStream.write(imgByte1);
            dataOutputStream.write(imgByte2);


            updated = true;

            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Socket", "connectWithTcp: ", e);
            updated = false;
        }

        return updated;
    }
}
