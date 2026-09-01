package com.haleydu.cimoc.ui.common
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.FragmentRecyclerViewBinding

abstract class RecyclerViewFragment : BaseFragment(), BaseAdapter.OnItemClickListener,
    BaseAdapter.OnItemLongClickListener {

    protected lateinit var mRecyclerView: RecyclerView

    override fun initView() {
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.itemAnimator = null
        mRecyclerView.layoutManager = initLayoutManager()
        val adapter = initAdapter()
        if (adapter is BaseAdapter<*>) {
            adapter.setOnItemClickListener(this)
            adapter.setOnItemLongClickListener(this)
            mRecyclerView.addItemDecoration(adapter.getItemDecoration())
        } else if (adapter is GridAdapter) {
            adapter.setOnItemClickListener(this)
            adapter.setOnItemLongClickListener(this)
            mRecyclerView.addItemDecoration(adapter.getItemDecoration())
        }
        if (adapter != null) {
            mRecyclerView.adapter = adapter
        }
    }

    protected abstract fun initAdapter(): RecyclerView.Adapter<*>?

    protected abstract fun initLayoutManager(): RecyclerView.LayoutManager

    override fun onItemClick(view: View, position: Int) {
    }

    override fun onItemLongClick(view: View, position: Int): Boolean {
        return false
    }

    override fun getLayoutRes(): Int {
        return R.layout.fragment_recycler_view
    }

    override fun bindViews(view: View) {
        super.bindViews(view)
        if (getLayoutRes() == R.layout.fragment_recycler_view) {
            val binding = FragmentRecyclerViewBinding.bind(view)
            mRecyclerView = binding.recyclerViewContent
        } else {
            mRecyclerView = view.findViewById(R.id.recycler_view_content)
        }
    }
}
