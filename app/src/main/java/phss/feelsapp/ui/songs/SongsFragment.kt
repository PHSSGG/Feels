package phss.feelsapp.ui.songs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.FragmentSongsBinding
import phss.feelsapp.ui.songs.adapters.SongsAdapter
import phss.feelsapp.ui.songs.adapters.SongsAdapterItemInteractListener

class SongsFragment : Fragment() {

    private val songsViewModel: SongsViewModel by inject()

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

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
                updateSongsViewWithPlaylist()
            }
            else updateSongsView()
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
                songsViewModel.deleteSong(song)
            }
        }
    }

    private fun updateSongsView() {
        binding.songsAddButton.visibility = View.INVISIBLE

        lifecycle.coroutineScope.launchWhenStarted {
            songsViewModel.loadAllSongs().collect {
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
                songsAdapter?.updateList(it.songs)
            }
        }
    }

}