/*
 * Copyright (c) 2015 Martin Norbert
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.app4life.sitemonitor.activity.fragment.floatingButton;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * A Floating Action Button is a {@link android.widget.Checkable} view distinguished by a circled
 * icon floating above the UI, with special motion behaviors.
 *
 * @modifiedBy Martin Norbert
 */
public class FloatingButton extends FrameLayout {

    private static final String TAG = "FloatingActionButton";
    private OnClickListener onClickListener;

    public FloatingButton(Context context) {
        this(context, null, 0, 0);
    }

    public FloatingButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public FloatingButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FloatingButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        setClickable(true);

        // Set the outline provider for this view. The provider is given the outline which it can then modify as needed.
        // In this case we set the outline to be an oval fitting the height and width.
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, getWidth(), getHeight());
            }
        });

        // Finally, enable clipping to the outline, using the provider we set above
        setClipToOutline(true);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    /**
     * Override performClick() so that we can toggle the checked state when the view is clicked
     */
    @Override
    public boolean performClick() {
        if (onClickListener != null) {
            onClickListener.onClick(this);
        }
        return super.performClick();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // As we have changed size, we should invalidate the outline so that is the the correct size
        invalidateOutline();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return super.onCreateDrawableState(extraSpace + 1);
    }

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changes.
     */
    public interface OnCheckedChangeListener {

        /**
         * Called when the checked state of a FAB has changed.
         *
         * @param fabView   The FAB view whose state has changed.
         * @param isChecked The new checked state of buttonView.
         */
        void onCheckedChanged(FloatingButton fabView, boolean isChecked);
    }
}
