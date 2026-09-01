package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.haleydu.cimoc.R

open class Option : FrameLayout {

    protected lateinit var mTitleView: TextView
    protected lateinit var mSummaryView: TextView

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.custom_option, this)
        initText(context, attrs)
    }

    private fun initText(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Option)
        val title = typedArray.getString(R.styleable.Option_title)
        val summary = typedArray.getString(R.styleable.Option_summary)
        mTitleView = findViewById(R.id.custom_option_title)
        mSummaryView = findViewById(R.id.custom_option_summary)
        mTitleView.text = title
        mSummaryView.text = summary
        typedArray.recycle()
    }

    fun setSummary(text: CharSequence?) {
        mSummaryView.text = text
    }
}
