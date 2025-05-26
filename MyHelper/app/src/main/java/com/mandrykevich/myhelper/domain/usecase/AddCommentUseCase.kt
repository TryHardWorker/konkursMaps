package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.tasks.Task
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

        val userId = auth.currentUser ?.email ?: run {
            callback(Result.Error("Пользователь не аутентифицирован"))
            return
        }

        val commentId = database.getReference("Comments").push().key ?: run {
            callback(Result.Error("Ошибка при создании ID комментария"))
            return
        }

        val comment = Comment(
            buildingId = buildingId,
            userId = userId,
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

    sealed class Result {
        data class Success(val task: Task<Void>) : Result()
        data class Error(val message: String) : Result()
    }
}
