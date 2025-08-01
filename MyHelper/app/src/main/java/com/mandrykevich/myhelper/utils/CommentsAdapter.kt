package com.mandrykevich.myhelper.utils

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.Comment

class CommentsAdapter(private var comments: List<Comment>, private val showReportButton: Boolean = true) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvStars: TextView = itemView.findViewById(R.id.tv_stars)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val ivHasHelper: ImageView = itemView.findViewById(R.id.item_iv_has_helper)
        val ivHasElevator: ImageView = itemView.findViewById(R.id.item_iv_has_elevator)
        val ivHasParking: ImageView = itemView.findViewById(R.id.item_iv_has_parking)
        val imReport: ImageView? = itemView.findViewById(R.id.im_report)
        val cvReport: View? = itemView.findViewById(R.id.cv_report)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvName.text = comment.userId // Or comment.nickname if you have it
        holder.tvStars.text = comment.rating.toString()
        holder.tvComment.text = comment.comment

        // Handle colors for hasHelper, hasElevator, hasParking with fallback
        val colorBlue = try { ContextCompat.getColor(holder.itemView.context, R.color.Blue) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light) }
        val colorGreen = try { ContextCompat.getColor(holder.itemView.context, R.color.Green) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light) }
        val colorOrange = try { ContextCompat.getColor(holder.itemView.context, R.color.orange) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_light) }
        val colorGrey1 = try { ContextCompat.getColor(holder.itemView.context, R.color.Grey1) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray) }

        holder.ivHasHelper.setColorFilter(if (comment.hasHelper == true) colorBlue else colorGrey1, PorterDuff.Mode.SRC_IN)
        holder.ivHasElevator.setColorFilter(if (comment.hasElevator == true) colorGreen else colorGrey1, PorterDuff.Mode.SRC_IN)
        holder.ivHasParking.setColorFilter(if (comment.hasDisabledParking == true) colorOrange else colorGrey1, PorterDuff.Mode.SRC_IN)

        if (showReportButton) {
            holder.cvReport?.visibility = View.VISIBLE
            holder.imReport?.setOnClickListener {
                val reportedRef = FirebaseDatabase.getInstance().getReference("Reported")
                val newId = reportedRef.push().key ?: return@setOnClickListener
                reportedRef.child(newId).setValue(comment).addOnSuccessListener {
                    Toast.makeText(holder.itemView.context, "Комментарий отправлен на модерацию", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { error ->
                    Log.e("CommentsAdapter", "Failed to report comment: ${error.message}")
                    Toast.makeText(holder.itemView.context, "Ошибка при отправке жалобы", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.cvReport?.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<Comment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }
}
