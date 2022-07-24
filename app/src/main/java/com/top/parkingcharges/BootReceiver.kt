package com.top.parkingcharges

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blankj.utilcode.util.ActivityUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                ActivityUtils.startActivity(MainActivity::class.java)
            } else {
                val notificationUtils = NotificationUtils(context)
                notificationUtils.clearAllNotification()
                notificationUtils.sendNotificationFullScreen("后台启动", "启动")
            }
        }
    }
}