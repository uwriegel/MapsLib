package eu.selfhost.riegel.mapslib;

import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.MapViewPosition;

public class TouchGestureHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, Runnable {

    public TouchGestureHandler(MapsView mapView) {
        this.mapView = mapView;
        flinger = new Scroller(mapView.getContext());
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public void run() {

    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isInDoubleTap = true;
                break;
            case MotionEvent.ACTION_UP:
                // Quick scale in between (cancel double tap)
                if (isInDoubleTap) {
                    MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
                    if (mapViewPosition.getZoomLevel() < mapViewPosition.getZoomLevelMax()) {
                        Point center = mapView.getModel().mapViewDimension.getDimension().getCenter();

                    }
                }
                break;

        }
        return false;
    }

    public void destroy() {
        handler.removeCallbacksAndMessages(null);
    }

    private MapsView mapView;
    private Scroller flinger;
    private int flingLastX;
    private int flingLastY;
    private float focusX;
    private float focusY;
    private Handler handler;
    private boolean isInDoubleTap;
    private boolean isInScale;
    private LatLong pivot = null;
    /**
     * Get state of scale gestures:<br></br>
     * - Scale<br></br>
     * - Scale with focus<br></br>
     * - Quick scale (double tap + swipe)
     */
    /**
     * Set state of scale gestures:<br></br>
     * - Scale<br></br>
     * - Scale with focus<br></br>
     * - Quick scale (double tap + swipe)
     */
    Boolean isScaleEnabled = true;
    private Float scaleFactorCumulative;
}
