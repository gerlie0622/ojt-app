package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.ScanMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Outbound extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private CodeScanner mCodeScanner;

    private TextView scannedBarcode, prdName, prdColor, prdSize, prdQty;
    private Button confirmBtn, manualSubtractBtn, clearBtn;
    private Shoe shoe;

    // Dictionary for scanned shoes
    Map<String, Shoe> inventory = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outbound);

        // Text Views
        scannedBarcode = findViewById(R.id.barcodeScan);
        prdName = findViewById(R.id.prdName);
        prdColor = findViewById(R.id.prdColor);
        prdSize = findViewById(R.id.prdSize);
        prdQty = findViewById(R.id.prdQty);

        // Buttons
        Button confirmBtn = findViewById(R.id.confirmBtn);
        Button manualSubtractBtn = findViewById(R.id.manualSubtractBtn);
        Button clearBtn = findViewById(R.id.clearBtn);

        // Text Fields
        EditText barcodeField = (EditText) findViewById(R.id.barcodeManualAdd);

        // Ask for permissions
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);

        mCodeScanner.setScanMode(ScanMode.SINGLE);
        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            scanResult(result.getText());
        }));

        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());

        // Confirm button to update the database
        confirmBtn.setOnClickListener(view -> updateDatabase());

        // Manually subtract item to database, if scanner is not working
        manualSubtractBtn.setOnClickListener(view -> {
            String barcodeFieldText = barcodeField.getText().toString();

            if(!barcodeFieldText.isEmpty() && barcodeFieldText.length() >= 12){
                scanResult(barcodeField.getText().toString());
            }
        });

        // Clear button for text field
        clearBtn.setOnClickListener(view -> {
            barcodeField.setText(null);
        });
    }

    // Process scan result
    private void scanResult(String result) {
        // Display scanned code
        scannedBarcode.setText((CharSequence) result);

        // Create or modify scanned shoe object
        if (inventory.containsKey(result)) {
            // Previously scanned shoe
            shoe = inventory.get(result);
        } else {
            // Newly scanned shoe
            String bounds = "outbound";

            if(!checkValidity(result)) { return; }
            inventory.put(result, (Shoe) createShoeObj(result, bounds));
            shoe = inventory.get(result);
            shoe.findData(result);

            shoe.setDatabaseListener(() -> updateInfo());
            return;
        }

        if(shoe.exists) {
            shoe.decrement();
            shoe.setDatabaseListener(() -> updateInfo());
        }
    }

    private Object createShoeObj(String result, String bounds) {
        try {
            Class<?> c = Class.forName("com.example.myapplication.Shoe");
            Constructor<?> con = c.getConstructor(String.class, String.class);
            return (Shoe) con.newInstance(result, bounds);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateInfo() {
        if(shoe.exists) {
            prdName.setText(shoe.name);
            prdColor.setText(shoe.color);
            prdSize.setText(shoe.size.toString());
            prdQty.setText(shoe.count.toString());
        }
    }

    private void updateDatabase() {
        for(String key : inventory.keySet()) {
            inventory.get(key).writeData();
        }
        Toast.makeText(this, "Database Updated", Toast.LENGTH_SHORT).show();
    }

    private boolean checkValidity(String code) {
        try {
            Integer.parseInt(code.substring(10, 12));
            return true;
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Invalid barcode", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    //region Method to check for permissions
    private void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(Outbound.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(Outbound.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(Outbound.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(Outbound.this, "Camera Permission Granted", Toast.LENGTH_SHORT) .show();
            else
                Toast.makeText(Outbound.this, "Camera Permission Required", Toast.LENGTH_SHORT) .show();
        }
    }
    //endregion

    //region Requirements
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
    //endregion
}