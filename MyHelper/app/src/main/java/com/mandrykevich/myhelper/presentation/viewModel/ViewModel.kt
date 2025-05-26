package com.mandrykevich.myhelper.presentation.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainViewModel : ViewModel() {

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> get() = _userRole

    init {
        checkUserRole()
    }

    private fun checkUserRole() {
        FirebaseAuth.getInstance().currentUser ?.let { user ->
            // Здесь вы должны получить роль пользователя из Firebase или другого источника
            getUserRoleFromDatabase(user.uid)
        } ?: run {
            _userRole.value = "guest" // Если пользователь не авторизован
        }
    }

    private fun getUserRoleFromDatabase(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users/$userId/role")
            .get()
            .addOnSuccessListener { snapshot ->
                _userRole.value = snapshot.value as? String ?: "user" // По умолчанию - обычный пользователь
            }
            .addOnFailureListener {
                _userRole.value = "user" // Если произошла ошибка, назначаем обычного пользователя
            }
    }
}
