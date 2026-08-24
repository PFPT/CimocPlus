package com.haleydu.cimoc.ui.search
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Context
import android.content.Intent
import android.view.View
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityContainerBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.ui.search.SearchFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BackActivity() {

    private lateinit var binding: ActivityContainerBinding

    override fun inflateContentView(): View {
        binding = ActivityContainerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        if (supportFragmentManager.findFragmentById(R.id.container_fragment) == null) {
            val source = intent.getIntExtra(Extra.EXTRA_SOURCE, -1)
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, SearchFragment.newInstance(source))
                .commit()
        }
    }

    override fun getDefaultTitle(): String {
        val title = intent.getStringExtra(Extra.EXTRA_KEYWORD)
        return if (!title.isNullOrEmpty()) title else getString(R.string.comic_search)
    }

    override fun getLayoutView(): View {
        return binding.containerLayout
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_container
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, sourceType: Int, title: String): Intent {
            val intent = Intent(context, SearchActivity::class.java)
            intent.putExtra(Extra.EXTRA_SOURCE, sourceType)
            intent.putExtra(Extra.EXTRA_KEYWORD, title)
            return intent
        }
    }
}
