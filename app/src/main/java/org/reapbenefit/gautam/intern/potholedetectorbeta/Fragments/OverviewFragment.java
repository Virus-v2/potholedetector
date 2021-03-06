package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.maps.android.clustering.ClusterManager;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.APIService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.CustomClusterRenderer;
import org.reapbenefit.gautam.intern.potholedetectorbeta.DefinitePotholeCluster;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.MapStateManager;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OverviewFragment extends Fragment implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationSource.OnLocationChangedListener {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private OnFragmentInteractionListener mListener;
    private MapView mapView;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationRequest locationRequest;
    private Location currentLocation;
    private LatLng currentLatLng;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastLocation;
    private CameraPosition cameraPosition;
    private boolean zoomFlag;
    private static final String KEY_LOCATION = "keyLocation";
    private static final String CAMERA_POSITION = "cameraPosition";
    private FloatingActionButton starButton;
    private FloatingActionButton groupButton;
    private TripViewModel tripViewModel;
    private List<LatLng> potholeLocations = new ArrayList<>();
    private List<String> tripIds = new ArrayList<>();
    private List<LocalTripEntity> localTripEntities = new ArrayList<>();
    private List<LatLng> probableLatLngList = new ArrayList<>();
    private List<LatLng> definiteLatLngList = new ArrayList<>();
    private int locIndex;
    private LinearLayout bottomSheet;
    private TextView bottomSheetText;
    private SharedPreferences tripStatsPreferences;
    private RecyclerView highestPotholeListView;
    private Trip highestPotholeTrip;
    private TripListAdapter highestPotholeAdapter;
    private int definitePotholeCount;
    private int probablePotholeCount;
    private TextView startTimeTextView;
    private TextView countTextView;
    private TextView distanceTextView;
    private TextView sizeTextView;
    private GridLayout mostPotholesGrid;
    private FloatingActionButton floatingButton;
    private LatLng[] uniquePotholeLatLng;
    private int[] uniquePotholeHits;
    private int totalUniqueHits;
    private final String TAG = getClass().getSimpleName();
    private ClusterManager<DefinitePotholeCluster> definitePotholeClusterManager;
    private CoordinatorLayout overviewCoordinator;
    private int maxPotholeCount;

    private BroadcastReceiver uniquePotholesLatLngReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Log.d("OverviewFragment", "Broadcast received");
            uniquePotholeLatLng = (LatLng[]) intent.getParcelableArrayExtra(getString(R.string.global_unique_pothole_locations));
            uniquePotholeHits = intent.getIntArrayExtra(getString(R.string.hits_unique_potholes));
            int sum = 0;
            for (int i = 0; i < uniquePotholeHits.length; ++i) {
                sum += uniquePotholeHits[i];
            }
            totalUniqueHits = sum;
        }
    };

    private BroadcastReceiver newTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Log.d(TAG, "Broadcast received for newTrip");
            if (tripViewModel != null) {
                Trip newTrip = intent.getParcelableExtra("trip_object");
                if (newTrip.getDistanceInKM() < 0.5 && !BuildConfig.DEBUG) {
                    return;
                }
                tripViewModel.insert(Trip.tripToLocalTripEntity(newTrip));
                maxPotholeCount = tripStatsPreferences.getInt("maxPotholeCount", 0);
                if (newTrip.getProbablePotholeCount() + newTrip.getDefinitePotholeCount() >= maxPotholeCount) {
                    try {
                        highestPotholeTrip = newTrip;
                        maxPotholeCount = newTrip.getProbablePotholeCount() + newTrip.getDefinitePotholeCount();
                        tripStatsPreferences.edit().putString("highestPotholeTrip", new Gson().toJson(newTrip)).apply();
                        tripStatsPreferences.edit().putInt("maxPotholeTrip", maxPotholeCount).apply();
                        updateHPTData();
                    } catch (IllegalArgumentException illegal) {
                        // catches a crash in case a user hits zero potholes and creates a NaN trip
                    }
                }
            }
        }
    };

    private BroadcastReceiver definitePotholeLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Set<String> definitePotholeSet = new HashSet<>();
            Set<String> probablePotholeSet = new HashSet<>();
            definitePotholeSet = intent.getParcelableExtra(getString(R.string.highest_pothole_trip_definite_potholes));
            probablePotholeSet = intent.getParcelableExtra(getString(R.string.highest_pothole_trip_probable_potholes));
            if (((definitePotholeSet != null && !definitePotholeSet.isEmpty())) || (probablePotholeSet != null &&
                    !probablePotholeSet.isEmpty())) {
                if (definitePotholeSet.size() + probablePotholeSet.size() >= tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set),
                        new HashSet<String>()).size() + tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>()).size()) {
                    //definitePotholeSet represents location set of highestPotholeTrip
                    tripStatsPreferences.edit().putStringSet(getString(R.string.highest_pothole_trip_definite_potholes),
                            definitePotholeSet).apply();
                }
            }
        }
    };


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            definitePotholeCount = tripStatsPreferences.getInt("definitePotholes", 0);
            probablePotholeCount = tripStatsPreferences.getInt("probablePotholes", 0);
            int numberOfValidTrips = tripStatsPreferences.getInt("validTrips", 0);
            String bottomSheetString = numberOfValidTrips + (numberOfValidTrips == 1 ? " trip" : " trips") + " taken" +
                    "\n" + definitePotholeCount + " definite" + (definitePotholeCount == 1 ? " pothole" : " potholes") +
                    "\n" + probablePotholeCount + " probable" + (probablePotholeCount == 1 ? " pothole" : " potholes");
            bottomSheetText.setText(bottomSheetString);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
        tripViewModel.getAllTrips().observe(getActivity(), new android.arch.lifecycle.Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                if (localTripEntities != null)
                    OverviewFragment.this.localTripEntities = localTripEntities;
            }
        });

        // initialising TripViewModel for making database operations
        tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
        tripViewModel.getOfflineTrips().observe(this, new Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                Set<Trip> tripSet = new HashSet<>();
                for (LocalTripEntity offlineTripEntity: localTripEntities) {
                    Trip offlineTrip = Trip.localTripEntityToTrip(offlineTripEntity);
                    tripSet.add(offlineTrip);
                }
            }
        });
        SharedPreferences logoutPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        boolean loggedOut = logoutPreferences.getBoolean("loggedOut", false);
        if (loggedOut) {
            if (tripViewModel == null) {
                tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
            }
            tripViewModel.deleteAll();
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(definitePotholeLocationReceiver, new IntentFilter(getString(R.string.highest_pothole_latlngs_check)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent fetchGlobalPotholeIntent = new Intent(getContext(), APIService.class);
        fetchGlobalPotholeIntent.putExtra("request", "GET");
        fetchGlobalPotholeIntent.putExtra("table", "UniquePotholes");
        getContext().startService(fetchGlobalPotholeIntent);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        tripStatsPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        String highestPotholeTripJson = tripStatsPreferences.getString("highestPotholeTrip", null);
        // Log.d(getClass().getSimpleName(), highestPotholeTripJson + "");
        if (highestPotholeTripJson != null) {
            highestPotholeTrip = new Gson().fromJson(highestPotholeTripJson, Trip.class);
        }
        zoomFlag = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (googleMap != null) {
            outState.putParcelable(CAMERA_POSITION, googleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastLocation);
            super.onSaveInstanceState(outState);
        }
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }

    @SuppressLint("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_overview, container, false);
        overviewCoordinator = (CoordinatorLayout) fragmentView.findViewById(R.id.overview_coordinator);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(uniquePotholesLatLngReceiver, new IntentFilter(getString(R.string.global_unique_pothole_locations)));
        // registering BroadcastReceivers
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newTripReceiver, new IntentFilter(getString(R.string.new_trip_insert)));
        starButton = fragmentView.findViewById(R.id.personal_scores);
        groupButton = fragmentView.findViewById(R.id.group_scores);
        if (starButton.getVisibility() == View.VISIBLE)
            floatingButton = starButton;
        else
            floatingButton = groupButton;

        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
            // zoomFlag = true;
        }

        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleMap != null) {
                    definitePotholeClusterManager.clearItems();
                    definitePotholeClusterManager.cluster();
                    populatePersonalMap();
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12.0f));
                    // Log.d(TAG, "populating personal map");
                }
                groupButton.setVisibility(View.VISIBLE);
                starButton.setVisibility(View.INVISIBLE);
                floatingButton = groupButton;
            }
        });

        groupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupButton.setVisibility(View.INVISIBLE);
                starButton.setVisibility(View.VISIBLE);
                floatingButton = starButton;
                if (googleMap != null) {
                    definitePotholeClusterManager.clearItems();
                    definitePotholeClusterManager.cluster();
                    populateGlobalMap();
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18.0f));
                }
            }
        });


        bottomSheet = fragmentView.findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (floatingButton != null) {
                    // Log.d(TAG, String.valueOf((floatingButton.equals(groupButton))));
                    floatingButton.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
                }
            }
        });

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        mapView = fragmentView.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        if (mapView != null)
            mapView.getMapAsync((OnMapReadyCallback) this);

        definitePotholeCount = tripStatsPreferences.getInt("definitePotholes", 0);
        probablePotholeCount = tripStatsPreferences.getInt("probablePotholes", 0);

        bottomSheetText = fragmentView.findViewById(R.id.overview_sheet_text);
        int numberOfValidTrips = tripStatsPreferences.getInt("validTrips", 0);
        String bottomSheetString = numberOfValidTrips + (numberOfValidTrips == 1 ? " trip" : " trips") + " taken" +
                "\n" + definitePotholeCount + " definite" + (definitePotholeCount == 1 ? " pothole" : " potholes") +
                "\n" + probablePotholeCount + " probable" + (probablePotholeCount == 1 ? " pothole" : " potholes");
        bottomSheetText.setText(bottomSheetString);

        startTimeTextView = fragmentView.findViewById(R.id.start_time);
        countTextView = fragmentView.findViewById(R.id.hits);
        distanceTextView = fragmentView.findViewById(R.id.distance_view);
        sizeTextView = fragmentView.findViewById(R.id.size);
        if (highestPotholeTrip != null) {
            //adding details of highestPotholeTrip to GridLayout
            String startTime = highestPotholeTrip.getStartTime();
            startTime = startTime.substring(4, startTime.indexOf("GMT") - 4);
            startTimeTextView.setText(startTime);
            countTextView.setText(highestPotholeTrip.getProbablePotholeCount() +
                    highestPotholeTrip.getDefinitePotholeCount() + " potholes");
            distanceTextView.setText(TripListAdapter.roundTwoDecimals(highestPotholeTrip.getDistanceInKM()) + "km");
            sizeTextView.setText(TripListAdapter.humanReadableByteCount(highestPotholeTrip.getFilesize(), true));

            mostPotholesGrid = fragmentView.findViewById(R.id.highest_pothole_grid);
            mostPotholesGrid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getContext(), MapsActivity.class);
                    i.putExtra("trip", highestPotholeTrip);
                    i.putExtra(getString(R.string.is_viewing_highest_pothole_trip), true);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(i);
                }
            });
        }

        //getting Set<String> from SharedPreferences
        Set<String> probablePotholeLocationSet = tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>());
        Set<String> definitePotholeLocationSet = tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
        if (probablePotholeLocationSet != null) {
            List<String> probablePotholeLocationArrayList = new ArrayList<>(probablePotholeLocationSet);
            for (String potholeLocationString : probablePotholeLocationArrayList) {
                probableLatLngList.add(new Gson().fromJson(potholeLocationString, LatLng.class));
            }
        }
        // Log.d(getClass().getSimpleName(), probableLatLngList.toString());
        if (definitePotholeLocationSet != null) {
            List<String> definitePotholeLocationArrayList = new ArrayList<>(definitePotholeLocationSet);
            for (String definitePotholeLocationString : definitePotholeLocationArrayList) {
                definiteLatLngList.add(new Gson().fromJson(definitePotholeLocationString, LatLng.class));
            }
        }
        drawMarkers();
        // Log.d(getClass().getSimpleName(), definiteLatLngList.toString());
        return fragmentView;
    }

    private void updateHPTData() {
        if (highestPotholeTrip != null) {
            try {
                //adding details of highestPotholeTrip to GridLayout
                String startTime = highestPotholeTrip.getStartTime();
                startTime = startTime.substring(4, startTime.indexOf("GMT") - 4);
                startTimeTextView.setText(startTime);
                countTextView.setText(highestPotholeTrip.getProbablePotholeCount() +
                        highestPotholeTrip.getDefinitePotholeCount() + " potholes");
                distanceTextView.setText(TripListAdapter.roundTwoDecimals(highestPotholeTrip.getDistanceInKM()) + "km");
                sizeTextView.setText(TripListAdapter.humanReadableByteCount(highestPotholeTrip.getFilesize(), true));

                mostPotholesGrid.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(getContext(), MapsActivity.class);
                        i.putExtra("trip", highestPotholeTrip);
                        i.putExtra(getString(R.string.is_viewing_highest_pothole_trip), true);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(i);
                    }
                });
            } catch (NullPointerException nullPointerException) {
                // catches an NPE in case the broadcast is received and trip updated before view is created
            }
        }
    }

    private void drawMarkers() {
        if (starButton.getVisibility() == View.VISIBLE) {
            populateGlobalMap();
        } else {
            populatePersonalMap();
        }
    }

    private void populateGlobalMap() {
        if (!isInternetAvailable()) {
            Snackbar.make(overviewCoordinator, "Couldn't connect to the Internet.", Snackbar.LENGTH_SHORT).setAction("Settings", new SettingsButtonListener()).show();
            return;
        }
        if (googleMap != null && uniquePotholeLatLng != null) {
            for (int i = 0; i < uniquePotholeLatLng.length; ++i) {
                CustomClusterRenderer customClusterRenderer = new CustomClusterRenderer(getContext(), googleMap, definitePotholeClusterManager);
                customClusterRenderer.setHitPercentage(((double) uniquePotholeHits[i] / (double) (totalUniqueHits)) * 100.0);
                definitePotholeClusterManager.setRenderer(new CustomClusterRenderer(getContext(), googleMap, definitePotholeClusterManager));
                definitePotholeClusterManager.addItem(new DefinitePotholeCluster(uniquePotholeLatLng[i]));
            }
            definitePotholeClusterManager.cluster();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void populatePersonalMap() {
        if (googleMap != null && definitePotholeClusterManager != null) {
            for (LatLng potholeLocation : definiteLatLngList) {
                definitePotholeClusterManager.addItem(new DefinitePotholeCluster(potholeLocation));
                definitePotholeClusterManager.cluster();
            }
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                    @Override
                    public void onCameraMoveStarted(int i) {
                        if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                            //executed when the user changes map camera position
                            zoomFlag = false;
                        }
                    }
                });
                if (zoomFlag) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,
                            floatingButton.equals(groupButton)?18.0f:12.0f));
                }
            }
        };
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MILLISECONDS / 5);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (!(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        //setting up cluster manager
        definitePotholeClusterManager = new ClusterManager<>(getContext(), googleMap);
        googleMap.setOnCameraIdleListener(definitePotholeClusterManager);
        drawMarkers();
        googleApiClient = new GoogleApiClient.Builder(getActivity()).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (mapView != null) {
            if (googleMap != null && !(ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            mapView.onResume();
        }
        drawMarkers();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
        MapStateManager mapStateManager = new MapStateManager(getContext());
        if (googleMap != null)
            mapStateManager.saveMapState(googleMap);
        stopLocationUpdates();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(uniquePotholesLatLngReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newTripReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(definitePotholeLocationReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        if (!isConnected)
            return false;
        //TODO: HOW TO FIX THIS WHEN ASYNCTASK MIGHT TAKE TIME TO COMPLETE?
        //new CheckWifiNoInternetAsyncTask().execute();
        return true;
    }

    private class SettingsButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
        }
    }
}
