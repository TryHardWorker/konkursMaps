package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.ActivityMainBinding
import com.mandrykevich.myhelper.managers.NavigationManager
import com.mandrykevich.myhelper.presentation.viewModel.MainViewModel
import com.mandrykevich.myhelper.utils.Constants
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    private lateinit var navigationManager: NavigationManager
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Constants.initialize(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navController = Navigation.findNavController(this, R.id.fragment_container)
        MapKitFactory.setApiKey("af7170fa-1d7c-41f3-8178-4b80f3dcf435")
        MapKitFactory.initialize(this)

        navigationManager = NavigationManager(this)

        // Скрываем NavHostFragment до проверки


        checkAuthAndRedirect()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mapFragment, R.id.profileFragment, R.id.moderationFragment -> binding.bNav.visibility = View.VISIBLE
                else -> binding.bNav.visibility = View.GONE
            }
        }
    }

    fun setBottomMenu(isModerator: Boolean) {
        val menuRes = if (isModerator) R.menu.bottom_menu_moderator else R.menu.bottom_menu
        binding.bNav.menu.clear()
        binding.bNav.inflateMenu(menuRes)
        NavigationUI.setupWithNavController(binding.bNav, navController)
    }

    fun checkAuthAndRedirect() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(user.uid).child("role")
                .get().addOnSuccessListener { snapshot ->
                    val role = snapshot.getValue(String::class.java)
                    setBottomMenu(role == "moderator")
                    // Переходим на карту, если нужно
                    if (navController.currentDestination?.id == R.id.logInFragment) {
                        navController.navigate(R.id.mapFragment)
                    }
                    // Показываем NavHostFragment
                    binding.bNav.visibility = View.VISIBLE
                }.addOnFailureListener {
                    setBottomMenu(false)
                    if (navController.currentDestination?.id == R.id.logInFragment) {
                        navController.navigate(R.id.mapFragment)
                    }
                    binding.bNav.visibility = View.VISIBLE
                }
        } else {
            setBottomMenu(false)
            if (navController.currentDestination?.id != R.id.logInFragment) {
                navController.navigate(R.id.logInFragment)
            }
            binding.bNav.visibility = View.VISIBLE
        }
    }
}


