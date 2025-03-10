package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class RegisterUserUseCase(private val auth: FirebaseAuth, private val database: FirebaseDatabase) {

    fun execute(email: String, password: String, confirmPassword: String): Result {

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            return Result.Error("Не все поля заполнены")
        }
        if (password != confirmPassword) {
            return Result.Error("Введенные пароли не совпадают")
        }
        if (password.length < 8) {
            return Result.Error("Пароль должен быть хотя бы из 8 символов")
        }

        val task = auth.createUserWithEmailAndPassword(email, password)
        task.addOnCompleteListener { registrationTask ->
            if (registrationTask.isSuccessful) {
                val userInfo: MutableMap<String, String> = HashMap()
                userInfo["email"] = email
                userInfo["username"] = email
                auth.currentUser ?.let { user ->
                    database.getReference("Users").child(user.uid).setValue(userInfo)
                }
            }
        }
        return Result.Success(task)
    }

    sealed class Result {
        data class Success(val task: Task<AuthResult>) : Result()
        data class Error(val message: String) : Result()
    }
}
