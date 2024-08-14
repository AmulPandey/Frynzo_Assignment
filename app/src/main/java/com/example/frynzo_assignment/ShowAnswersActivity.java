package com.example.frynzo_assignment;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ShowAnswersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_answers);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        if (isFirstTime) {
            Toast.makeText(this, "Long press on a row to delete form data", Toast.LENGTH_SHORT).show();
            prefs.edit().putBoolean("isFirstTime", false).apply();
        }

        TableLayout tableLayout = findViewById(R.id.tableLayout);

        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "form_data.json");
            if (!file.exists() || file.length() == 0) {
                Log.d("ShowAnswersActivity", "File does not exist or is empty");
                return;
            }

            FileReader fileReader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            fileReader.read(buffer);
            fileReader.close();
            String jsonData = new String(buffer);

            Log.d("ShowAnswersActivity", "JSON data: " + jsonData);

            JSONArray formDataArray = new JSONArray(jsonData);

            for (int i = 0; i < formDataArray.length(); i++) {
                JSONObject formData = formDataArray.getJSONObject(i);

                TableRow row = new TableRow(this);
                TextView q1TextView = new TextView(this);
                q1TextView.setText(String.valueOf(formData.getInt("Q1")));
                q1TextView.setTextColor(getResources().getColor(R.color.black));
                q1TextView.setPadding(0, 5, 0, 5);
                row.addView(q1TextView);

                TextView q2TextView = new TextView(this);
                q2TextView.setText(formData.getString("Q2"));
                q2TextView.setTextColor(getResources().getColor(R.color.black));
                q2TextView.setPadding(20, 5, 0, 5);
                row.addView(q2TextView);

                TextView recordingTextView = new TextView(this);
                recordingTextView.setText(formData.getString("recording"));
                recordingTextView.setTextColor(getResources().getColor(R.color.black));
                recordingTextView.setPadding(20, 5, 0, 5);
                row.addView(recordingTextView);

                TextView submissionTextView = new TextView(this);
                submissionTextView.setText(formData.getString("submit_time"));
                submissionTextView.setTextColor(getResources().getColor(R.color.black));
                submissionTextView.setPadding(20, 5, 0, 5);
                row.addView(submissionTextView);

                final int index = i;
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        showDeleteDialog(index, formDataArray);
                        return true;
                    }
                });

                tableLayout.addView(row);
            }
        } catch (IOException | JSONException e) {
            Log.e("ShowAnswersActivity", "Error reading or parsing JSON data", e);
        }
    }

    private void showDeleteDialog(final int index, final JSONArray formDataArray) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this form data?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    formDataArray.remove(index);
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "form_data.json");
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(formDataArray.toString());
                    fileWriter.close();
                    recreate(); // Refresh the activity to update the table layout
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}