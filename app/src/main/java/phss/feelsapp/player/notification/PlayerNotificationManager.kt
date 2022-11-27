package phss.feelsapp.player.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import phss.feelsapp.R
import phss.feelsapp.service.PlayerService
import java.io.File

class PlayerNotificationManager(
    private val playerService: PlayerService
) : BroadcastReceiver() {

    private val CHANNEL_ID = "phss.feelsapp.FEELS_PLAYER_CHANNEL"
    private val REQUEST_CODE = 100
    val NOTIFICATION_ID = 101

    val PLAY_PAUSE_ACTION = "phss.feelsapp.PLAYPAUSE"
    val NEXT_ACTION = "phss.feelsapp.NEXT"
    val PREVIOUS_ACTION = "phss.feelsapp.PREVIOUS"
    val BACKWARD_ACTION = "phss.feelsapp.BACKWARD"
    val FORWARD_ACTION = "phss.feelsapp.FORWARD"
    val STOP_ACTION = "phss.feelsapp.STOP"

    lateinit var notificationManager: NotificationManager
    lateinit var mediaSession: MediaSessionCompat

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            if (playerService.playerManager.getCurrentPlaying() != null && !playerService.playerManager.isPlaying())
                playerService.playerManager.resumePlayer()
        }

        override fun onPause() {
            if (playerService.playerManager.isPlaying())
                playerService.playerManager.pausePlayer()
        }

        override fun onStop() {
            playerService.playerManager.stopPlayer()
        }

        override fun onSkipToNext() {
            playerService.playerManager.playNext(true)
        }

        override fun onSkipToPrevious() {
            playerService.playerManager.playPrevious()
        }
    }

    fun getReceiverFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(PLAY_PAUSE_ACTION)
        filter.addAction(NEXT_ACTION)
        filter.addAction(PREVIOUS_ACTION)
        filter.addAction(BACKWARD_ACTION)
        filter.addAction(FORWARD_ACTION)
        filter.addAction(STOP_ACTION)

        return filter
    }

    fun buildNotification(): Notification {
        if (!::notificationManager.isInitialized) notificationManager = playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val deletePendingIntent = getDeleteIntent()

        val style = androidx.media.app.NotificationCompat.MediaStyle()
        style.setMediaSession(createMediaSession().sessionToken)
        style.setShowActionsInCompactView(1, 2, 3)
        style.setShowCancelButton(true)
        style.setCancelButtonIntent(deletePendingIntent)

        val builder = NotificationCompat.Builder(playerService.baseContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_song_icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(playerService.playerManager.isPlaying())
            .addAction(getNotificationAction(BACKWARD_ACTION))
            .addAction(getNotificationAction(PREVIOUS_ACTION))
            .addAction(getNotificationAction(PLAY_PAUSE_ACTION))
            .addAction(getNotificationAction(NEXT_ACTION))
            .addAction(getNotificationAction(STOP_ACTION))
            .setDeleteIntent(deletePendingIntent)
            .setStyle(style)

        return builder.build()
    }

    private fun getNotificationAction(action: String): NotificationCompat.Action {
        var icon: Int = if (playerService.playerManager.isPlaying()) R.drawable.ic_notification_pause else R.drawable.ic_notification_resume

        when (action) {
            PREVIOUS_ACTION -> icon = R.drawable.ic_notification_previous
            NEXT_ACTION -> icon = R.drawable.ic_notification_next
            BACKWARD_ACTION -> icon = R.drawable.ic_notification_backward
            FORWARD_ACTION -> icon = R.drawable.ic_notification_forward
            STOP_ACTION -> icon = R.drawable.ic_notification_stop
        }

        val actionIntent = Intent()
        actionIntent.action = action

        val actionPendingIntent = PendingIntent.getBroadcast(playerService, REQUEST_CODE, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Action.Builder(icon, action, actionPendingIntent).build()
    }

    private fun getDeleteIntent(): PendingIntent {
        val deleteIntent = Intent(playerService.baseContext, PlayerService::class.java)
        deleteIntent.action = STOP_ACTION

        return PendingIntent.getService(
            playerService.baseContext,
            1,
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createMediaSession(): MediaSessionCompat {
        if (!::mediaSession.isInitialized) {
            mediaSession = MediaSessionCompat(playerService.baseContext, "feels_player")
            mediaSession.setCallback(mediaSessionCallback)
        }

        setupMediaSession(mediaSession)
        return mediaSession
    }

    private fun setupMediaSession(mediaSession: MediaSessionCompat): MediaSessionCompat {
        val currentPlaying = playerService.playerManager.getCurrentPlaying()!!
        val songThumbnail = runBlocking {
            val result = GlobalScope.async(Dispatchers.IO) {
                Picasso.get().load(File(currentPlaying.thumbnailPath)).get()
            }

            result.await()
        }

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, songThumbnail)
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentPlaying.name)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentPlaying.artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, playerService.playerManager.getSongDuration().toLong())
                .build()
        )
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(if (playerService.playerManager.isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, playerService.playerManager.getProgress().toLong(), 1f)
                .build()
        )

        return mediaSession
    }

    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Feels Player", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PLAY_PAUSE_ACTION -> if (playerService.playerManager.isPlaying()) mediaSessionCallback.onPause() else mediaSessionCallback.onPlay()
            NEXT_ACTION -> mediaSessionCallback.onSkipToNext()
            PREVIOUS_ACTION -> mediaSessionCallback.onSkipToPrevious()
            BACKWARD_ACTION -> playerService.playerManager.backward5()
            FORWARD_ACTION -> playerService.playerManager.forward5()
            STOP_ACTION -> mediaSessionCallback.onStop()
        }
    }

}