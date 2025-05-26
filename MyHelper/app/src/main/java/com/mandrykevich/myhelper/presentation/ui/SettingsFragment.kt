package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentSettingsBinding
import com.mandrykevich.myhelper.utils.Constants.MAIN
import androidx.navigation.fragment.findNavController

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var auth: FirebaseAuth

    // Чтобы знать, какое поле сейчас редактируется
    private var currentEditField: String? = null  // "firstName", "lastName", "nickname"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.imBack.setOnClickListener {
            findNavController().popBackStack()
        }

        fetchUserData()

        // Обработчики кнопок редактирования
        binding.icEditName.setOnClickListener {
            showEditCard("firstName", "Изменение имени", binding.tvUserNametag.text.toString())
        }

        binding.icEditSername.setOnClickListener {
            showEditCard("lastName", "Изменение фамилии", binding.tvUserSaremanetag.text.toString())
        }

        binding.icEditNick.setOnClickListener {
            showEditCard("nickname", "Изменение псевдонима", binding.tvUserNickname.text.toString())
        }

        // Кнопка сохранить
        binding.btnEdit.setOnClickListener {
            val newValue = binding.editEdit.text.toString().trim()
            if (newValue.isEmpty()) {
                // Можно показать ошибку
                binding.editEdit.error = "Поле не может быть пустым"
                return@setOnClickListener
            }
            saveFieldToFirebase(newValue)
        }
    }

    private fun showEditCard(field: String, title: String, currentValue: String) {
        currentEditField = field
        binding.tvEdit.text = title
        binding.editEdit.setText(if (currentValue == "Имя отсутствует" || currentValue == "Фамилия отсутствует" || currentValue == "Псевдоним отсутствует" || currentValue == "Email отсутствует") "" else currentValue)
        binding.cardEdit.visibility = View.VISIBLE
    }

    private fun saveFieldToFirebase(value: String) {
        val userId = auth.currentUser!!.uid
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        val field = currentEditField ?: return

        val updates = mapOf<String, Any>(field to value)

        databaseRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Обновляем UI
                when (field) {
                    "firstName" -> binding.tvUserNametag.text = value
                    "lastName" -> binding.tvUserSaremanetag.text = value
                    "nickname" -> binding.tvUserNickname.text = value
                }
                binding.cardEdit.visibility = View.GONE
            } else {
                Log.e("SettingsFragment", "Ошибка обновления данных: ${task.exception?.message}")
                // Можно показать сообщение об ошибке пользователю
            }
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser!!.uid
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val userSurname = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val userEmail = snapshot.child("email").getValue(String::class.java) ?: ""
                    val userNickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                    val userReputation = snapshot.child("reputation").getValue(Long::class.java) ?: ""

                    binding.tvReputation.text = "Текущая репутация: " + userReputation.toString()
                    binding.tvUserNametag.text = if (userName.isNotEmpty()) userName else "Имя отсутствует"
                    binding.tvUserSaremanetag.text = if (userSurname.isNotEmpty()) userSurname else "Фамилия отсутствует"
                    binding.tvUserEmail.text = if (userEmail.isNotEmpty()) userEmail else "Email отсутствует"
                    binding.tvUserNickname.text = if (userNickname.isNotEmpty()) userNickname else "Псевдоним отсутствует"
                } else {
                    binding.tvReputation.text = "Текущая репутация: 0"
                    binding.tvUserNametag.text = "Имя отсутствует"
                    binding.tvUserSaremanetag.text = "Фамилия отсутствует"
                    binding.tvUserEmail.text = "Email отсутствует"
                    binding.tvUserNickname.text = "Псевдоним отсутствует"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SettingsFragment", "Ошибка получения данных: ${error.message}")
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}

