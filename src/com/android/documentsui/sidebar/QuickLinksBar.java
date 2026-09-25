/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.documentsui.sidebar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.documentsui.R;
import com.android.documentsui.base.RootInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A row of one-press links to the storage roots (internal storage, SD cards, USB drives) shown
 * above the directory list, so they can be reached with a D-pad without opening the roots
 * drawer. The link of the root that is currently open is marked as activated.
 */
public final class QuickLinksBar extends HorizontalScrollView {

    private final List<RootItem> mItems = new ArrayList<>();
    private ViewGroup mGroup;
    private @Nullable RootInfo mCurrentRoot;
    private @Nullable View mCurrentLink;

    public QuickLinksBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Only the links take focus, never the scroll container around them.
        setFocusable(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mGroup = findViewById(R.id.quick_links_group);
    }

    /**
     * Shows one link per storage root. Existing link views are rebound rather than recreated, so
     * a refresh of the roots doesn't knock the D-pad focus off the bar.
     */
    void setRoots(List<RootItem> items) {
        final int focusedIndex = mGroup.indexOfChild(mGroup.getFocusedChild());

        mItems.clear();
        mItems.addAll(items);

        while (mGroup.getChildCount() > mItems.size()) {
            mGroup.removeViewAt(mGroup.getChildCount() - 1);
        }
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < mItems.size(); i++) {
            View link = mGroup.getChildAt(i);
            if (link == null) {
                link = inflater.inflate(R.layout.item_quick_link, mGroup, false);
                link.setOnClickListener(this::onLinkClicked);
                mGroup.addView(link);
            }
            bindLink(link, mItems.get(i));
        }

        setVisibility(mItems.isEmpty() ? GONE : VISIBLE);
        updateCurrentLink();

        // The focused link was removed; keep the focus on the bar.
        if (focusedIndex >= 0 && !mGroup.hasFocus() && !mItems.isEmpty()) {
            mGroup.getChildAt(Math.min(focusedIndex, mItems.size() - 1)).requestFocus();
        }
    }

    /** Marks the link of the given root, if it has one, as the open one. */
    void setCurrentRoot(@Nullable RootInfo root) {
        mCurrentRoot = root;
        updateCurrentLink();
    }

    /**
     * Opens the storage root next to the current one, wrapping around at the ends. Meant for
     * controller shoulder buttons.
     *
     * @return false if there is no other root to switch to.
     */
    public boolean openAdjacent(int delta) {
        final int count = mItems.size();
        if (getVisibility() != VISIBLE || count == 0) {
            return false;
        }

        final int current = mCurrentLink == null ? -1 : mGroup.indexOfChild(mCurrentLink);
        final int next;
        if (current < 0) {
            next = delta > 0 ? 0 : count - 1;
        } else {
            next = Math.floorMod(current + delta, count);
            if (next == current) {
                return false;
            }
        }
        mItems.get(next).open();
        return true;
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        // Focus coming from outside of the bar lands on the root that is currently open rather
        // than on whichever link happens to be geometrically closest.
        if (mCurrentLink != null && findFocus() == null) {
            mCurrentLink.addFocusables(views, direction, focusableMode);
            return;
        }
        super.addFocusables(views, direction, focusableMode);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
            // Sideways moves stay on the bar instead of jumping diagonally into the toolbar.
            final View next = FocusFinder.getInstance().findNextFocus(this, focused, direction);
            return next != null ? next : focused;
        }
        return super.focusSearch(focused, direction);
    }

    private void onLinkClicked(View link) {
        final int index = mGroup.indexOfChild(link);
        if (index >= 0 && index < mItems.size()) {
            mItems.get(index).open();
        }
    }

    private void bindLink(View link, RootItem item) {
        final ImageView icon = link.findViewById(android.R.id.icon);
        final TextView title = link.findViewById(android.R.id.title);
        icon.setImageDrawable(item.root.loadDrawerIcon(getContext(), item.mMaybeShowBadge));
        title.setText(item.title);
    }

    private void updateCurrentLink() {
        mCurrentLink = null;
        for (int i = 0; i < mItems.size(); i++) {
            final View link = mGroup.getChildAt(i);
            final boolean current = Objects.equals(mItems.get(i).root, mCurrentRoot);
            link.setActivated(current);
            if (current) {
                mCurrentLink = link;
            }
        }
    }
}
