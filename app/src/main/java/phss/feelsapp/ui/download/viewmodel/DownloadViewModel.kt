package phss.feelsapp.ui.download.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.models.Song
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.service.DownloaderService

class DownloadViewModel(
    private val downloaderService: DownloaderService,
    private val songsRepository: SongsRepository
) : ViewModel() {

    fun downloadSong(
        song: RemoteSong,
        onDownloadProgressUpdate: (Float) -> Unit,
        onDownloadFinish: (Boolean) -> Unit
    ) {
        if (song.downloading) {
            downloaderService.downloadSong(song) { progress, _, _ ->
                onDownloadProgressUpdate(progress)

                if (progress == 100f) onDownloadFinish(true)
            }
        } else {
            downloaderService.cancelSongDownload(song)
            onDownloadFinish(false)
        }
    }

    fun deleteSong(remoteSong: RemoteSong) {
        viewModelScope.launch(Dispatchers.IO) {
            val localSong = songsRepository.getLocalSongByKey(remoteSong.item.key)

            remoteSong.alreadyDownloaded = false
            songsRepository.deleteSong(localSong)
        }
    }

}