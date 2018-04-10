package eu.selfhost.riegel.mapslib;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.util.ArrayList;

public class MapLayout extends LinearLayout {
    public MapLayout(Context context)
    {
        super(context);
        mapView = new MapsView(context);
        init(context);
    }

    public MapLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mapView = new MapsView(context, attrs);
        init(context);
    }

    void init(Context context) {
        preferencesFacade = new AndroidPreferences(context.getSharedPreferences(MapLayout.class.getSimpleName(), Context.MODE_PRIVATE));
        mapView.getModel().frameBufferModel.setOverdrawFactor(1.0);
        mapView.getModel().init(preferencesFacade);
        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true); // false

        createTileCaches();
        createLayers();

//        mapView.setLocationSetter(this);
        //mapView.getLayerManager().getLayers().add(currentTrack);
    }

    private void createTileCaches() {
        tileCaches.add(AndroidUtil.createTileCache(getContext(), MapLayout.class.getSimpleName(),
                mapView.getModel().displayModel.getTileSize(), 1.0f,
                mapView.getModel().frameBufferModel.getOverdrawFactor()));
    }

    private void createLayers() {
        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(tileCaches.get(0), mapView.getModel().mapViewPosition, getMapFile(),
                InternalRenderTheme.OSMARENDER, false, true, false);
        mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    private MapDataStore getMapFile() {
        return new MapFile(new File(getMapFileDirectory(), "germany.map"));
    }

    private File getMapFileDirectory() {
        String dir = getExternalStorageDirectory(getContext());
        return new File(dir + "/Maps");
    }

    private String getRootOfExternalStorage(File file, Context context) {
        return file.getAbsolutePath().replace("/Android/data/" + context.getPackageName() + "/files", "");
    }

    private String getExternalStorageDirectory(Context context) {
        File[] externalStorageFiles = ContextCompat.getExternalFilesDirs(context, null);
        for (File externalStorageFile : externalStorageFiles)
        {
            String file = getRootOfExternalStorage(externalStorageFile, context);
            if (file.contains("emulated"))
                return file;
        };
        return null;
    }

    private MapsView mapView;
    private PreferencesFacade preferencesFacade;
    private ArrayList<TileCache> tileCaches = new ArrayList<TileCache>();
    private boolean followLocation = true;
    private Location recentLocation = null;
    //private val currentTrack = TrackingLine(AndroidGraphicFactory.INSTANCE, false)
    //private TrackingLine loadedTrack = null;
    private boolean withBearing = false;
    private float bearing = 0F;
}
