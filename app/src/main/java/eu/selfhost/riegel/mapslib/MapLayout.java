package eu.selfhost.riegel.mapslib;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MapLayout extends LinearLayout {
    public MapLayout(Context context) {
        super(context);
    }

    public MapLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mapView = new MapsView(context);
    }

    public String testNeu(String text) {
        return "Das wärs mit MapLayout 2: " + text;
    }

    MapsView mapView;
}
