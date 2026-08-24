package com.haleydu.cimoc.ui.explore
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Context
import android.content.Intent
import android.view.View
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityContainerBinding
import com.haleydu.cimoc.ui.explore.SourceFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SourceActivity : BackActivity() {

    private lateinit var binding: ActivityContainerBinding

    override fun inflateContentView(): View {
        binding = ActivityContainerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        if (supportFragmentManager.findFragmentById(R.id.container_fragment) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, SourceFragment())
                .commit()
        }
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.drawer_source)
    }

    override fun getLayoutView(): View {
        return binding.containerLayout
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_container
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SourceActivity::class.java)
        }
    }
}
