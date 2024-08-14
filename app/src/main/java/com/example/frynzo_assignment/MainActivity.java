package com.example.frynzo_assignment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private static final int REQUEST_PERMISSIONS = 1;

    private EditText etAge;
    private Button btnTakeSelfie, btnSubmit;
    private File imageFile;
    private String geolocation;
    private MediaRecorder recorder;
    private File audioFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        etAge = findViewById(R.id.et_age);
        btnTakeSelfie = findViewById(R.id.btn_take_selfie);
        btnSubmit = findViewById(R.id.btn_submit);

        btnTakeSelfie.setOnClickListener(view -> dispatchTakePictureIntent());
        btnSubmit.setOnClickListener(view -> submitForm());

        // Start audio recording when typing age
        etAge.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                startRecording();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with the operation
            } else {
                // Permissions not granted, show a message to the user
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                imageFile = createImageFile();
            } catch (IOException ex) {
                // Handle error
            }
            if (imageFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        imageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1); // 1 for front camera
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            geotagImage();
        }
    }

    private void submitForm() {
        if (recorder!= null) {
            stopRecording();
        }
        if (etAge == null || etAge.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
            return;
        }
        int age = Integer.parseInt(etAge.getText().toString());
        String imagePath = imageFile.getAbsolutePath();
        String audioPath = audioFile.getAbsolutePath();
        String submitTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        JSONObject formData = new JSONObject();
        try {
            formData.put("Q1", age);
            formData.put("Q2", imagePath);
            formData.put("recording", audioPath);
            formData.put("submit_time", submitTime);
            formData.put("geolocation", geolocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        saveFormData(formData);
        goToShowAnswersActivity();
    }


    private void startRecording() {
        if (recorder!= null) {
            recorder.release();
            recorder = null;
        }
        audioFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.wav");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audioFile.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            // Handle the exception here, such as by showing a toast message or logging the error
            recorder = null; // Set recorder to null if an exception is thrown
        }
    }

    private void geotagImage() {
        // Get location and tag the image
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location!= null) {
                geolocation = location.getLatitude() + ", " + location.getLongitude();
            } else {
                geolocation = "Unknown location";
            }
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }


    private void saveFormData(JSONObject formData) {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "form_data.json");
        try {
            JSONArray formDataArray;
            if (file.exists()) {
                FileReader fileReader = new FileReader(file);
                char[] buffer = new char[(int) file.length()];
                fileReader.read(buffer);
                fileReader.close();
                String jsonData = new String(buffer);
                formDataArray = new JSONArray(jsonData);
            } else {
                formDataArray = new JSONArray();
            }
            formDataArray.put(formData);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(formDataArray.toString());
            fileWriter.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void goToShowAnswersActivity() {
        Intent intent = new Intent(this, ShowAnswersActivity.class);
        startActivity(intent);
    }

}
