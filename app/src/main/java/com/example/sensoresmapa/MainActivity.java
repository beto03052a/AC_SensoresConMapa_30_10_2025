package com.example.sensoresmapa;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.chip.Chip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
    }

    // Bus de estado (renombrado + distinto)
    public static class MotionBus extends ViewModel {
        private final MutableLiveData<Boolean> motionLive = new MutableLiveData<>(false);
        public void publish(boolean moving) { motionLive.setValue(moving); }
        public LiveData<Boolean> stream()   { return motionLive; }
    }

    //Fragmento Superior: Acelerómetro
    public static class AccelFrag extends Fragment implements SensorEventListener {

        private TextView xLabel, yLabel, zLabel;
        private Chip statusChip;
        private SensorManager mgr;
        private Sensor acc;
        private MotionBus bus;


        private final float[] last = new float[3];
        private boolean hasLast = false;
        private long lastUpdateMs = 0L;
        private static final float DELTA_THRESHOLD = 1.0f; // m/s^2
        private static final long MIN_INTERVAL_MS = 150L;  //

        @Nullable
        @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_sensor, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
            xLabel = v.findViewById(R.id.tvX);
            yLabel = v.findViewById(R.id.tvY);
            zLabel = v.findViewById(R.id.tvZ);
            statusChip = v.findViewById(R.id.tvState);

            bus = new ViewModelProvider(requireActivity()).get(MotionBus.class);
            mgr = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
            acc = (mgr != null) ? mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        }

        @Override public void onResume() {
            super.onResume();
            if (acc != null) mgr.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
        }

        @Override public void onPause() {
            super.onPause();
            if (mgr != null) mgr.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

            long now = System.currentTimeMillis();
            if (now - lastUpdateMs < MIN_INTERVAL_MS) return;
            lastUpdateMs = now;

            float x = e.values[0];
            float y = e.values[1];
            float z = e.values[2];

            xLabel.setText(String.format("X: %.2f", x));
            yLabel.setText(String.format("Y: %.2f", y));
            zLabel.setText(String.format("Z: %.2f", z));

            boolean moving;
            if (hasLast) {
                float dx = Math.abs(x - last[0]);
                float dy = Math.abs(y - last[1]);
                float dz = Math.abs(z - last[2]);
                float delta = Math.max(dx, Math.max(dy, dz));
                moving = delta > DELTA_THRESHOLD;
            } else {
                moving = false;
            }

            last[0] = x; last[1] = y; last[2] = z; hasLast = true;

            statusChip.setText(moving ? "Movimiento: SÍ" : "Movimiento: NO");
            statusChip.setTextColor(moving ? Color.parseColor("#FFFFFF") : Color.parseColor("#FFFFFF"));
            statusChip.setChipBackgroundColorResource(moving ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);

            bus.publish(moving);
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }

    // Fragmento Inferior: Google Map
    public static class GMapFrag extends Fragment implements OnMapReadyCallback {

        private GoogleMap map;
        private Marker pin;
        private MotionBus bus;

        private static final LatLng SCZ = new LatLng(-17.7833, -63.1821);
        // Radio aproximado para aleatoriedad (en grados). 0.1° ~ 11 km.
        private static final double RAND_DEGREES = 0.10;

        @Nullable
        @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_map, container, false);
            SupportMapFragment child = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            if (child != null) child.getMapAsync(this);
            return root;
        }

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);

            LatLng randomSantaCruz = randomSantaCruzLocation();

            pin = map.addMarker(new MarkerOptions()
                    .position(randomSantaCruz)
                    .title("Santa Cruz (inicial)")
                    .snippet("Esperando movimiento…")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(randomSantaCruz, 12.0f));

            bus = new ViewModelProvider(requireActivity()).get(MotionBus.class);
            bus.stream().observe(getViewLifecycleOwner(), moving -> {
                if (pin == null) return;
                if (Boolean.TRUE.equals(moving)) {
                    pin.setTitle("¡En movimiento!");
                    pin.setSnippet("Acelerómetro activo");
                    pin.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    pin.setAlpha(1.0f);
                    pin.showInfoWindow();
                } else {
                    pin.setTitle("Santa Cruz (reposo)");
                    pin.setSnippet("Sin movimiento detectado");
                    pin.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    pin.setAlpha(0.9f);
                }
            });
        }

        private LatLng randomSantaCruzLocation() {
            double latOffset = (Math.random() - 0.5) * 2.0 * RAND_DEGREES;
            double lngOffset = (Math.random() - 0.5) * 2.0 * RAND_DEGREES;
            return new LatLng(SCZ.latitude + latOffset, SCZ.longitude + lngOffset);
        }

        public void recenter() {
            LatLng target = (pin != null) ? pin.getPosition() : SCZ;
            if (map != null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 12.0f));
            }
        }
    }

    public void onRecenterClick(View v) {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragMap);
        if (f instanceof GMapFrag) {
            ((GMapFrag) f).recenter();
        }
    }
}
