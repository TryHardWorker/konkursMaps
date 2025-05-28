package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mandrykevich.myhelper.data.repository.Comment

class AddCommentUseCase(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {

    fun execute(
        buildingId: String,
        commentText: String,
        rating: Int,
        hasDisabledParking: Boolean,
        hasElevator: Boolean,
        hasHelper: Boolean,
        callback: (Result) -> Unit
    ) {
        if (buildingId.isEmpty()) {
            callback(Result.Error("ID здания не может быть пустым"))
            return
        }
        if (commentText.isEmpty()) {
            callback(Result.Error("Комментарий не может быть пустым"))
            return
        }
        if (rating < 0 || rating > 5) {
            callback(Result.Error("Оценка должна быть от 0 до 5"))
            return
        }

        val userUid = auth.currentUser?.uid ?: run {
            callback(Result.Error("Пользователь не аутентифицирован"))
            return
        }

        val userRef = database.getReference("Users").child(userUid)
        userRef.child("nickname").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nickname = snapshot.getValue(String::class.java) ?: "Аноним"
                val commentId = database.getReference("Comments").push().key ?: run {
                    callback(Result.Error("Ошибка при создании ID комментария"))
                    return
                }
                val comment = Comment(
                    buildingId = buildingId,
                    userId = nickname,
                    rating = rating,
                    comment = commentText,
                    hasDisabledParking = hasDisabledParking,
                    hasElevator = hasElevator,
                    hasHelper = hasHelper
                )
                database.getReference("MustChecked").child(commentId).setValue(comment)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(Result.Success(task))
                        } else {
                            callback(Result.Error("Ошибка при добавлении комментария: ${task.exception?.message}"))
                        }
                    }
            }
            override fun onCancelled(error: DatabaseError) {
                callback(Result.Error("Ошибка получения никнейма"))
            }
        })
    }

    sealed class Result {
        data class Success(val task: Task<Void>) : Result()
        data class Error(val message: String) : Result()
    }
}
