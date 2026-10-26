package vn.edu.usth.flightinfo.ui.detail;

import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.flightinfo.R;
import vn.edu.usth.flightinfo.model.Airport;
import vn.edu.usth.flightinfo.ui.map.MapStyle;
import vn.edu.usth.flightinfo.util.GeoUtils;

/**
 * Shared helper for drawing great-circle route polylines, airport markers,
 * and current aircraft position on both the detail card and full-screen map.
 */
public final class RouteMapHelper {

    private static final int ROUTE_POINTS = 64;

    private RouteMapHelper() {
    }

    /**
     * Renders a flight route on {@code mapView}.
     *
     * @param mapView MapView to configure
     * @param dep Departure airport
     * @param arr Arrival airport
     * @param progressPercent Flight progress percentage (0..100), or -1 if unknown
     */
    public static void setupRoute(MapView mapView, Airport dep, Airport arr, int progressPercent) {
        if (mapView == null || dep == null || arr == null) {
            return;
        }

        Context context = mapView.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        float strokeWidth = 3f * density;

        MapStyle.applyNightStyle(mapView);
        mapView.getOverlays().clear();

        // Generate points along the great circle
        int clampedPercent = Math.max(0, Math.min(100, progressPercent < 0 ? 0 : progressPercent));
        double fraction = clampedPercent / 100.0;

        List<GeoPoint> allPoints = new ArrayList<>(ROUTE_POINTS);
        for (int i = 0; i < ROUTE_POINTS; i++) {
            double f = (double) i / (ROUTE_POINTS - 1);
            double[] coords = GeoUtils.interpolate(dep.getLat(), dep.getLon(), arr.getLat(), arr.getLon(), f);
            allPoints.add(new GeoPoint(coords[0], coords[1]));
        }

        double[] planeCoords = GeoUtils.interpolate(dep.getLat(), dep.getLon(), arr.getLat(), arr.getLon(), fraction);
        GeoPoint planePoint = new GeoPoint(planeCoords[0], planeCoords[1]);

        // Split into flown (solid) and remaining (dashed) polylines
        List<GeoPoint> flownPoints = new ArrayList<>();
        List<GeoPoint> remainingPoints = new ArrayList<>();

        if (clampedPercent <= 0) {
            remainingPoints.addAll(allPoints);
        } else if (clampedPercent >= 100) {
            flownPoints.addAll(allPoints);
        } else {
            // Find transition segment
            int cutIndex = (int) Math.floor(fraction * (ROUTE_POINTS - 1));
            for (int i = 0; i <= cutIndex; i++) {
                flownPoints.add(allPoints.get(i));
            }
            flownPoints.add(planePoint);

            remainingPoints.add(planePoint);
            for (int i = cutIndex + 1; i < ROUTE_POINTS; i++) {
                remainingPoints.add(allPoints.get(i));
            }
        }

        // 1. Flown segment (solid, brand_primary_light)
        if (flownPoints.size() >= 2) {
            Polyline flownLine = new Polyline(mapView);
            flownLine.setPoints(flownPoints);
            flownLine.getOutlinePaint().setColor(ContextCompat.getColor(context, R.color.brand_primary_light));
            flownLine.getOutlinePaint().setStrokeWidth(strokeWidth);
            flownLine.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            flownLine.setInfoWindow(null);
            mapView.getOverlays().add(flownLine);
        }

        // 2. Remaining segment (dashed, text_hint)
        if (remainingPoints.size() >= 2) {
            Polyline remainingLine = new Polyline(mapView);
            remainingLine.setPoints(remainingPoints);
            remainingLine.getOutlinePaint().setColor(ContextCompat.getColor(context, R.color.text_hint));
            remainingLine.getOutlinePaint().setStrokeWidth(strokeWidth);
            remainingLine.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            remainingLine.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{10f * density, 8f * density}, 0));
            remainingLine.setInfoWindow(null);
            mapView.getOverlays().add(remainingLine);
        }

        // 3. Departure & Arrival airport markers
        Marker depMarker = new Marker(mapView);
        depMarker.setPosition(new GeoPoint(dep.getLat(), dep.getLon()));
        Drawable depIcon = MapStyle.airportIcon(context, dep.getIata());
        depMarker.setIcon(depIcon);
        depMarker.setAnchor(MapStyle.dotAnchorU(depIcon, context), Marker.ANCHOR_CENTER);
        depMarker.setInfoWindow(null);
        mapView.getOverlays().add(depMarker);

        Marker arrMarker = new Marker(mapView);
        arrMarker.setPosition(new GeoPoint(arr.getLat(), arr.getLon()));
        Drawable arrIcon = MapStyle.airportIcon(context, arr.getIata());
        arrMarker.setIcon(arrIcon);
        arrMarker.setAnchor(MapStyle.dotAnchorU(arrIcon, context), Marker.ANCHOR_CENTER);
        arrMarker.setInfoWindow(null);
        mapView.getOverlays().add(arrMarker);

        // 4. Aircraft marker at current position
        Marker planeMarker = new Marker(mapView);
        planeMarker.setPosition(planePoint);
        Drawable planeDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flight);
        if (planeDrawable != null) {
            planeDrawable = planeDrawable.mutate();
            planeDrawable.setTint(ContextCompat.getColor(context, R.color.brand_accent));
            planeMarker.setIcon(planeDrawable);
        }

        // Bearing for aircraft rotation:
        double heading;
        if (clampedPercent >= 100) {
            double[] prev = GeoUtils.interpolate(dep.getLat(), dep.getLon(), arr.getLat(), arr.getLon(), 0.98);
            heading = GeoUtils.bearing(prev[0], prev[1], arr.getLat(), arr.getLon());
        } else {
            double nextFraction = Math.min(1.0, fraction + 0.02);
            double[] next = GeoUtils.interpolate(dep.getLat(), dep.getLon(), arr.getLat(), arr.getLon(), nextFraction);
            heading = GeoUtils.bearing(planeCoords[0], planeCoords[1], next[0], next[1]);
        }
        planeMarker.setRotation((float) heading);
        planeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        planeMarker.setFlat(true);
        planeMarker.setInfoWindow(null);
        mapView.getOverlays().add(planeMarker);

        // 5. Frame map bounds with padding
        double north = Math.max(dep.getLat(), arr.getLat());
        double south = Math.min(dep.getLat(), arr.getLat());
        double east = Math.max(dep.getLon(), arr.getLon());
        double west = Math.min(dep.getLon(), arr.getLon());

        BoundingBox box = new BoundingBox(north, east, south, west);
        int marginPx = (int) (44 * density);

        Runnable zoomTask = () -> {
            try {
                mapView.zoomToBoundingBox(box, false, marginPx);
            } catch (Exception ignored) {
            }
        };

        if (mapView.getWidth() > 0 && mapView.getHeight() > 0) {
            zoomTask.run();
        } else {
            mapView.post(zoomTask);
        }

        mapView.invalidate();
    }
}
