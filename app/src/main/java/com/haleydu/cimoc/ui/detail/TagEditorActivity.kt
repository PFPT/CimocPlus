package com.haleydu.cimoc.ui.detail
import com.haleydu.cimoc.ui.common.BackActivity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Checkbox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.haleydu.cimoc.R
import com.haleydu.cimoc.databinding.ActivityComposeBinding
import com.haleydu.cimoc.global.Extra
import com.haleydu.cimoc.misc.Switcher
import com.haleydu.cimoc.model.Tag
import com.haleydu.cimoc.ui.common.collectOnStart
import com.haleydu.cimoc.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagEditorActivity : BackActivity() {

    private val vm: TagEditorViewModel by viewModels()
    private lateinit var binding: ActivityComposeBinding
    private var tags by mutableStateOf(emptyList<Switcher<Tag>>(), neverEqualPolicy())

    override fun inflateContentView(): View {
        binding = ActivityComposeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        hideProgressBar()
        binding.composeView.setContent {
            MaterialTheme {
                TagEditorScreen(
                    tags = tags,
                    onToggle = { index ->
                        tags[index].switchEnable()
                        tags = tags
                    },
                    onDone = { onDoneClick() }
                )
            }
        }
    }

    override fun initData() {
        val id = intent.getLongExtra(Extra.EXTRA_ID, -1)
        vm.tags.collectOnStart(this) { onTagLoadSuccess(it) }
        vm.loadFail.collectOnStart(this) { onTagLoadFail() }
        vm.updateSuccess.collectOnStart(this) { onTagUpdateSuccess() }
        vm.updateFail.collectOnStart(this) { onTagUpdateFail() }
        vm.load(id)
    }

    private fun onTagLoadSuccess(list: List<Switcher<Tag>>) {
        hideProgressBar()
        tags = ArrayList(list)
    }

    private fun onTagLoadFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_data_load_fail)
    }

    private fun onTagUpdateSuccess() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_success)
    }

    private fun onTagUpdateFail() {
        hideProgressDialog()
        showSnackbar(R.string.common_execute_fail)
    }

    private fun onDoneClick() {
        showProgressDialog()
        val ids = ArrayList<Long>()
        for (switcher in tags) {
            if (switcher.isEnable) {
                ids.add(switcher.element.id)
            }
        }
        vm.updateRef(ids)
    }

    override fun getDefaultTitle(): String = getString(R.string.tag_editor)

    override fun getLayoutView(): View = binding.composeLayout

    override fun getLayoutRes(): Int = R.layout.activity_compose

    companion object {
        @JvmStatic
        fun createIntent(context: Context, id: Long): Intent {
            val intent = Intent(context, TagEditorActivity::class.java)
            intent.putExtra(Extra.EXTRA_ID, id)
            return intent
        }
    }
}

@Composable
private fun TagEditorScreen(
    tags: List<Switcher<Tag>>,
    onToggle: (Int) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(ContextCompat.getColor(context, ThemeUtils.getResourceId(context, R.attr.colorAccent)))
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onDone, backgroundColor = accent) {
                Icon(
                    painter = painterResource(R.drawable.ic_done_white_24dp),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(tags, key = { _, item -> item.element.id }) { index, switcher ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(index) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = switcher.element.title,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(checked = switcher.isEnable, onCheckedChange = null)
                }
            }
        }
    }
}
