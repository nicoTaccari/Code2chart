package com.example.proyectofinal.code2chart;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;

public class CrearDiagrama extends AppCompatActivity implements View.OnClickListener{

    Button boton;
    TextView uriTexto;
    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        boton = (Button) findViewById(R.id.button2);
        boton.setOnClickListener(this);

        uriTexto = (TextView) findViewById(R.id.uri);
        uriTexto.setText("Rosa");

    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        uriTexto.setText(getPath(CrearDiagrama.this, uri));
                    } catch (Exception e) {
                        // Eat it
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_display_name" };
            Cursor cursor = null;

            try {

                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(0);

                    cursor.close();
                    return name;
                }

            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        showFileChooser();
    }
}