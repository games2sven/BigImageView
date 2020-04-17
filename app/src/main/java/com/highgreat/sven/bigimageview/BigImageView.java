package com.highgreat.sven.bigimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

public class BigImageView extends View implements View.OnTouchListener, GestureDetector.OnGestureListener {

    private Rect mRect;
    private BitmapFactory.Options mOptions;
    private BitmapRegionDecoder mDecoder;
    private int mImageWidth;
    private int mImageHeight;
    private int mViewHeight;
    private int mViewWidth;
    private float mScale;
    private Bitmap bitmap;
    private GestureDetector gestureDetector;
    private Scroller mScroller;

    public BigImageView(Context context) {
        this(context,null);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //指定要加载的区域
        mRect = new Rect();
        //需要复用
        mOptions = new BitmapFactory.Options();
        //手势识别
        gestureDetector = new GestureDetector(context,this);
        setOnTouchListener(this);

        //滑动帮助
        mScroller = new Scroller(context);
    }

    /**
     * 由使用者输入一张图片
     * @param is
     */
    public void setImage(InputStream is){
        //先读取原图片的信息  宽高（这个时候不往内存加载）
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is,null,mOptions);
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        Log.i("Sven","mImageWidth = "+ mImageWidth +" mImageHeight = "+ mImageHeight);
        //开启复用
        mOptions.inMutable = true;
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;//占位为2个字节
        mOptions.inJustDecodeBounds = false;

        //创建一个区域解码器
        try {
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //会导致onLayout,OnMesure,ondraw方法都调用
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //获取测量的view的大小
        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();
        Log.i("Sven","mViewWidth = "+ mViewWidth +" mViewHeight = "+ mViewHeight);

        //确定要加载的图片的区域
        mRect.top = 0;
        mRect.left = 0;
        mRect.right = mViewWidth;
        //获取一个缩放因子
        mScale=mViewWidth/(float)mImageWidth;
        //高度就根据缩放比进行获取
        mRect.bottom=(int)(mViewHeight/mScale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //如果解码器拿不到，表示没有设置过要显示的图片
        if(null==mDecoder){
            return;
        }

        //复用上一张bitmap
        mOptions.inBitmap = bitmap;

        //解码指定的区域
        bitmap = mDecoder.decodeRegion(mRect, mOptions);
        //矩阵缩放
        Matrix matrix = new Matrix();
        matrix.setScale(mScale,mScale);
        canvas.drawBitmap(bitmap,matrix,null);
    }

    //将手势交给gestureDetector去处理
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {

        //如果移动还没有停止，强制停止
        if(!mScroller.isFinished()){
            mScroller.forceFinished(true);
        }

        //（具备点击功能的view默认是会消费掉按下事件，不具备的就需要手动消费掉后续的事件，即返回true）
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //移动的时候需要改变显示的区域
        mRect.offset(0, (int) distanceY);
        //处理上下两个顶端的问题
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - (int) (mViewHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mViewHeight / mScale);
        }
        //重绘
        invalidate();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        mScroller.fling(0,mRect.top,0,
                (int) -velocityY,0,0,0,
                mImageHeight-(int)(mViewHeight/mScale));
        return false;
    }


    //计算当前滑动到什么位置，或者是否滑动完成
    @Override
    public void computeScroll() {
        if(mScroller.isFinished()){
            return;
        }
        //true表示还没有滑动结束
        if(mScroller.computeScrollOffset()){
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + (int)(mViewHeight/mScale);
            invalidate();
        }
    }
}
