package eu.selfhost.riegel.mapslib;

import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.MapViewPosition;

public class TouchGestureHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, Runnable {

    public TouchGestureHandler(MapsView mapView) {
        this.mapView = mapView;
        flinger = new Scroller(mapView.getContext());
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scaleFactorCumulative *= detector.getScaleFactor();
        mapView.getModel().mapViewPosition.setPivot(pivot);
        mapView.getModel().mapViewPosition.setScaleFactorAdjustment(scaleFactorCumulative);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!isScaleEnabled)
            return false;

        isInScale = true;
        scaleFactorCumulative = 1f;

        // Quick scale (no pivot)
        if (isInDoubleTap)
            pivot = null;
        else {
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            pivot = this.mapView.getMapViewProjection().fromPixels(focusX, focusY);
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        double zoomLevelOffset = Math.log(this.scaleFactorCumulative) / Math.log(2.0);
        byte zoomLevelDiff;
        if (Math.abs(zoomLevelOffset) > 1)
            // Complete large zooms towards gesture direction
            zoomLevelDiff = (byte)Math.round(zoomLevelOffset < 0 ? Math.floor(zoomLevelOffset) : Math.ceil(zoomLevelOffset));
        else
            zoomLevelDiff = (byte)Math.round(zoomLevelOffset);


        MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        if (zoomLevelDiff != 0 && pivot != null) {
            // Zoom with focus
            double moveHorizontal = 0.0;
            double moveVertical = 0.0;
            Point center = mapView.getModel().mapViewDimension.getDimension().getCenter();
            if (zoomLevelDiff > 0) {
                // Zoom in
                for (int i = 1; i <= zoomLevelDiff; i++) {
                    if (mapViewPosition.getZoomLevel() + i > mapViewPosition.getZoomLevelMax())
                        break;
                    moveHorizontal += (center.x - focusX) / Math.pow(2.0, i);
                    moveVertical += (center.y - focusY) / Math.pow(2.0, i);
                }
            } else {
                // Zoom out
                for (int i = -1; i >= zoomLevelDiff; i--) {
                    if (mapViewPosition.getZoomLevel() + i < mapViewPosition.getZoomLevelMax())
                        break;
                    moveHorizontal -= (center.x - focusX) / Math.pow(2.0, (i + 1));
                    moveVertical -= (center.y - focusY) / Math.pow(2.0, (i + 1));
                }
            }
            mapViewPosition.setPivot(pivot);
            mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
        } else
            // Zoom without focus
            mapViewPosition.zoom(zoomLevelDiff);

        isInDoubleTap = false;
    }

    @Override
    public void run() {
        boolean flingerRunning = !flinger.isFinished() && flinger.computeScrollOffset();
        mapView.getModel().mapViewPosition.moveCenter((flingLastX - flinger.getCurrX()), (flingLastY - flinger.getCurrY()));
        flingLastX = this.flinger.getCurrX();
        flingLastY = this.flinger.getCurrY();
        if (flingerRunning)
            this.handler.post(this);
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
                        byte zoomLevelDiff = 1;
                        double moveHorizontal = (center.x - e.getX()) / Math.pow(2.0, zoomLevelDiff);
                        double moveVertical = (center.y - e.getY()) / Math.pow(2.0, zoomLevelDiff);
                        LatLong pivot = mapView.getMapViewProjection().fromPixels(e.getX(), e.getY());
                        if (pivot != null) {
                            mapViewPosition.setPivot(pivot);
                            mapViewPosition.moveCenterAndZoom(moveHorizontal, moveVertical, zoomLevelDiff);
                        }
                    }
                    isInDoubleTap = false;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        isInScale = false;
        flinger.forceFinished(true);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!isInScale && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
            flinger.fling(0, 0, (int)-velocityX, (int)-velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            flingLastY = 0;
            flingLastX = flingLastY;
            handler.removeCallbacksAndMessages(null);
            handler.post(this);
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Normal or quick scale (no long press)
        if (!isInScale && !isInDoubleTap) {
            mapView.onCenter();
            Point tapXY = new Point(e.getX(), e.getY());
            LatLong tapLatLong = mapView.getMapViewProjection().fromPixels(tapXY.x, tapXY.y);
            if (tapLatLong != null) {
                for (int i = mapView.getLayerManager().getLayers().size() - 1; i >= 0; i--) {
                    Layer layer = mapView.getLayerManager().getLayers().get(i);
                    Point layerXY = mapView.getMapViewProjection().toPixels(layer.getPosition());
                    if (layer.onLongPress(tapLatLong, layerXY, tapXY))
                        break;
                }
            }
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!isInScale && e1.getPointerCount() == 1 && e2.getPointerCount() == 1) {
            mapView.getModel().mapViewPosition.moveCenter(-distanceX, -distanceY, false);
            mapView.onMove();
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Point tapXY = new Point(e.getX(), e.getY());
        LatLong tapLatLong = mapView.getMapViewProjection().fromPixels(tapXY.x, tapXY.y);
        if (tapLatLong != null) {
            for (int i = mapView.getLayerManager().getLayers().size() - 1; i >= 0; i--) {
                Layer layer = mapView.getLayerManager().getLayers().get(i);
                Point layerXY = mapView.getMapViewProjection().toPixels(layer.getPosition());
                if (layer.onTap(tapLatLong, layerXY, tapXY))
                    return true;
            }
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
