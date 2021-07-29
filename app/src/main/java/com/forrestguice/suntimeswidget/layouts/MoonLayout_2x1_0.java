/**
   Copyright (C) 2018-2021 Forrest Guice
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

package com.forrestguice.suntimeswidget.layouts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.calculator.MoonPhaseDisplay;
import com.forrestguice.suntimeswidget.calculator.SuntimesMoonData;
import com.forrestguice.suntimeswidget.calculator.core.SuntimesCalculator;
import com.forrestguice.suntimeswidget.settings.WidgetSettings;
import com.forrestguice.suntimeswidget.themes.SuntimesTheme;

import java.text.NumberFormat;

/**
 * Moonrise / Moonset / Phase (2x1)
 */
public class MoonLayout_2x1_0 extends MoonLayout
{
    protected int illumColor = Color.WHITE;
    protected int moonriseColor = Color.WHITE;
    protected int moonsetColor = Color.GRAY;

    public MoonLayout_2x1_0()
    {
        super();
    }

    @Override
    public void initLayoutID()
    {
        this.layoutID = R.layout.layout_widget_moon_2x1_0;
    }

    private WidgetSettings.RiseSetOrder order = WidgetSettings.RiseSetOrder.TODAY;

    @Override
    public void updateViews(Context context, int appWidgetId, RemoteViews views, SuntimesMoonData data)
    {
        super.updateViews(context, appWidgetId, views, data);
        boolean showLabels = WidgetSettings.loadShowLabelsPref(context, appWidgetId);
        boolean showSeconds = WidgetSettings.loadShowSecondsPref(context, appWidgetId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            if (WidgetSettings.loadScaleTextPref(context, appWidgetId))
            {
                int numRows = 2;
                int[] maxDp = new int[] {(maxDimensionsDp[0] - (paddingDp[0] + paddingDp[2] + 32)) / 2,
                        ((maxDimensionsDp[1] - (paddingDp[1] + paddingDp[3])) / numRows)};
                float maxSp = ClockLayout.CLOCKFACE_MAX_SP;
                float[] adjustedSizeSp = adjustTextSize(context, maxDp, paddingDp, "sans-serif", boldTime, (showSeconds ? "00:00:00" : "00:00"), timeSizeSp, maxSp, "MM", suffixSizeSp, iconSizeDp);
                if (adjustedSizeSp[0] > timeSizeSp)
                {
                    float textScale = Math.max(adjustedSizeSp[0] / timeSizeSp, 1);
                    float scaledPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textScale * 2, context.getResources().getDisplayMetrics());

                    views.setTextViewTextSize(R.id.text_time_moonrise, TypedValue.COMPLEX_UNIT_DIP, adjustedSizeSp[0]);
                    views.setTextViewTextSize(R.id.text_time_moonrise_suffix, TypedValue.COMPLEX_UNIT_DIP, adjustedSizeSp[1]);
                    views.setTextViewTextSize(R.id.text_time_moonset, TypedValue.COMPLEX_UNIT_DIP, adjustedSizeSp[0]);
                    views.setTextViewTextSize(R.id.text_time_moonset_suffix, TypedValue.COMPLEX_UNIT_DIP, adjustedSizeSp[1]);
                    views.setTextViewTextSize(R.id.text_info_moonillum, TypedValue.COMPLEX_UNIT_DIP, textScale * textSizeSp);
                    views.setTextViewTextSize(R.id.text_info_moonphase, TypedValue.COMPLEX_UNIT_DIP, textScale * textSizeSp);

                    views.setViewPadding(R.id.text_title, (int)(scaledPadding), 0, (int)(scaledPadding), 0);
                    views.setViewPadding(R.id.text_time_moonset_suffix, (int)(scaledPadding/2), 0, 0, (int)scaledPadding/2);
                    views.setViewPadding(R.id.icon_time_moonset, (int)(scaledPadding/2), 0, (int)scaledPadding/2, (int)scaledPadding/2);
                    views.setViewPadding(R.id.text_time_moonrise_suffix, (int)(scaledPadding/2), 0, 0, (int)scaledPadding/2);
                    views.setViewPadding(R.id.icon_time_moonrise, (int)(scaledPadding/2), 0, (int)scaledPadding/2, (int)scaledPadding/2);

                    views.setViewPadding(R.id.text_info_moonillum, (int)scaledPadding/2, 0, (int)scaledPadding, 0);

                    Drawable d1 = ResourcesCompat.getDrawable(context.getResources(), R.drawable.svg_sunrise1, null);
                    SuntimesUtils.tintDrawable(d1, moonriseColor);
                    views.setImageViewBitmap(R.id.icon_time_moonrise, SuntimesUtils.drawableToBitmap(context, d1, (int)adjustedSizeSp[2], (int)adjustedSizeSp[2] / 2, false));

                    Drawable d2 = ResourcesCompat.getDrawable(context.getResources(), R.drawable.svg_sunset1, null);
                    SuntimesUtils.tintDrawable(d2, moonsetColor);
                    views.setImageViewBitmap(R.id.icon_time_moonset, SuntimesUtils.drawableToBitmap(context, d2, (int)adjustedSizeSp[2], (int)adjustedSizeSp[2] / 2, false));

                    // TODO: scale moonphase icon
                }
            }
        }

        updateViewsMoonRiseSetText(context, views, data, showSeconds, order);

        NumberFormat percentage = NumberFormat.getPercentInstance();
        String illum = percentage.format(data.getMoonIlluminationToday());
        String illumNote = context.getString(R.string.moon_illumination, illum);
        SpannableString illumNoteSpan = (boldTime ? SuntimesUtils.createBoldColorSpan(null, illumNote, illum, illumColor) : SuntimesUtils.createColorSpan(null, illumNote, illum, illumColor));
        views.setTextViewText(R.id.text_info_moonillum, illumNoteSpan);

        for (MoonPhaseDisplay moonPhase : MoonPhaseDisplay.values()) {
            views.setViewVisibility(moonPhase.getView(), View.GONE);
        }

        MoonPhaseDisplay phase = data.getMoonPhaseToday();
        if (phase != null)
        {
            if (phase == MoonPhaseDisplay.FULL || phase == MoonPhaseDisplay.NEW) {
                SuntimesCalculator.MoonPhase majorPhase = (phase == MoonPhaseDisplay.FULL ? SuntimesCalculator.MoonPhase.FULL : SuntimesCalculator.MoonPhase.NEW);
                views.setTextViewText(R.id.text_info_moonphase, data.getMoonPhaseLabel(context, majorPhase));
            } else views.setTextViewText(R.id.text_info_moonphase, phase.getLongDisplayString());

            views.setViewVisibility(R.id.text_info_moonphase, (showLabels ? View.VISIBLE : View.GONE));
            views.setViewVisibility(phase.getView(), View.VISIBLE);

            Integer phaseColor = phaseColors.get(phase);
            if (phaseColor != null)
            {
                views.setTextColor(R.id.text_info_moonphase, phaseColor);
            }
        }
    }

    @Override
    public void themeViews(Context context, RemoteViews views, SuntimesTheme theme)
    {
        super.themeViews(context, views, theme);
        iconSizeDp = 22;   // override 32
        illumColor = theme.getTimeColor();
        moonriseColor = theme.getMoonriseTextColor();
        moonsetColor = theme.getMoonsetTextColor();

        themeViewsMoonPhase(context, views, theme);
        themeViewsMoonPhaseText(context, views, theme);
        themeViewsMoonPhaseIcons(context, views, theme);

        themeViewsMoonRiseSetText(context, views, theme);
        themeViewsMoonRiseSetIcons(context, views, theme);
    }

    @Override
    public void prepareForUpdate(Context context, int appWidgetId, SuntimesMoonData data)
    {
        order = WidgetSettings.loadRiseSetOrderPref(context, appWidgetId);
        this.layoutID = chooseMoonLayout(R.layout.layout_widget_moon_2x1_0, R.layout.layout_widget_moon_2x1_01, data, order);
    }
}

