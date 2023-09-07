package tokyo.leadershouse.rarextractor

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

class RarExtractorActivity : Activity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, RarExtractorService::class.java)
        serviceIntent.action = intent.action
        serviceIntent.clipData = intent.clipData
        startService(serviceIntent) // RarExtractorServiceを起動し、共有インテントを渡す
        finish() // Activityを終了
    }
}
