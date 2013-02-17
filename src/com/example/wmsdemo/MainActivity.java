package com.example.wmsdemo;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends android.support.v4.app.FragmentActivity {

	/*	GOAL:
	 *  Display a WMS overlay from OSGEO on top of a google base map.  
	 *  (The data is a white map with state boundaries.)
	 * 
	 *  GOTCHAS:
	 *  Add the google-play-services_lib as a build dependency
	 *  	Project=>Properties=>Android=>Library=>Add
	 *  
	 *  Create a debugging Maps API Key and add it to the manifest.
	 * 
	 */
	private GoogleMap mMap;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            } else {
            	Log.e("WMSDEMO", "Map was null!");
            }
        }       
    }
    
    private void setUpMap() {
    	TileProvider wmsTileProvider = TileProviderFactory.getOsgeoWmsTileProvider();
        TileOverlay wmsTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(wmsTileProvider));

        // Because the demo WMS layer we are using is just a white background map, switch the base layer
        // to satellite so we can see the WMS overlay.
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }
}