package phss.feelsapp.ui.songs.dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.ui.songs.SongsViewModel
import phss.feelsapp.ui.songs.adapters.playlists.songs.AddToPlaylistSongsAdapter

class PlaylistAddSongsDialog(
    context: Context,
    private val scope: CoroutineScope,
    private val songsViewModel: SongsViewModel,
    private val playlist: Playlist?,
    private val currentListOfSongs: List<Song>
) : Dialog(context) {

    fun openDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.playlist_add_songs_dialog)

        val songsRecyclerView: RecyclerView = findViewById(R.id.playlistAddSongsRecyclerView)
        val songsAdapter = AddToPlaylistSongsAdapter(listOf())
        songsRecyclerView.visibility = View.VISIBLE
        songsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        songsRecyclerView.adapter = songsAdapter

        findViewById<Button>(R.id.playlistAddSongsAddButton).setOnClickListener {
            val checked = songsAdapter.getCheckedSongs()
            if (checked.isEmpty()) {
                cancel()
                return@setOnClickListener
            }

            songsViewModel.addSongsToPlaylist(checked, playlist!!)
            dismiss()
        }
        findViewById<Button>(R.id.playlistAddSongsCancelButton).setOnClickListener {
            cancel()
        }

        songsRecyclerView.loadSkeleton(R.layout.playlist_add_songs_view) {
            itemCount(4)
        }
        show()

        scope.launch {
            songsViewModel.loadAllSongs().collect { songsList ->
                songsRecyclerView.hideSkeleton()

                val filteredSongsList = songsList.filterNot {
                    currentListOfSongs.find { song -> song.songId == it.songId } != null
                }
                if (filteredSongsList.isEmpty()) {
                    songsRecyclerView.visibility = View.INVISIBLE
                    findViewById<TextView>(R.id.playlistAddSongsEmpty).visibility = View.VISIBLE
                } else songsAdapter.updateList(filteredSongsList)
            }
        }
    }

}