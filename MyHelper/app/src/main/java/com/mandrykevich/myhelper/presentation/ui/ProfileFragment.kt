package com.mandrykevich.myhelper.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.Comment
import com.mandrykevich.myhelper.databinding.FragmentProfileBinding
import com.mandrykevich.myhelper.domain.usecase.SignOutUseCase
import com.mandrykevich.myhelper.presentation.viewModel.SearchViewModel
import com.mandrykevich.myhelper.utils.CommentsAdapter
import com.mandrykevich.myhelper.utils.Constants.MAIN

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var signOutUseCase: SignOutUseCase
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var adapter: CommentsAdapter
    private val userItemsList = mutableListOf<Comment>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        searchViewModel = ViewModelProvider(requireActivity()).get(SearchViewModel::class.java)
        binding = FragmentProfileBinding.inflate(inflater)
        setupRecyclerView()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvResult.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentsAdapter(userItemsList, showReportButton = false, )
        binding.rvResult.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnToSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        val auth = FirebaseAuth.getInstance()
        signOutUseCase = SignOutUseCase(auth)

        binding.cardExit.setOnClickListener {
            confirmSignOut()
        }

        // Получаем пользовательские комментарии
        fetchUserItems(auth.currentUser?.email)
    }

    private fun fetchUserItems(userId: String?) {
        if (userId == null) return

        val databaseRef = FirebaseDatabase.getInstance().getReference("Comments")
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userItemsList.clear()
                binding.tvEmpty.visibility = View.GONE
                snapshot.children.mapNotNull { it.getValue(Comment::class.java) }
                    .filter { it.userId == userId }
                    .forEach { comment ->
                        userItemsList.add(comment)
                    }
                binding.tvEmpty.visibility = if (userItemsList.isEmpty()) View.VISIBLE else View.GONE
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProfileFragment", "Ошибка получения данных: ${error.message}")
            }
        })
    }

    private fun confirmSignOut() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { dialog, which ->
                signOutUseCase.execute()

                findNavController().navigate(R.id.logInFragment)
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
