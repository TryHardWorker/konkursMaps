package com.mandrykevich.myhelper.presentation.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentProfileBinding
import com.mandrykevich.myhelper.domain.usecase.SignOutUseCase
import com.mandrykevich.myhelper.presentation.viewModel.SearchViewModel
import com.mandrykevich.myhelper.utils.Constants.MAIN
import com.mandrykevich.myhelper.utils.SearchQueryAdapter


class ProfileFragment : Fragment() {

    lateinit var binding : FragmentProfileBinding
    private lateinit var signOutUseCase: SignOutUseCase
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var adapter: SearchQueryAdapter


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

        searchViewModel = ViewModelProvider(requireActivity()).get(SearchViewModel::class.java)

        searchViewModel.searchQueries.observe(viewLifecycleOwner) { queries ->
            adapter = SearchQueryAdapter(queries)
            binding.rvResult.adapter = adapter
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