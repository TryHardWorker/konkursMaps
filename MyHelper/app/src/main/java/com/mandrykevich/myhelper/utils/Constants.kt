package com.mandrykevich.myhelper.utils

import com.mandrykevich.myhelper.presentation.ui.MainActivity

object Constants {
    lateinit var MAIN: MainActivity

    fun initialize(mainActivity: MainActivity) {
        MAIN = mainActivity
    }
}