package com.haleydu.cimoc.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.haleydu.cimoc.R;
import com.haleydu.cimoc.ui.reader.ReaderActivity;
import com.haleydu.cimoc.component.DialogCaller;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.Task;
import com.haleydu.cimoc.event.AppEventBus;
import com.haleydu.cimoc.event.AppEvent;
import com.haleydu.cimoc.service.DownloadService;
import com.haleydu.cimoc.service.DownloadService.DownloadServiceBinder;
import com.haleydu.cimoc.ui.FlowExtKt;
import com.haleydu.cimoc.ui.adapter.BaseAdapter;
import com.haleydu.cimoc.ui.adapter.TaskAdapter;
import com.haleydu.cimoc.ui.fragment.dialog.ItemDialogFragment;
import com.haleydu.cimoc.utils.StringUtils;
import com.haleydu.cimoc.utils.ThemeUtils;
import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by Hiroshi on 2016/9/7.
 */
@AndroidEntryPoint
public class TaskActivity extends CoordinatorActivity implements DialogCaller {

    private static final int REQUEST_CODE_DELETE = 0;
    private static final int DIALOG_REQUEST_OPERATION = 1;

    private static final int OPERATION_READ = 0;
    private static final int OPERATION_DELETE = 1;

    private TaskAdapter mTaskAdapter;
    private TaskViewModel vm;
    private ServiceConnection mConnection;
    private DownloadServiceBinder mBinder;
    private boolean mTaskOrder;

    private Task mSavedTask;

    public static Intent createIntent(Context context, Long id) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(Extra.EXTRA_ID, id);
        return intent;
    }

    @Override
    protected void initViewModel() {
        vm = new ViewModelProvider(this).get(TaskViewModel.class);
    }

    @Override
    protected BaseAdapter initAdapter() {
        mTaskAdapter = new TaskAdapter(this, new LinkedList<Task>());
        return mTaskAdapter;
    }

    @Override
    protected void initActionButton() {
        mActionButton.setImageResource(R.drawable.ic_launch_white_24dp);
        mActionButton.show();
        mActionButton2.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        mActionButton2.show();
    }

    void onActionButton2Click() {
        for (int i = 0; i < mTaskAdapter.getDateSet().size(); i++) {
            Task task = mTaskAdapter.getItem(i);
            if (task.getState() == Task.STATE_PAUSE || task.getState() == Task.STATE_ERROR) {
                task.setState(Task.STATE_WAIT);
                mTaskAdapter.notifyItemChanged(i);
                Intent taskIntent = DownloadService.createIntent(this, task);
                DownloadService.start(this, taskIntent);
            }
        }
    }

    @Override
    protected void initData() {
        long key = getIntent().getLongExtra(Extra.EXTRA_ID, -1);
        mTaskOrder = mPreference.getBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, false);
        FlowExtKt.collectOnStart(vm.getLoadSuccess(), this, result ->
                onTaskLoadSuccess(result.getList(), result.isLocal()));
        FlowExtKt.collectOnStart(vm.getLoadFail(), this, unit -> onTaskLoadFail());
        FlowExtKt.collectOnStart(vm.getDeleteSuccess(), this, this::onTaskDeleteSuccess);
        FlowExtKt.collectOnStart(vm.getDeleteFail(), this, unit -> onTaskDeleteFail());
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_TASK_STATE_CHANGE), this, event -> {
            long id = (long) event.getData(1);
            switch ((int) event.getData()) {
                case Task.STATE_PARSE:
                    onTaskParse(id);
                    break;
                case Task.STATE_ERROR:
                    onTaskError(id);
                    break;
                case Task.STATE_PAUSE:
                    onTaskPause(id);
                    break;
            }
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_TASK_PROCESS), this, event ->
                onTaskProcess((long) event.getData(), (int) event.getData(1), (int) event.getData(2)));
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_TASK_INSERT), this, event -> {
            @SuppressWarnings("unchecked")
            List<Task> list = (List<Task>) event.getData(1);
            Task task = list.get(0);
            Comic comic = vm.getComic();
            if (comic != null && task.getKey() == comic.getId()) {
                onTaskAdd(list);
            }
        });
        FlowExtKt.collectOnStart(AppEventBus.observe(AppEvent.EVENT_COMIC_UPDATE), this, event -> {
            Comic comic = vm.getComic();
            if (comic != null && comic.getId() != null && comic.getId() == (long) event.getData()) {
                vm.refreshLast();
                onLastChange(vm.getComic().getLast());
            }
        });
        vm.load(key, mTaskOrder);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task, menu);
        return super.onCreateOptionsMenu(menu);
    }

    void onActionButtonClick() {
        Intent intent = DetailActivity.createIntent(this, vm.getComic().getId(),
                vm.getComic().getSource(), vm.getComic().getCid());
        startActivity(intent);
    }

    public void onLastChange(String path) {
        mTaskAdapter.setLast(path);
    }

    @Override
    public void onItemClick(View view, final int position) {
        Task task = mTaskAdapter.getItem(position);
        switch (task.getState()) {
            case Task.STATE_FINISH:
                startReader(task.getPath(), false);
                break;
            case Task.STATE_PAUSE:
            case Task.STATE_ERROR:
                task.setState(Task.STATE_WAIT);
                mTaskAdapter.notifyItemChanged(position);
                Intent taskIntent = DownloadService.createIntent(this, task);
                DownloadService.start(this, taskIntent);
                break;
            case Task.STATE_WAIT:
                task.setState(Task.STATE_PAUSE);
                mTaskAdapter.notifyItemChanged(position);
                mBinder.getService().removeDownload(task.getId());
                break;
            case Task.STATE_DOING:
            case Task.STATE_PARSE:
                mBinder.getService().removeDownload(task.getId());
                break;
        }
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        mSavedTask = mTaskAdapter.getItem(position);
        String[] item = {getString(R.string.task_read), getString(R.string.task_delete)};
        ItemDialogFragment fragment = ItemDialogFragment.newInstance(R.string.common_operation_select,
                item, DIALOG_REQUEST_OPERATION);
        fragment.show(getSupportFragmentManager(), null);
        return true;
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_OPERATION:
                int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
                switch (index) {
                    case OPERATION_READ:
                        startReader(mSavedTask.getPath(), true);
                        break;
                    case OPERATION_DELETE:
                        showProgressDialog();
                        List<Chapter> list = new ArrayList<>(1);
                        Long sourceComic =  Long.parseLong(mSavedTask.getSource()+"000"+mSavedTask.getId());
                        Long id = Long.parseLong(sourceComic+"000"+0);
                        list.add(new Chapter(id,sourceComic,mSavedTask.getTitle(), mSavedTask.getPath(), mSavedTask.getId()));
                        if (!vm.getComic().getLocal()) {
                            mBinder.getService().removeDownload(mSavedTask.getId());
                        }
                        vm.deleteTask(list, mTaskAdapter.getItemCount() == 1);
                        break;
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mTaskAdapter.getDateSet().isEmpty()) {
            switch (item.getItemId()) {
                case R.id.task_history:
                    String path = vm.getComic().getLast();
                    if (path == null) {
                        path = mTaskAdapter.getItem(mTaskOrder ?
                                0 : mTaskAdapter.getDateSet().size() - 1).getPath();
                    }
                    startReader(path, true);
                    break;
                case R.id.task_delete:
                    ArrayList<Chapter> list = new ArrayList<>(mTaskAdapter.getItemCount());
                    int i = 0;
                    for (Task task : mTaskAdapter.getDateSet()) {
                        Long sourceComic = Long.parseLong(task.getSource()+"000"+task.getId());
                        Long id = Long.parseLong(sourceComic+"000"+i);
                        list.add(new Chapter(id,sourceComic, task.getTitle(), task.getPath(), task.getId()));
                    }
                    Intent intent = ChapterActivity.createIntent(this, list);
                    startActivityForResult(intent, REQUEST_CODE_DELETE);
                    break;
                case R.id.detail_search_title:
                    if (!StringUtils.isEmpty(vm.getComic().getTitle())) {
                        intent = ResultActivity.createIntent(this, vm.getComic().getTitle(),
                                null, ResultActivity.LAUNCH_MODE_SEARCH);
                        startActivity(intent);
                    } else {
                        showSnackbar(R.string.common_keyword_empty);
                    }
                    break;
                case R.id.detail_search_author:
                    if (!StringUtils.isEmpty(vm.getComic().getAuthor())) {
                        intent = ResultActivity.createIntent(this, vm.getComic().getAuthor(),
                                null, ResultActivity.LAUNCH_MODE_SEARCH);
                        startActivity(intent);
                    } else {
                        showSnackbar(R.string.common_keyword_empty);
                    }
                    break;
                case R.id.task_sort:
                    mTaskAdapter.reverse();
                    mTaskOrder = !mTaskOrder;
                    mPreference.putBoolean(PreferenceManager.PREF_CHAPTER_ASCEND_MODE, mTaskOrder);
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void startReader(String path, boolean preview) {
        List<Chapter> list = new ArrayList<>();
        int i=0;
        for (Task t : mTaskAdapter.getDateSet()) {
            Long sourceComic = Long.parseLong(t.getSource()+"000"+t.getId());
            Long id = Long.parseLong(sourceComic+""+i);
            if (preview && t.getProgress() > 0) {
                list.add(new Chapter(id,sourceComic, t.getTitle(), t.getPath(), t.getProgress(), true, true, t.getId()));
            } else if (t.getState() == Task.STATE_FINISH) {
                list.add(new Chapter(id,sourceComic, t.getTitle(), t.getPath(), t.getMax(), true, true, t.getId()));
            }
            i++;
        }
        mTaskAdapter.setLast(path);
        long id = vm.updateLast(path);
        int mode = mPreference.getInt(PreferenceManager.PREF_READER_MODE, PreferenceManager.READER_MODE_PAGE);
        Intent readerIntent = ReaderActivity.createIntent(this, id, list, mode);
        startActivity(readerIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_DELETE:
                    List<Chapter> list = data.getParcelableArrayListExtra(Extra.EXTRA_CHAPTER);
                    if (!list.isEmpty()) {
                        showProgressDialog();
                        for (Chapter chapter : list) {
                            mBinder.getService().removeDownload(chapter.getTid());
                        }
                        vm.deleteTask(list, mTaskAdapter.getItemCount() == list.size());
                    } else {
                        showSnackbar(R.string.task_empty);
                    }
                    break;
            }
        }
    }

    public void onTaskDeleteSuccess(List<Long> list) {
        hideProgressDialog();
        mTaskAdapter.removeById(list);
        showSnackbar(R.string.common_execute_success);
    }

    public void onTaskDeleteFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    public void onTaskLoadSuccess(final List<Task> list, boolean local) {
        mTaskAdapter.setColorId(ThemeUtils.getResourceId(this, R.attr.colorAccent));
        mTaskAdapter.setLast(vm.getComic().getLast());
        mTaskAdapter.addAll(list);
        if (!local) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mBinder = (DownloadServiceBinder) service;
                    mBinder.getService().initTask(mTaskAdapter.getDateSet());
                    hideProgressBar();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            bindService(new Intent(this, DownloadService.class), mConnection, BIND_AUTO_CREATE);
        } else {
            hideProgressBar();
            mLayoutView.removeView(mActionButton);
        }
    }

    public void onTaskLoadFail() {
        hideProgressBar();
        mLayoutView.removeView(mActionButton);
        showSnackbar(R.string.task_load_task_fail);
    }

    public void onTaskAdd(List<Task> list) {
        mTaskAdapter.addAll(0, list);
    }

    /**
     * task state
     */

    public void onTaskError(long id) {
        int position = mTaskAdapter.getPositionById(id);
        if (position != -1) {
            Task task = mTaskAdapter.getItem(position);
            if (task.getState() != Task.STATE_PAUSE) {
                task.setState(Task.STATE_ERROR);
                notifyItemChanged(position);
            }
        }
    }

    public void onTaskPause(long id) {
        int position = mTaskAdapter.getPositionById(id);
        if (position != -1) {
            mTaskAdapter.getItem(position).setState(Task.STATE_PAUSE);
            notifyItemChanged(position);
        }
    }

    public void onTaskParse(long id) {
        int position = mTaskAdapter.getPositionById(id);
        if (position != -1) {
            Task task = mTaskAdapter.getItem(position);
            if (task.getState() != Task.STATE_PAUSE) {
                task.setState(Task.STATE_PARSE);
                notifyItemChanged(position);
            }
        }
    }

    public void onTaskProcess(long id, int progress, int max) {
        int position = mTaskAdapter.getPositionById(id);
        if (position != -1) {
            Task task = mTaskAdapter.getItem(position);
            task.setMax(max);
            task.setProgress(progress);
            if (task.getState() != Task.STATE_PAUSE) {
                int state = max == progress ? Task.STATE_FINISH : Task.STATE_DOING;
                task.setState(state);
            }
            notifyItemChanged(position);
        }
    }

    private void notifyItemChanged(int position) {
        if (!mRecyclerView.isComputingLayout()) {
            mTaskAdapter.notifyItemChanged(position);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.task_list);
    }


    @Override
    protected void bindViews() {
        super.bindViews();
        mActionButton2.setOnClickListener(v -> onActionButton2Click());
        mActionButton.setOnClickListener(v -> onActionButtonClick());
    }

}
