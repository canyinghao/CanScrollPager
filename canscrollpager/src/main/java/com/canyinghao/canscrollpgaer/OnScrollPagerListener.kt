package com.canyinghao.canscrollpgaer

import android.view.View

/**
 * View滑动监听
 */
interface OnScrollPagerListener {

    /**
     * 页面不可见, 释放
     * @param isNext 是否有下一个
     * @param position 下标
     */
    fun onPageRelease(isNext: Boolean, position: Int, view: View?)

    /**
     * 选中的index
     * @param position 下标
     * @param bottom 是否到底部
     */
    fun onPageSelected(position: Int, bottom: Boolean, view: View?)
}