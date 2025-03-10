package com.mandrykevich.myhelper.presentation.ui


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.ActivityMainBinding
import com.mandrykevich.myhelper.domain.usecase.AddCommentUseCase
import com.mandrykevich.myhelper.managers.FragmentSwitcher
import com.mandrykevich.myhelper.managers.NavigationManager
import com.mandrykevich.myhelper.utils.Constants
import com.mandrykevich.myhelper.utils.Constants.MAIN
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    private lateinit var navigationManager: NavigationManager
    private lateinit var fragmentSwitcher: FragmentSwitcher


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeComponents()
        MAIN = this

        if(FirebaseAuth.getInstance().currentUser != null){
            navigationManager.navigateToInitialFragment()
            fragmentSwitcher.setupBottomNavigation()
        } else {navController.navigate(R.id.logInFragment)}

    }

    private fun initializeComponents() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navController = Navigation.findNavController(this, R.id.fragment_container)
        MapKitFactory.setApiKey("af7170fa-1d7c-41f3-8178-4b80f3dcf435")
        MapKitFactory.initialize(this)
        val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        navigationManager = NavigationManager(this)
        fragmentSwitcher = FragmentSwitcher(this)
    }


}