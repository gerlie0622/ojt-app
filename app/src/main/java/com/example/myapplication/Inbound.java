package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.*;

import org.w3c.dom.Text;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Inbound extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private CodeScanner mCodeScanner;

    private TextView barcodeScan;
    private TextView prdName, prdColor, prdSize, prdQty;
    private Shoe shoe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbound);

        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);

        mCodeScanner.setScanMode(ScanMode.SINGLE);
        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            scanResult(result.getText());
            UpdateInfo();
        }));

        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());

        Button btnTestButton = findViewById(R.id.testButton);
        Button confirmBtn = findViewById(R.id.confirmBtn);
        Button manualAddBtn = findViewById(R.id.manualAddBtn);

        // Button for testing
        btnTestButton.setOnClickListener(view -> scanResult("100013202"));

        // Confirm button to update the database
        confirmBtn.setOnClickListener(view -> UpdateDatabase());

        // Manually add item to database, if scanner is not working
        manualAddBtn.setOnClickListener(view -> {
            EditText barcodeText = (EditText) findViewById(R.id.barcodeManualAdd);
            scanResult(barcodeText.getText().toString());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(Inbound.this, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(Inbound.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(Inbound.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(Inbound.this, "Camera Permission Granted", Toast.LENGTH_SHORT) .show();
            else
                Toast.makeText(Inbound.this, "Camera Permission Required", Toast.LENGTH_SHORT) .show();
        }
    }

    Map<String, Shoe> inventory = new HashMap<>();
    @SuppressLint("SetTextI18n")
    public void scanResult(String result){
        barcodeScan = findViewById(R.id.barcodeScan);

        // Display scanned code
        barcodeScan.setText((CharSequence) result);

        // Create or edit shoe object
        if(inventory.containsKey(result)) {
            // Increment on existing inventory object
            shoe = inventory.get(result);
            Log.d("Inventory", "Old");
        } else {
            // Check if barcode is valid or invalid
            if(CheckItemDB(result)) {
                // Create new shoe object
                inventory.put(result, (Shoe) CreateShoeObj(result));
                shoe = inventory.get(result); // Checks item in database
                Log.d("Inventory", "New");
            } else {
                Toast.makeText(this, "ITEM NOT FOUND IN DATABASE", Toast.LENGTH_LONG).show();
                return;
            }
        }

        shoe.setDatabaseListener(new DatabaseListener() {
            @Override
            public void OnCallUpdateInfo() {
                UpdateInfo();
            }
        });
        UpdateInfo();
    }

    private Object CreateShoeObj(String result) {
        try {
            Class<?> c = Class.forName("com.example.myapplication.Shoe");
            Constructor<?> con = c.getConstructor(String.class);
            return (Shoe) con.newInstance(result);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean CheckItemDB(String result) {
        // Replace code with check if item exists in the database
        return result.length() == 12;
    }

    public void UpdateInfo() {
        prdQty = findViewById(R.id.prdQty);
        prdName = findViewById(R.id.prdName);
        prdColor = findViewById(R.id.prdColor);
        prdSize = findViewById(R.id.prdSize);

        prdName.setText(shoe.name);
        prdColor.setText(shoe.color);
        prdSize.setText(shoe.size.toString());
        if(shoe.name != null) {
            Objects.requireNonNull(shoe).count += 1;
        }
        prdQty.setText(shoe.count.toString());
    }

    private void UpdateDatabase() {
        for(String key : inventory.keySet()) {
            inventory.get(key).writeData();
        }
        Toast.makeText(this, "Database Updated", Toast.LENGTH_LONG).show();
    }
}