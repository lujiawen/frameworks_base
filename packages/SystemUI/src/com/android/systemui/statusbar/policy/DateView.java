/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.android.systemui.R;

import java.util.Date;

public final class DateView extends TextView {
    private static final String TAG = "DateView";

    private boolean mAttachedToWindow;
    private boolean mWindowVisible;
    private boolean mUpdating;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NOTIFICATION_TOGGLE_COLOR_BAR),
                    false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        int mColorChanger = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_TOGGLE_COLOR_BAR, 0xFF33B5E5);

        setTextColor(mColorChanger);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        setUpdates();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        setUpdates();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisible = visibility == VISIBLE;
        setUpdates();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        setUpdates();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // makes the large background bitmap not force us to full width
        return 0;
    }

    private final void updateClock() {
        final Context context = getContext();
        Date now = new Date();
        CharSequence dow = DateFormat.format("EEEE", now);
        CharSequence date = DateFormat.getLongDateFormat(context).format(now);
        setText(context.getString(R.string.status_bar_date_formatter, dow, date));
    }

    private boolean isVisible() {
        View v = this;
        while (true) {
            if (v.getVisibility() != VISIBLE) {
                return false;
            }
            final ViewParent parent = v.getParent();
            if (parent instanceof View) {
                v = (View) parent;
            } else {
                return true;
            }
        }
    }

    private void setUpdates() {
        boolean update = mAttachedToWindow && mWindowVisible && isVisible();
        if (update != mUpdating) {
            mUpdating = update;
            if (update) {
                // Register for Intent broadcasts for the clock and battery
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_TIME_TICK);
                filter.addAction(Intent.ACTION_TIME_CHANGED);
                filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                mContext.registerReceiver(mIntentReceiver, filter, null, null);
                updateClock();
            } else {
                mContext.unregisterReceiver(mIntentReceiver);
            }
        }
    }
}
