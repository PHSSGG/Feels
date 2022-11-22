package phss.feelsapp.ui.songs.dialogs

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.ui.songs.SongsViewModel
import phss.feelsapp.ui.songs.adapters.playlists.OptionsMenuPlaylistsAdapter
import phss.feelsapp.ui.songs.adapters.playlists.OptionsMenuPlaylistsItemInteractListener
import java.io.File

class SongOptionsDialog(
    context: Context,
    private val scope: CoroutineScope,
    private val songsViewModel: SongsViewModel,
    private val song: Song,
    private val playlist: Playlist?
) : Dialog(context) {

    fun openDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setContentView(R.layout.song_options_menu_dialog)

        findViewById<ImageView>(R.id.songOptionsMenuSongThumb).run {
            Picasso.get().load(File(song.thumbnailPath)).into(this)
        }
        findViewById<TextView>(R.id.songOptionsMenuSongTitle).text = song.name
        findViewById<TextView>(R.id.songOptionsMenuSongArtist).text = song.artist
        findViewById<TextView>(R.id.songOptionsMenuSongDuration).text = song.duration

        val songOptionsMenuAddToPlaylistButton: View = findViewById(R.id.songOptionsMenuAddToPlaylistButton)
        val songOptionsMenuRemoveFromPlaylistButton: View = findViewById(R.id.songOptionsMenuRemoveFromPlaylistButton)
        val songOptionsMenuDeleteSongButton: View = findViewById(R.id.songOptionsMenuDeleteSongButton)

        songOptionsMenuAddToPlaylistButton.setOnClickListener {
            songOptionsMenuAddToPlaylistButton.visibility = View.GONE
            songOptionsMenuRemoveFromPlaylistButton.visibility = View.GONE
            songOptionsMenuDeleteSongButton.visibility = View.GONE

            updatePlaylistsOnSongOptionsDialog(song)
        }
        songOptionsMenuRemoveFromPlaylistButton.setOnClickListener {
            songsViewModel.removeSongFromPlaylist(song, playlist!!)
            dismiss()
        }
        songOptionsMenuDeleteSongButton.setOnClickListener {
            songsViewModel.deleteSong(song)
            dismiss()
        }

        if (playlist == null) songOptionsMenuRemoveFromPlaylistButton.visibility = View.GONE

        show()
    }

    private fun updatePlaylistsOnSongOptionsDialog(song: Song) {
        val playlistsRecyclerView: RecyclerView = findViewById(R.id.songOptionsMenuPlaylistsRecyclerView)
        val playlistsAdapter = OptionsMenuPlaylistsAdapter(listOf(), object : OptionsMenuPlaylistsItemInteractListener {
            override fun onClick(playlist: Playlist) {
                songsViewModel.addSongToPlaylist(song, playlist)
                dismiss()
            }
        })
        playlistsRecyclerView.visibility = View.VISIBLE
        playlistsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        playlistsRecyclerView.adapter = playlistsAdapter

        playlistsRecyclerView.loadSkeleton(R.layout.library_playlists_view) {
            itemCount(4)
        }

        scope.launch {
            songsViewModel.loadPlaylists().collect {
                playlistsRecyclerView.hideSkeleton()

                if (it.isEmpty()) {
                    playlistsRecyclerView.visibility = View.GONE
                    findViewById<TextView>(R.id.songOptionsMenuPlaylistsEmptyText).visibility = View.VISIBLE
                } else {
                    playlistsAdapter.updateList(it)
                }
            }
        }
    }

}