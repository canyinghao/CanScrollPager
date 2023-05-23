
package com.canyinghao.canscrollpgaer

import android.content.Context

import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.Throws
import androidx.recyclerview.widget.LinearSmoothScroller
import android.util.DisplayMetrics
import android.view.View
import java.lang.IllegalStateException

class ScrollPagerSnapHelper : SnapHelper() {

    private var mVerticalHelper: OrientationHelper? = null
    private var mHorizontalHelper: OrientationHelper? = null
    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        val out = IntArray(2)
        if (layoutManager.canScrollHorizontally()) {
            out[0] = distanceToCenter(
                layoutManager, targetView,
                getHorizontalHelper(layoutManager)
            )
        } else {
            out[0] = 0
        }
        if (layoutManager.canScrollVertically()) {
            out[1] = distanceToCenter(
                layoutManager, targetView,
                getVerticalHelper(layoutManager)
            )
        } else {
            out[1] = 0
        }
        return out
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
        if (layoutManager!!.canScrollVertically()) {
            return findCenterView(layoutManager, getVerticalHelper(layoutManager))
        } else if (layoutManager.canScrollHorizontally()) {
            return findCenterView(layoutManager, getHorizontalHelper(layoutManager))
        }
        return null
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager?, velocityX: Int,
        velocityY: Int
    ): Int {
        val itemCount = layoutManager!!.itemCount
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val orientationHelper =
            getOrientationHelper(layoutManager) ?: return RecyclerView.NO_POSITION

        // A child that is exactly in the center is eligible for both before and after
        var closestChildBeforeCenter: View? = null
        var distanceBefore = Int.MIN_VALUE
        var closestChildAfterCenter: View? = null
        var distanceAfter = Int.MAX_VALUE

        // Find the first view before the center, and the first view after the center
        val childCount = layoutManager.childCount
        //        View snapView =findSnapView(layoutManager);
        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            if (child.height < layoutManager.height / 2f) {
                continue
            }
            val distance = distanceToCenter(layoutManager, child, orientationHelper)
            if (distance in (distanceBefore + 1)..0) {
                // Child is before the center and closer then the previous best
                distanceBefore = distance
                closestChildBeforeCenter = child
            }
            if (distance in 0 until distanceAfter) {
                // Child is after the center and closer then the previous best
                distanceAfter = distance
                closestChildAfterCenter = child
            }
        }

        val forwardDirection = isForwardFling(layoutManager, velocityX, velocityY)
        if (forwardDirection && closestChildAfterCenter != null) {

            return layoutManager.getPosition(closestChildAfterCenter)
        } else if (!forwardDirection && closestChildBeforeCenter != null) {

            return layoutManager.getPosition(closestChildBeforeCenter)
        }


        val visibleView =
            (if (forwardDirection) closestChildBeforeCenter else closestChildAfterCenter)
                ?: return RecyclerView.NO_POSITION
        val visiblePosition = layoutManager.getPosition(visibleView)
        val snapToPosition = (visiblePosition
                + if (isReverseLayout(layoutManager) == forwardDirection) -1 else +1)
        return if (snapToPosition < 0 || snapToPosition >= itemCount) {
            RecyclerView.NO_POSITION
        } else snapToPosition
    }

    override fun isForwardFling(
        layoutManager: RecyclerView.LayoutManager?, velocityX: Int,
        velocityY: Int
    ): Boolean {
        return if (layoutManager!!.canScrollHorizontally()) {
            velocityX > 0
        } else {
            velocityY > 0
        }
    }

    private fun isReverseLayout(layoutManager: RecyclerView.LayoutManager?): Boolean {
        val itemCount = layoutManager!!.itemCount
        if (layoutManager is ScrollVectorProvider) {
            val vectorProvider = layoutManager as ScrollVectorProvider
            val vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1)
            if (vectorForEnd != null) {
                return vectorForEnd.x < 0 || vectorForEnd.y < 0
            }
        }
        return false
    }

    private var context: Context? = null
    @Throws(IllegalStateException::class)
    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        context = recyclerView!!.context
        super.attachToRecyclerView(recyclerView)
    }

    override fun createSnapScroller(layoutManager: RecyclerView.LayoutManager?): LinearSmoothScroller? {
        return if (layoutManager !is ScrollVectorProvider) {
            null
        } else object : LinearSmoothScroller(context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action
            ) {
                val snapDistances = calculateDistanceToFinalSnap(
                    layoutManager,
                    targetView
                )
                val dx = snapDistances[0]
                val dy = snapDistances[1]
                val time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)))
                if (time > 0) {
                    action.update(dx, dy, time, mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 25f / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return Math.min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx))
            }
        }
    }

    override fun getPicSnapDistance(
        targetView: View?,
        layoutManager: RecyclerView.LayoutManager?,
        isTop: Boolean
    ): IntArray {

        val dx = 0

        val dy = if (isTop) {
            targetView!!.top

        } else {

            targetView!!.bottom - layoutManager!!.height
        }
        return intArrayOf(dx, dy)
    }

    override fun createPicSnapScroller(
        layoutManager: RecyclerView.LayoutManager?,
        isTop: Boolean
    ): LinearSmoothScroller? {
        return if (layoutManager !is ScrollVectorProvider) {
            null
        } else object : LinearSmoothScroller(context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action
            ) {
                val snapDistances = getPicSnapDistance(targetView, layoutManager, isTop)
                val dx = snapDistances[0]
                val dy = snapDistances[1]
                val time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)))
                if (time > 0) {
                    action.update(dx, dy, time, mDecelerateInterpolator)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 25f / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return Math.min(MAX_SCROLL_ON_FLING_DURATION, super.calculateTimeForScrolling(dx))
            }
        }
    }

    private fun distanceToCenter(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View, helper: OrientationHelper
    ): Int {
        val childCenter = (helper.getDecoratedStart(targetView)
                + helper.getDecoratedMeasurement(targetView) / 2)
        val containerCenter = helper.startAfterPadding + helper.totalSpace / 2
        return childCenter - containerCenter
    }

    /**
     * Return the child view that is currently closest to the center of this parent.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @param helper The relevant [OrientationHelper] for the attached [RecyclerView].
     *
     * @return the child view that is currently closest to the center of this parent.
     */
    private fun findCenterView(
        layoutManager: RecyclerView.LayoutManager?,
        helper: OrientationHelper
    ): View? {
        val childCount = layoutManager!!.childCount
        if (childCount == 0) {
            return null
        }
        var closestChild: View? = null
        val center = helper.startAfterPadding + helper.totalSpace / 2
        var absClosest = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)
            val childCenter = (helper.getDecoratedStart(child)
                    + helper.getDecoratedMeasurement(child) / 2)
            val absDistance = Math.abs(childCenter - center)

            /* if child center is closer than previous closest, set it as closest  */if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = child
            }
        }
        return closestChild
    }

    private fun getOrientationHelper(layoutManager: RecyclerView.LayoutManager?): OrientationHelper? {
        return if (layoutManager!!.canScrollVertically()) {
            getVerticalHelper(layoutManager)
        } else if (layoutManager.canScrollHorizontally()) {
            getHorizontalHelper(layoutManager)
        } else {
            null
        }
    }

    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (mVerticalHelper == null) {
            mVerticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return mVerticalHelper!!
    }

    private fun getHorizontalHelper(
        layoutManager: RecyclerView.LayoutManager
    ): OrientationHelper {
        if (mHorizontalHelper == null) {
            mHorizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return mHorizontalHelper!!
    }

    companion object {
        private const val MAX_SCROLL_ON_FLING_DURATION = 100 // ms
    }
}