package org.lastmilehealth.kiosk;

/**
 * Preference Input Field for Password
 */


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PasswordTextPreference extends DialogPreference {
    /**
     * The edit text shown in the dialog.
     */
    private EditText password;
    private EditText reenteredPassword;

    private String mText;
    private int mWhichButtonClicked;


    public PasswordTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        //setPersistent(false);
        setDialogLayoutResource(R.layout.dialog_password);
    }

    public PasswordTextPreference(Context context) {
        this(context, null);
    }

    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param text The text to save
     */
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        mText = text;

        persistString(text);
        Toast.makeText(getContext(),"Password changed.",Toast.LENGTH_LONG).show();
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    /**
     * Gets the text from the {@link SharedPreferences}.
     *
     * @return The current preference value.
     */
    public String getText() {
        return mText;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        password = (EditText) view.findViewById(R.id.new_password);
        reenteredPassword = (EditText) view.findViewById(R.id.new_password_reentered);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
        super.onClick(dialog, which);
    }


    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final AlertDialog d = (AlertDialog) getDialog();

        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean canCloseDialog = password.getText().toString().equals(reenteredPassword.getText().toString());

                if (canCloseDialog) {
                    d.dismiss();
                    onDialogClosed(true);
                } else {
                    Toast t = Toast.makeText(getContext(), "Different password entered!", Toast.LENGTH_LONG);
                    t.show();
                }
            }
        });
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String value = password.getText().toString();
            setText(value);
        }
    }

//    @Override
//    protected Object onGetDefaultValue(TypedArray a, int index) {
//        return a.getString(index);
//    }
//
//    @Override
//    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
//        setText(restoreValue ? getPersistedString(mText) : (String) defaultValue);
//    }
//
//    @Override
//    public boolean shouldDisableDependents() {
//        return TextUtils.isEmpty(mText) || super.shouldDisableDependents();
//    }
//    public EditText getEditText() {
//        return password;
//    }
//
//    /** @hide */
//    @Override
//    protected boolean needInputMethod() {
//        // We want the input method to show, if possible, when dialog is displayed
//        return true;
//    }

//    @Override
//    protected Parcelable onSaveInstanceState() {
//        final Parcelable superState = super.onSaveInstanceState();
//        if (isPersistent()) {
//            // No need to save instance state since it's persistent
//            return superState;
//        }
//
//        final SavedState myState = new SavedState(superState);
//        myState.text = getText();
//        return myState;
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Parcelable state) {
//        if (state == null || !state.getClass().equals(SavedState.class)) {
//            // Didn't save state for us in onSaveInstanceState
//            super.onRestoreInstanceState(state);
//            return;
//        }
//
//        SavedState myState = (SavedState) state;
//        super.onRestoreInstanceState(myState.getSuperState());
//        setText(myState.text);
//    }

//    private static class SavedState extends BaseSavedState {
//        String text;
//
//        public SavedState(Parcel source) {
//            super(source);
//            text = source.readString();
//        }
//
//        @Override
//        public void writeToParcel(Parcel dest, int flags) {
//            super.writeToParcel(dest, flags);
//            dest.writeString(text);
//        }
//
//        public SavedState(Parcelable superState) {
//            super(superState);
//        }
//
//        public static final Parcelable.Creator<SavedState> CREATOR =
//                new Parcelable.Creator<SavedState>() {
//                    public SavedState createFromParcel(Parcel in) {
//                        return new SavedState(in);
//                    }
//
//                    public SavedState[] newArray(int size) {
//                        return new SavedState[size];
//                    }
//                };
//    }

}