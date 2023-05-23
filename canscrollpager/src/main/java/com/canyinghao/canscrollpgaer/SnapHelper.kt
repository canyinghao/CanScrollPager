
package com.canyinghao.canscrollpgaer


import androidx.recyclerview.widget.RecyclerView.OnFlingListener
import androidx.recyclerview.widget.RecyclerView
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import kotlin.Throws
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.recyclerview.widget.LinearSmoothScroller
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.max

abstract class SnapHelper : OnFlingListener() {
    private var mRecyclerView: RecyclerView? = null
    private var mGravityScroller: Scroller? = null
    protected var mOnViewPagerListener: OnScrollPagerListener? = null
    var lastPosition = 0
    var velocityY = 0
        protected set
    var scrollY = 0
    fun setOnPageChangedListener(listener: OnScrollPagerListener?) {
        mOnViewPagerListener = listener
    }


    private val mChildAttachStateChangeListener: OnChildAttachStateChangeListener =
        object : OnChildAttachStateChangeListener {
            var releasePosition = -1
            var lastVelocityY = 0
            override fun onChildViewAttachedToWindow(view: View) {

            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (velocityY == lastVelocityY) return
                lastVelocityY = velocityY
                if (velocityY >= 0) {
                    if (mOnViewPagerListener != null) {
                        val position = getPosition(view)
                        val currentP = snapPosition
                        if (position == currentP && position != 1) {
                            return
                        }
                        if (releasePosition == position && position != 1) {
                            return
                        }
                        releasePosition = position
                        mOnViewPagerListener!!.onPageRelease(true, releasePosition, view)
                    }
                } else {
                    if (mOnViewPagerListener != null) {
                        val position = getPosition(view)
                        val currentP = snapPosition
                        if (position == currentP && position != 1) {
                            return
                        }
                        if (releasePosition == position && position != 1) {
                            return
                        }
                        releasePosition = position
                        mOnViewPagerListener!!.onPageRelease(false, releasePosition, view)
                    }
                }
            }
        }

    // Handles the snap on scroll case.
    private val mScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            private var mScrolled = false
            private var recyclerView: RecyclerView? = null
            private val runnable = Runnable {
                if (recyclerView != null && recyclerView!!.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    stopScroll(recyclerView!!, false)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                this.recyclerView = recyclerView
                try {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        scrollY = 0
                        recyclerView.tag = null
                    }
                    if (newState == RecyclerView.SCROLL_STATE_IDLE && mScrolled) {
                        mScrolled = false
                        stopScroll(recyclerView, true)
                        recyclerView.removeCallbacks(runnable)
                        recyclerView.postDelayed(runnable, 1000)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                this.recyclerView = recyclerView
                if (dx != 0 || dy != 0) {
                    mScrolled = true
                }
            }
        }
    private val snapPosition: Int
        get() {
            val layoutManager = mRecyclerView!!.layoutManager ?: return -1
            val snapView = findSnapView(layoutManager) ?: return -1
            return layoutManager.getPosition(snapView)
        }

    private fun getPosition(view: View): Int {
        val layoutManager = mRecyclerView!!.layoutManager ?: return -1
        return layoutManager.getPosition(view)
    }



    fun stopScroll(recyclerView: RecyclerView, isCallBack: Boolean) {
        try {
            val layoutManager = recyclerView.layoutManager ?: return
            val snapView = findSnapView(layoutManager) ?: return
            val position = layoutManager.getPosition(snapView)
            if (snapView.tag is Boolean) {
                var b = snapView.tag as Boolean
                if (!b) {
                    var v = layoutManager.findViewByPosition(position + 1)
                    if (v != null && v.height < 10) {
                        v = layoutManager.findViewByPosition(position + 2)
                    }
                    if (v != null && v.tag is Boolean) {
                        b = v.tag as Boolean
                        if (b) {
                            val snapDistance = getPicSnapDistance(snapView, layoutManager, false)
                            if (snapDistance[0] != 0 || snapDistance[1] != 0) {
                                mRecyclerView!!.smoothScrollBy(snapDistance[0], snapDistance[1])
                            }
                            if (isCallBack) {
                                callback(lastPosition, position, velocityY > 0, snapView)
                                lastPosition = position
                            }
                            return
                        }
                    }
                    v = layoutManager.findViewByPosition(position - 1)
                    if (v != null && v.tag is Boolean) {
                        b = v.tag as Boolean
                        if (b) {
                            val snapDistance = getPicSnapDistance(snapView, layoutManager, true)
                            if (snapDistance[0] != 0 || snapDistance[1] != 0) {
                                mRecyclerView!!.smoothScrollBy(snapDistance[0], snapDistance[1])
                            }
                            if (isCallBack) {
                                callback(lastPosition, position, velocityY > 0, snapView)
                                lastPosition = position
                            }
                            return
                        }
                    }
                    if (isCallBack) {
                        callback(lastPosition, position, velocityY > 0, snapView)
                        lastPosition = position
                    }
                    return
                } else {
                    if (isCallBack) {
                        callback(lastPosition, position, velocityY > 0, snapView)
                        lastPosition = position
                    }
                }
            }
            val snapDistance = calculateDistanceToFinalSnap(layoutManager, snapView)
            if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                mRecyclerView!!.smoothScrollBy(snapDistance[0], snapDistance[1])
            }
            if (isCallBack) {
                callback(lastPosition, position, velocityY > 0, snapView)
                lastPosition = position
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    abstract fun isForwardFling(
        layoutManager: RecyclerView.LayoutManager?, velocityX: Int,
        velocityY: Int
    ): Boolean

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        val layoutManager = mRecyclerView!!.layoutManager ?: return false
        if (mRecyclerView!!.adapter == null) {
            return false
        }
        try {
            this.velocityY = velocityY
            scrollY = velocityY
            val minFlingVelocity = mRecyclerView!!.minFlingVelocity
            return ((abs(velocityY) > minFlingVelocity || abs(velocityX) > minFlingVelocity)
                    && snapFromFling(layoutManager, velocityX, velocityY))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Attaches the [SnapHelper] to the provided RecyclerView, by calling
     * [RecyclerView.setOnFlingListener].
     * You can call this method with `null` to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     * `null` if you want to remove SnapHelper from the current
     * RecyclerView.
     * @throws IllegalArgumentException if there is already a [RecyclerView.OnFlingListener]
     * attached to the provided [RecyclerView].
     */
    @Throws(IllegalStateException::class)
    open fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks()
        }
        mRecyclerView = recyclerView
        if (mRecyclerView != null) {
            setupCallbacks()
            mGravityScroller = Scroller(
                mRecyclerView!!.context,
                DecelerateInterpolator()
            )
            snapToTargetExistingView()
        }
    }

    /**
     * Called when an instance of a [RecyclerView] is attached.
     */
    private fun setupCallbacks() {
        if (mRecyclerView!!.onFlingListener != null) {
            return
        }
        mRecyclerView!!.addOnScrollListener(mScrollListener)
        mRecyclerView!!.addOnChildAttachStateChangeListener(mChildAttachStateChangeListener)
        mRecyclerView!!.onFlingListener = this
    }

    /**
     * Called when the instance of a [RecyclerView] is detached.
     */
    private fun destroyCallbacks() {
        mRecyclerView!!.removeOnScrollListener(mScrollListener)
        mRecyclerView!!.removeOnChildAttachStateChangeListener(mChildAttachStateChangeListener)
        mRecyclerView!!.onFlingListener = null
    }

    /**
     * Calculated the estimated scroll distance in each direction given velocities on both axes.
     *
     * @param velocityX Fling velocity on the horizontal axis.
     * @param velocityY Fling velocity on the vertical axis.
     * @return array holding the calculated distances in x and y directions
     * respectively.
     */
    fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray {
        val outDist = IntArray(2)
        mGravityScroller!!.fling(
            0,
            0,
            velocityX,
            velocityY,
            Int.MIN_VALUE,
            Int.MAX_VALUE,
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        outDist[0] = mGravityScroller!!.finalX
        outDist[1] = mGravityScroller!!.finalY
        return outDist
    }

    /**
     * Helper method to facilitate for snapping triggered by a fling.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @param velocityX     Fling velocity on the horizontal axis.
     * @param velocityY     Fling velocity on the vertical axis.
     * @return true if it is handled, false otherwise.
     */
    private fun snapFromFling(
        layoutManager: RecyclerView.LayoutManager, velocityX: Int,
        velocityY: Int
    ): Boolean {
        if (layoutManager !is ScrollVectorProvider) {
            return false
        }
        var smoothScroller:SmoothScroller? = createScroller(layoutManager) ?: return false
        val targetPosition = findTargetSnapPosition(layoutManager, velocityX, velocityY)
        if (targetPosition == RecyclerView.NO_POSITION) {
            return false
        }
        val snapView = findSnapView(layoutManager)
        var isPager = false
        var position = 0
        if (snapView != null && snapView.tag is Boolean) {
            isPager = snapView.tag as Boolean
            position = layoutManager.getPosition(snapView)
            Log.d("snap position", "" + position)
        }
        val targetView = layoutManager.findViewByPosition(targetPosition)
        var isTargetPager = false
        if (targetView != null && targetView.tag is Boolean) {
            isTargetPager = targetView.tag as Boolean
        }
        Log.e("snap targetPosition", "$targetPosition   $targetView")
        if (isTargetPager) {
            mRecyclerView!!.tag = true
            smoothScroller?.targetPosition = targetPosition
            layoutManager.startSmoothScroll(smoothScroller)
            Log.d("snap isTargetPager", "$targetPosition   $position")
            return true
        } else if (!isPager) {
            return false
        } else {
            if (position > targetPosition) {
                mRecyclerView!!.tag = true
                smoothScroller = createPicSnapScroller(layoutManager, false)
                smoothScroller?.targetPosition = targetPosition
                layoutManager.startSmoothScroll(smoothScroller)
            } else {
                mRecyclerView!!.tag = true
                smoothScroller = createPicSnapScroller(layoutManager, true)
                smoothScroller?.targetPosition = targetPosition
                layoutManager.startSmoothScroll(smoothScroller)
            }

        }
        return true
    }

    /**
     * Snaps to a target view which currently exists in the attached [RecyclerView]. This
     * method is used to snap the view when the [RecyclerView] is first attached; when
     * snapping was triggered by a scroll and when the fling is at its final stages.
     */
    private fun snapToTargetExistingView() {
        try {
            if (mRecyclerView == null) {
                return
            }
            val layoutManager = mRecyclerView!!.layoutManager ?: return
            val snapView = findSnapView(layoutManager) ?: return
            if (snapView.tag is Boolean) {
                val b = snapView.tag as Boolean
                if (!b) {
                    return
                }
            }
            val snapDistance = calculateDistanceToFinalSnap(layoutManager, snapView)
            if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                mRecyclerView!!.smoothScrollBy(snapDistance[0], snapDistance[1])
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a scroller to be used in the snapping implementation.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @return a [RecyclerView.SmoothScroller] which will handle the scrolling.
     */
    private fun createScroller(layoutManager: RecyclerView.LayoutManager?): SmoothScroller? {
        return createSnapScroller(layoutManager)
    }

    /**
     * Creates a scroller to be used in the snapping implementation.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @return a [LinearSmoothScroller] which will handle the scrolling.
     */

    protected open fun createSnapScroller(layoutManager: RecyclerView.LayoutManager?): LinearSmoothScroller? {
        return if (layoutManager !is ScrollVectorProvider) {
            null
        } else object : LinearSmoothScroller(mRecyclerView!!.context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action
            ) {
                if (mRecyclerView == null) {
                    // The associated RecyclerView has been removed so there is no action to take.
                    return
                }
                val snapDistances = calculateDistanceToFinalSnap(
                    mRecyclerView!!.layoutManager!!,
                    targetView
                )
                val dx = snapDistances!![0]
                val dy = snapDistances[1]
                val time = calculateTimeForDeceleration(max(abs(dx), abs(dy)))
                if (time > 0) {
                    action.update(dx, dy, time, mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }
        }
    }

    protected open fun getPicSnapDistance(
        targetView: View?,
        layoutManager: RecyclerView.LayoutManager?,
        isTop: Boolean
    ): IntArray {
        return intArrayOf(0, 0)
    }

    protected open fun createPicSnapScroller(
        layoutManager: RecyclerView.LayoutManager?,
        isTop: Boolean
    ): LinearSmoothScroller? {
        return if (layoutManager !is ScrollVectorProvider) {
            null
        } else object : LinearSmoothScroller(mRecyclerView!!.context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action
            ) {
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }
        }
    }

    /**
     * Override this method to snap to a particular point within the target view or the container
     * view on any axis.
     *
     *
     * This method is called when the [SnapHelper] has intercepted a fling and it needs
     * to know the exact distance required to scroll by in order to snap to the target view.
     *
     * @param layoutManager the [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView]
     * @param targetView    the target view that is chosen as the view to snap
     * @return the output coordinates the put the result into. out[0] is the distance
     * on horizontal axis and out[1] is the distance on vertical axis.
     */
    abstract fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray?

    /**
     * Override this method to provide a particular target view for snapping.
     *
     *
     * This method is called when the [SnapHelper] is ready to start snapping and requires
     * a target view to snap to. It will be explicitly called when the scroll state becomes idle
     * after a scroll. It will also be called when the [SnapHelper] is preparing to snap
     * after a fling and requires a reference view from the current set of child views.
     *
     *
     * If this method returns `null`, SnapHelper will not snap to any view.
     *
     * @param layoutManager the [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView]
     * @return the target view to which to snap on fling or end of scroll
     */
    abstract fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View?

    /**
     * Override to provide a particular adapter target position for snapping.
     *
     * @param layoutManager the [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView]
     * @param velocityX     fling velocity on the horizontal axis
     * @param velocityY     fling velocity on the vertical axis
     * @return the target adapter position to you want to snap or [RecyclerView.NO_POSITION]
     * if no snapping should happen
     */
    abstract fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager?, velocityX: Int,
        velocityY: Int
    ): Int

    private fun callback(beforeScroll: Int, currentPosition: Int, bottom: Boolean, view: View?) {
        if (beforeScroll == currentPosition && currentPosition != 1) {
            return
        }
        mOnViewPagerListener!!.onPageSelected(currentPosition, bottom, view)
    }

    companion object {
        const val MILLISECONDS_PER_INCH = 100f
    }
}