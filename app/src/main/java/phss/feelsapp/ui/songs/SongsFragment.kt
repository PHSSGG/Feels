package phss.feelsapp.ui.songs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist
import phss.feelsapp.data.models.Song
import phss.feelsapp.databinding.FragmentSongsBinding
import phss.feelsapp.player.observers.PlayerObserver
import phss.feelsapp.service.PlayerService
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapter
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener
import phss.feelsapp.ui.songs.dialogs.PlaylistAddSongsDialog
import phss.feelsapp.ui.songs.dialogs.SongOptionsDialog

class SongsFragment : Fragment() {

    private val songsViewModel: SongsViewModel by inject()

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var playerService: PlayerService

    var currentListOfSongs: List<Song> = ArrayList()
        set(value) {
            if (::playerService.isInitialized && playerService.playerManager.getCurrentPlaying() != null) {
                val currentPlaying = playerService.playerManager.getCurrentPlaying()!!
                val playingFromPlaylist = playerService.playerManager.playingFromPlaylist
                value.forEach {
                    it.isPlaying = currentPlaying.key == it.key
                            && if (playingFromPlaylist != null) playingFromPlaylist.playlistId == playlist?.playlistId
                    else playlist == null
                }
            }

            field = value
        }
    var playlist: Playlist? = null
    var songsAdapter: SongsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        bindPlayerService()
        songsAdapter = SongsAdapter(listOf(), setupSongItemInteractListener())
        setupSongsView()
        setFragmentResultListener("requestKey") { _, bundle ->
            val playlistId = bundle.getLong("playlistId", -1L)
            if (playlistId != -1L) songsViewModel.getPlaylistById(playlistId) {
                playlist = it
                requireActivity().runOnUiThread {
                    binding.songsLibraryText.text = playlist!!.playlistName

                    setupAddSongsButton()
                    updateSongsViewWithPlaylist()
                }
            }
            else {
                binding.songsAddButton.visibility = View.INVISIBLE
                updateSongsView()
            }
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        playerService.playerManager.unregisterPlayerObserver(this::class)
    }

    override fun onResume() {
        super.onResume()
        // update playing info
        currentListOfSongs = currentListOfSongs
        songsAdapter?.updateList(currentListOfSongs)
        setupPlayerObserver()
    }

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            playerService.playerManager.unregisterPlayerObserver(this::class)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()

            val currentPlaying = playerService.playerManager.getCurrentPlaying()
            if (currentPlaying != null) songsAdapter?.updateItems(currentPlaying)

            setupPlayerObserver()
        }
    }
    private fun bindPlayerService() {
        val playerServiceIntent = Intent(requireContext(), PlayerService::class.java)
        requireActivity().bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupPlayerObserver() {
        if (!::playerService.isInitialized) return

        playerService.playerManager.registerPlayerObserver(this::class, object : PlayerObserver {
            override fun onPlay(song: Song, previous: Song?) {
                requireActivity().runOnUiThread { songsAdapter?.updateItems(song, previous) }
            }
            override fun onStop(song: Song) {
                requireActivity().runOnUiThread { songsAdapter?.updateItems(song) }
            }

            override fun onShuffleStateChange(newSongsList: List<Song>) {
                currentListOfSongs = newSongsList
                requireActivity().runOnUiThread { songsAdapter?.updateList(currentListOfSongs) }
            }
        })
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
                val previousSong = playerService.playerManager.getCurrentPlaying()
                if (previousSong != null && previousSong.key == song.key && song.isPlaying) {
                    if (playerService.playerManager.isPlaying()) playerService.playerManager.pausePlayer()
                    else playerService.playerManager.resumePlayer()
                    return
                }

                playerService.playerManager.setupPlayer(currentListOfSongs, song, playlist)
                songsAdapter?.updateItems(song, previousSong)
            }
            override fun onLongClick(song: Song) {
                SongOptionsDialog(
                    requireContext(),
                    lifecycleScope,
                    songsViewModel,
                    song, playlist
                ).openDialog()
            }
        }
    }

    private fun setupAddSongsButton() {
        binding.songsAddButton.setOnClickListener {
            PlaylistAddSongsDialog(
                requireContext(),
                lifecycleScope,
                songsViewModel,
                playlist, currentListOfSongs
            ).openDialog()
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