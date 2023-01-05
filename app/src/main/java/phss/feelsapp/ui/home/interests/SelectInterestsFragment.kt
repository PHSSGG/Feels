package phss.feelsapp.ui.home.interests

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.koin.android.ext.android.inject
import phss.feelsapp.R
import phss.feelsapp.databinding.SelectInterestsFragmentBinding
import phss.feelsapp.ui.home.HomeFragment

class SelectInterestsFragment : Fragment() {

    private val selectInterestsViewModel: SelectInterestsViewModel by inject()

    private var _binding: SelectInterestsFragmentBinding? = null
    private val binding get() = _binding!!

    val interests = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SelectInterestsFragmentBinding.inflate(inflater, container, false)

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.GONE
        setupInterestsButtons()

        return binding.root
    }

    private fun setupInterestsButtons() {
        val buttons = mapOf(
            binding.genre1 to "pop",
            binding.genre2 to "hip hop",
            binding.genre3 to "rock",
            binding.genre4 to "blues",
            binding.genre5 to "eletronic",
            binding.genre6 to "reggae",
            binding.genre7 to "country",
            binding.genre8 to "funk",
            binding.genre9 to "gospel",
            binding.genre10 to "jazz",
            binding.genre11 to "disco",
            binding.genre12 to "classical",
            binding.genre13 to "sertanejo",
            binding.genre14 to "samba",
            binding.genre15 to "pagode",
            binding.genre16 to "kpop",
            binding.genre17 to "j-pop",
            binding.genre18 to "c-pop",
        )

        buttons.forEach { (button, genre) ->
            button.setOnClickListener {
                if (interests.contains(genre)) {
                    interests.remove(genre)
                    button.background = ContextCompat.getDrawable(requireContext(), R.drawable.interests_genre_button_normal)
                } else {
                    interests.add(genre)
                    button.background = ContextCompat.getDrawable(requireContext(), R.drawable.interests_genre_button_selected)
                }

                if (interests.size == 5) selectInterestsViewModel.createNewUser(interests) {
                    requireActivity().runOnUiThread {
                        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).visibility = View.VISIBLE

                        val transaction = activity?.supportFragmentManager?.beginTransaction() ?: return@runOnUiThread
                        transaction.replace(R.id.nav_host_fragment_content, HomeFragment())
                        transaction.commit()
                    }
                }
            }
        }
    }

}