/*    
    This file is part of Gmail Notifier.
    Copyright 2010 Jonas Larsson, jonas@hallerud.se

    Gmail Notifier is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gmail Notifier is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gmail Notifier. If not, see <http://www.gnu.org/licenses/>.

 */

package com.jonasl.gmailnotifier2;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

public class IconListPreference extends ListPreference {

    private int mClickedDialogEntryIndex;

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null) {
            throw new IllegalStateException(
                    "IconListPreference requires an entries array and an entryValues array.");
        }

        String value = getValue();
        mClickedDialogEntryIndex = findIndexOfValue(value);
        int[] iconIds = new int[entryValues.length];
        for (int i = 0; i < entryValues.length; i++) {
            try {
                iconIds[i] = R.drawable.class.getField(entryValues[i].toString()).getInt(null);
            } catch (Exception e) {
                Log.e(GmailReceiver.TAG, e.toString());
            }
        }
        IconListAdapter adapter = new IconListAdapter(getContext(), entries, iconIds,
                mClickedDialogEntryIndex);
        builder.setSingleChoiceItems(adapter, mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;

                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        IconListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && mClickedDialogEntryIndex >= 0 && entryValues != null) {
            String value = entryValues[mClickedDialogEntryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }
}
