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
import com.haleydu.cimoc.ui.common.dialog.SliderDialogFragment
import com.haleydu.cimoc.ui.widget.Option
import dagger.hilt.android.EntryPointAccessors

class SliderPreference : Option, View.OnClickListener {

    private val mPreferenceManager: PreferenceManager
    private var mFragmentManager: FragmentManager? = null
    private var mTargetFragment: Fragment? = null
    private var mPreferenceKey: String? = null
    private var mMin = 0
    private var mMax = 0
    private var mTitle = 0
    private var mValue = 0
    private var mRequestCode = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.custom_option, this)
        mPreferenceManager = EntryPointAccessors.fromApplication(
            context.applicationContext, AppEntryPoint::class.java
        ).preferenceManager()
        initRange(context, attrs)
        setOnClickListener(this)
    }

    private fun initRange(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference)
        mMin = typedArray.getInt(R.styleable.Preference_min, -1)
        mMax = typedArray.getInt(R.styleable.Preference_max, -1)
        typedArray.recycle()
    }

    override fun onClick(v: View) {
        val manager = mFragmentManager
        if (manager != null) {
            val fragment = SliderDialogFragment.newInstance(mTitle, mMin, mMax, mValue, mRequestCode)
            val target = mTargetFragment
            if (target != null) {
                fragment.show(target.childFragmentManager, null)
            } else {
                fragment.show(manager, null)
            }
        }
    }

    fun bindPreference(manager: FragmentManager, key: String, def: Int, title: Int, request: Int) {
        bindPreference(manager, null, key, def, title, request)
    }

    fun bindPreference(manager: FragmentManager, fragment: BaseFragment?, key: String, def: Int, title: Int, request: Int) {
        mFragmentManager = manager
        mTargetFragment = fragment
        mPreferenceKey = key
        mValue = mPreferenceManager.getInt(key, def)
        mTitle = title
        mRequestCode = request
        mSummaryView.text = mValue.toString()
    }

    var value: Int
        get() = mValue
        set(value) {
            val key = mPreferenceKey ?: return
            mPreferenceManager.putInt(key, value)
            mValue = value
            mSummaryView.text = mValue.toString()
        }
}
