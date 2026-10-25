package vn.edu.usth.flightinfo.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.model.FlightStatus;

/** Label and color of each flight status, applied to a status chip. */
public final class StatusUi {

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
        // Dark text on the yellow "delayed" chip for contrast, white elsewhere.
        chip.setTextColor(ContextCompat.getColor(chip.getContext(),
                status == FlightStatus.DELAYED ? R.color.brand_primary_dark : R.color.white));
    }

    /** Tints the chip background with a color resource. */
    public static void tint(TextView chip, @ColorRes int colorRes) {
        Context context = chip.getContext();
        ViewCompat.setBackgroundTintList(chip,
                ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)));
    }
}
