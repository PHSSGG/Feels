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
import com.airbnb.lottie.LottieAnimationView
import com.squareup.picasso.Picasso
import phss.feelsapp.R
import phss.feelsapp.data.models.RemoteSong
import phss.feelsapp.ui.download.DownloadAdapterItemInteractListener

class DownloadSongItemAdapter(
    private var songsList: ArrayList<RemoteSong>,
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

        holder.downloadButton.isEnabled = true
        holder.downloadCircleProgressBar.isEnabled = true
        holder.deleteButton.isEnabled = true

        if (song.downloading) {
            if (song.downloadProgress == 0f) {
                holder.downloadCircleProgressBar.progress = 0

                holder.downloadCircleClickAnimation.visibility = View.VISIBLE
                holder.downloadCircleClickAnimation.animate().startDelay = 0
                holder.downloadCircleClickAnimation.playAnimation()
            } else {
                if (holder.downloadCircleClickAnimation.visibility == View.VISIBLE) {
                    holder.downloadCircleClickAnimation.pauseAnimation()
                    holder.downloadCircleClickAnimation.visibility = View.GONE
                }
            }

            holder.downloadCircleProgressBar.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.GONE
            holder.downloadButton.visibility = View.INVISIBLE

            holder.songItemDownloadButtonAnimated.animate().startDelay = 0
            holder.songItemDownloadButtonAnimated.playAnimation()
        } else {
            if (!song.alreadyDownloaded) {
                holder.deleteButton.visibility = View.GONE
                holder.songItemDownloadButtonAnimated.visibility = View.GONE
                holder.downloadButton.visibility = View.VISIBLE
            }
            else {
                holder.downloadButton.visibility = View.GONE

                if (!song.isPlayingAnimation) {
                    holder.deleteButton.visibility = View.VISIBLE
                    holder.songItemDownloadButtonAnimated.visibility = View.GONE
                } else {
                    holder.songItemDownloadButtonAnimated.visibility = View.VISIBLE
                    holder.songItemDownloadButtonAnimated.animate().startDelay = 0
                    holder.songItemDownloadButtonAnimated.playAnimation()
                }
            }

            holder.downloadCircleProgressBar.visibility = View.GONE
            holder.downloadCircleClickAnimation.visibility = View.GONE
        }

        holder.songView.setOnClickListener {
            adapterListener.onSongViewClick(song)
        }
        holder.downloadButton.setOnClickListener {
            it.isEnabled = false
            adapterListener.onDownloadButtonClick(song)
        }
        holder.downloadCircleProgressBar.setOnClickListener {
            it.isEnabled = false
            adapterListener.onDownloadButtonClick(song)
        }
        holder.deleteButton.setOnClickListener {
            it.isEnabled = false
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
        songsList = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun replaceSong(previous: RemoteSong, new: RemoteSong) {
        val song = songsList.find { it.item.key == previous.item.key } ?: return
        songsList[songsList.indexOf(song)] = new

        notifyItemChanged(songsList.indexOf(new))
    }

    fun updateSong(remoteSong: RemoteSong) {
        val song = songsList.find { it.item.key == remoteSong.item.key } ?: return
        song.alreadyDownloaded = remoteSong.alreadyDownloaded

        notifyItemChanged(songsList.indexOf(song))
    }

    fun updateDownloading(remoteSong: RemoteSong, isDownloading: Boolean) {
        val song = songsList.find { it.item.key == remoteSong.item.key } ?: return
        song.downloading = isDownloading
        song.alreadyDownloaded = remoteSong.alreadyDownloaded

        notifyItemChanged(songsList.indexOf(song))
    }

    fun updateDownloadProgress(remoteSong: RemoteSong, progress: Float) {
        val song = songsList.find { it.item.key == remoteSong.item.key } ?: return
        song.downloadProgress = progress

        notifyItemChanged(songsList.indexOf(song), Bundle().apply { putFloat("progress", progress) })
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val songView: ConstraintLayout = itemView.findViewById(R.id.songItemView)
        val imageView: ImageView = itemView.findViewById(R.id.songItemThumb)
        val title: TextView = itemView.findViewById(R.id.songItemTitle)
        val artist: TextView = itemView.findViewById(R.id.songItemArtist)
        val downloadButton: ImageButton = itemView.findViewById(R.id.songItemDownloadButton)
        val songItemDownloadButtonAnimated: LottieAnimationView = itemView.findViewById(R.id.songItemDownloadButtonAnimated)
        val downloadCircleProgressBar: ProgressBar = itemView.findViewById(R.id.downloadCircleProgressBar)
        val downloadCircleClickAnimation: LottieAnimationView = itemView.findViewById(R.id.downloadCircleClickAnimation)
        val deleteButton: ImageButton = itemView.findViewById(R.id.songItemDeleteButton)
    }

}