package com.mandrykevich.myhelper.presentation.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentProfileBinding
import com.mandrykevich.myhelper.domain.usecase.SignOutUseCase
import com.mandrykevich.myhelper.utils.Constants.MAIN


class ProfileFragment : Fragment() {

    lateinit var binding : FragmentProfileBinding
    private lateinit var signOutUseCase: SignOutUseCase


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        signOutUseCase = SignOutUseCase(auth)

        binding.cardExit.setOnClickListener {
            confirmSignOut()
        }


    }

    private fun confirmSignOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { dialog, which ->
                signOutUseCase.execute()
                MAIN.binding.bNav.visibility = View.GONE
                MAIN.navController.navigate(R.id.logInFragment)
            }
            .setNegativeButton("Нет") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {

        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}