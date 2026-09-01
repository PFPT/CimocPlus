package com.haleydu.cimoc.ui.widget

interface OnScaleDragGestureListener {
    fun onDrag(dx: Float, dy: Float)

    fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float)

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)

    fun onScaleEnd()
}
