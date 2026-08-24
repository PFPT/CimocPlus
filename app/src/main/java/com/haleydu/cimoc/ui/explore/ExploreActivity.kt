package com.haleydu.cimoc.ui.explore
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Context
import android.content.Intent
import android.view.View
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityExploreBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.ui.explore.ExploreFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExploreActivity : BackActivity() {

    private lateinit var binding: ActivityExploreBinding

    override fun inflateContentView(): View {
        binding = ActivityExploreBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        if (supportFragmentManager.findFragmentById(R.id.explore_fragment_container) == null) {
            val source = intent.getIntExtra(Extra.EXTRA_SOURCE, -1)
            supportFragmentManager.beginTransaction()
                .replace(R.id.explore_fragment_container, ExploreFragment.newInstance(source))
                .commit()
        }
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.explore)
    }

    override fun getLayoutView(): View {
        return binding.exploreLayout
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_explore
    }

    override fun isNavTranslation(): Boolean {
        return true
    }

    companion object {
        fun createIntent(context: Context, source: Int): Intent {
            val intent = Intent(context, ExploreActivity::class.java)
            intent.putExtra(Extra.EXTRA_SOURCE, source)
            return intent
        }
    }
}
