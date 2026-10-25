package vn.edu.usth.flightinfo.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.TextViewCompat;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.model.FlightStatus;

/** Label and color of each flight status, applied to a status chip. */
public final class StatusUi {

    /** Alpha of the chip fill and rim, so the glass behind stays visible. */
    private static final int FILL_ALPHA = 0x29;
    private static final int STROKE_ALPHA = 0x73;
    /** How much the label is mixed with white to stay readable on the tinted glass. */
    private static final float LABEL_LIGHTEN = 0.45f;

    private StatusUi() {
    }

    @StringRes
    public static int label(FlightStatus status) {
        switch (status) {
            case SCHEDULED: return R.string.status_scheduled;
            case DELAYED: return R.string.status_delayed;
            case EN_ROUTE: return R.string.status_en_route;
            case LANDED: return R.string.status_landed;
            case CANCELLED: return R.string.status_cancelled;
            default: return R.string.status_unknown;
        }
    }

    @ColorRes
    public static int color(FlightStatus status) {
        switch (status) {
            case SCHEDULED: return R.color.status_on_time;
            case DELAYED: return R.color.status_delayed;
            case EN_ROUTE: return R.color.status_en_route;
            case LANDED: return R.color.status_landed;
            case CANCELLED: return R.color.status_cancelled;
            default: return R.color.status_unknown;
        }
    }

    public static void bind(TextView chip, FlightStatus status) {
        chip.setText(label(status));
        tint(chip, color(status));
    }

    /**
     * Turns the chip into tinted glass: a translucent fill and rim of the color,
     * a solid dot before the label, and the label in a lighter shade of the color.
     */
    public static void tint(TextView chip, @ColorRes int colorRes) {
        Context context = chip.getContext();
        @ColorInt int color = ContextCompat.getColor(context, colorRes);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(context.getResources().getDimension(R.dimen.pill_radius));
        background.setColor(ColorUtils.setAlphaComponent(color, FILL_ALPHA));
        background.setStroke(Math.round(context.getResources().getDisplayMetrics().density),
                ColorUtils.setAlphaComponent(color, STROKE_ALPHA));
        chip.setBackground(background);

        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.bg_dot, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(chip, ColorStateList.valueOf(color));
        chip.setTextColor(ColorUtils.blendARGB(color, Color.WHITE, LABEL_LIGHTEN));
    }
}
