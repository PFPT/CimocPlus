package com.haleydu.cimoc.ui.explore
import android.content.Context
import android.util.Pair
import android.widget.ArrayAdapter
import com.haleydu.cimoc.R
import com.haleydu.cimoc.utils.CollectionUtils

class CategoryAdapter(context: Context, list: List<Pair<String, String>>) :
    ArrayAdapter<String>(context, R.layout.item_spinner, CollectionUtils.map(list) { it.first }) {

    private val mCategoryList: List<Pair<String, String>> = list

    fun getValue(position: Int): String {
        return mCategoryList[position].second
    }
}
