package io.github.sof3.andtransfer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import net.gotev.uploadservice.BuildConfig
import net.gotev.uploadservice.UploadServiceConfig

const val UPLOAD_NOTIF_CHANNEL = "io.github.sof3.andtransfer.UPLOAD_NOTIF_CHANNEL"

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(UPLOAD_NOTIF_CHANNEL, getString(R.string.upload_channel_name), NotificationManager.IMPORTANCE_HIGH))
        }

        UploadServiceConfig.initialize(this, UPLOAD_NOTIF_CHANNEL, BuildConfig.DEBUG)
    }
}