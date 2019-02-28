package com.morrisware.nestedscrolllayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/**
 * Created by mmw on 2019/2/26.
 **/
public class NestedScrollLayout extends FrameLayout implements NestedScrollingParent2 {

    private View headerLayout;
    private View scrollLayout;

    private ViewOffsetHelper mViewOffsetHelper;

    private int mTempTopBottomOffset = 0;
    private int mTempLeftRightOffset = 0;

    OverScroller mScroller;

    private NestedScrollingParentHelper mNestedScrollingParentHelper =
            new NestedScrollingParentHelper(this);
    private int lastY;
    private FlingRunnable mFlingRunnable;

    public NestedScrollLayout(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

    private int getTotalScrollRange() {
        int range = 0;
        if (headerLayout != null) {
            range = headerLayout.getMeasuredHeight();
            if (headerLayout instanceof NestedScrollView) {
                NestedScrollView parent = (NestedScrollView) headerLayout;
                if (parent.getChildCount() > 0) {
                    View view = parent.getChildAt(0);
                    if (view instanceof ViewGroup) {
                        ViewGroup viewGroup = (ViewGroup) view;
                        for (int i = 0; i < viewGroup.getChildCount(); i++) {
                            View child = viewGroup.getChildAt(i);
                            if (child instanceof TabLayout) {
                                range -= child.getMeasuredHeight();
                            }
                        }
                    }
                }
            }
        }
        return Math.max(0, range);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) > 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        mNestedScrollingParentHelper.onStopNestedScroll(target, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (dy < 0 && scrollLayout.canScrollVertically(-1)) {
            return;
        }
//        Log.d("TAG", "onNestedPreScroll:dy" + dy + "====type:" + type);

        int min, max;
        min = -getTotalScrollRange();
        max = 0;
        final int curOffset = getTopAndBottomOffset();
        int newOffset = MathUtils.clamp(curOffset - dy, min, max);
        if (curOffset != newOffset) {
            setTopAndBottomOffset(newOffset);
            consumed[1] = curOffset - newOffset;
        }

        if (target == headerLayout && type == ViewCompat.TYPE_NON_TOUCH) {
            if (dy > 0) {
                if (consumed[1] != dy) {
                    int unconsumedY = dy - consumed[1];
                    if (scrollLayout instanceof NestedScrollingParent) {
                        ((NestedScrollingParent) scrollLayout).onNestedScroll(target, 0, consumed[1], 0, unconsumedY);
                    }
                    consumed[1] = dy;
                }
            }
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        Log.d("TAG", "onNestedScroll:dyConsumed" + dyConsumed + "====dyUnconsumed" + dyUnconsumed + "====type:" + type);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.d("TAG", "onNestedPreFling:velocityY" + velocityY);
        return super.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.d("TAG", "onNestedFling:velocityY" + velocityY + "====consumed:" + consumed);
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    private class FlingRunnable implements Runnable {

        private View header;

        public FlingRunnable(View header) {
            this.header = header;
        }

        @Override
        public void run() {
            if (header != null && mScroller != null) {
                if (mScroller.computeScrollOffset()) {
                    Log.d("TAG", "getCurrVelocity:" + mScroller.getCurrVelocity());

                    setTopAndBottomOffset(mScroller.getCurrY());
                    // Post ourselves so that we run on the next animation
                    ViewCompat.postOnAnimation(header, this);
                } else {
                    Log.d("TAG", "final getCurrVelocity:" + mScroller.getCurrVelocity());
//                    onFlingFinished(mParent, mLayout);
                }
            }
        }
    }

    public boolean setTopAndBottomOffset(int offset) {
        if (mViewOffsetHelper != null) {
            ViewCompat.offsetTopAndBottom(scrollLayout, offset - getTopAndBottomOffset());
            return mViewOffsetHelper.setTopAndBottomOffset(offset);
        } else {
            mTempTopBottomOffset = offset;
        }
        return false;
    }

    public boolean setLeftAndRightOffset(int offset) {
        if (mViewOffsetHelper != null) {
            return mViewOffsetHelper.setLeftAndRightOffset(offset);
        } else {
            mTempLeftRightOffset = offset;
        }
        return false;
    }

    public int getTopAndBottomOffset() {
        return mViewOffsetHelper != null ? mViewOffsetHelper.getTopAndBottomOffset() : 0;
    }

    public int getLeftAndRightOffset() {
        return mViewOffsetHelper != null ? mViewOffsetHelper.getLeftAndRightOffset() : 0;
    }
}
