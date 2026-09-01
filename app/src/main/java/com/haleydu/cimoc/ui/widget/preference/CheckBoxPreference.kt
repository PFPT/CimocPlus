package com.haleydu.cimoc.ui.widget.preference
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.haleydu.cimoc.R
import com.haleydu.cimoc.data.PreferenceManager
import com.haleydu.cimoc.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors

class CheckBoxPreference : FrameLayout, View.OnClickListener {

    private lateinit var mCheckBox: AppCompatCheckBox
    private val mPreferenceManager: PreferenceManager
    private var mPreferenceKey: String? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.custom_option_checkbox, this)
        mPreferenceManager = EntryPointAccessors.fromApplication(
            context.applicationContext, AppEntryPoint::class.java
        ).preferenceManager()
        init(context, attrs)
        setOnClickListener(this)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Option)
        val title = typedArray.getString(R.styleable.Option_title)
        val summary = typedArray.getString(R.styleable.Option_summary)
        mCheckBox = findViewById(R.id.custom_option_checkbox)
        val titleView = findViewById<TextView>(R.id.custom_option_title)
        val summaryView = findViewById<TextView>(R.id.custom_option_summary)
        titleView.text = title
        summaryView.text = summary
        typedArray.recycle()
    }

    override fun onClick(v: View) {
        val key = mPreferenceKey
        if (key != null) {
            val checked = !mCheckBox.isChecked
            mCheckBox.isChecked = checked
            mPreferenceManager.putBoolean(key, checked)
        }
    }

    fun bindPreference(key: String, def: Boolean) {
        mPreferenceKey = key
        mCheckBox.isChecked = mPreferenceManager.getBoolean(key, def)
    }

    fun setColorStateList(stateList: ColorStateList?) {
        mCheckBox.supportButtonTintList = stateList
    }
}
