package phss.feelsapp.ui.download.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener

class DownloadSongItemAdapter(
    private var songsList: List<RemoteSong>,
    private val adapterListener: DownloadAdapterItemInteractListener
) : RecyclerView.Adapter<DownloadSongItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item_download_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songsList[position]

        if (song.item.thumbnail != null) Picasso.get().load(song.item.thumbnail?.setSize(95)).into(holder.imageView)
        holder.title.text = song.item.info?.name ?: "Null"
        holder.artist.text = song.item.authors?.firstOrNull()?.name ?: "Null"
        holder.songView.background = null

        if (song.downloading) {
            if (song.downloadProgress == 0f) holder.downloadCircleProgressBar.progress = 0

            holder.downloadButton.visibility = View.INVISIBLE
            holder.downloadCircleProgressBar.visibility = View.VISIBLE
        } else {
            if (!song.alreadyDownloaded) {
                holder.downloadCircleProgressBar.visibility = View.GONE
                holder.deleteButton.visibility = View.GONE

                holder.downloadButton.visibility = View.VISIBLE
            }
            else {
                holder.downloadCircleProgressBar.visibility = View.GONE
                holder.downloadButton.visibility = View.GONE

                holder.deleteButton.visibility = View.VISIBLE
            }

            holder.downloadCircleProgressBar.visibility = View.GONE
        }

        holder.songView.setOnClickListener {
            adapterListener.onSongViewClick(song)
        }
        holder.downloadButton.setOnClickListener {
            adapterListener.onDownloadButtonClick(song)
        }
        holder.downloadCircleProgressBar.setOnClickListener {
            adapterListener.onDownloadButtonClick(song)
        }
        holder.deleteButton.setOnClickListener {
            adapterListener.onDeleteButtonClick(song)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.firstOrNull() != null) {
            (payloads.first() as Bundle).getFloat("progress").also {
                holder.downloadCircleProgressBar.progress = it.toInt()
            }
        }

        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int {
        return songsList.size
    }

    fun updateList(newList: List<RemoteSong>) {
        songsList = newList
        notifyDataSetChanged()
    }

    fun updateSong(song: RemoteSong) {
        notifyItemChanged(songsList.indexOf(song))
    }

    fun updateDownloading(song: RemoteSong, isDownloading: Boolean) {
        song.downloading = isDownloading
        notifyItemChanged(songsList.indexOf(song))
    }

    fun updateDownloadProgress(song: RemoteSong, progress: Float) {
        song.downloadProgress = progress
        notifyItemChanged(songsList.indexOf(song), Bundle().apply { putFloat("progress", progress) })
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val songView: ConstraintLayout = itemView.findViewById(R.id.songItemView)
        val imageView: ImageView = itemView.findViewById(R.id.songItemThumb)
        val title: TextView = itemView.findViewById(R.id.songItemTitle)
        val artist: TextView = itemView.findViewById(R.id.songItemArtist)
        val downloadButton: ImageButton = itemView.findViewById(R.id.songItemDownloadButton)
        val downloadCircleProgressBar: ProgressBar = itemView.findViewById(R.id.downloadCircleProgressBar)
        val deleteButton: ImageButton = itemView.findViewById(R.id.songItemDeleteButton)
    }

}