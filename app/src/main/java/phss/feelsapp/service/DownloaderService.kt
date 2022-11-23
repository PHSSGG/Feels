package phss.feelsapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import phss.feelsapp.constants.YOUTUBE_VIDEO_URL
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.utils.getSongArtists
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloaderService(
    private val songsRepository: SongsRepository,
    private val applicationContext: Context
) {

    val downloading = HashMap<String, Job>()

    fun downloadSong(song: RemoteSong, downloadProgressCallback: DownloadProgressCallback) {
        val directory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "songs")

        val request = YoutubeDLRequest(YOUTUBE_VIDEO_URL.replace("{id}", song.item.key))
        request.addOption("-f", "m4a")
        request.addOption("-o", "${directory.absolutePath}/${song.item.key}.%(ext)s")

        val job = GlobalScope.launch (Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().execute(request, song.item.key, downloadProgressCallback)

                var thumbnailPath = ""
                if (song.item.thumbnail != null) {
                    val (target, path) = getThumbnailTarget(song.item.key ?: "Null")

                    saveThumbnail(song, target)
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
                        filePath = "${directory.absolutePath}/${song.item.key ?: "Null"}.m4a"
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            this.cancel()
            downloading.remove(song.item.key)
        }
        downloading[song.item.key] = job

        job.start()
    }

    fun cancelSongDownload(song: RemoteSong) {
        if (downloading.containsKey(song.item.key)) {
            YoutubeDL.getInstance().destroyProcessById(song.item.key)
            downloading[song.item.key]!!.cancel("download cancelled")

            downloading.remove(song.item.key)
        }
    }

    private fun saveThumbnail(song: RemoteSong, target: Target) {
        val thumbnailHandler = Handler(Looper.getMainLooper())
        thumbnailHandler.post {
            Picasso.get().load(song.item.thumbnail!!.setSize(300)).into(target)
        }
    }

    private fun getThumbnailTarget(name: String): Pair<Target, String> {
        val directory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
        val file = File("${directory.absolutePath}/$name.jpg")
        val target = object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                Thread {
                    try {
                        file.createNewFile()

                        val outputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.flush()
                        outputStream.close()
                    } catch (exception: IOException) {
                        exception.localizedMessage?.let { Log.e("IOException", it) }
                    }
                }.start()
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            }
        }

        return target to file.absolutePath
    }

}