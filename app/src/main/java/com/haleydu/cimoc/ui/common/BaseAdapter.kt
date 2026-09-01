package com.haleydu.cimoc.ui.common
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.global.FastClick

abstract class BaseAdapter<T>(
    context: Context,
    list: MutableList<T>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected val mContext: Context = context.applicationContext
    protected val mDataSet: MutableList<T> = list
    protected val mInflater: LayoutInflater = LayoutInflater.from(context)

    private var mClickListener: OnItemClickListener? = null
    private var mLongClickListener: OnItemLongClickListener? = null

    fun add(data: T) {
        if (mDataSet.add(data)) {
            notifyItemInserted(mDataSet.size)
        }
    }

    fun add(location: Int, data: T) {
        mDataSet.add(location, data)
        notifyItemInserted(location)
    }

    fun addAll(collection: Collection<T>) {
        addAll(mDataSet.size, collection)
    }

    fun addAll(location: Int, collection: Collection<T>) {
        if (mDataSet.addAll(location, collection)) {
            notifyItemRangeInserted(location, location + collection.size)
        }
    }

    fun exist(data: T): Boolean {
        return mDataSet.indexOf(data) != -1
    }

    fun remove(data: T): Boolean {
        val position = mDataSet.indexOf(data)
        if (position != -1) {
            remove(position)
            return true
        }
        return false
    }

    fun remove(position: Int) {
        mDataSet.removeAt(position)
        notifyItemRemoved(position)
    }

    fun removeAll(collection: Collection<T>) {
        mDataSet.removeAll(collection)
        notifyDataSetChanged()
    }

    fun contains(data: T): Boolean {
        return mDataSet.contains(data)
    }

    fun reverse() {
        mDataSet.reverse()
        notifyDataSetChanged()
    }

    fun clear() {
        mDataSet.clear()
        notifyDataSetChanged()
    }

    val dateSet: List<T>
        get() = mDataSet

    fun setData(collection: Collection<T>) {
        mDataSet.clear()
        mDataSet.addAll(collection)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): T {
        return mDataSet[position]
    }

    override fun getItemCount(): Int {
        return mDataSet.size
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mClickListener = onItemClickListener
    }

    fun setOnItemLongClickListener(onItemLongClickListener: OnItemLongClickListener?) {
        mLongClickListener = onItemLongClickListener
    }

    abstract fun getItemDecoration(): RecyclerView.ItemDecoration

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.setOnClickListener { v ->
            val pos = holder.bindingAdapterPosition
            if (mClickListener != null && isClickValid() && pos != RecyclerView.NO_POSITION) {
                mClickListener!!.onItemClick(v, pos)
            }
        }
        holder.itemView.setOnLongClickListener { v ->
            val listener = mLongClickListener ?: return@setOnLongClickListener false
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) {
                false
            } else {
                listener.onItemLongClick(v, pos)
            }
        }
    }

    protected open fun isClickValid(): Boolean {
        return FastClick.isClickValid()
    }

    fun interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, position: Int): Boolean
    }

    open class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
