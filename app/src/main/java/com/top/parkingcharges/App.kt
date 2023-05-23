package com.top.parkingcharges

import android.app.Application
import android.support.multidex.MultiDex
import com.kongqw.serialportlibrary.SerialUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        SerialUtils.getInstance().init(this, true, "TAG");
//        Toasty.Config.getInstance()
//            .setGravity(Gravity.CENTER) // optional (set toast gravity, offsets are optional)
//            .apply(); // required
    }
}