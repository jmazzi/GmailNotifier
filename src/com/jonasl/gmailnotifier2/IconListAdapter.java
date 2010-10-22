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

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

public class IconListAdapter implements ListAdapter {

    private CharSequence[] mLabels;

    private int[] mIconIds;

    private int mCheckedPosition;

    private LayoutInflater mInflater;

    public IconListAdapter(Context context, CharSequence[] labels, int[] iconIds,
            int checkedPosition) {
        if (labels == null || iconIds == null || labels.length != iconIds.length) {
            throw new IllegalArgumentException();
        }

        mLabels = labels;
        mIconIds = iconIds;
        mCheckedPosition = checkedPosition;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mLabels.length;
    }

    @Override
    public Object getItem(int position) {
        return mLabels[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout l = (LinearLayout) convertView;
        if (l == null) {
            l = (LinearLayout) mInflater.inflate(R.layout.niconentry, null);
        }

        ImageView iv = (ImageView) l.findViewById(R.id.icon);
        iv.setImageResource(mIconIds[position]);

        CheckedTextView tv = (CheckedTextView) l.findViewById(R.id.iconlabel);
        tv.setText(mLabels[position]);
        tv.setChecked(position == mCheckedPosition);

        return l;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return mLabels.length == 0;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }
}
