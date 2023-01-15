package phss.feelsapp.ui.recently

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import phss.feelsapp.ui.songs.adapters.songs.SongsAdapterItemInteractListener
import java.io.File

class RecentlyAdapter(
    private var songsList: List<Song>,
    private var adapterListener: SongsAdapterItemInteractListener
) : RecyclerView.Adapter<RecentlyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recently_song_view, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position]

        Picasso.get().load(File(item.thumbnailPath)).into(holder.imageView)
        holder.title.text = item.name
        holder.artist.text = item.artist

        holder.title.setTextColor(holder.getContext().resources.getColor(if (item.isPlaying) R.color.green else R.color.white, null))
        holder.artist.setTextColor(holder.getContext().resources.getColor(if (item.isPlaying) R.color.green else R.color.white, null))

        holder.songView.setOnClickListener {
            adapterListener.onClick(item)
        }
        holder.songView.setOnLongClickListener {
            adapterListener.onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int {
        return songsList.size
    }

    fun updateList(newList: List<Song>) {
        songsList = newList
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View, private val context: Context) : RecyclerView.ViewHolder(ItemView) {
        val songView: View = itemView.findViewById(R.id.recentlySongItemView)
        val imageView: ImageView = itemView.findViewById(R.id.recentlyThumbnail)
        val title: TextView = itemView.findViewById(R.id.recentlyTitle)
        val artist: TextView = itemView.findViewById(R.id.recentlyArtist)

        fun getContext(): Context {
            return context
        }
    }

}