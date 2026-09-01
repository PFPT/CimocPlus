package com.haleydu.cimoc.ui.common.dialog
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.DialogFragment
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller

class MultiAdpaterDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var mCheckArray: BooleanArray? = null
    private var adapter: SimpleAdapter? = null
    private val arrayList = ArrayList<Map<String, Any>>()
    private var getlistview: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        var item = requireArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS)
        if (item == null) {
            item = arrayOf()
        }
        initCheckArray(item.size)

        for (i in item.indices) {
            val hashMap = HashMap<String, Any>()
            hashMap["text"] = item[i]
            arrayList.add(hashMap)
        }

        val inflater = LayoutInflater.from(requireActivity())
        getlistview = inflater.inflate(R.layout.listview_adapter, null)
        val listview = getlistview!!.findViewById<ListView>(R.id.listview_adapter)

        adapter = SetSimpleAdapter(
            requireActivity(),
            arrayList,
            R.layout.item_select_mutil,
            arrayOf("text"),
            intArrayOf(R.id.item_select_title_mutil)
        )
        listview.adapter = adapter
        listview.cacheColorHint = Color.TRANSPARENT
        listview.divider = null
        listview.selector = ColorDrawable()
        listview.itemsCanFocus = true
        listview.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listview.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            val cBox = view.findViewById<CheckBox>(R.id.item_select_checkbox_mutil)
            if (cBox.isChecked) {
                cBox.isChecked = false
                mCheckArray!![position] = false
            } else {
                cBox.isChecked = true
                mCheckArray!![position] = true
            }
            adapter?.notifyDataSetChanged()
        }

        builder.setTitle(requireArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
            .setView(getlistview)
            .setPositiveButton(R.string.dialog_positive, this)
            .setNeutralButton(R.string.comic_inverse_selection, this)
        return builder.create()
    }

    private fun initCheckArray(length: Int) {
        mCheckArray = requireArguments().getBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS)
        if (mCheckArray == null) {
            mCheckArray = BooleanArray(length)
            for (i in 0 until length) {
                mCheckArray!![i] = false
            }
        }
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        when (which) {
            Dialog.BUTTON_POSITIVE -> {
                val requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE)
                val bundle = Bundle()
                bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, mCheckArray)
                val target = DialogCaller.from(this)
                target?.onDialogResult(requestCode, bundle)
                isCloseDialog(dialogInterface, true)
            }
            Dialog.BUTTON_NEUTRAL -> {
                val checks = mCheckArray!!
                for (i in checks.indices) {
                    checks[i] = !checks[i]
                }
                adapter?.notifyDataSetChanged()
                isCloseDialog(dialogInterface, false)
            }
            else -> isCloseDialog(dialogInterface, true)
        }
    }

    private fun isCloseDialog(dialog: DialogInterface, close: Boolean) {
        try {
            val field = dialog.javaClass.superclass!!.getDeclaredField("mShowing")
            field.isAccessible = true
            field.set(dialog, close)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class SetSimpleAdapter(
        context: Context,
        data: List<Map<String, *>>,
        resource: Int,
        from: Array<String>,
        to: IntArray
    ) : SimpleAdapter(context, data, resource, from, to) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            if (view == null) {
                view = LinearLayout.inflate(activity, R.layout.item_select_mutil, null)
            }
            val ckBox = view!!.findViewById<CheckBox>(R.id.item_select_checkbox_mutil)
            ckBox.isChecked = mCheckArray!![position]
            return super.getView(position, view, parent)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(title: Int, item: Array<String>?, check: BooleanArray?, requestCode: Int): MultiAdpaterDialogFragment {
            val fragment = MultiAdpaterDialogFragment()
            val bundle = Bundle()
            bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title)
            bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item)
            bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS, check)
            bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode)
            fragment.arguments = bundle
            return fragment
        }
    }
}
