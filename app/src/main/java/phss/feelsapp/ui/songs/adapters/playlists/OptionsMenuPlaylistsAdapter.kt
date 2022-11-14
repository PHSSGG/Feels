package phss.feelsapp.ui.songs.adapters.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist

class OptionsMenuPlaylistsAdapter(
    private var playlists: List<Playlist>,
    private var adapterListener: OptionsMenuPlaylistsItemInteractListener
) : RecyclerView.Adapter<OptionsMenuPlaylistsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.options_menu_playlists_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = playlists[position]

        holder.playlistName.text = item.playlistName

        holder.itemView.setOnClickListener {
            adapterListener.onClick(item)
        }
    }

    override fun getItemCount(): Int {
        return playlists.size
    }

    fun updateList(newList: List<Playlist>) {
        playlists = newList
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val playlistName: TextView = itemView.findViewById(R.id.optionsMenuPlaylistTitle)
    }

}