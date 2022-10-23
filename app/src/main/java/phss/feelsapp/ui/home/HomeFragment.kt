package phss.feelsapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import koleton.api.hideSkeleton
import koleton.api.loadSkeleton
import org.koin.androidx.viewmodel.ext.android.viewModel
import phss.feelsapp.R
import phss.feelsapp.databinding.FragmentHomeBinding
import phss.feelsapp.ui.home.adapters.recommendations.RecommendationsAdapter

class HomeFragment : Fragment() {

    private val homeViewModel: HomeViewModel by viewModel()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecentlyPlayedView()
        setupRecommendationsView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecentlyPlayedView() {
        binding.recentlyNothingToShow.visibility = View.VISIBLE
        binding.recentlyHorizontalRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupRecommendationsView() {
        binding.recommendationsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        updateRecommendationsView()
    }

    private fun updateRecommendationsView() {
        binding.recommendationsRecyclerView.loadSkeleton(R.layout.recommendations_view) {
            itemCount(4)
        }

        homeViewModel.getRecommendationsList { songsList ->
            requireActivity().runOnUiThread {
                binding.recommendationsRecyclerView.hideSkeleton()

                if (songsList.isEmpty()) binding.recommendationsNothingToShow.visibility = View.VISIBLE
                else {
                    binding.recommendationsNothingToShow.visibility = View.GONE
                    binding.recommendationsRecyclerView.adapter = RecommendationsAdapter(songsList)
                }
            }
        }
    }

}