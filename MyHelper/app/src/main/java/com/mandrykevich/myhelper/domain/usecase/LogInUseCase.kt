package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

class LogInUseCase(private val auth: FirebaseAuth, private val database: FirebaseDatabase) {

    fun execute(email: String, password: String, callback: (Result) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            callback(Result.Error("Не все поля заполнены"))
            return
        }

        if (!isValidEmail(email)) {
            callback(Result.Error("Некорректный формат email"))
            return
        }

        val task = auth.signInWithEmailAndPassword(email, password)
        task.addOnCompleteListener { loginTask ->
            if (loginTask.isSuccessful) {
                // Получаем текущего пользователя
                val userId = auth.currentUser ?.uid
                if (userId != null) {
                    // Проверяем роль пользователя в базе данных
                    database.getReference("Users").child(userId).get()
                        .addOnSuccessListener { dataSnapshot ->
                            val role = dataSnapshot.child("role").getValue(String::class.java)
                            if (role == "moderator") {
                                // Возвращаем сообщение о входе как модератор
                                callback(Result.Success(loginTask, "Вы вошли как модератор"))
                            } else {
                                // Возвращаем стандартный успех
                                callback(Result.Success(loginTask))
                            }
                        }
                        .addOnFailureListener {
                            // Ошибка при получении данных пользователя
                            callback(Result.Error("Ошибка при получении данных пользователя"))
                        }
                }
            } else {
                // Ошибка при входе
                callback(Result.Error("Неверный email или пароль"))
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    sealed class Result {
        data class Success(val task: Task<AuthResult>, val message: String? = null) : Result()
        data class Error(val message: String) : Result()
    }
}
