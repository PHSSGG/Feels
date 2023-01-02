package phss.feelsapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import phss.feelsapp.R
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.databinding.FragmentHomeBinding
import phss.feelsapp.download.listeners.DownloadUpdateListener
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener
import phss.feelsapp.ui.download.adapter.DownloadSongItemAdapter
import phss.feelsapp.ui.download.viewmodel.DownloadViewModel

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModel()
    private val downloadViewModel: DownloadViewModel by inject()
    private val downloaderService: DownloaderService by inject()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var downloadAdapter: DownloadSongItemAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupObservableDownloadUpdateListener()
        setupRecentlyPlayedView()
        setupRecommendationsView()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupObservableDownloadUpdateListener()
    }

    override fun onPause() {
        super.onPause()
        downloaderService.unregisterDownloadUpdateListener(this::class)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservableDownloadUpdateListener() {
        downloaderService.registerDownloadUpdateListener(this::class, object :
            DownloadUpdateListener {
            override fun onDownloadStart(song: RemoteSong) {
                requireActivity().runOnUiThread { downloadAdapter?.updateDownloading(song, song.downloading) }
            }

            override fun onDownloadFinish(song: RemoteSong, success: Boolean) {
                lifecycleScope.launch {
                    delay(1000L)
                    requireActivity().runOnUiThread { downloadAdapter?.updateDownloading(song, song.downloading) }
                }
            }

            override fun onDownloadProgressUpdate(song: RemoteSong, progress: Float) {
                requireActivity().runOnUiThread { downloadAdapter?.updateDownloadProgress(song, progress) }
            }
        })
    }

    private fun setupRecentlyPlayedView() {
        binding.recentlyNothingToShow.visibility = View.VISIBLE
        binding.recentlyHorizontalRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupRecommendationsView() {
        if (downloadAdapter == null) downloadAdapter = DownloadSongItemAdapter(listOf(), setupRecommendationsViewClickListener())

        binding.recommendationsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.recommendationsRecyclerView.adapter = downloadAdapter!!

        updateRecommendationsView()
    }

    private fun setupRecommendationsViewClickListener(): DownloadAdapterItemInteractListener {
        return object : DownloadAdapterItemInteractListener {
            override fun onSongViewClick(song: RemoteSong) {
                song.playing = !song.playing
            }

            override fun onDownloadButtonClick(song: RemoteSong) {
                if (!song.downloading) downloadViewModel.downloadSong(song)
                else downloadViewModel.cancelSongDownload(song)
            }

            override fun onDeleteButtonClick(song: RemoteSong) {
                downloadViewModel.deleteSong(song)
                downloadAdapter?.updateSong(song)
            }
        }
    }

    private fun updateRecommendationsView() {
        binding.recommendationsRecyclerView.loadSkeleton(R.layout.song_item_download_view) {
            itemCount(4)
        }

        homeViewModel.getRecommendationsList { songsList ->
            songsList.forEach {
                if (downloaderService.downloading.contains(it.item.key)) it.downloading = true
            }
            requireActivity().runOnUiThread {
                binding.recommendationsRecyclerView.hideSkeleton()

                if (songsList.isEmpty()) binding.recommendationsNothingToShow.visibility = View.VISIBLE
                else {
                    binding.recommendationsNothingToShow.visibility = View.GONE

                    downloadAdapter?.updateList(songsList)
                    binding.recommendationsRecyclerView.adapter = downloadAdapter
                }
            }
        }
    }

}