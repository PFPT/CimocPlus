package com.haleydu.cimoc.ui.widget
import android.content.Context
import android.util.AttributeSet
import com.facebook.drawee.controller.AbstractDraweeController
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.view.SimpleDraweeView

open class RetryDraweeView : SimpleDraweeView {

    constructor(context: Context, hierarchy: GenericDraweeHierarchy) : super(context, hierarchy)

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun retry(): Boolean {
        val current = getController() as? AbstractDraweeController<*, *>
        return current != null && current.onClick()
    }
}
