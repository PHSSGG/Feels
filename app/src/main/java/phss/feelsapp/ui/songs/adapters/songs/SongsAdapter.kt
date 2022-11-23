package phss.feelsapp.ui.songs.adapters.songs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
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
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position]

        Picasso.get().load(File(item.thumbnailPath)).into(holder.imageView)
        holder.title.text = item.name
        holder.artist.text = item.artist
        holder.duration.text = item.duration

        if (item.isPlaying) {
            holder.songView.background = AppCompatResources.getDrawable(holder.getContext(), R.drawable.playing_song_style)
        } else holder.songView.background = null

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

    fun updateItems(vararg songs: Song?) {
        songs.filterNotNull().forEach {
            notifyItemChanged(songsList.indexOf(it))
        }
    }

    fun updateList(newList: List<Song>) {
        songsList = newList
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View, private val context: Context) : RecyclerView.ViewHolder(ItemView) {
        val songView: View = itemView.findViewById(R.id.librarySongItemView)
        val imageView: ImageView = itemView.findViewById(R.id.songThumb)
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val artist: TextView = itemView.findViewById(R.id.songArtist)
        val duration: TextView = itemView.findViewById(R.id.songDuration)

        fun getContext(): Context {
            return context
        }
    }

}