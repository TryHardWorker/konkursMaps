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

enum class ModerationState {
    MUST_CHECKED,
    REPORTED
}

class ModerationFragment : Fragment() {

    lateinit var binding: FragmentModerationBinding

    private lateinit var adapter: ModerationAdapterComms
    private var currentComment: Comment? = null
    private var currentCommentId: String? = null
    private var currentState: ModerationState = ModerationState.MUST_CHECKED
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

        setupButtons()

        binding.btnSwap.setOnClickListener {
            isShowingReported = !isShowingReported
            currentState = if (isShowingReported) ModerationState.REPORTED else ModerationState.MUST_CHECKED
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

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            if (currentState == ModerationState.MUST_CHECKED) {
                // Логика для MustChecked: публикация
                currentComment?.let { comment ->
                    // Добавляем балл репутации (максимум 5)
                    updateUserReputation(comment.userId, 1)
                    // Перемещаем в Comments
                    moveCommentToComments(comment)
                }
            } else {
                // Логика для Reported: оставить комментарий
                currentComment?.let { comment ->
                    // Просто удаляем из Reported
                    deleteCommentFromReported(comment)
                }
            }
        }

        binding.btnDenied.setOnClickListener {
            if (currentState == ModerationState.MUST_CHECKED) {
                // Логика для MustChecked: запрет публикации
                currentComment?.let { comment ->
                    // Отнимаем балл репутации (минимум -5)
                    updateUserReputation(comment.userId, -1)
                    // Удаляем из MustChecked
                    deleteCommentFromMustChecked(comment)
                }
            } else {
                // Логика для Reported: удалить комментарий
                currentComment?.let { comment ->
                    // Отнимаем балл репутации
                    updateUserReputation(comment.userId, -1)
                    // Удаляем из Reported и Comments
                    deleteCommentFromReported(comment)
                    deleteCommentFromComments(comment)
                }
            }
        }
    }

    private fun updateUserReputation(userId: String, change: Int) {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            val currentReputation = snapshot.child("reputation").getValue(Int::class.java) ?: 0
            val newReputation = (currentReputation + change).coerceIn(-5, 5)
            userRef.child("reputation").setValue(newReputation)
        }
    }

    private fun moveCommentToComments(comment: Comment) {
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments")
        val mustCheckedRef = FirebaseDatabase.getInstance().getReference("MustChecked")
        
        // Добавляем в Comments
        commentsRef.push().setValue(comment)
            .addOnSuccessListener {
                // После успешного добавления удаляем из MustChecked
                currentCommentId?.let { id ->
                    mustCheckedRef.child(id).removeValue()
                        .addOnSuccessListener {
                            if (currentState == ModerationState.MUST_CHECKED) loadFirstComment() else loadFirstReported()
                        }
                }
            }
    }

    private fun deleteCommentFromMustChecked(comment: Comment) {
        FirebaseDatabase.getInstance().getReference("MustChecked")
            .child(currentCommentId!!)
            .removeValue()
            .addOnSuccessListener {
                if (currentState == ModerationState.MUST_CHECKED) loadFirstComment() else loadFirstReported()
            }
    }

    private fun deleteCommentFromReported(comment: Comment) {
        currentCommentId?.let { id ->
            FirebaseDatabase.getInstance().getReference("Reported")
                .child(id)
                .removeValue()
                .addOnSuccessListener {
                    if (currentState == ModerationState.MUST_CHECKED) loadFirstComment() else loadFirstReported()
                }
        }
    }

    private fun deleteCommentFromComments(comment: Comment) {
        comment.id?.let { originalId ->
            FirebaseDatabase.getInstance().getReference("Comments")
                .child(originalId)
                .removeValue()
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