package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentLogInBinding
import com.mandrykevich.myhelper.domain.usecase.LogInUseCase
import com.mandrykevich.myhelper.managers.FragmentSwitcher
import com.mandrykevich.myhelper.utils.Constants.MAIN

class LogInFragment : Fragment() {

    private lateinit var binding: FragmentLogInBinding
    private lateinit var logInUseCase: LogInUseCase
    private lateinit var fragmentSwitcher: FragmentSwitcher

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentSwitcher = FragmentSwitcher(requireActivity() as MainActivity)
        binding = FragmentLogInBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        logInUseCase = LogInUseCase(auth)

        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.trim().toString()
            val password = binding.etLoginPassword.text.trim().toString()

            when (val result = logInUseCase.execute(email, password)) {
                is LogInUseCase.Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                is LogInUseCase.Result.Success -> {
                    result.task.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Вход успешен", Toast.LENGTH_SHORT).show()
                            MAIN.navController.navigate(R.id.action_logInFragment_to_mapFragment)
                            fragmentSwitcher.setupBottomNavigation()
                            MAIN.binding.bNav.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(context, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
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
