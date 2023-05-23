package com.canyinghao.canscrollpgaer

import android.view.ScaleGestureDetector
import android.view.MotionEvent

interface OnGestureListener {
    fun onScale(detector: ScaleGestureDetector?): Boolean
    fun onSingleTapConfirmed(e: MotionEvent?): Boolean
    fun onDoubleTap(e: MotionEvent?): Boolean
    fun onLongClick(e: MotionEvent?): Boolean
}