package phss.feelsapp.ui.download.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.data.repository.SongsRepository
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.utils.getOnlySongName
import phss.feelsapp.utils.getSongArtists

class DownloadViewModel(
    private val downloaderService: DownloaderService,
    private val songsRepository: SongsRepository
) : ViewModel() {

    fun downloadSong(remoteSong: RemoteSong?, onSongUpdate: RemoteSong.() -> Unit) {
        if (remoteSong == null) return

        var downloadInfo = remoteSong to false
        if (remoteSong.isFromPlaylist) {
            downloadInfo = runBlocking(Dispatchers.IO) {
                val newSong = songsRepository.searchForSongsRemote("${remoteSong.item.getSongArtists()} ${remoteSong.item.getOnlySongName()}").first()
                onSongUpdate(newSong)

                newSong to (songsRepository.getLocalSongByKey(newSong.item.key) != null)
            }
        }

        val (songToDownload, alreadyExists) = downloadInfo
        if (alreadyExists) return

        downloaderService.downloadSong(songToDownload)
    }

    fun cancelSongDownload(song: RemoteSong?) {
        if (song == null) return
        downloaderService.cancelSongDownload(song)
    }

    fun deleteSong(remoteSong: RemoteSong?) {
        if (remoteSong == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val localSong = songsRepository.getLocalSongByKey(remoteSong.item.key)

            remoteSong.alreadyDownloaded = false
            songsRepository.deleteSong(localSong)
        }
    }

}