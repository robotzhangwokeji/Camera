package com.mycamera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * Created by suziqiang on 2017/12/15.
 */

public class AutoFitView extends TextureView {
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    public AutoFitView(Context context) {
        super(context);
    }

    public AutoFitView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void setAspectRatio(int width,int height) {
        mRatioHeight = height;
        mRatioWidth = width;

        requestLayout();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.e("aaaa", "width=" + width + " height=" + height);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);

            }
        }


    }
}
