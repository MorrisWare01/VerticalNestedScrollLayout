package com.morrisware.nestedscrolllayout;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.math.MathUtils;
import android.support.v4.util.Pools;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/**
 * Created by mmw on 2019/2/28.
 **/
public class NestedScrollViewGroup extends FrameLayout implements NestedScrollingParent2 {

    private View headerLayout;
    private View scrollLayout;

    private ViewOffsetHelper mViewOffsetHelper;

    private int mTempTopBottomOffset = 0;
    private int mTempLeftRightOffset = 0;

    private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);
    private int mLastMotionY;

    private NestedScrollingParentHelper mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
    private boolean isInHeaderLayout;
    private VelocityTracker mVelocityTracker;
    private MotionEvent currentEvent;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private OverScroller mScroller;
    private int mLastScrollerY;

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
        mScroller = new OverScroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
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
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int x = (int) ev.getX();
        final int y = (int) ev.getY();
        final int dy = mLastMotionY - y;

        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                if (isPointInChildBounds(headerLayout, x, y)) {
                    isInHeaderLayout = true;
                } else {
                    isInHeaderLayout = false;
                }
                headerLayout.onTouchEvent(ev);
                scrollLayout.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isInHeaderLayout) {
                    if (dy > 0) {
                        if (headerLayout.canScrollVertically(dy)) {
                            headerLayout.onTouchEvent(ev);
                        } else {
                            int min, max;
                            min = -getTotalScrollRange();
                            max = 0;
                            final int curOffset = getTopAndBottomOffset();
                            int newOffset = MathUtils.clamp(curOffset - dy, min, max);
                            if (curOffset != newOffset) {
                                setTopAndBottomOffset(newOffset);
                            }
                        }
                    } else {
                        if (getTopAndBottomOffset() == 0 && headerLayout.canScrollVertically(dy)) {
                            headerLayout.onTouchEvent(ev);
                        } else {
                            int min, max;
                            min = -getTotalScrollRange();
                            max = 0;
                            final int curOffset = getTopAndBottomOffset();
                            int newOffset = MathUtils.clamp(curOffset - dy, min, max);
                            if (curOffset != newOffset) {
                                setTopAndBottomOffset(newOffset);
                            }
                        }
                    }
                } else {
                    if (scrollLayout.canScrollVertically(dy)) {
                        scrollLayout.onTouchEvent(ev);
                    } else {
                        scrollLayout.onTouchEvent(ev);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if (isInHeaderLayout) {
                    boolean consumed = false;
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        if (getTopAndBottomOffset() != 0) {
                            fling(initialVelocity);
                            consumed = true;
                        }
                    }
                    if (!consumed) {
                        headerLayout.onTouchEvent(ev);
                    }
                } else {
                    scrollLayout.onTouchEvent(ev);
                }
                recycleVelocityTracker();
                break;
        }

        mLastMotionY = y;
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(ev);
        }
        return true;
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            mScroller.fling(0, getTopAndBottomOffset(), // start
                    0, velocityY, // velocities
                    0, 0, // x
                    Integer.MIN_VALUE, Integer.MAX_VALUE, // y
                    0, 0); // overscroll
            mLastScrollerY = getTopAndBottomOffset();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            final int y = mScroller.getCurrY();
            int dy = mLastScrollerY - y;

            int min, max;
            min = -getTotalScrollRange();
            max = 0;
            final int curOffset = getTopAndBottomOffset();
            int newOffset = MathUtils.clamp(curOffset - dy, min, max);
            if (curOffset != newOffset) {
                setTopAndBottomOffset(newOffset);
                dy -= curOffset - newOffset;
            }

            if (dy != 0) {
                final long now = System.currentTimeMillis();
                final long motionY = mLastMotionY + (y - mScroller.getStartX());
                if (headerLayout.canScrollVertically(dy)) {
                    headerLayout.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, motionY, 0));
                } else {
                    scrollLayout.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, motionY, 0));
                }
            }

            mLastScrollerY = y;
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            mLastScrollerY = 0;
        }
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
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            Log.d("TAG", "onNestedPreScroll===dy:" + dy);

            if (isInHeaderLayout) {
                if (dy > 0) {
                    if (headerLayout.canScrollVertically(dy)) {
                        return;
                    } else {
                        int min, max;
                        min = -getTotalScrollRange();
                        max = 0;
                        final int curOffset = getTopAndBottomOffset();
                        int newOffset = MathUtils.clamp(curOffset - dy, min, max);
                        if (curOffset != newOffset) {
                            setTopAndBottomOffset(newOffset);
                            consumed[1] = curOffset - newOffset;
                        }

                        final int dyUnconsumed = dy - consumed[1];
                        if (dyUnconsumed != 0) {
                            final int y = mLastMotionY - dyUnconsumed;
                            final long now = System.currentTimeMillis();
                            scrollLayout.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, y, 0));
                            mLastMotionY = y;
                            consumed[1] = dy;
                        }
                    }
                } else {
//                    if (headerLayout.canScrollVertically(dy)) {
//                        return;
//                    } else {
//                        int min, max;
//                        min = -getTotalScrollRange();
//                        max = 0;
//                        final int curOffset = getTopAndBottomOffset();
//                        int newOffset = MathUtils.clamp(curOffset - dy, min, max);
//                        if (curOffset != newOffset) {
//                            setTopAndBottomOffset(newOffset);
//                            consumed[1] = dy;
//                        }
//                    }
                }
            }
        }

//        if (target.canScrollVertically(dy)) {
//            return;
//        }
//
//        int min, max;
//        min = -getTotalScrollRange();
//        max = 0;
//        final int curOffset = getTopAndBottomOffset();
//        int newOffset = MathUtils.clamp(curOffset - dy, min, max);
//        if (curOffset != newOffset) {
//            setTopAndBottomOffset(newOffset);
//            consumed[1] = curOffset - newOffset;
//        }
//
//        if (target == headerLayout && type == ViewCompat.TYPE_NON_TOUCH) {
//            if (dy > 0) {
//                if (consumed[1] != dy) {
//                    int unconsumedY = dy - consumed[1];
//                    if (scrollLayout instanceof NestedScrollingParent) {
//                        ((NestedScrollingParent) scrollLayout).onNestedScroll(target, 0, consumed[1], 0, unconsumedY);
//                    }
//                    consumed[1] = dy;
//                }
//            }
//        }
        Log.d("TAG", "onNestedPreScroll:dy" + dy + "====type:" + type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        Log.d("TAG", "onNestedScroll:dyConsumed" + dyConsumed + "====dyUnconsumed" + dyUnconsumed + "====type:" + type);
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

    public int getTopAndBottomOffset() {
        return mViewOffsetHelper != null ? mViewOffsetHelper.getTopAndBottomOffset() : 0;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
}
