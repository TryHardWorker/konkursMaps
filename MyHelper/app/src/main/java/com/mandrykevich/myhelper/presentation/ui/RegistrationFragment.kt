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
import com.mandrykevich.myhelper.databinding.FragmentRegistrationBinding
import com.mandrykevich.myhelper.domain.usecase.RegisterUserUseCase
import com.mandrykevich.myhelper.managers.FragmentSwitcher
import com.mandrykevich.myhelper.utils.Constants.MAIN

class RegistrationFragment : Fragment() {

    private lateinit var binding: FragmentRegistrationBinding
    private lateinit var registerUserUseCase: RegisterUserUseCase
    private lateinit var fragmentSwitcher: FragmentSwitcher

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentSwitcher = FragmentSwitcher(requireActivity() as MainActivity)
        binding = FragmentRegistrationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        registerUserUseCase = RegisterUserUseCase(auth, database)

        binding.btnReg.setOnClickListener {
            val email = binding.etRegEmail.text.trim().toString()
            val password = binding.etRegPassword.text.trim().toString()
            val confirmPassword = binding.etRegRpassword.text.trim().toString()

            when (val result = registerUserUseCase.execute(email, password, confirmPassword)) {
                is RegisterUserUseCase.Result.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                is RegisterUserUseCase.Result.Success -> {
                    result.task.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        MAIN.navController.navigate(R.id.action_registrationFragment_to_mapFragment)
                        fragmentSwitcher.setupBottomNavigation()
                        MAIN.binding.bNav.visibility = View.VISIBLE
                    } else {
                    Toast.makeText(context, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.tvBtnTologinpage.setOnClickListener {
            MAIN.navController.navigate(R.id.action_registrationFragment_to_logInFragment)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RegistrationFragment()
    }
}
