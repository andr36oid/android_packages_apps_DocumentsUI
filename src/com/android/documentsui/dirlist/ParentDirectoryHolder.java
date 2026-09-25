/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.view.ViewGroup;

import com.android.documentsui.R;

/**
 * The ".." row on top of a directory listing. Activating it opens the parent directory, which
 * saves hunting for a back key when all there is to navigate with is a D-pad.
 */
final class ParentDirectoryHolder extends MessageHolder {

    ParentDirectoryHolder(Context context, ViewGroup parent) {
        super(context, parent, R.layout.item_parent_directory);
    }

    void bind(Runnable openParent) {
        itemView.setOnClickListener(v -> openParent.run());
    }

    @Override
    public void bind(Cursor cursor, String modelId) {
        // Nothing to bind, the row always reads "..".
    }
}
