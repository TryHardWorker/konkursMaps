package com.mandrykevich.myhelper.domain.usecase

import com.google.firebase.auth.FirebaseAuth

class SignOutUseCase(private val auth: FirebaseAuth) {

    fun execute() {
        auth.signOut()
    }
}