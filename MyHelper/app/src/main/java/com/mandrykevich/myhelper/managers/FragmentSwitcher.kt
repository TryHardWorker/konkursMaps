package com.mandrykevich.myhelper.managers

import android.view.View
import androidx.fragment.app.Fragment
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.presentation.ui.LogInFragment
import com.mandrykevich.myhelper.presentation.ui.MainActivity
import com.mandrykevich.myhelper.presentation.ui.MapFragment
import com.mandrykevich.myhelper.presentation.ui.ProfileFragment
import com.mandrykevich.myhelper.presentation.ui.RegistrationFragment
import com.mandrykevich.myhelper.presentation.ui.SearchFragment

class FragmentSwitcher(private val activity: MainActivity) {
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private var activeFragment: Fragment? = null

    fun setupBottomNavigation() {
        activity.binding.bNav.setOnItemSelectedListener { item ->
            val fragment = fragmentMap[item.itemId] ?: createFragment(item.itemId)
            fragment?.let {
                if (activeFragment != it) {
                    switchFragment(it)
                }
            }
            true
        }
    }

    private fun createFragment(itemId: Int): Fragment? {
        return when (itemId) {
            R.id.item_map -> fragmentMap.getOrPut(itemId) { MapFragment() }

            R.id.item_profile -> fragmentMap.getOrPut(itemId) { ProfileFragment() }
            else -> null
        }
    }

    private fun switchFragment(fragment: Fragment) {
        activeFragment?.let { activity.supportFragmentManager.beginTransaction().hide(it).commit() }
        if (!fragment.isAdded) {
            activity.supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment).commit()
        } else {
            activity.supportFragmentManager.beginTransaction().show(fragment).commit()
        }
        activeFragment = fragment
        activity.binding.bNav.visibility = if (fragment is RegistrationFragment || fragment is LogInFragment) View.GONE else View.VISIBLE
    }
}