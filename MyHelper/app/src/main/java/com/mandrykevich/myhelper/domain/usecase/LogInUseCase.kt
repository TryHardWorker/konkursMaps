package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class LogInUseCase(private val auth: FirebaseAuth) {

    fun execute(email: String, password: String): Result {

        if (email.isEmpty() || password.isEmpty()) {
            return Result.Error("Не все поля заполнены")
        }

        if (!isValidEmail(email)) {
            return Result.Error("Некорректный формат email")
        }

        val task = auth.signInWithEmailAndPassword(email, password)
        return Result.Success(task)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    sealed class Result {
        data class Success(val task: Task<AuthResult>) : Result()
        data class Error(val message: String) : Result()
    }
}