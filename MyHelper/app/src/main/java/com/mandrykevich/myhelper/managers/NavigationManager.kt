package com.mandrykevich.myhelper.managers

import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.presentation.ui.MainActivity
import com.mandrykevich.myhelper.utils.Constants

class NavigationManager(private val activity: MainActivity) {
    fun navigateToInitialFragment() {
        with(activity.binding) {
            bNav.visibility = View.VISIBLE
            if (FirebaseAuth.getInstance().currentUser  != null) {
                Constants.MAIN.navController.navigate(R.id.mapFragment)
            } else {
                Constants.MAIN.navController.navigate(R.id.logInFragment)
            }
        }
    }
}