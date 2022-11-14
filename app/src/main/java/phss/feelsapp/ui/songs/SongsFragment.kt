package phss.feelsapp.ui.songs

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.FragmentSongsBinding
import phss.feelsapp.ui.songs.adapters.playlists.OptionsMenuPlaylistsAdapter
import phss.feelsapp.ui.songs.adapters.playlists.OptionsMenuPlaylistsItemInteractListener
import phss.feelsapp.ui.songs.adapters.playlists.songs.AddToPlaylistSongsAdapter
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapter
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener
import java.io.File

class SongsFragment : Fragment() {

    private val songsViewModel: SongsViewModel by inject()

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    var currentListOfSongs: List<Song> = ArrayList()
    var playlist: Playlist? = null
    var songsAdapter: SongsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        songsAdapter = SongsAdapter(listOf(), setupSongItemInteractListener())
        setupSongsView()
        setFragmentResultListener("requestKey") { _, bundle ->
            val playlistId = bundle.getLong("playlistId", -1L)
            if (playlistId != -1L) songsViewModel.getPlaylistById(playlistId) {
                playlist = it
                binding.songsLibraryText.text = playlist!!.playlistName

                setupAddSongsButton()
                updateSongsViewWithPlaylist()
            }
            else {
                binding.songsAddButton.visibility = View.INVISIBLE
                updateSongsView()
            }
        }

        return binding.root
    }

    private fun setupSongsView() {
        binding.songsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.songsRecyclerView.adapter = songsAdapter

        binding.songsRecyclerView.loadSkeleton(R.layout.library_songs_view) {
            itemCount(10)
        }
    }

    private fun setupSongItemInteractListener(): SongsAdapterItemInteractListener {
        return object : SongsAdapterItemInteractListener {
            override fun onClick(song: Song) {
                // TODO: Play song
            }
            override fun onLongClick(song: Song) {
                val songOptionsDialog = Dialog(requireContext()).also {
                    it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    it.setCancelable(true)
                    it.setContentView(R.layout.song_options_menu_dialog)
                }

                songOptionsDialog.findViewById<ImageView>(R.id.songOptionsMenuSongThumb).run {
                    Picasso.get().load(File(song.thumbnailPath)).into(this)
                }
                songOptionsDialog.findViewById<TextView>(R.id.songOptionsMenuSongTitle).text = song.name
                songOptionsDialog.findViewById<TextView>(R.id.songOptionsMenuSongArtist).text = song.artist
                songOptionsDialog.findViewById<TextView>(R.id.songOptionsMenuSongDuration).text = song.duration

                val songOptionsMenuAddToPlaylistButton: View = songOptionsDialog.findViewById(R.id.songOptionsMenuAddToPlaylistButton)
                val songOptionsMenuRemoveFromPlaylistButton: View = songOptionsDialog.findViewById(R.id.songOptionsMenuRemoveFromPlaylistButton)
                val songOptionsMenuDeleteSongButton: View = songOptionsDialog.findViewById(R.id.songOptionsMenuDeleteSongButton)

                songOptionsMenuAddToPlaylistButton.setOnClickListener {
                    songOptionsMenuAddToPlaylistButton.visibility = View.GONE
                    songOptionsMenuRemoveFromPlaylistButton.visibility = View.GONE
                    songOptionsMenuDeleteSongButton.visibility = View.GONE

                    updatePlaylistsOnSongOptionsDialog(songOptionsDialog, song)
                }
                songOptionsMenuRemoveFromPlaylistButton.setOnClickListener {
                    songsViewModel.removeSongFromPlaylist(song, playlist!!)
                    songOptionsDialog.dismiss()
                }
                songOptionsMenuDeleteSongButton.setOnClickListener {
                    songsViewModel.deleteSong(song)
                    songOptionsDialog.dismiss()
                }

                if (playlist == null) songOptionsMenuRemoveFromPlaylistButton.visibility = View.GONE

                songOptionsDialog.show()
            }
        }
    }

    private fun updatePlaylistsOnSongOptionsDialog(songOptionsDialog: Dialog, song: Song) {
        val playlistsRecyclerView: RecyclerView = songOptionsDialog.findViewById(R.id.songOptionsMenuPlaylistsRecyclerView)
        val playlistsAdapter = OptionsMenuPlaylistsAdapter(listOf(), object : OptionsMenuPlaylistsItemInteractListener {
            override fun onClick(playlist: Playlist) {
                songsViewModel.addSongToPlaylist(song, playlist)
                songOptionsDialog.dismiss()
            }
        })
        playlistsRecyclerView.visibility = View.VISIBLE
        playlistsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        playlistsRecyclerView.adapter = playlistsAdapter

        playlistsRecyclerView.loadSkeleton(R.layout.library_playlists_view) {
            itemCount(4)
        }

        lifecycleScope.launch {
            songsViewModel.loadPlaylists().collect {
                playlistsRecyclerView.hideSkeleton()

                if (it.isEmpty()) {
                    playlistsRecyclerView.visibility = View.GONE
                    songOptionsDialog.findViewById<TextView>(R.id.songOptionsMenuPlaylistsEmptyText).visibility = View.VISIBLE
                } else {
                    playlistsAdapter.updateList(it)
                }
            }
        }
    }

    private fun setupAddSongsButton() {
        binding.songsAddButton.setOnClickListener {
            val addSongsDialog = Dialog(requireContext()).also {
                it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                it.setCancelable(true)
                it.setContentView(R.layout.playlist_add_songs_dialog)
            }
            val songsRecyclerView: RecyclerView = addSongsDialog.findViewById(R.id.playlistAddSongsRecyclerView)
            val songsAdapter = AddToPlaylistSongsAdapter(listOf())
            songsRecyclerView.visibility = View.VISIBLE
            songsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            songsRecyclerView.adapter = songsAdapter

            addSongsDialog.findViewById<Button>(R.id.playlistAddSongsAddButton).setOnClickListener {
                val checked = songsAdapter.getCheckedSongs()
                if (checked.isEmpty()) {
                    addSongsDialog.cancel()
                    return@setOnClickListener
                }

                songsViewModel.addSongsToPlaylist(checked, playlist!!)
                addSongsDialog.dismiss()
            }
            addSongsDialog.findViewById<Button>(R.id.playlistAddSongsCancelButton).setOnClickListener {
                addSongsDialog.cancel()
            }

            songsRecyclerView.loadSkeleton(R.layout.playlist_add_songs_view) {
                itemCount(4)
            }
            addSongsDialog.show()

            lifecycleScope.launch {
                songsViewModel.loadAllSongs().collect { songsList ->
                    songsRecyclerView.hideSkeleton()

                    val filteredSongsList = songsList.filterNot {
                        currentListOfSongs.find { song -> song.songId == it.songId } != null
                    }
                    if (filteredSongsList.isEmpty()) {
                        songsRecyclerView.visibility = View.INVISIBLE
                        addSongsDialog.findViewById<TextView>(R.id.playlistAddSongsEmpty).visibility = View.VISIBLE
                    } else songsAdapter.updateList(filteredSongsList)
                }
            }
        }
    }

    private fun updateSongsView() {
        lifecycle.coroutineScope.launchWhenStarted {
            songsViewModel.loadAllSongs().collect {
                currentListOfSongs = it

                binding.songsRecyclerView.hideSkeleton()
                songsAdapter?.updateList(it)
            }
        }
    }

    private fun updateSongsViewWithPlaylist() {
        if (playlist == null) return

        lifecycle.coroutineScope.launchWhenStarted {
            songsViewModel.loadPlaylistWithSongs(playlist!!).collect {
                binding.songsRecyclerView.hideSkeleton()

                if (it != null && it.songs != null) {
                    currentListOfSongs = it.songs
                    songsAdapter?.updateList(it.songs)
                }
            }
        }
    }

}