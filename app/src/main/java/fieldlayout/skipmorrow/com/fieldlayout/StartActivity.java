package fieldlayout.skipmorrow.com.fieldlayout;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;


public class StartActivity extends Activity {

    private enum Direction {CW, CCW}
    private enum FirstSide {LONG, SHORT}

    private Location currentLocation;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE=1;
    private static LocationManager locationManager;
    private LocationListener locationListener;
    private Direction direction;
    private FirstSide firstside;
    FieldClass field;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("FieldLayout_StartAct", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_REQUEST_CODE);

    }


    // from https://stackoverflow.com/questions/33865445/gps-location-provider-requires-access-fine-location-permission-for-android-6-0
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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

        if (checkLocationPermission()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = 1F;
            getWindow().setAttributes(layout);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            //String dir = prefs.getString(getString(R.string.SP_DIRECTION), "CW");
            String dir = prefs.getString(getString(R.string.SP_DIRECTION), "CW");
            direction = Objects.equals(dir, "CW") ? Direction.CW : Direction.CCW;
            ColorDirectionButtons(direction);

            String fs = prefs.getString(getString(R.string.SP_FIRST_SIDE), "LONG");
            firstside = Objects.equals(fs, "LONG") ? FirstSide.LONG : FirstSide.SHORT;
            ColorFirstSideButtons(firstside);

            SetupFieldSpinner();
            Spinner spinner = findViewById(R.id.fieldtypedropdown);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.i("FieldLayout_StartAct", "SpinnerItemSelected");
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(getString(R.string.SP_DEFAULTFIELD), position);
                    editor.apply();
                    field = FieldClass.BuildFieldList().get(position);
                    UpdateFieldParametersTextView(field.get_index());
                    UpdateFieldImage();
                    CheckBox cbLayOutEndZones = findViewById(R.id.CbLayOutEndZones);
                    cbLayOutEndZones.setEnabled(field.get_bHasEndZone());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // Acquire a reference to the system Location Manager
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    Log.i("FieldLayout_StartAct", "Got a location update");
                    currentLocation = location;
                    UpdateGpsStatusText(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.i("FieldLayout_StartAct", "Status Changed: " + String.valueOf(status));
                }

                public void onProviderEnabled(String provider) {
                    Log.i("FieldLayout_StartAct", "onProviderEnabled");
                }

                public void onProviderDisabled(String provider) {}
            };

            // Register the listener with the Location Manager to receive location updates
            Log.i("FieldLayout_StartAct", "requesting Location Updates");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (currentLocation != null) {
                UpdateGpsStatusText(currentLocation);
            }
        } else {
            Toast.makeText(this, "Must enable GPS Permissions for this app", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }

    public void StartButtonClicked(View v) {
        double lat = currentLocation.getLatitude();
        double lng = currentLocation.getLongitude();
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra(getString(R.string.SP_START_LATITUDE), lat);
        intent.putExtra(getString(R.string.SP_START_LONGITUDE), lng);
        Log.i("FieldLayout_StartAct", "Start location lat = " + lat);
        Log.i("FieldLayout_StartAct", "Start location lng = " + lng);
        Spinner spinner = findViewById(R.id.fieldtypedropdown);
        int sel = spinner.getSelectedItemPosition();
        intent.putExtra(getString(R.string.SP_DEFAULTFIELD), sel);
        CheckBox cbLayoutEndZones = findViewById(R.id.CbLayOutEndZones);
        boolean bLayoutEndZones = cbLayoutEndZones.isChecked();
        intent.putExtra(getString(R.string.SP_LAYOUT_ENDZONES), bLayoutEndZones);
        startActivity(intent);
        locationManager.removeUpdates(locationListener);
    }

    private void ColorDirectionButtons (Direction d) {
        String strDirection = (d == Direction.CW ? "CW" : "CCW");
        Log.i("FieldLayout_StartAct", "Coloring Direction buttons: " + strDirection);
        Button btnCw = findViewById(R.id.BtnDir_CW);
        Button btnCcw = findViewById(R.id.BtnDir_CCW);
        btnCw.setBackgroundColor(d==Direction.CW ? Color.GREEN : Color.LTGRAY);
        btnCcw.setBackgroundColor(d==Direction.CCW ? Color.GREEN : Color.LTGRAY);
    }

    private void ColorFirstSideButtons (FirstSide fs) {
        String strFirstSide = (fs == FirstSide.LONG ? "LONG" : "SHORT");
        Log.i("FieldLayout_StartAct", "Coloring First Side buttons: " + strFirstSide);
        Button btnLong = findViewById(R.id.BtnFirstSideLong);
        Button btnShort = findViewById(R.id.BtnFirstSideShort);
        btnLong.setBackgroundColor(fs== FirstSide.LONG ? Color.GREEN : Color.LTGRAY);
        btnShort.setBackgroundColor(fs== FirstSide.SHORT ? Color.GREEN : Color.LTGRAY);
    }

    public void UpdateFieldImage() {
        boolean bEndZones = field.get_bHasEndZone();
        ImageView iv = findViewById(R.id.fieldimage);

        if (bEndZones && direction.equals(Direction.CW) && firstside.equals(FirstSide.LONG)) {iv.setImageResource(R.drawable.field_with_end_zone_cw_long);}
        if (bEndZones && direction.equals(Direction.CCW) && firstside.equals(FirstSide.LONG)) {iv.setImageResource(R.drawable.field_with_end_zone_ccw_long);}
        if (bEndZones && direction.equals(Direction.CW) && firstside.equals(FirstSide.SHORT)) {iv.setImageResource(R.drawable.field_with_end_zone_cw_short);}
        if (bEndZones && direction.equals(Direction.CCW) && firstside.equals(FirstSide.SHORT)) {iv.setImageResource(R.drawable.field_with_end_zone_ccw_short);}
        if (!bEndZones && direction.equals(Direction.CW) && firstside.equals(FirstSide.LONG)) {iv.setImageResource(R.drawable.field_without_end_zone_cw_long);}
        if (!bEndZones && direction.equals(Direction.CCW) && firstside.equals(FirstSide.LONG)) {iv.setImageResource(R.drawable.field_without_end_zone_ccw_long);}
        if (!bEndZones && direction.equals(Direction.CW) && firstside.equals(FirstSide.SHORT)) {iv.setImageResource(R.drawable.field_without_end_zone_cw_short);}
        if (!bEndZones && direction.equals(Direction.CCW) && firstside.equals(FirstSide.SHORT)) {iv.setImageResource(R.drawable.field_without_end_zone_ccw_short);}

    }

    public void UpdateFieldParametersTextView(int index) {
        TextView tv = findViewById(R.id.fieldproperties);
        FieldClass fc = FieldClass.GetFieldWithIndex(index);
        if (fc==null) {
            tv.setText(getString(R.string.field_index_missing, index));
        } else {
            String units = fc.get_unit() == Unit.YARDS ? " yards" : " meters";
            String s;
            s = "Field length = " + fc.get_fFieldLength() + units + System.getProperty("line.separator") +
                "Field Width = " + fc.get_fFieldWidth()  + units + System.getProperty("line.separator") +
                "Has End Zone? " + fc.get_bHasEndZone();
            if (fc.get_bHasEndZone()) {
                s = s + System.getProperty("line.separator") +
                        "End Zone Length: " + fc.get_fEndZoneLength() + units;
            }
            tv.setText (s);
        }
    }

    public void UpdateGpsStatusText(Location currentLocation) {
        TextView tv = findViewById(R.id.gps_fix_quality);

        if (currentLocation != null) {
            tv.setText(getString(R.string.gps_status, isGPSEnabled(), currentLocation.getAccuracy(), currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            tv.setText(getString(R.string.gps_status_null, isGPSEnabled()));
        }

        Button startButton = findViewById(R.id.start_button);
        startButton.setEnabled((currentLocation != null ? currentLocation.getAccuracy() : 100) < 15f);
    }

    public boolean isGPSEnabled(){
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    void SetupFieldSpinner() {
        Spinner fieldTypes = findViewById(R.id.fieldtypedropdown);
        ArrayList<String> fcl = FieldClass.GetFieldtypeList();
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fcl);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fieldTypes.setAdapter(dataAdapter);

        // Select the default field from the spinner. The default field is the last selected field,
        // or, if no fields have been selected (as in the first time run, just select the first field.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int defaultField = prefs.getInt(getString(R.string.SP_DEFAULTFIELD), 0);
        fieldTypes.setSelection(defaultField);

        boolean layoutEndZones = prefs.getBoolean(getString(R.string.SP_LAYOUT_ENDZONES), true);
        FieldClass fc = FieldClass.BuildFieldList().get(defaultField);
        CheckBox cbLayOutEndZones = findViewById(R.id.CbLayOutEndZones);
        cbLayOutEndZones.setEnabled(fc.get_bHasEndZone());
        cbLayOutEndZones.setChecked(layoutEndZones);
    }

    public void BtnCcwClicked(View view) {
        ColorDirectionButtons(Direction.CCW);
        direction = Direction.CCW;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.SP_DIRECTION), "CCW");
        editor.apply();
        UpdateFieldImage();
    }

    public void BtnCwClicked(View view) {
        ColorDirectionButtons(Direction.CW);
        direction = Direction.CW;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.SP_DIRECTION), "CW");
        editor.apply();
        UpdateFieldImage();
    }

    public void BtnLongClicked(View view) {
        ColorFirstSideButtons(FirstSide.LONG);
        firstside = FirstSide.LONG;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.SP_FIRST_SIDE), "LONG");
        editor.apply();
        UpdateFieldImage();
    }

    public void BtnShortClicked(View view) {
        ColorFirstSideButtons(FirstSide.SHORT);
        firstside = FirstSide.SHORT;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.SP_FIRST_SIDE), "SHORT");
        editor.apply();
        UpdateFieldImage();
    }

    public void CbLayOutEndZonesClicked(View view) {
        CheckBox cbLayOutEndZones = findViewById(R.id.CbLayOutEndZones);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(StartActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(getString(R.string.SP_LAYOUT_ENDZONES), cbLayOutEndZones.isChecked());
        editor.apply();
        UpdateFieldImage();
    }
}
