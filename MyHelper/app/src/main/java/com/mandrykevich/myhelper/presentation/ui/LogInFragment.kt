package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentLogInBinding
import com.mandrykevich.myhelper.domain.usecase.LogInUseCase
import com.mandrykevich.myhelper.utils.Constants.MAIN

class LogInFragment : Fragment() {

    private lateinit var binding: FragmentLogInBinding
    private lateinit var logInUseCase: LogInUseCase


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLogInBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Скрываем layout до проверки
        binding.root.visibility = View.INVISIBLE

        // Проверяем авторизацию
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Уже авторизован — не показываем логин, сразу переходим на карту
            (requireActivity() as? MainActivity)?.checkAuthAndRedirect()
        } else {
            // Не авторизован — показываем логин
            binding.root.visibility = View.VISIBLE
        }

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance() // Инициализация FirebaseDatabase
        logInUseCase = LogInUseCase(auth, database) // Передаем database в конструктор

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.trim().toString()
            val password = binding.etLoginPassword.text.trim().toString()

            logInUseCase.execute(email, password) { result ->
                when (result) {
                    is LogInUseCase.Result.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                    is LogInUseCase.Result.Success -> {
                        Toast.makeText(context, "Вход успешен", Toast.LENGTH_SHORT).show()
                        // Проверяем, есть ли сообщение о модераторе
                        result.message?.let { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        (requireActivity() as? MainActivity)?.checkAuthAndRedirect()
                    }
                }
            }
        }

        binding.tvBtnToregpage.setOnClickListener {
            MAIN.navController.navigate(R.id.action_logInFragment_to_registrationFragment)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LogInFragment()
    }
}
