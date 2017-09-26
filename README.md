[![DOI](https://zenodo.org/badge/50740613.svg)](https://zenodo.org/badge/latestdoi/50740613)

# Android Web Map Service

## Implemented Using a Google Maps Android API v2, TileLayer

### Android Weekly, 2/2013
Sam Halperin
email: [sam@samhalperin.com](mailto:sam@samhalperin.com)


*There is also a README-QuickStart.md in the root of the GitHub repo.*

![WMS Image](https://github.com/shalperin/android-wms/blob/master/designAssets/ptm.png?raw=true
)


*[PhillyTreeMap](http://www.phillytreemap.org) Android App (Released Spring 2013) showing WMS technique described in the article.*

## Intro

This brief article documents some early exploration into using WMS with the new Google Maps V2 API.  It is intended as a reference to help someone trying to get WMS tiles (IE from GeoServer) onto an Android map.

WMS is used to serve map tiles over HTTP by back end frameworks like GeoServer.  Some set of geo-referenced data, typically shape files or data stored in a PostGIS database, are returned as raster map tiles.  In the past, this data has been consumed by web applications using a client library such as Leaflet or OpenLayers.  With Google’s v2 mapping API for android, it is now relatively straightforward to build Android apps that combine WMS tiles with Google’s base maps and other data such as vector shapes and map markers.

For basic *getting started* info for v2 Maps, see the [Google Developer's site](https://developers.google.com/maps/documentation/android/) for the v2 API.  This article assumes a working v2 setup with the sample code running without error. After downloading the google play SDK and setting up the library make sure you can view the TileOverlayDemo. ($ANDROID_SDK_ROOT/extras/google/google_play_services/samples/maps)</p>

## Extending the UrlTileProvider class.
*Please refer to the sample code at the end of this post.*

The v2 API provides the UrlTileProvider class,  a partial implementation of the TileProvider class which allows developers to pull in map tiles by composing a URL string.  

The API to UrlTileProvider is its getTileUrl method.  To request a WMS tile, we override this method to compose the right URL, and the Android mapping SDK does the rest for us. It seems simple enough, but the problem is that the signature of getTileUrl which is
  
  getTileUrl(int x, int y, int zoom)

provides tile indexes (x and y) and a zoom level, but WMS requires that we provide a bounding box (xmin, ymin, xmax, ymax) in the request URL.  The x, y, zoom parameters provide us enough information to figure this out, but we have to do a little bit of math.


## Calculating the map bounds

We know the bounds of the entire map which is square. (roughly -20037508m to 20037508m in both directions using Web Mercator.  See the graphic below or the [map tiler site](http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/) for exact values.)    We don't use Latitude/Longitude though, because it is unprojected, and this will cause map distortions.


From the [google api docs for TileOverlay](https://developers.google.com/maps/documentation/android/reference/com/google/android/gms/maps/model/TileOverlay)

"Note that the world is projected using the Mercator projection (see http://en.wikipedia.org/wiki/Mercator_projection) with the left (west) side of the map corresponding to -180 degrees of longitude and the right (east) side of the map corresponding to 180 degrees of longitude. To make the map square, the top (north) side of the map corresponds to 85.0511 degrees of latitude and the bottom (south) side of the map corresponds to -85.0511 degrees of latitude. Areas outside this latitude range are not rendered."

### Dividing by the number of tiles for a given zoom level.

The number of tiles in either x or y at any zoom level is  n = 2^z.   With this, and the bounds of the map, we can figure out the size of the tile.  Using this information combined with the maps origin (see graphic) and the x, y, zoom data for a given tile, we can find out its bounding box.

Again from the [google api docs for TileOverlay](https://developers.google.com/maps/documentation/android/reference/com/google/android/gms/maps/model/TileOverlay):

+ At each zoom level, the map is divided into tiles and only the tiles that overlap the screen are downloaded and rendered. Each tile is square and the map is divided into tiles as follows:
  
+ At zoom level 0, one tile represents the entire world. The coordinates of that tile are (x, y) = (0, 0).
  
+ At zoom level 1, the world is divided into 4 tiles arranged in a 2 x 2 grid.</li>
  
+ ...

+ At zoom level N, the world is divided into 4N tiles arranged in a 2^N x 2^N grid."

![Map Bounds](https://github.com/shalperin/android-wms/blob/master/designAssets/web_merc.png?raw=true)

## Summary
+ zoom  level: z = [0..21] (See GoogleMap.get[Min|Max]ZoomLevel)
+ map size: S = 20037508.34789244 * 2 *This constant comes from converting the lat/long values above to EPSG:900913, Web Mercator. Again, see <a href="http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/">this page on maptiler</a> for a fantastic visual explanation.*
+ tile size = S / Math.pow(2, z) *So @ zoom level 0, S is the full map, at zoom level 1 there are 2x2 tiles, and at zoom 3 there are 8x8 tiles and so forth.*
+ tile origin = (-20037508.34789244, 20037508.34789244)
+ minX of the tiles bbox (for tile index x,y) = origin.x + x * S *Where x is the tile index in the east-west direction passed to the getTileUrl function discussed above.* 
+ maxX of the tiles bbox (for tile index x,y) = origin.x + (x+1) * S *x+1 because we are looking for the right edge of the tile.*
+ minY of the tiles bbox (for tile index x,y) = origin.y + y * S
+ maxY of the tiles bbox (for tile index x,y)= origin.y + (y+1) * S


## Demo Code

In addition to the following code snippets, please see the complete demo in this repository.

Here is a WMSTileProvider class, which inherits from UrlTileProvider.  It supports the above bounding box calculation.

  import com.google.android.gms.maps.model.UrlTileProvider;
 
  public abstract class WMSTileProvider extends UrlTileProvider {
  
    // Web Mercator n/w corner of the map.
    private static final double[] TILE_ORIGIN = {-20037508.34789244, 20037508.34789244};
    //array indexes for that data
    private static final int ORIG_X = 0; 
    private static final int ORIG_Y = 1; // "
    
    // Size of square world map in meters, using WebMerc projection.
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    
    // array indexes for array to hold bounding boxes.
    protected static final int MINX = 0;
    protected static final int MAXX = 1;
    protected static final int MINY = 2;
    protected static final int MAXY = 3;
    
    
    // Construct with tile size in pixels, normally 256, see parent class.
    public WMSTileProvider(int x, int y) {
        super(x, y);
    }
        
    
    // Return a web Mercator bounding box given tile x/y indexes and a zoom
    // level.
    protected double[] getBoundingBox(int x, int y, int zoom) {
        double tileSize = MAP_SIZE / Math.pow(2, zoom);
        double minx = TILE_ORIGIN[ORIG_X] + x * tileSize;
        double maxx = TILE_ORIGIN[ORIG_X] + (x+1) * tileSize;
        double miny = TILE_ORIGIN[ORIG_Y] - (y+1) * tileSize;
        double maxy = TILE_ORIGIN[ORIG_Y] - y * tileSize;
  
        double[] bbox = new double[4];
        bbox[MINX] = minx;
        bbox[MINY] = miny;
        bbox[MAXX] = maxx;
        bbox[MAXY] = maxy;
        
        return bbox;
    }}

You might use this class in a *factory* class as follows:

    import java.net.MalformedURLException;
    import java.net.URL;
    import java.util.Locale;
    import android.util.Log;
    import com.google.android.gms.maps.model.TileProvider;
 
      public class TileProviderFactory {
      
          private static final String GEOSERVER_FORMAT =
                "http://yourApp.org/geoserver/wms" +
                "?service=WMS" +
                "&version=1.1.1" +              
                "&request=GetMap" +
                "&layers=yourLayer" +
                "&bbox=%f,%f,%f,%f" +
                "&width=256" +
                "&height=256" +
                "&srs=EPSG:900913" +
                "&format=image/png" +               
                "&transparent=true";    
        
        // return a geoserver wms tile layer
        private static TileProvider getTileProvider() {
            TileProvider tileProvider = new WMSTileProvider(256,256) {
                
                @Override
                public synchronized URL getTileUrl(int x, int y, int zoom) {
                    double[] bbox = getBoundingBox(x, y, zoom);
                    String s = String.format(Locale.US, GEOSERVER_FORMAT, bbox[MINX], 
                            bbox[MINY], bbox[MAXX], bbox[MAXY]);
                    URL url = null;
                    try {
                        url = new URL(s);
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                    return url;
                }
            };
            return tileProvider;
          }
        
      }


So your Activity code (again see the sample Google maps code referenced above) would have the following calls to add the overlay:

    public class MapDisplay extends android.support.v4.app.FragmentActivity {
 
    //...
 
    private void setUpMap() {
          TileProvider tileProvider = TileProviderFactory.getTileProvider();
          mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));    
    }}



## Conclusion

This article presented a demonstration of a simple WMS client using Google's Android v2 Maps API.  It covered the math involved with converting from tile index/zoom level to Web Mercator bounding box, and showed how to compose a URL using these values and an instance of Google's UrlTileProvider class. 


### This article was covered in the following Google Maps Garage episode:


[![Google Maps Garage](https://github.com/shalperin/android-wms/blob/master/designAssets/google_maps_garage.png?raw=true)](https://www.youtube.com/watch?feature=player_embedded&v=U6ZbHAXPnhg)

