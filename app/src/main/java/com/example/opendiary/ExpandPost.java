package com.example.opendiary;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class ExpandPost extends AppCompatActivity {

    ImageView imageView;
    String url;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expand_post);

        imageView = findViewById(R.id.expand_post);

        Bundle bundle = getIntent().getExtras();
        if (bundle!= null){

            url = bundle.getString("url");
        }else {

            Toast.makeText(this, "No url received", Toast.LENGTH_SHORT).show();
        }

        Picasso.get().load(url).into(imageView);
    }
}