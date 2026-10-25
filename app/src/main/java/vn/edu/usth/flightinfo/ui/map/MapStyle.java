package vn.edu.usth.flightinfo.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.views.MapView;

import vn.edu.usth.flightinfo.BuildConfig;
import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.util.AppExecutors;

/**
 * Shared styling, icons, and osmdroid initialization for night-blue maps.
 */
public final class MapStyle {

    private static final float[] NIGHT_BLUE_MATRIX = nightBlueMatrix();
    private static volatile boolean osmdroidConfigured;

    private MapStyle() {
    }

    private static float[] nightBlueMatrix() {
        // out = base + scale * (255 - luminance), per channel
        float[] scale = {0.20f, 0.33f, 0.60f};
        float[] base = {4f, 12f, 30f};
        float[] matrix = new float[20];
        for (int c = 0; c < 3; c++) {
            matrix[c * 5] = -0.299f * scale[c];
            matrix[c * 5 + 1] = -0.587f * scale[c];
            matrix[c * 5 + 2] = -0.114f * scale[c];
            matrix[c * 5 + 4] = base[c] + 255f * scale[c];
        }
        matrix[18] = 1f; // alpha unchanged
        return matrix;
    }

    /**
     * Applies the dark night-blue styling and background colors to the given MapView.
     */
    public static void applyNightStyle(MapView mapView) {
        if (mapView == null) {
            return;
        }
        Context context = mapView.getContext();
        mapView.getOverlayManager().getTilesOverlay()
                .setColorFilter(new ColorMatrixColorFilter(NIGHT_BLUE_MATRIX));
        int navy = ContextCompat.getColor(context, R.color.background);
        mapView.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(navy);
        mapView.getOverlayManager().getTilesOverlay().setLoadingLineColor(navy);
    }

    /**
     * Loads osmdroid disk configuration asynchronously before creating a MapView.
     */
    public static void loadConfigThen(Context context, Runnable onReady) {
        if (osmdroidConfigured) {
            onReady.run();
            return;
        }
        Context app = context.getApplicationContext();
        AppExecutors executors = AppExecutors.get();
        executors.io(() -> {
            IConfigurationProvider config = Configuration.getInstance();
            config.load(app, PreferenceManager.getDefaultSharedPreferences(app));
            config.setUserAgentValue(BuildConfig.APPLICATION_ID);
            osmdroidConfigured = true;
            executors.main(onReady);
        });
    }

    /**
     * Draws a white dot followed by the airport IATA code as a BitmapDrawable.
     */
    public static Drawable airportIcon(Context context, String iata) {
        float density = context.getResources().getDisplayMetrics().density;
        float dotRadius = 4 * density;
        float ring = 3 * density;
        float gap = 5 * density;

        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        text.setTextSize(11 * density);
        text.setColor(ContextCompat.getColor(context, R.color.text_secondary));

        float dotSize = 2 * (dotRadius + ring);
        int width = (int) Math.ceil(dotSize + gap + text.measureText(iata));
        int height = (int) Math.ceil(Math.max(dotSize, text.getFontSpacing()));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(ContextCompat.getColor(context, R.color.glass_stroke));
        float cx = dotSize / 2f;
        float cy = height / 2f;
        canvas.drawCircle(cx, cy, dotRadius + ring, dot);
        dot.setColor(ContextCompat.getColor(context, R.color.white));
        canvas.drawCircle(cx, cy, dotRadius, dot);
        Paint.FontMetrics fm = text.getFontMetrics();
        canvas.drawText(iata, dotSize + gap, cy - (fm.ascent + fm.descent) / 2f, text);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * Horizontal anchor fraction (0..1) of the dot's center inside an airport icon.
     */
    public static float dotAnchorU(Drawable icon, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return (4 + 3) * density / icon.getIntrinsicWidth();
    }
}
