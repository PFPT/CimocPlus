package com.haleydu.cimoc.ui.library
import com.haleydu.cimoc.ui.common.BackActivity
import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.haleydu.cimoc.utils.ThemeUtils
import java.io.File

class DirPickerActivity : BackActivity() {

    private lateinit var binding: ActivityComposeBinding
    private var file: File = Environment.getExternalStorageDirectory()
    private var entries by mutableStateOf<List<String>>(emptyList())

    override fun inflateContentView(): View {
        binding = ActivityComposeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun initView() {
        super.initView()
        binding.composeView.setContent {
            MaterialTheme {
                DirPickerScreen(
                    entries = entries,
                    onItemClick = { onEntryClick(it) },
                    onDone = { onDoneClick() }
                )
            }
        }
    }

    override fun initData() {
        updateData()
        hideProgressBar()
    }

    private fun onDoneClick() {
        val intent = Intent()
        intent.putExtra(Extra.EXTRA_PICKER_PATH, file.absolutePath)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun onEntryClick(position: Int) {
        if (position == 0) {
            val parent = file.parentFile ?: return
            file = parent
        } else {
            file = File(file.absolutePath, entries[position])
        }
        updateData()
    }

    private fun updateData() {
        entries = listDir(file)
        mToolbar?.title = file.absolutePath
    }

    private fun listDir(parent: File): List<String> {
        val list = ArrayList<String>()
        val files = parent.listFiles()
        if (files != null) {
            for (dir in files) {
                if (dir.isDirectory) {
                    list.add(dir.name)
                }
            }
            list.sort()
        }
        list.add(0, getString(R.string.dir_picker_parent))
        return list
    }

    override fun getDefaultTitle(): String = getString(R.string.dir_picker)

    override fun getLayoutView(): View = binding.composeLayout

    override fun getLayoutRes(): Int = R.layout.activity_compose
}

@Composable
private fun DirPickerScreen(
    entries: List<String>,
    onItemClick: (Int) -> Unit,
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
            itemsIndexed(entries) { index, title ->
                Text(
                    text = title,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(index) }
                        .padding(14.dp)
                )
            }
        }
    }
}
