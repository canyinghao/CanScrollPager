package com.canyinghao.canscrollpgaer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Created by canyinghao on 23/05/23..
 * Copyright 2023 canyinghao
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class CanScrollPager @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(
    context!!, attrs, defStyle
) {
    private val FACTOR = 1
    private var mCurrentScaleFactor: Float
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mOffsetX = 0f
    private var mOffsetY = 0f
    private var centerX = 0f
    private var centerY = 0f
    var minScaleFactor = 0.8f
    private var mMidScaleFactor = 2f
    var maxScaleFactor = 3f
    private var isScale = false
    var isTwoStage = false

    //    是否回调单击事件
    var isCanSingleTapListener = true

    //    是否回调双击事件
    var isCanDoubleTapListener = true

    //    是否回调缩放事件
    var isCanScaleListener = true

    //    是否回调缩放事件
    var isCanLongListener = true

    //    是否可双击缩放
    var isCanDoubleScale = true

    //    是否可缩放
    var isCanScale = true
    private var mGestureDetector: GestureDetector? = null
    private var mScaleGestureDetector: ScaleGestureDetector? = null
    var onGestureListener: OnGestureListener? = null
    private var mAutoScaleRunnable: Runnable? = null
    private var mZoomDuration: Long = 200
    private val mZoomInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private var snapHelper: ScrollPagerSnapHelper? = null
    private var isCurrentPage = false

    /**
     * 设置缩放时间
     *
     * @param duration long
     */
    fun setZoomTransitionDuration(duration: Long) {

        mZoomDuration = if (duration < 0) 200 else duration

    }

    /**
     * 双击缩放时平滑缩放
     */
    private inner class AnimatedZoomRunnable(targetZoom: Float) : Runnable {
        private val mStartTime: Long = System.currentTimeMillis()
        private val mZoomStart: Float = mCurrentScaleFactor
        private val mZoomEnd: Float

        init {
            mZoomEnd = targetZoom
        }

        override fun run() {
            val t = interpolate()
            mCurrentScaleFactor = mZoomStart + t * (mZoomEnd - mZoomStart)
            if (t < 1f) {
                postOnAnimation(this@CanScrollPager, this)
            }
            checkOffsetBorder()
            invalidate()
        }

        private fun interpolate(): Float {
            var t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration
            t = min(1f, t)
            t = mZoomInterpolator.getInterpolation(t)
            return t
        }
    }

    private fun postOnAnimation(view: View, runnable: Runnable) {
        view.postOnAnimation(runnable)
    }

    private inner class ScrollLinearLayoutManager(
        context: Context?,
        orientation: Int,
        reverseLayout: Boolean
    ) : LinearLayoutManager(context, orientation, reverseLayout) {
        override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
            return super.scrollVerticallyBy(
                ceil((dy / mCurrentScaleFactor).toDouble()).toInt(), recycler, state
            )
        }
    }

    init {
        layoutManager = ScrollLinearLayoutManager(context, VERTICAL, false)
        mMidScaleFactor = (FACTOR + maxScaleFactor) / 2
        mCurrentScaleFactor = FACTOR.toFloat()
        initDetector()



    }

    fun setSnapHelper(helper: ScrollPagerSnapHelper) {
        snapHelper = helper
        snapHelper?.attachToRecyclerView(this)
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                try {
                    if (newState == SCROLL_STATE_IDLE) {
                        val layoutManager = layoutManager ?: return
                        if (snapHelper != null) {
                            val view = snapHelper!!.findSnapView(layoutManager)
                            if (view != null && view.tag is Boolean) {
                                isCurrentPage = view.tag as Boolean
                                if (isCurrentPage) {
                                    resetSize()
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun setOnScrollPageChangedListener(listener: OnScrollPagerListener?) {
        if (snapHelper != null) {
            snapHelper!!.setOnPageChangedListener(listener)
        }
    }

    /**
     * 设置手势监听
     */
    private fun initDetector() {
        mScaleGestureDetector =
            ScaleGestureDetector(context, object : SimpleOnScaleGestureListener() {
                var fromBig = false
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {


                    // 获取缩放的中心点
                    centerX = detector.focusX
                    centerY = detector.focusY
                    isScale = true
                    fromBig = mCurrentScaleFactor > FACTOR
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    return if (isCanScale) {
                        if (!isCurrentPage) {
                            val tempFactor = mCurrentScaleFactor
                            mCurrentScaleFactor *= detector.scaleFactor
                            mCurrentScaleFactor = max(
                                minScaleFactor,
                                min(mCurrentScaleFactor, maxScaleFactor)
                            )
                            if (fromBig) {
                                if (tempFactor >= mCurrentScaleFactor) {
                                    if (mCurrentScaleFactor <= FACTOR) {
                                        mCurrentScaleFactor = FACTOR.toFloat()
                                    }
                                }
                            }
                            invalidate()
                        }
                        if (isCanScaleListener && onGestureListener != null) {
                            onGestureListener!!.onScale(detector)
                        }
                        true
                    } else {
                        super.onScale(detector)
                    }
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isScale = false
                    if (!isCurrentPage) {
                        if (mCurrentScaleFactor < FACTOR) {
                            postOnAnimation(
                                this@CanScrollPager,
                                AnimatedZoomRunnable(FACTOR.toFloat())
                            )
                        } else {
                            if (!isTwoStage && mCurrentScaleFactor > mMidScaleFactor) {
                                postOnAnimation(
                                    this@CanScrollPager,
                                    AnimatedZoomRunnable(mMidScaleFactor)
                                )
                            }
                        }
                        checkOffsetBorder()
                    }
                    super.onScaleEnd(detector)
                }
            }
            )
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return if (isCanSingleTapListener) {
                    //点击
                    onGestureListener != null && onGestureListener!!.onSingleTapConfirmed(e)
                } else {
                    super.onSingleTapConfirmed(e)
                }
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                if (isCanLongListener) {
                    if (onGestureListener != null) {
                        onGestureListener!!.onLongClick(e)
                    }
                }
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isCurrentPage) {
                    //双击缩放
                    centerX = e.rawX
                    centerY = e.rawY
                    if (mAutoScaleRunnable != null) {
                        removeCallbacks(mAutoScaleRunnable)
                    }
                    if (isCanDoubleScale) {
                        mAutoScaleRunnable = if (isTwoStage) {
                            if (mCurrentScaleFactor < mMidScaleFactor) {
                                AnimatedZoomRunnable(mMidScaleFactor)
                            } else if (mCurrentScaleFactor < maxScaleFactor) {
                                AnimatedZoomRunnable(maxScaleFactor)
                            } else {
                                AnimatedZoomRunnable(FACTOR.toFloat())
                            }
                        } else {
                            if (mCurrentScaleFactor < mMidScaleFactor) {
                                AnimatedZoomRunnable(mMidScaleFactor)
                            } else {
                                AnimatedZoomRunnable(FACTOR.toFloat())
                            }
                        }
                        postOnAnimation(this@CanScrollPager, mAutoScaleRunnable!!)
                    }
                }
                return if (isCanDoubleTapListener) {
                    if (onGestureListener != null) {
                        onGestureListener!!.onDoubleTap(e)
                    }
                    true
                } else {
                    super.onDoubleTap(e)
                }
            }
        }
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        try {
            if (isCanScale) {
                canvas.save()
                if (mCurrentScaleFactor <= 1.0f) {
                    mOffsetX = 0.0f
                    mOffsetY = 0.0f
                }
                canvas.translate(mOffsetX, mOffsetY) //偏移
                canvas.scale(mCurrentScaleFactor, mCurrentScaleFactor, centerX, centerY) //缩放
                super.dispatchDraw(canvas)
                canvas.restore()
            } else {
                super.dispatchDraw(canvas)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            super.dispatchDraw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isCanScale) {
            super.onTouchEvent(event)
            try {
                if (mGestureDetector!!.onTouchEvent(event)) {
                    return true
                }
                mScaleGestureDetector!!.onTouchEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (isScale) {
                return true
            }
            if (mCurrentScaleFactor == 1f) {
                return true
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mLastTouchX = event.x
                    mLastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val mainPointX = event.x
                    val mainPointY = event.y


                    //滑动时偏移
                    mOffsetX += mainPointX - mLastTouchX
                    mOffsetY += mainPointY - mLastTouchY
                    mLastTouchX = mainPointX
                    mLastTouchY = mainPointY
                    checkOffsetBorder()
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mLastTouchX = event.x
                    mLastTouchY = event.y
                }
            }
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    /**
     * 检测偏移边界,通过缩放中心距左右、上下边界的比例确定
     */
    private fun checkOffsetBorder() {
        if (mCurrentScaleFactor < FACTOR) {
            return
        }
        val sumOffsetX = width * (mCurrentScaleFactor - FACTOR)
        val sumOffsetY = height * (mCurrentScaleFactor - FACTOR)
        val numX = (width - centerX) / centerX + 1
        val offsetLeftX = sumOffsetX / numX
        val offsetRightX = (width - centerX) / centerX * offsetLeftX
        val numY = (height - centerY) / centerY + 1
        val offsetTopY = sumOffsetY / numY
        val offsetBottomY = (height - centerY) / centerY * offsetTopY
        if (mOffsetX > offsetLeftX) {
            mOffsetX = offsetLeftX
        }
        if (mOffsetX < -offsetRightX) {
            mOffsetX = -offsetRightX
        }
        if (mOffsetY > offsetTopY) {
            mOffsetY = offsetTopY
        }
        if (mOffsetY < -offsetBottomY) {
            mOffsetY = -offsetBottomY
        }
    }

    fun resetSize() {
        if (mCurrentScaleFactor != FACTOR.toFloat()) {
            postOnAnimation(this@CanScrollPager, AnimatedZoomRunnable(FACTOR.toFloat()))
        }
    }


}