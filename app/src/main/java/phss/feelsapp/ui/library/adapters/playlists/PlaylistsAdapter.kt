package phss.feelsapp.ui.library.adapters.playlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Playlist

class PlaylistsAdapter(
    private var playlists: List<Playlist>,
    private var adapterListener: PlaylistAdapterItemInteractListener
) : RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_playlists_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = playlists[position]

        holder.playlistName.text = item.playlistName

        holder.itemView.setOnClickListener {
            adapterListener.onClick(item)
        }
        holder.itemView.setOnLongClickListener {
            adapterListener.onLongClick(item)
            true
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
        val playlistName: TextView = itemView.findViewById(R.id.libraryPlaylistTitle)
        val playlistGoButton: ImageButton = itemView.findViewById(R.id.playlistGoButton)
    }

}