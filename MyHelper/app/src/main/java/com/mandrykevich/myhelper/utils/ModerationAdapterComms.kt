package com.mandrykevich.myhelper.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.Comment
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat

class ModerationAdapterComms(
    private var comments: List<Comment>,

) : RecyclerView.Adapter<ModerationAdapterComms.ModerationViewHolder>() {


    class ModerationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvStars: TextView = itemView.findViewById(R.id.tv_stars)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val ivHasHelper: ImageView = itemView.findViewById(R.id.item_iv_has_helper)
        val ivHasElevator: ImageView = itemView.findViewById(R.id.item_iv_has_elevator)
        val ivHasParking: ImageView = itemView.findViewById(R.id.item_iv_has_parking)
        val tvReputation: TextView = itemView.findViewById(R.id.tv_reputation)
        val imReport: ImageView? = itemView.findViewById(R.id.im_report)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModerationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review_moder, parent, false)
        return ModerationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModerationViewHolder, position: Int) {
        val comment = comments[position]
        Log.d("ModerationAdapter", "Binding comment: $comment")
        holder.tvName.text = comment.userId
        holder.tvStars.text = comment.rating.toString()
        holder.tvComment.text = comment.comment

        val colorBlue = try { ContextCompat.getColor(holder.itemView.context, R.color.Blue) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light) }
        val colorGreen = try { ContextCompat.getColor(holder.itemView.context, R.color.Green) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light) }
        val colorOrange = try { ContextCompat.getColor(holder.itemView.context, R.color.orange) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_light) }
        val colorGrey1 = try { ContextCompat.getColor(holder.itemView.context, R.color.Grey1) } catch (e: Exception) { ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray) }

        holder.ivHasHelper.setColorFilter(if (comment.hasHelper == true) colorBlue else colorGrey1, PorterDuff.Mode.SRC_IN)
        holder.ivHasElevator.setColorFilter(if (comment.hasElevator == true) colorGreen else colorGrey1, PorterDuff.Mode.SRC_IN)
        holder.ivHasParking.setColorFilter(if (comment.hasDisabledParking == true) colorOrange else colorGrey1, PorterDuff.Mode.SRC_IN)

        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.orderByChild("nickname").equalTo(comment.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var reputation: Any? = null
                    for (userSnap in snapshot.children) {
                        reputation = userSnap.child("reputation").value
                        break
                    }
                    holder.tvReputation.text = reputation?.toString() ?: "0"
                }
                override fun onCancelled(error: DatabaseError) {
                    holder.tvReputation.text = "?"
                }
            })


    }

    override fun getItemCount(): Int {
        Log.d("ModerationAdapter", "getItemCount: ${comments.size}")
        return comments.size
    }

    fun update(newComments: List<Comment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }
}