package phss.feelsapp.ui.download.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.databinding.FragmentDownloadsDrawerBinding
import phss.feelsapp.download.listeners.DownloadUpdateListener
import phss.feelsapp.service.DownloaderService
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener
import phss.feelsapp.ui.download.adapter.DownloadSongItemAdapter
import phss.feelsapp.ui.download.viewmodel.DownloadViewModel


class DownloadsDrawerFragment : Fragment() {

    private val downloadViewModel: DownloadViewModel by inject()
    private val downloaderService: DownloaderService by inject()

    private var _binding: FragmentDownloadsDrawerBinding? = null
    private val binding get() = _binding!!

    private var downloadAdapter: DownloadSongItemAdapter? = null

    var downloading = ArrayList<RemoteSong>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsDrawerBinding.inflate(inflater, container, false)

        if (downloadAdapter == null) downloadAdapter = DownloadSongItemAdapter(listOf(), setupDownloadingItemClickListener())

        binding.downloadingRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.downloadingRecyclerView.adapter = downloadAdapter!!

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

    private fun setupObservableDownloadUpdateListener() {
        downloaderService.registerDownloadUpdateListener(this::class, object :
            DownloadUpdateListener {
            override fun onDownloadStart(song: RemoteSong) {
                if (downloading.isEmpty()) {
                    requireActivity().findViewById<ConstraintLayout>(R.id.downloadingIndicator).run {
                        visibility = View.VISIBLE
                        val pulse: Animation = AnimationUtils.loadAnimation(requireContext(), R.anim.downloading_indicator_pulse)
                        startAnimation(pulse)
                    }
                }

                downloading.add(song)
                requireActivity().runOnUiThread {
                    binding.downloadsDrawerNothingDownloading.visibility = View.GONE

                    downloadAdapter?.updateList(downloading)
                    downloadAdapter?.updateDownloading(song, song.downloading)
                }
            }

            override fun onDownloadFinish(song: RemoteSong, success: Boolean) {
                downloading.remove(song)
                lifecycleScope.launch {
                    delay(1000L)
                    requireActivity().runOnUiThread {
                        if (downloading.isEmpty()) {
                            binding.downloadsDrawerNothingDownloading.visibility = View.VISIBLE
                            requireActivity().findViewById<ConstraintLayout>(R.id.downloadingIndicator).run {
                                clearAnimation()
                                visibility = View.GONE
                            }
                        }

                        downloadAdapter?.updateDownloading(song, song.downloading)
                        downloadAdapter?.updateList(downloading)
                    }
                }
            }

            override fun onDownloadProgressUpdate(song: RemoteSong, progress: Float) {
                requireActivity().runOnUiThread { downloadAdapter?.updateDownloadProgress(song, progress) }
            }
        })
    }

    private fun setupDownloadingItemClickListener(): DownloadAdapterItemInteractListener {
        return object : DownloadAdapterItemInteractListener {
            override fun onSongViewClick(song: RemoteSong) {
            }
            override fun onDeleteButtonClick(song: RemoteSong) {
            }

            override fun onDownloadButtonClick(song: RemoteSong) {
                downloading.remove(song)
                downloadAdapter?.updateList(downloading)

                downloadViewModel.cancelSongDownload(song)
            }
        }
    }

}