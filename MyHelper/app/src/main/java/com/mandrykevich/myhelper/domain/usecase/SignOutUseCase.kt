package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.SharedPreferences

class SignOutUseCase(private val auth: FirebaseAuth, private val context: Context) {

    private val PREFS_NAME = "RecentQueriesPrefs"

    fun execute() {
        // Очищаем SharedPreferences с последними запросами
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Выполняем выход из Firebase Auth
        auth.signOut()
    }
}