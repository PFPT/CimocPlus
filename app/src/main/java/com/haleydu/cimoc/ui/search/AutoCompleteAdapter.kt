package com.haleydu.cimoc.ui.search
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.haleydu.cimoc.R

class AutoCompleteAdapter(context: Context) : ArrayAdapter<String>(context, R.layout.item_spinner) {

    private val mFilter = CustomFilter()

    override fun getFilter(): Filter {
        return mFilter
    }

    private inner class CustomFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val result = FilterResults()
            val values = ArrayList<String>()
            for (i in 0 until count) {
                getItem(i)?.let { values.add(it) }
            }
            result.values = values
            result.count = values.size
            return result
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }
}
