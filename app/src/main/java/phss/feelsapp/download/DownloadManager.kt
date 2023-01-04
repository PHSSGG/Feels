package phss.feelsapp.download

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.download.observers.DownloadWorkerObserver
import phss.feelsapp.download.worker.DownloadWorker
import java.io.File

class DownloadManager(
    private val applicationContext: Context
) {

    var running = false

    private val queue = mutableListOf<Pair<RemoteSong, DownloadWorkerObserver>>()
    private val flow = flow {
        while (queue.isNotEmpty()) {
            emit(queue.removeAt(0))
        }

        running = false
    }

    suspend fun start() {
        if (running) return

        running = true

        flow.flowOn(Dispatchers.IO).collect { (song, callback) ->
            val directory = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "songs")

            DownloadWorker(song, directory).beginDownload(
                onDownloadProgressUpdate = { progress -> callback.onProgressUpdate(song, progress) },
                onDownloadFinish = { success, error -> callback.onFinish(song, success, error) }
            )
        }
    }

    fun addToQueue(song: RemoteSong, callback: DownloadWorkerObserver) {
        queue.add(song to callback)
    }

    fun removeFromQueue(song: RemoteSong) {
        val item = queue.find { it.first.item.key == song.item.key } ?: return
        queue.remove(item)
    }

}