package com.haleydu.cimoc.ui.fragment.recyclerview.grid

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.haleydu.cimoc.R
import com.haleydu.cimoc.component.DialogCaller
import com.haleydu.cimoc.manager.PreferenceManager
import com.haleydu.cimoc.misc.NotificationWrapper
import com.haleydu.cimoc.model.MiniComic
import com.haleydu.cimoc.event.AppEventBus
import com.haleydu.cimoc.event.AppEvent
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment
import com.haleydu.cimoc.utils.HintUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class FavoriteFragment : GridFragment() {

    private val vm: FavoriteViewModel by viewModels()
    private var notification: NotificationWrapper? = null

    override fun initView() {
        super.initView()
        mGridAdapter.setSymbol(true)
    }

    override fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.comics.collect { list -> onComicLoadSuccess(list) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.check.collect { event ->
                when (event) {
                    is FavoriteViewModel.CheckEvent.Progress ->
                        onComicCheckSuccess(event.comic, event.progress, event.max)
                    FavoriteViewModel.CheckEvent.Fail -> onComicCheckFail()
                    FavoriteViewModel.CheckEvent.Complete -> onComicCheckComplete()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AppEventBus.observe(AppEvent.EVENT_COMIC_FAVORITE).collect {
                OnComicFavorite(it.getData() as MiniComic)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AppEventBus.observe(AppEvent.EVENT_COMIC_UNFAVORITE).collect {
                OnComicUnFavorite(it.getData() as Long)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            @Suppress("UNCHECKED_CAST")
            AppEventBus.observe(AppEvent.EVENT_COMIC_FAVORITE_RESTORE).collect {
                OnComicRestore(it.getData() as List<Any>)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AppEventBus.observe(AppEvent.EVENT_COMIC_READ).collect {
                onComicRead(it.getData() as MiniComic)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            AppEventBus.observe(AppEvent.EVENT_COMIC_CANCEL_HIGHLIGHT).collect {
                onHighlightCancel(it.getData() as MiniComic)
            }
        }
        vm.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notification?.cancel()
    }

    override fun onDialogResult(requestCode: Int, bundle: Bundle) {
        when (requestCode) {
            DIALOG_REQUEST_OPERATION -> when (bundle.getInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX)) {
                OPERATION_INFO -> showComicInfo(vm.loadComic(mSavedId), DIALOG_REQUEST_INFO)
                OPERATION_DELETE -> {
                    val fragment = MessageDialogFragment.newInstance(
                        R.string.dialog_confirm,
                        R.string.favorite_delete_confirm,
                        true,
                        DIALOG_REQUEST_DELETE
                    )
                    fragment.setTargetFragment(this, 0)
                    fragment.show(requireActivity().supportFragmentManager, null)
                }
            }
            DIALOG_REQUEST_UPDATE -> checkUpdate()
            DIALOG_REQUEST_DELETE -> {
                vm.unfavoriteComic(mSavedId)
                OnComicUnFavorite(mSavedId)
                HintUtils.showToast(activity, R.string.common_execute_success)
            }
        }
    }

    fun cancelAllHighlight() {
        vm.cancelAllHighlight()
        mGridAdapter.cancelAllHighlight()
    }

    private fun checkUpdate() {
        if (notification == null) {
            vm.checkUpdate()
            notification = NotificationWrapper(
                activity,
                NOTIFICATION_CHECK_UPDATE,
                R.drawable.ic_sync_white_24dp,
                true
            )
            notification?.post(getString(R.string.favorite_check_update_doing), 0, 0)
        } else {
            HintUtils.showToast(activity, R.string.favorite_check_update_doing)
        }
    }

    override fun performActionButtonClick() {
        if (mGridAdapter.dateSet.isEmpty()) {
            return
        }
        val fragment = MessageDialogFragment.newInstance(
            R.string.dialog_confirm,
            R.string.favorite_check_update_confirm,
            true,
            DIALOG_REQUEST_UPDATE
        )
        fragment.setTargetFragment(this, 0)
        fragment.show(requireActivity().supportFragmentManager, null)
    }

    override fun onComicLoadSuccess(list: List<Any>) {
        super.onComicLoadSuccess(list)
        val manager = activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (manager != null && manager.isWifiEnabled &&
            mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_UPDATE, false)
        ) {
            val calendar = Calendar.getInstance()
            val day = calendar[Calendar.DAY_OF_YEAR]
            calendar.timeInMillis = mPreference.getLong(PreferenceManager.PREF_OTHER_CHECK_UPDATE_LAST, 0)
            if (day != calendar[Calendar.DAY_OF_YEAR]) {
                mPreference.putLong(PreferenceManager.PREF_OTHER_CHECK_UPDATE_LAST, System.currentTimeMillis())
                checkUpdate()
            }
        }
    }

    fun OnComicFavorite(comic: MiniComic) {
        mGridAdapter.add(mGridAdapter.findFirstNotHighlight(), comic)
    }

    fun OnComicRestore(list: List<Any>) {
        mGridAdapter.addAll(mGridAdapter.findFirstNotHighlight(), list)
    }

    fun OnComicUnFavorite(id: Long) {
        mGridAdapter.removeItemById(id)
    }

    fun onComicCheckSuccess(comic: MiniComic?, progress: Int, max: Int) {
        if (comic != null) {
            mGridAdapter.remove(comic)
            mGridAdapter.add(0, comic)
        }
        notification?.post(progress, max)
    }

    fun onComicCheckFail() {
        notification?.post(getString(R.string.favorite_check_update_fail), false)
        notification = null
    }

    fun onComicCheckComplete() {
        notification?.post(getString(R.string.favorite_check_update_done), false)
        notification?.cancel()
        notification = null
    }

    fun onHighlightCancel(comic: MiniComic) {
        mGridAdapter.moveItemTop(comic)
    }

    fun onComicRead(comic: MiniComic) {
        mGridAdapter.moveItemTop(comic)
    }

    override fun getActionButtonRes(): Int = R.drawable.ic_sync_white_24dp

    override fun getOperationItems(): Array<String> =
        arrayOf(getString(R.string.comic_info), getString(R.string.favorite_delete))

    companion object {
        private const val DIALOG_REQUEST_UPDATE = 1
        private const val DIALOG_REQUEST_INFO = 2
        private const val DIALOG_REQUEST_DELETE = 3
        private const val OPERATION_INFO = 0
        private const val OPERATION_DELETE = 1
        private const val NOTIFICATION_CHECK_UPDATE = "NOTIFICATION_CHECK_UPDATE"
    }
}
