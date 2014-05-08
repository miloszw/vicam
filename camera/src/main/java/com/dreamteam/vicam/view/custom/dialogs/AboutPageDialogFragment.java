package com.dreamteam.vicam.view.custom.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.dreamteam.camera.R;
/**
 * Manages a custom layout for the about dialog.
 */
public class AboutPageDialogFragment extends DialogFragment {

  Activity mActivity;

  public AboutPageDialogFragment(Activity activity) {
  //  Dagger.inject(this);
    mActivity = activity;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
    // Get the layout inflater
    LayoutInflater inflater = mActivity.getLayoutInflater();
    View view = inflater.inflate(R.layout.dialog_about_page, null);
    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    builder.setView(view)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {

          }
        });

    return builder.create();
  }
}