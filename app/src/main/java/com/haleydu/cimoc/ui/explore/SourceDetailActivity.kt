package com.haleydu.cimoc.ui.explore
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivitySourceDetailBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.ui.common.BackActivity
import com.haleydu.cimoc.ui.widget.Option
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourceDetailActivity : BackActivity() {

    lateinit var mSourceType: Option
    lateinit var mSourceTitle: Option
    lateinit var mSourceFavorite: Option
    private lateinit var vm: SourceDetailViewModel
    private lateinit var binding: ActivitySourceDetailBinding

    override fun initData() {
        vm = ViewModelProvider(this)[SourceDetailViewModel::class.java]
        val detail = vm.load(intent.getIntExtra(Extra.EXTRA_SOURCE, -1))
        onSourceLoadSuccess(detail.type, detail.title, detail.count)
    }

    fun onSourceFavoriteClick() {
    }

    fun onSourceLoadSuccess(type: Int, title: String, count: Long) {
        mSourceType.setSummary(type.toString())
        mSourceTitle.setSummary(title)
        mSourceFavorite.setSummary(count.toString())
    }

    override fun inflateContentView(): View {
        binding = ActivitySourceDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_source_detail
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.source_detail)
    }

    override fun bindViews() {
        super.bindViews()
        mSourceType = binding.sourceDetailType
        mSourceTitle = binding.sourceDetailTitle
        mSourceFavorite = binding.sourceDetailFavorite
        binding.sourceDetailFavorite.setOnClickListener { onSourceFavoriteClick() }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, type: Int): Intent {
            val intent = Intent(context, SourceDetailActivity::class.java)
            intent.putExtra(Extra.EXTRA_SOURCE, type)
            return intent
        }
    }
}
