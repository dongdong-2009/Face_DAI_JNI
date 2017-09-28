package com.xin.android.facerecdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuffXfermode;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;

public class CircleMaskView extends AppCompatImageView {
	private static final String TAG = "CircleMaskView";
	private Paint mLinePaint;

	public CircleMaskView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaint();
	}
	
	private void initPaint(){
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setStyle(Style.FILL_AND_STROKE);//空实心
		mLinePaint.setStrokeWidth(5f);
		mLinePaint.setAlpha(30);
		mLinePaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawARGB(125, 0, 0, 0);

        int xcenter = getWidth()/2;
		int ycenter = getHeight()/2;

        int min = (getWidth() > getHeight())?getHeight():getWidth();

		canvas.drawCircle(xcenter, ycenter, (min-200)/2, mLinePaint);//透明区域

        Log.d(TAG, "xcenter == " + xcenter + ", ycenter == " + ycenter + ", radius == " +
                (min-200)/2);

		super.onDraw(canvas);
	}
	
	
}
