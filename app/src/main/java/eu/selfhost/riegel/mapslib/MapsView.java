package eu.selfhost.riegel.mapslib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
import org.mapsforge.map.view.MapView;

public class MapsView extends ViewGroup  {

    public MapsView(Context context) {
        super(context);
    }

    public MapsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public String test(String text) {
        return "Das wärs mit MapView: " + text;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
