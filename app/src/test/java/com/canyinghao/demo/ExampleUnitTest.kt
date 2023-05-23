package com.canyinghao.demo

import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    //该方法指定了timeout时间为1秒，实际运行时会超过1秒，该方法测试无法通过
    @Test(timeout = 1000)
    fun testTimeout2() {
        try {
            Thread.sleep(1200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}