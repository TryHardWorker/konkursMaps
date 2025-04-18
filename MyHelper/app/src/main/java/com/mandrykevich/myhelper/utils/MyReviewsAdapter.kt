package com.mandrykevich.myhelper.utils

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.Comment
import com.mandrykevich.myhelper.data.repository.SearchQuery

class MyReviewsAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvStars: TextView = itemView.findViewById(R.id.tv_stars)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val ivHasHelper: ImageView = itemView.findViewById(R.id.item_iv_has_helper)
        val ivHasElevator: ImageView = itemView.findViewById(R.id.item_iv_has_elevator)
        val ivHasParking: ImageView = itemView.findViewById(R.id.item_iv_has_parking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsAdapter.CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return CommentsAdapter.CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsAdapter.CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvName.text = comment.userId
        holder.tvStars.text = comment.rating.toString()
        holder.tvComment.text = comment.comment

        if (comment.hasHelper) {
            holder.ivHasHelper.visibility = View.VISIBLE
            holder.ivHasHelper.setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.ivHasHelper.visibility = View.GONE
            holder.ivHasHelper.setBackgroundColor(Color.BLACK)
        }

        if (comment.hasElevator) {
            holder.ivHasElevator.visibility = View.VISIBLE
            holder.ivHasElevator.setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.ivHasElevator.visibility = View.GONE
            holder.ivHasElevator.setBackgroundColor(Color.BLACK)
        }

        if (comment.hasDisabledParking) {
            holder.ivHasParking.visibility = View.VISIBLE
            holder.ivHasParking.setBackgroundColor(Color.TRANSPARENT)
        } else {
            holder.ivHasParking.visibility = View.GONE
            holder.ivHasParking.setBackgroundColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}