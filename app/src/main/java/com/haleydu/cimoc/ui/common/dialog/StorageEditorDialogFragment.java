package com.haleydu.cimoc.ui.common.dialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;

import com.haleydu.cimoc.R;

public class StorageEditorDialogFragment extends EditorDialogFragment {

    public interface Host {
        void launchStoragePicker();
    }

    public static StorageEditorDialogFragment newInstance(int title, String content, int requestCode) {
        StorageEditorDialogFragment fragment = new StorageEditorDialogFragment();
        fragment.setArguments(createBundle(title, content, requestCode));
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = (AlertDialog) super.onCreateDialog(savedInstanceState);
        mEditText.setEnabled(false);
        String title = getString(R.string.settings_other_storage_edit_neutral);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (getActivity() instanceof Host) {
                    ((Host) getActivity()).launchStoragePicker();
                }
            }
        });
        return dialog;
    }

}
