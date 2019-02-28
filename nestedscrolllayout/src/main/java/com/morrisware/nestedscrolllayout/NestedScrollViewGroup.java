package com.morrisware.nestedscrolllayout;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by mmw on 2019/2/28.
 **/
public class NestedScrollViewGroup extends FrameLayout {

    private View headerLayout;
    private View scrollLayout;

    private ViewOffsetHelper mViewOffsetHelper;

    private int mTempTopBottomOffset = 0;
    private int mTempLeftRightOffset = 0;

    private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);
    private int mLastMotionY;

    @NonNull
    private static Rect acquireTempRect() {
        Rect rect = sRectPool.acquire();
        if (rect == null) {
            rect = new Rect();
        }
        return rect;
    }

    private static void releaseTempRect(@NonNull Rect rect) {
        rect.setEmpty();
        sRectPool.release(rect);
    }

    public NestedScrollViewGroup(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollViewGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollViewGroup(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new RuntimeException("getChildCount empty");
        }
        headerLayout = getChildAt(0);
        scrollLayout = getChildAt(1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        headerLayout.layout(0, 0, headerLayout.getMeasuredWidth(), headerLayout.getMeasuredHeight());
        if (mViewOffsetHelper == null) {
            mViewOffsetHelper = new ViewOffsetHelper(headerLayout);
        }
        mViewOffsetHelper.onViewLayout();

        if (mTempTopBottomOffset != 0) {
            mViewOffsetHelper.setTopAndBottomOffset(mTempTopBottomOffset);
            mTempTopBottomOffset = 0;
        }
        if (mTempLeftRightOffset != 0) {
            mViewOffsetHelper.setLeftAndRightOffset(mTempLeftRightOffset);
            mTempLeftRightOffset = 0;
        }

        scrollLayout.layout(0, headerLayout.getBottom(), scrollLayout.getMeasuredWidth(),
                scrollLayout.getMeasuredHeight() + headerLayout.getBottom());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = (int) ev.getY();
                Log.d("TAG", "onInterceptTouchEvent down");
                return true;
            case MotionEvent.ACTION_MOVE:
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                if (isPointInChildBounds(headerLayout, x,y)) {

                }


                Log.d("TAG", "onInterceptTouchEvent move");
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d("TAG", "onTouchEvent down");
                return true;
            case MotionEvent.ACTION_MOVE:
                Log.d("TAG", "onTouchEvent move");
                break;
        }


        return super.onTouchEvent(event);
    }

    private boolean isPointInChildBounds(View child, int x, int y) {
        final Rect r = acquireTempRect();
        r.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        try {
            return r.contains(x, y);
        } finally {
            releaseTempRect(r);
        }
    }


}
