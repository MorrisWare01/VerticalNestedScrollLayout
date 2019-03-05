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

    private NestedScrollingParentHelper mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);

    private boolean isInHeaderLayout;
    private int mLastMotionX;
    private int mLastMotionY;
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
//        final int x = (int) ev.getX();
//        final int y = (int) ev.getY();
//        final int action = ev.getActionMasked();
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                Log.d("TAG", "onInterceptTouchEvent:ACTION_DOWN");
//                if (!mScroller.isFinished()) {
//                    mScroller.abortAnimation();
//                }
//                isInHeaderLayout = isPointInChildBounds(headerLayout, x, y);
//                if (isInHeaderLayout) {
//                    if (getTopAndBottomOffset() != 0) {
//                        return true;
//                    }
//                } else {
//
//                }
//                break;
//            case MotionEvent.ACTION_MOVE:
//                Log.d("TAG", "onInterceptTouchEvent:ACTION_MOVE");
//                final int dx = mLastMotionX - x;
//                final int dy = mLastMotionY - y;
//                if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > mTouchSlop) {
//                    if (isInHeaderLayout) {
//
//                    } else {
//                        if (dy > 0) {
//                            if (getTopAndBottomOffset() != -getTotalScrollRange()) {
//                                return true;
//                            }
//                        } else {
//                            if (getTopAndBottomOffset() != -getTotalScrollRange()) {
//                                return true;
//                            }
//                        }
//                    }
//                }
//                break;
//        }
//
//        mLastMotionX = x;
//        mLastMotionY = y;
//        return super.onInterceptTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();

        final int x = (int) ev.getX();
        final int y = (int) ev.getY();
        final int dx = mLastMotionX - x;
        final int dy = mLastMotionY - y;

        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.d("TAG", "onTouchEvent:ACTION_DOWN");
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                isInHeaderLayout = isPointInChildBounds(headerLayout, x, y);
                if (isInHeaderLayout) {
                    headerLayout.dispatchTouchEvent(ev);
                } else {
                    scrollLayout.dispatchTouchEvent(ev);
                }
                Log.d("TAG", "isInHeaderLayout:" + isInHeaderLayout);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("TAG", "onTouchEvent:ACTION_MOVE");
                if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > mTouchSlop) {
                    if (isInHeaderLayout) {
                        if (dy > 0) {
                            if (headerLayout.canScrollVertically(dy)) {
                                headerLayout.dispatchTouchEvent(ev);
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
                            if (getTopAndBottomOffset() == 0) {
                                headerLayout.dispatchTouchEvent(ev);
                            } else {
                                if (!scrollLayout.canScrollVertically(dy)) {
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
                        }
                    } else {
                        if (dy > 0) {
                            if (getTopAndBottomOffset() > -getTotalScrollRange()) {
                                int min, max;
                                min = -getTotalScrollRange();
                                max = 0;
                                final int curOffset = getTopAndBottomOffset();
                                int newOffset = MathUtils.clamp(curOffset - dy, min, max);
                                if (curOffset != newOffset) {
                                    setTopAndBottomOffset(newOffset);
                                }
                            } else {
                                if (scrollLayout.canScrollVertically(dy)) {
                                    scrollLayout.dispatchTouchEvent(ev);
                                }
                            }
                        } else {
                            if (scrollLayout.canScrollVertically(dy)) {
                                scrollLayout.dispatchTouchEvent(ev);
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
                    }
                } else {
                    if (isInHeaderLayout) {
                        headerLayout.dispatchTouchEvent(ev);
                    } else {
                        scrollLayout.dispatchTouchEvent(ev);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d("TAG", "onTouchEvent:ACTION_UP");
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                Log.d("TAG", "initialVelocity:" + initialVelocity);


                if (Math.abs(dy) > Math.abs(dx) && Math.abs(initialVelocity) > mMinimumVelocity) {
                    if (isInHeaderLayout) {
                        boolean consumed = false;
                        if (getTopAndBottomOffset() != 0) {
                            if (initialVelocity < 0) { // dy < 0
                                fling(initialVelocity);
                                consumed = true;
                            } else if (initialVelocity > 0) { // dy > 0
                                if (!scrollLayout.canScrollVertically(-initialVelocity)) {
                                    fling(initialVelocity);
                                    consumed = true;
                                }
                            }
                        }
                        if (!consumed) {
                            headerLayout.dispatchTouchEvent(ev);
                        }
                    } else {
                        boolean consumed = false;
                        if (getTopAndBottomOffset() != 0) {
                            if (initialVelocity < 0) { // dy < 0
                                fling(initialVelocity);
                                consumed = true;
                            } else if (initialVelocity > 0) { // dy > 0
                                if (!scrollLayout.canScrollVertically(-initialVelocity)) {
                                    fling(initialVelocity);
                                    consumed = true;
                                }
                            }
                        }

                        if (!consumed) {
                            scrollLayout.dispatchTouchEvent(ev);
                        }
                    }
                } else {
                    boolean res;
                    if (isInHeaderLayout) {
                        res = headerLayout.dispatchTouchEvent(ev);
                    } else {
                        res = scrollLayout.dispatchTouchEvent(ev);
                    }
                    if (!res) {
                        super.onTouchEvent(ev);
                    }
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
                if (headerLayout.canScrollVertically(dy)) {
                    headerLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                    headerLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dy, 0));
                } else {
                    if (scrollLayout.canScrollVertically(dy)) {
                        scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                        scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dy, 0));
                    }
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
//        Log.d("TAG", "onNestedPreScroll:dy" + dy + "====type:" + type);
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            if (isInHeaderLayout) {
                if (dy > 0) {
                    if (!headerLayout.canScrollVertically(dy)) {
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
                            final long now = System.currentTimeMillis();
                            scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                            scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dyUnconsumed, 0));
                            consumed[1] = dy;
                        }
                    }
                }
            } else {
                if (dy < 0) {
                    if (!scrollLayout.canScrollVertically(dy)) {
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
                            final long now = System.currentTimeMillis();
                            headerLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                            headerLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dyUnconsumed, 0));
                            consumed[1] = dy;
                        }
                    }
                }
            }
        } else {
            if (isInHeaderLayout) {
                if (dy > 0) {
                    if (!headerLayout.canScrollVertically(dy)) {
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
                            final long now = System.currentTimeMillis();
                            scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
                            scrollLayout.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dyUnconsumed, 0));
                            consumed[1] = dy;
                        }
                    }
                }
            } else {
//                if (dy > 0) {
//                    if (!scrollLayout.canScrollVertically(dy)) {
//                        int min, max;
//                        min = -getTotalScrollRange();
//                        max = 0;
//                        final int curOffset = getTopAndBottomOffset();
//                        int newOffset = MathUtils.clamp(curOffset - dy, min, max);
//                        if (curOffset != newOffset) {
//                            setTopAndBottomOffset(newOffset);
//                            consumed[1] = curOffset - newOffset;
//                        }
//
//                        final int dyUnconsumed = dy - consumed[1];
//                        if (dyUnconsumed != 0) {
//                            final long now = System.currentTimeMillis();
//                            headerLayout.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0, 0, 0));
//                            headerLayout.onTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0, -dyUnconsumed, 0));
//                            consumed[1] = dy;
//                        }
//                    }
//                }
            }
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
//        Log.d("TAG", "onNestedScroll:dyConsumed" + dyConsumed + "====dyUnconsumed" + dyUnconsumed + "====type:" + type);
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
