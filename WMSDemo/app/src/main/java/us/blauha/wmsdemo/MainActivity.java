package us.blauha.wmsdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

public class MainActivity extends android.support.v4.app.FragmentActivity {

    /*	GOAL:
     *  Display a WMS overlay from OSGEO on top of a google base map.
     *  (The data is a white map with state boundaries.)
     *
     *  Create a debugging Maps API Key and add it to the manifest.
     *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        TileProvider wmsTileProvider = TileProviderFactory.getOsgeoWmsTileProvider();
                        googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(wmsTileProvider));
                    }
                });
    }
}