package roru.bbooktransmitter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final List<Integer> bId = new ArrayList<>();
    private final List<String> bProvider = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private BeaconTransmitter beaconTransmitter;
    private Beacon beacon;
    private Switch toggle;
    private Spinner beaconSpinner;
    private String selectedBeaconId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconSpinner = findViewById(R.id.serviceProvider);
        toggle = findViewById(R.id.toggleTransmit);

        toggle.setEnabled(false);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        beaconSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                selectedBeaconId = bId.get(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (!bluetoothAdapter.isEnabled()) {
                        toggle.setChecked(false);
                        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
                    } else {
                        beaconSpinner.setEnabled(false);
                        initBeacon();
                        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
                            @Override
                            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                                super.onStartSuccess(settingsInEffect);
                                Toast.makeText(getApplicationContext(),
                                        "Started signal transmission", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onStartFailure(int errorCode) {
                                super.onStartFailure(errorCode);
                                destroyBeacon();
                                Toast.makeText(getApplicationContext(),
                                        "Failed to transmit signal", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    beaconSpinner.setEnabled(true);
                    destroyBeacon();
                    Toast.makeText(getApplicationContext(), "Signal transmission ended", Toast.LENGTH_SHORT).show();
                }
            }
        });
        startDatabase();
    }

    private void initBeacon() {
        String deviceUUID = "5fee6dd7-2999-4e61-a6b8-0ca0803a4269";
        beacon = new Beacon.Builder()
                .setId1(deviceUUID)
                .setId2(selectedBeaconId)
                .setId3("0")
                .setTxPower(-69)
                .setRssi(-56)
                .setDataFields(Collections.singletonList(0L))
                .build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);
        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        beaconTransmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
    }

    private void destroyBeacon() {
        if (beaconTransmitter != null) {
            beaconTransmitter.stopAdvertising();
        }
        beaconTransmitter = null;
        beacon = null;
    }

    private void startDatabase() {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("beacon")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                bId.add(Integer.parseInt(document.getId()));
                                bProvider.add(Objects.requireNonNull(document.get("title")).toString());
                            }
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, bProvider);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        beaconSpinner.setAdapter(adapter);
                        toggle.setEnabled(true);
                    }
                });
    }
}