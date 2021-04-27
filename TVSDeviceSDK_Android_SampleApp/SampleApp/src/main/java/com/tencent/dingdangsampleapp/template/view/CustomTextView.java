package com.tencent.dingdangsampleapp.template.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by wzl on 2016/6/24.
 */
public class CustomTextView extends TextView {
    private Paint mPaint = null;
    private Paint.FontMetrics fm;
    private float offset;
    private String content = "";
    private float lineSpace = 0;

    public CustomTextView(Context context) {
        super(context);
        init();
    }

    public CustomTextView(Context context, AttributeSet set) {
        super(context, set, 0);
        init();
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(dip2px(getContext(),16));
        initParams();
    }


    private void initParams() {
        fm = mPaint.getFontMetrics();
        if (lineSpace > 0) {
            fm.leading = lineSpace;
        }else{
            fm.leading = dip2px(getContext(),1);
        }
        offset = fm.descent - fm.ascent + fm.leading;
    }

    public static int dip2px(Context context,float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            int textWidth = (int) mPaint.measureText(content);
            width = textWidth + getPaddingLeft() + getPaddingRight() > widthSize ? widthSize : textWidth + getPaddingLeft() + getPaddingRight();
        } else {
            int textWidth = (int) mPaint.measureText(content);
            width = textWidth + getPaddingLeft() + getPaddingRight();
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            int lines = calculateLines(content, width - getPaddingLeft() - getPaddingRight()).size();
            int indeedHeight = getPaddingTop() + getPaddingBottom() + (int) offset * lines + (int)fm.bottom;
            height = indeedHeight > heightSize ? heightSize : indeedHeight;
        } else {
            int lines = calculateLines(content, width - getPaddingLeft() - getPaddingRight()).size();
            height = getPaddingTop() + getPaddingBottom() + (int) offset * lines + (int)fm.bottom;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = getPaddingLeft();
        float y = -fm.top + getPaddingTop();
        ArrayList<String> list = calculateLines(content, getWidth() - getPaddingLeft() - getPaddingRight());
        for (String text : list) {
            canvas.drawText(text, x, y, mPaint);
            y += offset;
        }
    }

    private ArrayList<String> list = new ArrayList<>(0);

    private ArrayList<String> calculateLines(String content, int width) {
        list.clear();
        int length = content.length();
        float textWidth = mPaint.measureText(content);
        if (textWidth <= width) {
            list.add(content);
            return list;
        }

        int start = 0, end = 1;
        while (start < length) {
            if (mPaint.measureText(content, start, end) > width) {
                list.add(content.substring(start, end - 1));
                start = end - 1;
            } else if (end < length) {
                end++;
            }
            if (end == length) {
                list.add(content.subSequence(start, end).toString());
                break;
            }
        }
        return list;
    }

    public void setText(String text) {
        if (null == text || text.trim().length() == 0) {
            content = "";
        } else {
            content = text;
        }
        invalidate();
    }

    /**
     * @param textColor R.color.xx
     */
    public void setTextColor(@ColorRes int textColor) {
        mPaint.setColor(getResources().getColor(textColor));
        invalidate();
    }

    /**
     * @param textSize R.dimen.xx
     */
    public void setTextSize(int textSize) {
        mPaint.setTextSize(textSize);
        initParams();
        invalidate();
    }

    /**
     * @param spacing R.dimen.xx
     */
    public void setLineSpacingExtra(@DimenRes int spacing) {
        this.lineSpace = getResources().getDimension(spacing);
        initParams();
        invalidate();
    }
}