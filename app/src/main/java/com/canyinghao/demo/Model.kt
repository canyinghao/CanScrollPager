package com.canyinghao.demo

import androidx.databinding.BaseObservable
import androidx.databinding.ObservableField

/**
 * desc:
 * author: canyinghao
 * date: 2023/5/22
 */
class Model : BaseObservable(){
    val btnName = ObservableField<String>()
    val title = ObservableField<String>()
}



