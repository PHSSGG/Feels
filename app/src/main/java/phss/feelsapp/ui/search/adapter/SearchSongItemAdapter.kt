package phss.feelsapp.ui.search.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.ytmusicwrapper.response.models.SongItem

class SearchSongItemAdapter(
    private var songsList: List<SongItem>
) : RecyclerView.Adapter<SearchSongItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item_download_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position]

        if (item.thumbnail != null) Picasso.get().load(item.thumbnail?.setSize(95)).into(holder.imageView)
        holder.title.text = item.info?.name ?: "Null"
        holder.artist.text = item.authors?.firstOrNull()?.name ?: "Null"
    }

    override fun getItemCount(): Int {
        return songsList.size
    }

    fun updateList(newList: List<SongItem>) {
        songsList = newList
        notifyDataSetChanged()
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.songItemThumb)
        val title: TextView = itemView.findViewById(R.id.songItemTitle)
        val artist: TextView = itemView.findViewById(R.id.songItemArtist)
    }

}