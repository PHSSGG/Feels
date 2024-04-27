package phss.feelsapp.download.worker

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import phss.feelsapp.constants.YOUTUBE_VIDEO_URL
import phss.feelsapp.data.models.RemoteSong
import java.io.File

class DownloadWorker(
    private val song: RemoteSong,
    private val directory: File
) {

    fun beginDownload(
        onDownloadProgressUpdate: (Float) -> Unit,
        onDownloadFinish: (Boolean, Boolean) -> Unit
    ) {
        val request = YoutubeDLRequest(YOUTUBE_VIDEO_URL.replace("{id}", song.item.key))
        request.addOption("--no-check-certificate")
        request.addOption("--force-overwrites")
        request.addOption("--no-continue")
        request.addOption("-f", "bestaudio/m4a/bestaudio[ext=mp3]/best")
        request.addOption("-o", "${directory.absolutePath}/${song.item.key}.m4a")

        try {
            YoutubeDL.getInstance().execute(request, song.item.key) { progress, _, _ ->
                onDownloadProgressUpdate(progress)
                if (progress == 100f) onDownloadFinish(true, false)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            onDownloadFinish(false, exception::class.simpleName == YoutubeDLException::class.simpleName)
        }
    }

}