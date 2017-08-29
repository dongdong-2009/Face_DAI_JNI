package com.xin.android.facerecdemo.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xin.android.facerecdemo.R;
import com.xin.android.facerecdemo.util.JniLoader;

import java.io.File;

public class VerifyActivity extends Activity implements View.OnClickListener {

    private String TAG = getClass().getSimpleName();

    private TextView mDeviceInfo;
    private EditText mKeyInput;
    private Button mEnterKey;
    private String mPath;

    private boolean NEED_KEY = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
                .LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_verify);

        if (!NEED_KEY) {
            Intent intent = new Intent(VerifyActivity.this, FaceDetectorActivity.class);
            startActivity(intent);
            finish();
        }

        mPath = this.getFilesDir().getAbsolutePath() + File.separator;

        JniLoader.getInstance().loadLibrary();

        mDeviceInfo = (TextView) findViewById(R.id.device_info);
        mKeyInput = (EditText) findViewById(R.id.device_key_input);
        mEnterKey = (Button) findViewById(R.id.enter_key);

        mEnterKey.setOnClickListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (JniLoader.getInstance().checkDeviceState(mPath)) {
            Log.d(TAG, "checkDeviceState  true");
            Intent intent = new Intent(this, FaceDetectorActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "checkDeviceState  false");
            String productId = JniLoader.getInstance().getDeviceUuid();
            mDeviceInfo.setText(productId);
        }

//        myHandler.sendEmptyMessageDelayed(10000, 1* 1000);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.enter_key:
                if (!TextUtils.isEmpty(mKeyInput.getText())) {
                    JniLoader.getInstance().registerDeviceKey(mPath,
                            mKeyInput.getText().toString().trim());
                    if (JniLoader.getInstance().checkDeviceState(mPath)) {
                        Intent intent = new Intent(this, FaceDetectorActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "密钥错误，请输入正确的密钥！", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "密钥不能为空！", Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 10000:
                    Intent intent = new Intent(VerifyActivity.this, FaceDetectorActivity.class);
                    startActivity(intent);
                    finish();
                    break;
            }
        }
    };
}
