package phss.feelsapp.ui.songs.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import java.io.File

class SongsAdapter(
    private var songsList: List<Song>,
    private var adapterListener: SongsAdapterItemInteractListener
) : RecyclerView.Adapter<SongsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_songs_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position]

        Picasso.get().load(File(item.thumbnailPath)).into(holder.imageView)
        holder.title.text = item.name
        holder.artist.text = item.artist
        holder.duration.text = item.duration

        holder.itemView.setOnClickListener {
            adapterListener.onClick(item)
        }
        holder.itemView.setOnLongClickListener {
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

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.songThumb)
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val artist: TextView = itemView.findViewById(R.id.songArtist)
        val duration: TextView = itemView.findViewById(R.id.songDuration)
    }

}