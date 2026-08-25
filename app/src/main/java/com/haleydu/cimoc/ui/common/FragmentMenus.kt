package com.haleydu.cimoc.ui.common

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

fun Fragment.addMenu(
    menuRes: Int,
    onCreate: (Menu) -> Unit = {},
    onItem: (MenuItem) -> Boolean
) {
    requireActivity().addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(menuRes, menu)
            onCreate(menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = onItem(menuItem)
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
}
