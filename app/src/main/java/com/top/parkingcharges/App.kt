package com.top.parkingcharges

import android.app.Application
import android.view.Gravity
import es.dmoral.toasty.Toasty

class App : Application() {
    override fun onCreate() {
        super.onCreate()

//        Toasty.Config.getInstance()
//            .setGravity(Gravity.CENTER) // optional (set toast gravity, offsets are optional)
//            .apply(); // required
    }
}