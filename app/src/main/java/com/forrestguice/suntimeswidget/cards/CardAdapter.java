/**
    Copyright (C) 2019 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/ 

package com.forrestguice.suntimeswidget.cards;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.SuntimesCalculatorDescriptor;
import com.forrestguice.suntimeswidget.calculator.SuntimesData;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetData;
import com.forrestguice.suntimeswidget.calculator.SuntimesRiseSetDataset;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.AppSettings;
import com.forrestguice.suntimeswidget.settings.SolarEvents;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder>
{
    private static SuntimesUtils utils = new SuntimesUtils();

    private WeakReference<Context> contextRef;
    private CardAdapterOptions options = new CardAdapterOptions();

    public CardAdapter(Context context)
    {
        contextRef = new WeakReference<>(context);
        initTheme(context);
        SuntimesUtils.initDisplayStrings(context);
        CardViewHolder.utils = utils;
    }

    private void initTheme(Context context)
    {
        int[] attrs = new int[] { android.R.attr.textColorPrimary, R.attr.buttonPressColor, R.attr.text_disabledColor, R.attr.tagColor_warning, R.attr.text_accentColor, R.attr.colorBackgroundFloating };
        TypedArray a = context.obtainStyledAttributes(attrs);
        options.color_textTimeDelta = ContextCompat.getColor(context, a.getResourceId(0, Color.WHITE));
        options.color_enabled = options.color_textTimeDelta;
        options.color_pressed = ContextCompat.getColor(context, a.getResourceId(1, R.color.btn_tint_pressed_dark));
        options.color_disabled = ContextCompat.getColor(context, a.getResourceId(2, R.color.text_disabled_dark));
        options.color_warning = ContextCompat.getColor(context, a.getResourceId(3, R.color.warningTag_dark));
        options.color_accent = ContextCompat.getColor(context, a.getResourceId(4, R.color.text_accent_dark));
        options.color_background = ColorUtils.setAlphaComponent(ContextCompat.getColor(context, a.getResourceId(5, R.color.transparent)), (int)(9d * (254d / 10d)));
        a.recycle();
    }

    public static final int MAX_POSITIONS = 2000;
    public static final int TODAY_POSITION = (MAX_POSITIONS / 2);      // middle position is today
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Pair<SuntimesRiseSetDataset, SuntimesMoonData>> data = new HashMap<>();

    @Override
    public int getItemCount() {
        return MAX_POSITIONS;
    }

    public Pair<SuntimesRiseSetDataset, SuntimesMoonData> initData(Context context)
    {
        Pair<SuntimesRiseSetDataset, SuntimesMoonData> retValue;
        data.clear();
        invalidated = false;
        options.init(context);
        initData(context, TODAY_POSITION - 1);
        retValue = initData(context, TODAY_POSITION);
        initData(context, TODAY_POSITION + 1);
        initData(context, TODAY_POSITION + 2);
        notifyDataSetChanged();
        return retValue;
    }

    public Pair<SuntimesRiseSetDataset, SuntimesMoonData> initData(Context context, int position)
    {
        Pair<SuntimesRiseSetDataset, SuntimesMoonData> dataPair = data.get(position);
        if (dataPair == null && !invalidated) {
            data.put(position, dataPair = createData(context, position));   // data is removed in onViewRecycled
        }
        return dataPair;
    }

    protected Pair<SuntimesRiseSetDataset, SuntimesMoonData> createData(Context context, int position)
    {
        Calendar date = Calendar.getInstance(options.timezone);
        if (options.dateMode != WidgetSettings.DateMode.CURRENT_DATE) {
            date.set(options.dateInfo.getYear(), options.dateInfo.getMonth(), options.dateInfo.getDay());
        }
        date.add(Calendar.DATE, position - TODAY_POSITION);

        SuntimesRiseSetDataset sun = new SuntimesRiseSetDataset(context);
        sun.setTodayIs(date);
        sun.calculateData();

        SuntimesMoonData moon = null;
        if (options.showMoon)
        {
            moon = new SuntimesMoonData(context, 0, "moon");
            moon.setTodayIs(date);
            moon.calculate();
        }

        return new Pair<>(sun, moon);
    }

    public int findPositionForDate(Context context, long dateMillis)
    {
        Pair<SuntimesRiseSetDataset, SuntimesMoonData> data_today = initData(context, TODAY_POSITION);
        Calendar today = Calendar.getInstance(data_today.first.timezone());
        today.setTimeInMillis(data_today.first.calendar().getTimeInMillis());
        today.set(Calendar.HOUR_OF_DAY, 12);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        Calendar date = Calendar.getInstance(data_today.first.timezone());
        date.setTimeInMillis(dateMillis);
        date.set(Calendar.HOUR_OF_DAY, 12);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);

        long delta = date.getTimeInMillis() - today.getTimeInMillis();
        double offset= delta / (1000d * 60d * 60d * 24d);
        return CardAdapter.TODAY_POSITION + (int) Math.round(offset);
    }

    /**
     * onViewRecycled
     * @param holder
     */
    @Override
    public void onViewRecycled(CardViewHolder holder)
    {
        detachClickListeners(holder);
        if (holder.position >= 0 && (holder.position < TODAY_POSITION - 1 || holder.position > TODAY_POSITION + 2)) {
            data.remove(holder.position);
        }
        holder.position = RecyclerView.NO_POSITION;
    }

    /**
     * onCreateViewHolder
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater layout = LayoutInflater.from(parent.getContext());
        View view = layout.inflate(R.layout.info_time_card1b, parent, false);
        return new CardViewHolder(view, options);
    }

    /**
     * onBindViewHolder
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(CardViewHolder holder, int position)
    {
        Context context = (contextRef != null ? contextRef.get() : null);
        if (context == null) {
            Log.w("CardAdapter", "onBindViewHolder: null context!");
            return;
        }
        if (holder == null) {
            Log.w("CardAdapter", "onBindViewHolder: null view holder!");
            return;
        }
        holder.bindDataToPosition(context, position, initData(context, position), options);
        attachClickListeners(holder, position);
    }

    /**
     * Highlight next occurring event (and removes any previous highlight).
     * @param event SolarEvents enum
     * @return the event's card position if event was found and highlighted, -1 otherwise
     */
    public int highlightField(Context context, SolarEvents event)
    {
        options.highlightEvent = null;
        options.highlightPosition = -1;

        Calendar[] eventCalendars;
        int position = TODAY_POSITION;
        do {
            Pair<SuntimesRiseSetDataset, SuntimesMoonData> dataPair = initData(context, position);
            SuntimesRiseSetDataset sun = ((dataPair == null) ? null : dataPair.first);
            SuntimesMoonData moon = ((dataPair == null) ? null : dataPair.second);
            Calendar now;

            boolean found = false;
            switch (event) {
                case MOONRISE: case MOONSET:
                    if (moon != null) {
                        now = moon.now();
                        eventCalendars = moon.getRiseSetEvents(event);  // { yesterday, today, tomorrow }
                        found = now.before(eventCalendars[1]) && now.after(eventCalendars[0]);
                    }
                    break;
                case SUNRISE: case SUNSET: case NOON:
                case MORNING_CIVIL: case EVENING_CIVIL: case MORNING_NAUTICAL: case EVENING_NAUTICAL: case MORNING_ASTRONOMICAL: case EVENING_ASTRONOMICAL:
                case MORNING_BLUE4: case EVENING_BLUE4: case MORNING_BLUE8: case EVENING_BLUE8: case MORNING_GOLDEN: case EVENING_GOLDEN:
                    if (sun != null) {
                        now = sun.now();
                        eventCalendars = sun.getRiseSetEvents(event);  // { today, tomorrow }
                        found = now.before(eventCalendars[0]);
                    }
                    break;
            }

            if (found) {
                options.highlightEvent = event;
                options.highlightPosition = position;
                break;
            }
            position++;
        } while (position < TODAY_POSITION + 2);

        notifyDataSetChanged();
        return options.highlightPosition;
    }

    private boolean invalidated = false;
    public void invalidateData()
    {
        invalidated = true;
        data.clear();
        notifyDataSetChanged();
    }

    /**
     * setThemeOverride
     * @param theme SuntimesTheme
     */
    public void setThemeOverride(@NonNull SuntimesTheme theme) {
        options.themeOverride = theme;
    }

    /**
     * setCardAdapterListener
     * @param listener
     */
    public void setCardAdapterListener( @NonNull CardAdapterListener listener ) {
        adapterListener = listener;
    }
    private CardAdapterListener adapterListener = new CardAdapterListener();

    private void attachClickListeners(@NonNull CardViewHolder holder, int position)
    {
        holder.txt_date.setOnClickListener(onDateClick(position));
        holder.txt_date.setOnLongClickListener(onDateLongClick(position));
        holder.sunriseHeader.setOnClickListener(onSunriseHeaderClick(position));
        holder.sunriseHeader.setOnLongClickListener(onSunriseHeaderLongClick(position));
        holder.sunsetHeader.setOnClickListener(onSunsetHeaderClick(position));
        holder.sunsetHeader.setOnLongClickListener(onSunsetHeaderLongClick(position));
        holder.moonClickArea.setOnClickListener(onMoonHeaderClick(position));
        holder.moonClickArea.setOnLongClickListener(onMoonHeaderLongClick(position));
        holder.lightmapLayout.setOnClickListener(onLightmapClick(position));
        holder.lightmapLayout.setOnLongClickListener(onLightmapLongClick(position));
        holder.btn_flipperNext.setOnClickListener(onNextClick(position));
        holder.btn_flipperPrev.setOnClickListener(onPrevClick(position));
    }

    private void detachClickListeners(@NonNull CardViewHolder holder)
    {
        holder.txt_date.setOnClickListener(null);
        holder.txt_date.setOnLongClickListener(null);
        holder.sunriseHeader.setOnClickListener(null);
        holder.sunriseHeader.setOnLongClickListener(null);
        holder.sunsetHeader.setOnClickListener(null);
        holder.sunsetHeader.setOnLongClickListener(null);
        holder.moonClickArea.setOnClickListener(null);
        holder.moonClickArea.setOnLongClickListener(null);
        holder.lightmapLayout.setOnClickListener(null);
        holder.lightmapLayout.setOnLongClickListener(null);
        holder.btn_flipperNext.setOnClickListener(null);
        holder.btn_flipperPrev.setOnClickListener(null);
    }

    private View.OnClickListener onDateClick(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onDateClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onDateLongClick(final int position) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onDateLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunriseHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunriseHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunriseHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunriseHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onSunsetHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onSunsetHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onSunsetHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onSunsetHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onMoonHeaderClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onMoonHeaderClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onMoonHeaderLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onMoonHeaderLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onLightmapClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onLightmapClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnLongClickListener onLightmapLongClick(final int position)
    {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return adapterListener.onLightmapLongClick(CardAdapter.this, position);
            }
        };
    }
    private View.OnClickListener onNextClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //adapterListener.onNextClick(CardAdapter.this, position);
                onCenterClick(position).onClick(v);
            }
        };
    }
    private View.OnClickListener onPrevClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //adapterListener.onPrevClick(CardAdapter.this, position);
                onCenterClick(position).onClick(v);
            }
        };
    }
    private View.OnClickListener onCenterClick(final int position) {
        return  new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterListener.onCenterClick(CardAdapter.this, position);
            }
        };
    }

    /**
     * CardAdapterListener
     */
    public static class CardAdapterListener
    {
        public void onDateClick(CardAdapter adapter, int position) {}
        public boolean onDateLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunriseHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunriseHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onSunsetHeaderClick(CardAdapter adapter, int position) {}
        public boolean onSunsetHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onMoonHeaderClick(CardAdapter adapter, int position) {}
        public boolean onMoonHeaderLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onLightmapClick(CardAdapter adapter, int position) {}
        public boolean onLightmapLongClick(CardAdapter adapter, int position)
        {
            return false;
        }

        public void onNextClick(CardAdapter adapter, int position) {}
        public void onPrevClick(CardAdapter adapter, int position) {}
        public void onCenterClick(CardAdapter adapter, int position) {}
    }

    /**
     * CardViewDecorator
     */
    public static class CardViewDecorator extends RecyclerView.ItemDecoration
    {
        private int marginPx;

        public CardViewDecorator( Context context ) {
            marginPx = (int)context.getResources().getDimension(R.dimen.activity_margin);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
        {
            outRect.left = outRect.right = marginPx;
            outRect.top = outRect.bottom = 0;
        }
    }

    /**
     * CardViewScroller
     */
    public static class CardViewScroller extends LinearSmoothScroller
    {
        private static final float MILLISECONDS_PER_INCH = 125f;

        public CardViewScroller(Context context) {
            super(context);
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }
    }

    /**
     * CardAdapterOptions
     */
    public static class CardAdapterOptions
    {
        private WidgetSettings.DateInfo dateInfo = null;
        public WidgetSettings.DateMode dateMode = WidgetSettings.DateMode.CURRENT_DATE;
        public TimeZone timezone = null;

        public boolean supportsGoldBlue = false;
        public boolean showSeconds = false;
        public boolean showWarnings = AppSettings.PREF_DEF_UI_SHOWWARNINGS;
        public boolean showMoon = AppSettings.PREF_DEF_UI_SHOWMOON;
        public boolean showLightmap = AppSettings.PREF_DEF_UI_SHOWLIGHTMAP;

        public boolean[] showFields = null;
        public boolean showActual = true;
        public boolean showCivil = true;
        public boolean showNautical = true;
        public boolean showAstro = true;
        public boolean showNoon = true;
        public boolean showGold = false;
        public boolean showBlue = false;

        public int showHeaders = 1;      // 0: icon + text, 1: icon-only, 2: text-only, 3: none

        public SuntimesTheme themeOverride = null;
        public int color_textTimeDelta, color_enabled, color_disabled, color_pressed, color_warning, color_accent, color_background;

        public int highlightPosition = -1;
        public SolarEvents highlightEvent = null;

        public void init(Context context)
        {
            dateMode = WidgetSettings.loadDateModePref(context, 0);
            dateInfo = WidgetSettings.loadDatePref(context, 0);

            SuntimesRiseSetData data0 = new SuntimesRiseSetData(context, 0);
            data0.initCalculator(context);
            data0.initTimezone(context);
            timezone = data0.timezone();

            supportsGoldBlue = data0.calculatorMode().hasRequestedFeature(SuntimesCalculator.FEATURE_GOLDBLUE);
            showSeconds = WidgetSettings.loadShowSecondsPref(context, 0);
            showWarnings = AppSettings.loadShowWarningsPref(context);
            showMoon = AppSettings.loadShowMoonPref(context);
            showLightmap = AppSettings.loadShowLightmapPref(context);

            showFields = AppSettings.loadShowFieldsPref(context);
            showActual = showFields[AppSettings.FIELD_ACTUAL];
            showCivil = showFields[AppSettings.FIELD_CIVIL];
            showNautical = showFields[AppSettings.FIELD_NAUTICAL];
            showAstro = showFields[AppSettings.FIELD_ASTRO];
            showNoon = showFields[AppSettings.FIELD_NOON];
            showGold = showFields[AppSettings.FIELD_GOLD] && supportsGoldBlue;
            showBlue = showFields[AppSettings.FIELD_BLUE] && supportsGoldBlue;
        }
    }
}