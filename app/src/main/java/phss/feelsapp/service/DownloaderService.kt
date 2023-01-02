package phss.feelsapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.download.listeners.DownloadUpdateListener
import phss.feelsapp.download.listeners.DownloadWorkerListener
import phss.feelsapp.download.DownloadManager
import phss.feelsapp.utils.getSongArtists
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.KClass

class DownloaderService(
    private val songsRepository: SongsRepository,
    private val applicationContext: Context
) {

    private val downloadManager = DownloadManager(applicationContext)

    val downloading = ArrayList<String>()
    private val observables = HashMap<KClass<*>, DownloadUpdateListener>()

    fun downloadSong(song: RemoteSong) {
        val directory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "songs")

        CoroutineScope(Dispatchers.IO).launch {
            downloadManager.addToQueue(song, object : DownloadWorkerListener {
                override fun onProgressUpdate(song: RemoteSong, progress: Float) {
                    if (downloading.contains(song.item.key))
                        observables.values.forEach { it.onDownloadProgressUpdate(song, progress) }
                }

                override fun onFinish(song: RemoteSong, success: Boolean, error: Boolean) {
                    // prevent duplicates
                    if (!downloading.contains(song.item.key)) return
                    downloading.remove(song.item.key)

                    if (success) {
                        var thumbnailPath = ""
                        if (song.item.thumbnail != null) {
                            val path = saveThumbnailImage(song)
                            thumbnailPath = path
                        }

                        downloading.remove(song.item.key)

                        songsRepository.addSong(
                            Song(
                                songId = 0,
                                name = song.item.info?.name ?: "Null",
                                artist = song.item.getSongArtists(),
                                album = song.item.album?.name ?: "Null",
                                duration = song.item.durationText ?: "0:00",
                                key = song.item.key,
                                thumbnailPath = thumbnailPath,
                                filePath = "${directory.absolutePath}/${song.item.key}.m4a"
                            )
                        )
                    } else {
                        // delete temp files
                        File("${directory.absolutePath}/${song.item.key}.m4a.part").run {
                            if (exists()) delete()
                        }
                        File("${directory.absolutePath}/${song.item.key}.m4a").run {
                            if (exists()) delete()
                        }

                        if (error) Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext, "Couldn't download song ${song.item.info?.name ?: song.item.key}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    song.downloading = false
                    song.alreadyDownloaded = success

                    observables.values.forEach { it.onDownloadFinish(song, success) }
                }
            })

            downloadManager.start()
        }
        song.downloading = true
        song.downloadProgress = 0f

        downloading.add(song.item.key)
        observables.values.forEach { it.onDownloadStart(song) }
    }

    fun cancelSongDownload(song: RemoteSong) {
        if (downloading.contains(song.item.key)) {
            song.downloading = false
            song.alreadyDownloaded = false

            downloadManager.removeFromQueue(song)
            YoutubeDL.getInstance().destroyProcessById(song.item.key)

            downloading.remove(song.item.key)
            observables.values.forEach { it.onDownloadFinish(song, false) }
        }
    }

    private fun saveThumbnailImage(song: RemoteSong): String {
        val directory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
        val thumbnailFile = File("${directory.absolutePath}/${song.item.key}.jpg")
        thumbnailFile.createNewFile()

        try {
            val url = URL(song.item.thumbnail?.setSize(300))
            val connection = (url.openConnection() as HttpURLConnection).also {
                it.doInput = true
                it.connect()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream).also { inputStream.close() }

                val outputStream = FileOutputStream(thumbnailFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return thumbnailFile.absolutePath
    }

    fun registerDownloadUpdateListener(clazz: KClass<*>, downloadUpdateListener: DownloadUpdateListener) {
        observables[clazz] = downloadUpdateListener
    }

    fun unregisterDownloadUpdateListener(clazz: KClass<*>) {
        observables.remove(clazz)
    }

}