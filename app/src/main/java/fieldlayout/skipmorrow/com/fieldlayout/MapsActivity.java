package fieldlayout.skipmorrow.com.fieldlayout;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private Location startLocation;
    private Location currentLocation;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE=1;

    FieldClass field;

    Polyline fieldRectangle;
    Polyline endZoneLine1;
    Polyline endZoneLine2;

    private boolean rectangleLocked;
    private GoogleMap mMap;

    private enum Direction {CW, CCW}
    private Direction direction;

    private enum FirstSide {LONG, SHORT}
    private FirstSide firstside;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("FieldLayout_MapAct", "onCreate");
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);

        if (checkLocationPermission()) {
            Log.i("FieldLayout_MapAct", "Can access location. Good to go!");
            setContentView(R.layout.activity_maps);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = 1F;
            getWindow().setAttributes(layout);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Log.i("FieldLayout_MapAct", "mapFragment is null");
            }
            rectangleLocked = false;

            // Acquire a reference to the system Location Manager
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    //Log.i("FieldLayout_MapAct", "Got a location update");
                    currentLocation = location;
                    if (mMap != null) {
                        locationChanged(location);
                    } else {
                        //Log.i("MapsAct LocationChanged", "Location changed, but mMap is null so nothing is displayed");
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            // Register the listener with the Location Manager to receive location updates
            Log.i("FieldLayout_MapAct", "requesting Location Updates");
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            direction = prefs.getString(getString(R.string.SP_DIRECTION), "CW").equals("CW") ? Direction.CW : Direction.CCW;
            firstside = prefs.getString(getString(R.string.SP_FIRST_SIDE), "LONG").equals("LONG") ? FirstSide.LONG : FirstSide.SHORT;

            Button b = findViewById(R.id.btnChangeMapType);
            b.setText("Sat");
        }
        Log.i("FieldLayout_MapAct", "OnCreate complete");
    }

    // from https://stackoverflow.com/questions/33865445/gps-location-provider-requires-access-fine-location-permission-for-android-6-0
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0)
                        && !(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Must have GPS permissions for this app to work", Toast.LENGTH_LONG).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //iButtonSelected = extras.getInt("button_selected", 0);
            double lat = extras.getDouble(getString(R.string.SP_START_LATITUDE));
            double lng = extras.getDouble(getString(R.string.SP_START_LONGITUDE));
            int fieldNum = extras.getInt(getString(R.string.SP_DEFAULTFIELD));
            startLocation = new Location("");
            startLocation.setLatitude(lat);
            startLocation.setLongitude(lng);

            Log.i("MapsAct", "extras received.");
            Log.i("MapsAct", "lat = " + lat);
            Log.i("MapsAct", "lng = " + lng);
            Log.i("MapsAct", "fieldnum = " + fieldNum);

            field = FieldClass.GetFieldWithIndex(fieldNum);

            TextView tv = findViewById(R.id.tvInfo);
            if (field.get_unit().equals(Unit.METERS)) {
                tv.setText(getString(R.string.map_act_field_dims_meters, field.get_strFieldType(), field.get_fFieldLengthInMeters(), field.get_fFieldWidthInMeters()));
            } else {
                tv.setText(getString(R.string.map_act_field_dims_yards, field.get_strFieldType(), field.get_fFieldLengthInYards(), field.get_fFieldWidthInYards()));
            }
        }
    }


    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        super.onStop();
        mMap.clear();
        mMap = null;
        startLocation = null;
        currentLocation = null;
    }

    protected void DrawFieldRectangle(LatLng latLng) {
        //Log.i("FieldLayout_MapAct", "Drawing the field");
        if (fieldRectangle!=null) fieldRectangle.remove();
        if (endZoneLine1!=null) endZoneLine1.remove();
        if (endZoneLine2!=null) endZoneLine2.remove();
        LatLng startCorner = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
        Double startBearing = SphericalUtil.computeHeading(startCorner, latLng);
        Float fieldLengthMeters = field.get_fFieldLengthInMeters();
        Float fieldWidthMeters = field.get_fFieldWidthInMeters();
        Float endZoneLengthMeters = field.get_fEndZoneLengthInMeters();

        Button b = findViewById(R.id.btnMarkSecondCorner);

        LatLng corner1;
        LatLng corner2;
        LatLng corner7;
        LatLng corner8;
        LatLng endZoneCorner3;
        LatLng endZoneCorner4;
        LatLng endZoneCorner5;
        LatLng endZoneCorner6;
        LatLng firstStop;

        if (direction.equals(Direction.CW)) {
            if (firstside.equals(FirstSide.LONG)) {
                // 2-8-7-1, includes endzone, cw
                corner2 = startCorner;
                corner8 = SphericalUtil.computeOffset(startCorner, fieldLengthMeters + 2 * endZoneLengthMeters, startBearing);
                firstStop = corner8;
                corner7 = SphericalUtil.computeOffset(corner8, fieldWidthMeters, startBearing + 90f);
                corner1 = SphericalUtil.computeOffset(startCorner, fieldWidthMeters, startBearing + 90f);
                endZoneCorner3 = SphericalUtil.computeOffset(corner1, endZoneLengthMeters, startBearing);
                endZoneCorner4 = SphericalUtil.computeOffset(startCorner, endZoneLengthMeters, startBearing);
                endZoneCorner5 = SphericalUtil.computeOffset(corner1, endZoneLengthMeters + fieldLengthMeters, startBearing);
                endZoneCorner6 = SphericalUtil.computeOffset(startCorner, endZoneLengthMeters + fieldLengthMeters, startBearing);
                b.setEnabled(SphericalUtil.computeDistanceBetween(latLng, firstStop) < 15.0f);
            } else {
                // 1-2-8-7, start with short side
                corner1 = startCorner;
                corner2 = SphericalUtil.computeOffset(startCorner, fieldWidthMeters, startBearing);
                firstStop = corner2;
                corner8 = SphericalUtil.computeOffset(corner2, fieldLengthMeters + 2 * endZoneLengthMeters, startBearing + 90f);
                corner7 = SphericalUtil.computeOffset(corner8, fieldWidthMeters, startBearing - 180f);
                endZoneCorner3 = SphericalUtil.computeOffset(startCorner, endZoneLengthMeters, startBearing + 90f);
                endZoneCorner4 = SphericalUtil.computeOffset(corner2, endZoneLengthMeters, startBearing + 90f);
                endZoneCorner5 = SphericalUtil.computeOffset(corner7, endZoneLengthMeters, startBearing - 90f);
                endZoneCorner6 = SphericalUtil.computeOffset(corner8, endZoneLengthMeters, startBearing - 90f);
                b.setEnabled(SphericalUtil.computeDistanceBetween(latLng, firstStop) < 15.0f);
            }
        } else { // going CCW
            if (firstside.equals(FirstSide.LONG)) {
                // 1-7-8-2, includes endzone, ccw
                corner1 = startCorner;
                corner7 = SphericalUtil.computeOffset(startCorner, fieldLengthMeters + 2 * endZoneLengthMeters, startBearing);
                firstStop = corner7;
                corner8 = SphericalUtil.computeOffset(corner7, fieldWidthMeters, startBearing - 90f);
                corner2 = SphericalUtil.computeOffset(corner1, fieldWidthMeters, startBearing - 90f);
                endZoneCorner3 = SphericalUtil.computeOffset(startCorner, endZoneLengthMeters, startBearing);
                endZoneCorner4 = SphericalUtil.computeOffset(corner2, endZoneLengthMeters, startBearing);
                endZoneCorner5 = SphericalUtil.computeOffset(startCorner, endZoneLengthMeters + fieldLengthMeters, startBearing);
                endZoneCorner6 = SphericalUtil.computeOffset(corner2, endZoneLengthMeters + fieldLengthMeters, startBearing);
                b.setEnabled(SphericalUtil.computeDistanceBetween(latLng, firstStop) < 15.0f);
            } else {
                // 2-1-7-8, includes endzone, ccw
                corner2 = startCorner;
                corner1 = SphericalUtil.computeOffset(startCorner, fieldWidthMeters, startBearing);
                firstStop = corner1;
                corner7 = SphericalUtil.computeOffset(corner1, fieldLengthMeters + 2 * endZoneLengthMeters, startBearing - 90f);
                corner8 = SphericalUtil.computeOffset(corner7, fieldWidthMeters, startBearing - 180f);
                endZoneCorner3 = SphericalUtil.computeOffset(corner1, endZoneLengthMeters, startBearing - 90f);
                endZoneCorner4 = SphericalUtil.computeOffset(corner2, endZoneLengthMeters, startBearing - 90f);
                endZoneCorner5 = SphericalUtil.computeOffset(corner7, endZoneLengthMeters, startBearing + 90f);
                endZoneCorner6 = SphericalUtil.computeOffset(corner8, endZoneLengthMeters, startBearing + 90f);
                b.setEnabled(SphericalUtil.computeDistanceBetween(latLng, firstStop) < 15.0f);
            }
        }

        // enable the button if we are within 15 meters of corner2.

        fieldRectangle = mMap.addPolyline(new PolylineOptions()
                .add(corner1)
                .add(corner2)
                .add(corner8)
                .add(corner7)
                .add(corner1)
                .width(10)
                .color(Color.BLUE));
        if (field.get_bHasEndZone()) {
            endZoneLine1 = mMap.addPolyline(new PolylineOptions()
                    .add(endZoneCorner5)
                    .add(endZoneCorner6)
                    .width(10)
                    .color(Color.BLUE));
            endZoneLine2 = mMap.addPolyline(new PolylineOptions()
                    .add(endZoneCorner3)
                    .add(endZoneCorner4)
                    .width(10)
                    .color(Color.BLUE));
        }
        //CameraPosition oldPos = mMap.getCameraPosition();
        //CameraPosition pos = CameraPosition.builder(oldPos).bearing(startBearing.floatValue()).build();
        //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    public void BtnClicked(View v) {
        rectangleLocked = true;
        Button b = findViewById(R.id.btnMarkSecondCorner);
        b.setBackgroundColor(Color.GREEN);
    }

    public void BtnChangeMapTypeClicked(View v) {
        if (mMap != null) {
            Button b = findViewById(R.id.btnChangeMapType);
            mMap.setMapType(mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE ? GoogleMap.MAP_TYPE_NORMAL : GoogleMap.MAP_TYPE_SATELLITE);
            b.setText(mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE ? "Sat" : "Norm");
            CenterMap();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("maptype", mMap.getMapType());
            editor.apply();
        } else {
            Toast.makeText(this, "Map is not ready yet", Toast.LENGTH_LONG).show();
        }
    }

    // called from the onclick listener. It required a View parameter, so I don't use it from other methods here.
    // So all this does is call the CenterMap method. I also call the CenterMap method from other methods
    // so that method does all of the work.
    public void CenterMapOnSelf(View v) {
        CenterMap();
    }

    public void CenterMap() {
        if (currentLocation != null) {
            Log.i("FieldLayout_MapAct", "Centered map on latest GPS fix");
            CenterMapAtPosition(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else if (startLocation != null) {
            Log.i("FieldLayout_MapAct", "Centered map on the start location");
            CenterMapAtPosition(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()));
        }
    }

    public void CenterMapAtPosition(LatLng latLng) {
        double lat = latLng.latitude;
        double lng = latLng.longitude;
        Log.i("FieldLayout_MapAct", "Centered map");
        Log.i("FieldLayout_MapAct", "Start location lat = " + lat);
        Log.i("FieldLayout_MapAct", "Start location lng = " + lng);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .zoom(20.0f)
                .target(new LatLng(lat, lng))
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        //mMap.moveCamera(CameraUpdateFactory.zoomTo(20.0f));
    }

    // All of the things that need to be done when we get an updated location
    public void locationChanged(Location location) {
        //Log.i("FieldLayout_MapAct", "locationChanged");
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        double accuracy = location.getAccuracy();
        LatLng latLng = new LatLng(latitude, longitude);
        if (!rectangleLocked) {
            DrawFieldRectangle(latLng);
        }

        // add a breadcrumb circle on the map
        mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(0.5f)
                .strokeColor(Color.WHITE)
                .fillColor(Color.WHITE));

        // Update the status text on the map
        TextView tv = findViewById(R.id.tvInfo);
        if (field.get_unit().equals(Unit.YARDS)) {
            tv.setText(getString(R.string.map_act_status_yards,
                    field.get_strFieldType(),
                    field.get_fFieldLengthInYards(),
                    field.get_fFieldWidthInYards(),
                    accuracy,
                    SphericalUtil.computeDistanceBetween(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()), latLng) * 1.09361));
        } else {
            tv.setText(getString(R.string.map_act_status_meters,
                    field.get_strFieldType(),
                    field.get_fFieldLengthInYards(),
                    field.get_fFieldWidthInMeters(),
                    accuracy,
                    SphericalUtil.computeDistanceBetween(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()), latLng)));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("FieldLayout_MapAct", "onMapReady called. Map must be ready.");
        mMap = googleMap;
        if (mMap != null) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            DrawFieldRectangle(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()));
            CenterMap();
        } else {
            Log.i("MapsAct", "onMapReady called, but googleMap is null");
        }
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(startLocation.getLatitude(), startLocation.getLongitude())));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }
}
