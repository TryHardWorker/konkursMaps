package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mandrykevich.myhelper.R
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mandrykevich.myhelper.data.repository.Comment
import com.mandrykevich.myhelper.databinding.FragmentModerationBinding
import com.mandrykevich.myhelper.utils.ModerationAdapterComms

class ModerationFragment : Fragment() {

    lateinit var binding: FragmentModerationBinding

    private lateinit var adapter: ModerationAdapterComms
    private var currentComment: Comment? = null
    private var currentCommentId: String? = null
    private var isShowingReported = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentModerationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ModerationAdapterComms(listOf())
        val rv = binding.rvModeration
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadFirstComment()

        binding.btnAccept.setOnClickListener {
            currentComment?.let { comment ->
                currentCommentId?.let { id ->
                    publishComment(id, comment)
                }
            }
        }
        binding.btnDenied.setOnClickListener {
            if (isShowingReported) {
                currentCommentId?.let { id ->
                    currentComment?.let { comment ->
                        deleteReported(id, comment)
                    }
                }
            } else {
                currentCommentId?.let { id -> deleteComment(id) }
            }
        }
        binding.btnSwap.setOnClickListener {
            isShowingReported = !isShowingReported
            if (isShowingReported) {
                loadFirstReported()
                binding.btnSwap.text = "Показать комментарии на публикацию"
                binding.btnAccept.text = "Оставить комментарий"
                binding.btnDenied.text = "Удалить комментарий"
            } else {
                loadFirstComment()
                binding.btnSwap.text = "Модерация жалоб"
                binding.btnAccept.text = "Опубликовать"
                binding.btnDenied.text = "Запретить публикацию"
            }
        }
    }

    private fun loadFirstComment() {
        val ref = FirebaseDatabase.getInstance().getReference("MustChecked")
        ref.limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val first = snapshot.children.first()
                    val comment = first.getValue(Comment::class.java)
                    currentComment = comment
                    currentCommentId = first.key
                    adapter.update(listOf(comment!!))
                    setModerationButtonsEnabled(true)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    adapter.update(listOf())
                    setModerationButtonsEnabled(false)
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadFirstReported() {
        val ref = FirebaseDatabase.getInstance().getReference("Reported")
        ref.limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val first = snapshot.children.first()
                    val comment = first.getValue(Comment::class.java)
                    currentComment = comment
                    currentCommentId = first.key
                    adapter.update(listOf(comment!!))
                    setModerationButtonsEnabled(true)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    adapter.update(listOf())
                    setModerationButtonsEnabled(false)
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun changeReputation(nickname: String, delta: Int) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.orderByChild("nickname").equalTo(nickname)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnap in snapshot.children) {
                        val reputation = userSnap.child("reputation").getValue(Int::class.java) ?: 0
                        val newReputation = (reputation + delta).coerceIn(-5, 5)
                        userSnap.ref.child("reputation").setValue(newReputation)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun publishComment(id: String, comment: Comment) {
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments")
        commentsRef.child(id).setValue(comment).addOnSuccessListener {
            changeReputation(comment.userId, 1)
            deleteComment(id)
        }
    }

    private fun deleteComment(id: String) {
        currentComment?.let { comment ->
            changeReputation(comment.userId, -1)
        }
        val ref = FirebaseDatabase.getInstance().getReference("MustChecked")
        ref.child(id).removeValue().addOnSuccessListener {
            loadFirstComment()
        }
    }

    private fun deleteReported(id: String, comment: Comment) {
        val reportedRef = FirebaseDatabase.getInstance().getReference("Reported")
        reportedRef.child(id).removeValue().addOnSuccessListener {
            val commentsRef = FirebaseDatabase.getInstance().getReference("Comments")
            commentsRef.child(id).removeValue()
            loadFirstReported()
        }
    }

    private fun setModerationButtonsEnabled(enabled: Boolean) {
        binding.btnAccept.isEnabled = enabled
        binding.btnDenied.isEnabled = enabled
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) = ModerationFragment()
    }
}