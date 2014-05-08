package com.dreamteam.vicam.view.custom.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.dreamteam.camera.R;
import com.dreamteam.vicam.model.events.SavePresetEvent;
import com.dreamteam.vicam.presenter.utility.Dagger;

import de.greenrobot.event.EventBus;

import javax.inject.Inject;

/**
 * Manages a custom layout for the SavePreset dialog
 */
public class SavePresetDialogFragment extends DialogFragment {

  Activity mContext;
  @Inject
  EventBus eventBus;

  public SavePresetDialogFragment(Context context) {
    Dagger.inject(this);
    mContext = (Activity) context;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

    // Inflate the layout for the dialog
    LayoutInflater inflater = mContext.getLayoutInflater();
    // Pass null as the parent view because its going in the dialog layout
    View view = inflater.inflate(R.layout.dialog_save_preset, null);
    final EditText editText = (EditText) view.findViewById(R.id.edit_text_save_preset);

    builder.setView(view)
        // Add action buttons
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            // Add the preset to database
            eventBus.post(new SavePresetEvent(dialog, editText.getText().toString()));
          }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
          // Cancel dialog
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
    return builder.create();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.getDialog().setCanceledOnTouchOutside(true);
    return null;
  }
}
