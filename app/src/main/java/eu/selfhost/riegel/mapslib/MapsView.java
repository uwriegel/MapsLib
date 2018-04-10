package eu.selfhost.riegel.mapslib;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

import org.mapsforge.core.graphics.GraphicContext;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.FrameBufferHA;
import org.mapsforge.map.view.FrameBufferHA2;
import org.mapsforge.map.view.MapView;

class MapsView extends ViewGroup implements MapView, Observer {

    public MapsView(Context context) {
        super(context);
        init(context);
    }

    public MapsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void onCenter() {
        setter.changeValue(true);
    }

    public void onDoubleTab() {
        setter.onDoubleTab();
    }

    public void onMove() {
        setter.changeValue(false);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MapsView.LayoutParams;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Child views (besides zoom controls)
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE && checkLayoutParams(child.getLayoutParams())) {
                MapsView.LayoutParams params = (MapsView.LayoutParams) child.getLayoutParams();
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                Point point = getMapViewProjection().toPixels(params.latLong);
                if (point != null) {
                    int childLeft = getPaddingLeft() + (int)Math.round(point.x);
                    int childTop = getPaddingTop() + (int)Math.round(point.y);
                    switch (params.alignment) {
                        case TOP_LEFT:
                            break;
                        case TOP_CENTER:
                            childLeft -= childWidth / 2;
                            break;
                        case TOP_RIGHT:
                            childLeft -= childWidth;
                            break;
                        case CENTER_LEFT:
                            childTop -= childHeight / 2;
                            break;
                        case CENTER:
                            childLeft -= childWidth / 2;
                            childTop -= childHeight / 2;
                            break;
                        case CENTER_RIGHT:
                            childLeft -= childWidth;
                            childTop -= childHeight / 2;
                            break;
                        case BOTTOM_LEFT:
                            childTop -= childHeight;
                            break;
                        case BOTTOM_CENTER:
                            childLeft -= childWidth / 2;
                            childTop -= childHeight;
                            break;
                        case BOTTOM_RIGHT:
                            childLeft -= childWidth;
                            childTop -= childHeight;
                            break;
                    }
                    child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        this.model.mapViewDimension.setDimension(new Dimension(width, height));
    }

    @Override
    public void addLayer(Layer layer) {
        layerManager.getLayers().add(layer);
    }

    @Override
    public void destroy() {
        touchGestureHandler.destroy();
        layoutHandler.removeCallbacksAndMessages(null);
        layerManager.finish();
        layerManager = null;
        frameBufferController.destroy();
        frameBuffer.destroy();
        if (mapScaleBar != null)
            mapScaleBar.destroy();
        model.mapViewPosition.destroy();
    }

    /**
     * Clear all map view elements.<br></br>
     * i.e. layers, tile cache, label store, map view, resources, etc.
     */
    @Override
    public void destroyAll() {
        Layers layers = layerManager.getLayers();
        for (Layer layer: layers) {
            layers.remove(layer);
            layer.onDestroy();
            if (layer instanceof TileLayer)
                ((TileLayer)layer).getTileCache().destroy();

            if (layer instanceof TileRendererLayer)
                ((TileRendererLayer)layer).getLabelStore().clear();
        }
        destroy();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, null, Alignment.BOTTOM_CENTER);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                null, Alignment.BOTTOM_CENTER);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return MapPositionUtil.getBoundingBox(model.mapViewPosition.getMapPosition(), getDimension(), model.displayModel.getTileSize());
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(getWidth(), getHeight());
    }

    @Override
    public FpsCounter getFpsCounter() {
        return fpsCounter;
    }

    @Override
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    @Override
    public LayerManager getLayerManager() {
        return layerManager;
    }

    @Override
    public MapScaleBar getMapScaleBar() {
        return mapScaleBar;
    }

    @Override
    public MapViewProjection getMapViewProjection() {
        return mapViewProjection;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void repaint() {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread()))
            invalidate();
        else
            postInvalidate();
    }

    @Override
    public void setCenter(LatLong center) {
        model.mapViewPosition.setCenter(center);
    }

    @Override
    public void setMapScaleBar(MapScaleBar mapScaleBar) {
        if (this.mapScaleBar != null)
            this.mapScaleBar.destroy();
        this.mapScaleBar = mapScaleBar;
    }

    @Override
    public void setZoomLevel(byte zoomLevel) {
        model.mapViewPosition.setZoomLevel(zoomLevel);
    }

    @Override
    public void setZoomLevelMax(byte zoomLevelMax) {
        model.mapViewPosition.setZoomLevelMax(zoomLevelMax);
    }

    @Override
    public void setZoomLevelMin(byte zoomLevelMin) {
        model.mapViewPosition.setZoomLevelMin(zoomLevelMin);
    }

    @Override
    public void onChange() {
        // Request layout for child views (besides zoom controls)
        for (int i = 0; i < getChildCount(); i++) {
            layoutHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();
                }
            });
            break;
        }
    }

    @Override
    protected void onDraw(Canvas androidCanvas) {
        org.mapsforge.core.graphics.Canvas graphicContext = AndroidGraphicFactory.createGraphicContext(androidCanvas);
        frameBuffer.draw(graphicContext);
        if (mapScaleBar != null) {
            mapScaleBar.draw(graphicContext);
        }
        fpsCounter.draw(graphicContext);
        graphicContext.destroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isClickable())
            return false;

        if (gestureDetectorExternal != null && gestureDetectorExternal.onTouchEvent(event))
            return true;

        boolean retVal = scaleGestureDetector.onTouchEvent(event);
        if (!scaleGestureDetector.isInProgress())
            retVal = this.gestureDetector.onTouchEvent(event);

        return retVal;
    }

    public void setLocationSetter(LocationSetter setter) {
        this.setter = setter;
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        gestureDetectorExternal = gestureDetector;
    }

    void init(Context context) {
        layoutHandler = new Handler();
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        setWillNotDraw(false);

        model = new Model();

        fpsCounter = new FpsCounter(GRAPHIC_FACTORY, model.displayModel);
        if (Parameters.FRAME_BUFFER_HA2)
            frameBuffer = new FrameBufferHA2(model.frameBufferModel, model.displayModel, GRAPHIC_FACTORY);
        else
            frameBuffer = new FrameBufferHA(model.frameBufferModel, model.displayModel, GRAPHIC_FACTORY);
        frameBufferController = FrameBufferController.create(this.frameBuffer, model);

        layerManager = new LayerManager(this, model.mapViewPosition, GRAPHIC_FACTORY);
        layerManager.start();
        LayerManagerController.create(layerManager, model);

        MapViewController.create(this, model);

        touchGestureHandler = new TouchGestureHandler(this);
        gestureDetector = new GestureDetector(context, touchGestureHandler);
        scaleGestureDetector = new ScaleGestureDetector(context, touchGestureHandler);

        mapScaleBar = new DefaultMapScaleBar(model.mapViewPosition, model.mapViewDimension,
                GRAPHIC_FACTORY, model.displayModel);
        mapViewProjection = new MapViewProjection(this);

        model.mapViewPosition.addObserver(this);
    }

    enum Alignment {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    class LayoutParams extends ViewGroup.LayoutParams
    {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            alignment = Alignment.BOTTOM_CENTER;
        }

        /**
         * Creates a new set of layout parameters for a child view of MapView.
         * @param width     the width of the child, either [.MATCH_PARENT], [.WRAP_CONTENT] or a fixed size in pixels.
         * @param height    the height of the child, either [.MATCH_PARENT], [.WRAP_CONTENT] or a fixed size in pixels.
         * @param latLong   the location of the child within the map view.
         * @param alignment the alignment of the view compared to the location.
         */
        public LayoutParams(int width, int height, LatLong latLong, Alignment alignment) {
            super(width, height);
            this.latLong = latLong;
            this.alignment = alignment;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        LatLong latLong;
        Alignment alignment;
    }

    private static AndroidGraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
    private Model model;
    private LayerManager layerManager;
    private FpsCounter fpsCounter;
    private FrameBuffer frameBuffer;
    private FrameBufferController frameBufferController;
    private GestureDetector gestureDetector;
    private GestureDetector gestureDetectorExternal;
    private Handler layoutHandler;
    private MapScaleBar mapScaleBar;
    private MapViewProjection mapViewProjection;
    private TouchGestureHandler touchGestureHandler;
    private LocationSetter setter;
    private ScaleGestureDetector scaleGestureDetector;
}
