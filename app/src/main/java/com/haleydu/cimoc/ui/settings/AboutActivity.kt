package com.haleydu.cimoc.ui.settings
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import android.net.Uri
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityAboutBinding
import com.haleydu.cimoc.utils.StringUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AboutViewModel : ViewModel() {
    private val _version = MutableStateFlow("")
    val version: StateFlow<String> = _version

    fun setVersion(text: String) {
        _version.value = text
    }
}

class AboutActivity : BackActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun inflateContentView(): View {
        binding = ActivityAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        binding.aboutCompose.setContent {
            MaterialTheme {
                AboutScreen(onOpenUrl = { urlRes -> openUrl(getString(urlRes)) })
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            showSnackbar(R.string.about_resource_fail)
        }
    }

    override fun getDefaultTitle(): String {
        return getString(R.string.drawer_about)
    }

    override fun getLayoutView(): View {
        return binding.aboutLayout
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_about
    }
}

@Composable
private fun AboutScreen(onOpenUrl: (Int) -> Unit, vm: AboutViewModel = viewModel()) {
    val context = LocalContext.current
    val version by vm.version.collectAsState()
    LaunchedEffect(Unit) {
        try {
            val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            vm.setVersion(StringUtils.format("Version  %s (%s)", info.versionName, PackageInfoCompat.getLongVersionCode(info)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.app_name), fontSize = 40.sp, color = MaterialTheme.colors.primary)
        Text(text = version, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        AboutItem(R.string.about_update, R.string.about_update_summary) {
            onOpenUrl(R.string.about_update_url)
        }
        AboutItem(R.string.about_support, R.string.about_support_url) {
            onOpenUrl(R.string.about_support_url)
        }
    }
}

@Composable
private fun AboutItem(title: Int, summary: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    ) {
        Text(text = stringResource(title), fontSize = 16.sp)
        Text(text = stringResource(summary), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
