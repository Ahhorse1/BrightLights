package com.example.thiswillwork;

import android.graphics.BitmapFactory;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static java.lang.Boolean.FALSE;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

// classes needed to launch navigation UI
import android.view.View;
import android.widget.Button;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener, PermissionsListener {
    // variables for adding location layer
    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Point destinationPoint;
    private Point originPoint;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    private DirectionsRoute[] currentRoute = new DirectionsRoute[4];
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    // variables needed to initialize navigation
    private Button button;
    private boolean infoBtnToggle=false;
    private Button routeOneBtn;
    private Button routeTwoBtn;
    private Button infoBtn;
    private ImageView logo;
    private TextView infoTxt;
    private int MapSelected=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/microchipsdontlie/ck7rr0nhg2n3l1iplh5ob4klm"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                Layer test = style.getLayer("road-primary-navigation");
                Layer test2 = style.getLayer("road-secondary-tertiary-navigation");
                enableLocationComponent(style);
                //addDestinationIconSymbolLayer(style);
                style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                        MainActivity.this.getResources(), R.drawable.custom_marker));
                mapboxMap.addOnMapClickListener(MainActivity.this);
                //addMarkers(style);
                button = findViewById(R.id.startButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean simulateRoute = true;
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute[MapSelected])
                                .shouldSimulateRoute(simulateRoute)
                                .build();
                        // Call this method with Context from within an Activity
                        NavigationLauncher.startNavigation(MainActivity.this, options);
                    }
                });
                routeOneBtn = findViewById(R.id.routeOne);
                logo = findViewById(R.id.logo);
                infoTxt = findViewById(R.id.infoTxt);
                routeTwoBtn = findViewById(R.id.routeTwo);
                infoBtn = findViewById(R.id.getInfo);
                infoBtn.setVisibility(View.VISIBLE);
                infoBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (infoBtnToggle == FALSE) {
                            mapView.setVisibility(View.GONE);
                            routeOneBtn.setVisibility(View.GONE);
                            routeTwoBtn.setVisibility(View.GONE);
                            button.setVisibility(View.GONE);
                            infoBtn.setText("Back");
                            logo.setVisibility(View.VISIBLE);
                            infoTxt.setVisibility(View.VISIBLE);
                        } else {
                            mapView.setVisibility(View.VISIBLE);
                            routeOneBtn.setVisibility(View.INVISIBLE);
                            routeTwoBtn.setVisibility(View.INVISIBLE);
                            button.setVisibility(View.VISIBLE);
                            button.setText("Tap a location to begin");
                            button.setBackgroundResource(R.color.colorPrimary);
                            button.setEnabled(false);
                            infoBtn.setText("About Bright Trails");
                            logo.setVisibility(View.GONE);
                            infoTxt.setVisibility(View.GONE);
                            clearRoute();
                        }
                        infoBtnToggle = !infoBtnToggle;
                        //logo.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("mapbox://styles/microchipsdontlie/ck7rr0nhg2n3l1iplh5ob4klm");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }
        clearRoute();
        button.setEnabled(false);
        button.setBackgroundResource(R.color.colorPrimary);
        button.setText("Select a priority");
        routeOneBtn.setVisibility(View.VISIBLE);
        routeOneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setEnabled(true);
                button.setBackgroundResource(R.color.mapboxBlue);
                button.setText("Start Navigation");
                getSafeRoute(originPoint,destinationPoint);
                MapSelected=0;
            }
        });
        routeTwoBtn.setVisibility(View.VISIBLE);
        routeTwoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setEnabled(true);
                button.setBackgroundResource(R.color.mapboxBlue);
                button.setText("Start Navigation");
                getQuickRoute(originPoint,destinationPoint);
                MapSelected=1;
            }
        });
        return true;
    }
    private void clearRoute (){
        if (navigationMapRoute != null) {
            navigationMapRoute.removeRoute();
        } else {
            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
        }
        navigationMapRoute.updateRouteVisibilityTo(false);
    }
    private void showRoute (DirectionsRoute r){
        // Draw the route on the map
        if (navigationMapRoute != null) {
            navigationMapRoute.removeRoute();
        } else {
            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
        }
        navigationMapRoute.addRoute(r);
    }
    private void getSafeRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        Toast.makeText(getApplicationContext(),
                                response.body().routes().size() + " route generated",
                                Toast.LENGTH_SHORT)
                                .show();
                        currentRoute[0] = response.body().routes().get(0);
                        showRoute(currentRoute[0]);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }
    private void getQuickRoute(Point origin, Point destination) {
                            NavigationRoute.builder(this)
                                    .accessToken(Mapbox.getAccessToken())
                                    .origin(origin)
                                    .destination(destination)
                                    .profile(DirectionsCriteria.PROFILE_CYCLING)
                                    .build()
                                    .getRoute(new Callback<DirectionsResponse>() {
                                        @Override
                                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                                            // You can get the generic HTTP info about the response
                                            Log.d(TAG, "Response code: " + response.code());
                                            Toast.makeText(getApplicationContext(),
                                                    response.body().routes().size() + " route generated",
                                                    Toast.LENGTH_SHORT)
                                                    .show();
                                            currentRoute[1] = response.body().routes().get(0);
                                            showRoute(currentRoute[1]);
                                        }
                                        @Override
                                        public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                                            Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
