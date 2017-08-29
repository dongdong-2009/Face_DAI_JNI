package com.grg.idcard;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xin.android.facerecdemo.util.Constant;

public class IDCardRecognition extends Thread {
	private String TAG = getClass().getSimpleName();
	private Activity mContext;
	private boolean running = false;
	private IDCardManager mIDCardManager;
//	private String mIdCardNo = "";
//	private FileUtils mFileUtils;

	private boolean isCardFound = false;
    private Handler mHandler;

/*	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if(msg.what == 1) {
				mIDCardRecListener.onResp((IDCardMsg)msg.obj);
			} else {
				mIDCardRecListener.onResp(null);
			}
		};
	};*/

	public IDCardRecognition(Activity context, Handler handler, IDCardRecListener listener) {
		mContext = context;
        mHandler = handler;
		mIDCardRecListener = listener;
		running = true;

//		mFileUtils = new FileUtils();
	}

	public void close() {
		running = false;
	}

	@Override
	public void run() {
		super.run();
		Looper.prepare();
		while (this.running) {
//			mHandler.sendEmptyMessage(2);

            if(mIDCardManager == null) {
				init();
				sleepTime(5000);
			} else {
                int ret = mIDCardManager.SDT_GetSAMStatus();
				if(ret != 0X90) {
					init();
					sleepTime(2000);
				}
				try {
					IDCardMsg msg = mIDCardManager.getIDCardMsg();
//					if(!isCardFound) {
						isCardFound = true;
//						mFileUtils.saveCardImg(msg.getPortrait());
						mHandler.sendMessage(mHandler.obtainMessage(Constant.FIND_ID_CARD_MESSAGE, msg));
//					}
				} catch (IDCardException e) {
					isCardFound = false;
//                    Log.d(TAG, "IDCardException " + e.toString());
//                    Log.i(TAG, "IDCardException ", e);
//                    mHandler.sendEmptyMessage(2);

//					GrgLog.w(TAG, "IDCardException", e);
				}
//				GrgLog.w(TAG, "ret = " + ret + ", isCardFound = " + isCardFound);
				sleepTime(1000);
            }
		}
	}


	private void init() {
		try {
            mIDCardManager = new IDCardManager(mContext);

            mIDCardManager.SDT_ResetSAM();
		} catch (Exception e) {
			Log.w(TAG, "IDCardRecognition", e);
		}
	}

	private void sleepTime(long time) {
		try {
			sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}


	private IDCardRecListener mIDCardRecListener;

	public interface IDCardRecListener {
		public void onResp(IDCardMsg info);
	}

}
