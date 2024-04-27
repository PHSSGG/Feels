package phss.feelsapp.ui.songs.adapters.songs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import java.io.File
import java.util.*

class SongsAdapter(
    private var songsList: List<Song>,
    private var adapterListener: SongsAdapterItemInteractListener
) : RecyclerView.Adapter<SongsAdapter.ViewHolder>(), Filterable {

    private var songsListFiltered: List<Song> = songsList
    private var isFiltering = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_songs_view, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

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
        return if (isFiltering) songsListFiltered.size else songsList.size
    }

    private fun getItemIndex(song: Song): Int {
        println(songsListFiltered)
        if (isFiltering) {
            if (songsListFiltered.find { it.key == song.key } != null) return songsListFiltered.indexOf(song)
        }
        return songsList.indexOf(song)
    }

    private fun getItem(position: Int): Song? {
        println(songsListFiltered)
        if (isFiltering) return songsListFiltered.getOrNull(position)
        return songsList.getOrNull(position)
    }

    fun updateItems(vararg songs: Song?) {
        songs.filterNotNull().forEach {
            notifyItemChanged(getItemIndex(it))
        }
    }

    fun updateList(newList: List<Song>) {
        songsList = newList
        songsListFiltered = newList

        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val queryString = query?.toString()

                val results = FilterResults()
                if (queryString != null) {
                    results.values = songsList
                        .filter { it.name.lowercase().contains(queryString)
                                || it.artist.lowercase().contains(queryString)
                                || it.album.lowercase().contains(queryString) }
                    isFiltering = true
                } else {
                    results.values = songsList
                    isFiltering = false
                }

                return results
            }

            override fun publishResults(query: CharSequence?, results: FilterResults?) {
                songsListFiltered = if (results?.values == null) listOf() else {
                    if (results.values is List<*>) results.values as List<Song>
                    else listOf()
                }
                notifyDataSetChanged()
            }
        }
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