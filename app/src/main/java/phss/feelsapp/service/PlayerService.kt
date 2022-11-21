package phss.feelsapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.koin.android.ext.android.inject
import phss.feelsapp.player.PlayerManager

class PlayerService : Service() {

    var isRunning = false

    private val binder = LocalBinder()

    val playerManager: PlayerManager by inject()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_NOT_STICKY
    }

    inner class LocalBinder : Binder() {
        fun getService() = this@PlayerService
    }

}