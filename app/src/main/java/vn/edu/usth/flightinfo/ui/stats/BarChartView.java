package vn.edu.usth.flightinfo.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.R;

/** Lightweight Canvas bar chart with touch selection; no chart dependency required. */
public class BarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Bar> bars = new ArrayList<>();
    private int selected = -1;
    private int primary;
    private int primaryLight;
    private int accent;
    private int labelColor;
    private int hintColor;
    private float density;
    private float chartTop;
    private float chartBottom;
    private float left;
    private float right;
    private float slotWidth;

    public BarChartView(Context context) { this(context, null); }
    public BarChartView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        primary = ContextCompat.getColor(context, R.color.brand_primary);
        primaryLight = ContextCompat.getColor(context, R.color.brand_primary_light);
        accent = ContextCompat.getColor(context, R.color.brand_accent);
        labelColor = ContextCompat.getColor(context, R.color.text_primary);
        hintColor = ContextCompat.getColor(context, R.color.text_hint);
        setMinimumHeight(dp(180));
        setContentDescription(context.getString(R.string.stats_chart_accessibility_empty));
        setClickable(true);
    }

    public void setData(List<Bar> data) {
        bars.clear();
        if (data != null) bars.addAll(data);
        selected = bars.isEmpty() ? -1 : 0;
        StringBuilder description = new StringBuilder(getContext().getString(R.string.stats_chart_accessibility));
        for (Bar bar : bars) description.append(' ').append(bar.label).append(": ").append(bar.value).append('.');
        setContentDescription(bars.isEmpty()
                ? getContext().getString(R.string.stats_chart_accessibility_empty) : description.toString());
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = dp(205);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) {
            paint.setShader(null);
            paint.setColor(hintColor);
            paint.setTextSize(sp(13));
            canvas.drawText(getContext().getString(R.string.stats_chart_empty), dp(8), dp(28), paint);
            return;
        }
        left = dp(8);
        right = getWidth() - dp(8);
        chartTop = dp(28);
        chartBottom = getHeight() - dp(42);
        slotWidth = (right - left) / bars.size();

        int max = 1;
        for (Bar bar : bars) max = Math.max(max, bar.value);
        paint.setShader(null);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0x55FFFFFF);
        canvas.drawLine(left, chartBottom, right, chartBottom, paint);

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            float barWidth = Math.min(dp(34), slotWidth * 0.56f);
            float centerX = left + slotWidth * (i + 0.5f);
            float barHeight = Math.max(dp(3), (chartBottom - chartTop - dp(16)) * bar.value / max);
            RectF rect = new RectF(centerX - barWidth / 2f, chartBottom - barHeight,
                    centerX + barWidth / 2f, chartBottom);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                    i == selected ? accent : primaryLight, i == selected ? accent : primary,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, dp(7), dp(7), paint);
            paint.setShader(null);

            paint.setColor(labelColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(sp(12));
            canvas.drawText(String.valueOf(bar.value), centerX, Math.max(dp(12), rect.top - dp(7)), paint);
            paint.setColor(hintColor);
            paint.setTextSize(sp(11));
            String label = bar.label.length() > 7 ? bar.label.substring(0, 7) : bar.label;
            canvas.drawText(label, centerX, getHeight() - dp(13), paint);
        }

        if (selected >= 0 && selected < bars.size()) {
            Bar bar = bars.get(selected);
            float centerX = left + slotWidth * (selected + 0.5f);
            float barWidth = Math.min(dp(34), slotWidth * 0.56f);
            float barHeight = Math.max(dp(3), (chartBottom - chartTop - dp(16)) * bar.value / max);
            float top = chartBottom - barHeight;
            RectF bubble = new RectF(centerX - dp(28), Math.max(dp(1), top - dp(31)),
                    centerX + dp(28), Math.max(dp(1), top - dp(7)));
            paint.setColor(accent);
            canvas.drawRoundRect(bubble, dp(8), dp(8), paint);
            paint.setColor(0xFF071B3D);
            paint.setTextSize(sp(12));
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(bar.value), bubble.centerX(), bubble.centerY() + dp(4), paint);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
        }
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && !bars.isEmpty()) {
            float x = event.getX();
            if (x >= left && x <= right && slotWidth > 0) {
                selected = Math.max(0, Math.min(bars.size() - 1, (int) ((x - left) / slotWidth)));
                performClick();
                invalidate();
                return true;
            }
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private int dp(float value) { return (int) (value * density + 0.5f); }
    private float sp(float value) { return value * getResources().getDisplayMetrics().scaledDensity; }

    public static final class Bar {
        public final String label;
        public final int value;
        public Bar(String label, int value) { this.label = label; this.value = value; }
    }
}
