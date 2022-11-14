package phss.feelsapp.ui.songs.adapters.playlists.songs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.Song
import java.io.File

class AddToPlaylistSongsAdapter(
    private var songsList: List<Song>
) : RecyclerView.Adapter<AddToPlaylistSongsAdapter.ViewHolder>() {

    private val checked = ArrayList<Song>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_add_songs_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = songsList[position]

        Picasso.get().load(File(item.thumbnailPath)).into(holder.imageView)
        holder.title.text = item.name
        holder.artist.text = item.artist
        holder.checkBox.isChecked = checked.contains(item)

        holder.checkBox.setOnClickListener { _, ->
            if (!checked.contains(item)) checked.add(item)
            else checked.remove(item)
        }
    }

    override fun getItemCount(): Int {
        return songsList.size
    }

    fun updateList(newList: List<Song>) {
        songsList = newList
        notifyDataSetChanged()
    }

    fun getCheckedSongs(): List<Song> {
        return checked
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.playlistAddSongsSongThumb)
        val title: TextView = itemView.findViewById(R.id.playlistAddSongsSongTitle)
        val artist: TextView = itemView.findViewById(R.id.playlistAddSongsSongArtist)
        val checkBox: CheckBox = itemView.findViewById(R.id.playlistAddSongsSongCheckBox)
    }

}