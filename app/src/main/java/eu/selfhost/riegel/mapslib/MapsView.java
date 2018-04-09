package eu.selfhost.riegel.mapslib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.ViewDebug;
import android.view.ViewGroup;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.TouchGestureHandler;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.FrameBufferHA;
import org.mapsforge.map.view.FrameBufferHA2;
import org.mapsforge.map.view.MapView;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

class MapsView extends ViewGroup implements MapView {

    public MapsView(Context context) {
        super(context);
        init();
    }

    public MapsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public String test(String text) {
        return "Das wärs mit MapView: " + text;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public void addLayer(Layer layer) {
        layerManager.getLayers().add(layer);
    }

    @Override
    public void destroy() {
        layerManager.finish();
        layerManager = null;
        model.mapViewPosition.destroy();
    }

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
        return null;
    }

    @Override
    public MapScaleBar getMapScaleBar() {
        return null;
    }

    @Override
    public MapViewProjection getMapViewProjection() {
        return null;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void repaint() {

    }

    @Override
    public void setCenter(LatLong center) {

    }

    @Override
    public void setMapScaleBar(MapScaleBar mapScaleBar) {

    }

    @Override
    public void setZoomLevel(byte zoomLevel) {

    }

    @Override
    public void setZoomLevelMax(byte zoomLevelMax) {

    }

    @Override
    public void setZoomLevelMin(byte zoomLevelMin) {

    }

    void init() {
        layoutHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {

            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        };
        //descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
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
        LayerManagerController.create(this.layerManager, model);

        MapViewController.create(this, model);

        //touchGestureHandler = new TouchGestureHandler(this);
        //this.gestureDetector = GestureDetector(context, touchGestureHandler)
        //this.scaleGestureDetector = ScaleGestureDetector(context, touchGestureHandler)

        this.mapScaleBar = new DefaultMapScaleBar(model.mapViewPosition, model.mapViewDimension,
                GRAPHIC_FACTORY, model.displayModel);
        mapViewProjection = new MapViewProjection(this);

        //model.mapViewPosition.addObserver(this);
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
}
