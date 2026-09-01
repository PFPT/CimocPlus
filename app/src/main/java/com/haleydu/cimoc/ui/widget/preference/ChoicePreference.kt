package com.haleydu.cimoc.ui.widget.preference
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.haleydu.cimoc.R
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.di.AppEntryPoint
import com.haleydu.cimoc.ui.common.BaseFragment
import com.haleydu.cimoc.ui.common.dialog.ChoiceDialogFragment
import com.haleydu.cimoc.ui.widget.Option
import dagger.hilt.android.EntryPointAccessors

class ChoicePreference : Option, View.OnClickListener {

    private val mPreferenceManager: PreferenceManager
    private var mFragmentManager: FragmentManager? = null
    private var mTargetFragment: Fragment? = null
    private var mPreferenceKey: String? = null
    private var mItems: Array<String>? = null
    private var mChoice = 0
    private var mRequestCode = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.custom_option, this)
        mPreferenceManager = EntryPointAccessors.fromApplication(
            context.applicationContext, AppEntryPoint::class.java
        ).preferenceManager()
        setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val manager = mFragmentManager
        if (manager != null) {
            val fragment = ChoiceDialogFragment.newInstance(R.string.dialog_choice, mItems, mChoice, mRequestCode)
            val target = mTargetFragment
            if (target != null) {
                fragment.show(target.childFragmentManager, null)
            } else {
                fragment.show(manager, null)
            }
        }
    }

    fun bindPreference(manager: FragmentManager, key: String, def: Int, item: Int, request: Int) {
        bindPreference(manager, null, key, def, item, request)
    }

    fun bindPreference(manager: FragmentManager, fragment: BaseFragment?, key: String, def: Int, item: Int, request: Int) {
        mFragmentManager = manager
        mTargetFragment = fragment
        mPreferenceKey = key
        mChoice = mPreferenceManager.getInt(key, def)
        mItems = resources.getStringArray(item)
        mRequestCode = request
        mSummaryView.text = mItems!![mChoice]
    }

    fun getValue(): Int {
        return mChoice
    }

    fun setValue(choice: Int) {
        mPreferenceManager.putInt(mPreferenceKey, choice)
        mChoice = choice
        mSummaryView.text = mItems!![mChoice]
    }
}
