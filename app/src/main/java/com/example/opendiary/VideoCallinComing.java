package com.example.opendiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;

public class VideoCallinComing extends AppCompatActivity {

    DatabaseReference referencecaller,referenceVc,vcref;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    String sender_url,sender_prof,sender_name,sender_uid,receiver_uid;
    VcModel model;
    ImageView imageView;
    FloatingActionButton declinebtn,acceptbtn;
    TextView tvname,tvprof;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_callin_coming);

        imageView = findViewById(R.id.iv_ic_vc);
        tvname = findViewById(R.id.name_vc_ic);
        tvprof = findViewById(R.id.prof_ic_vc);
        declinebtn = findViewById(R.id.decline_vc_ic);
        acceptbtn = findViewById(R.id.accept_vc_ic);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        receiver_uid = user.getUid();

        Bundle bundle = getIntent().getExtras();
        if (bundle!= null){
            sender_uid = bundle.getString("uid");

        }else {
            Toast.makeText(this, "Data missing", Toast.LENGTH_SHORT).show();
        }

        model = new VcModel();
        checkCallstatus();
        referencecaller = database.getReference("All Users").child(sender_uid);

        referencecaller.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    sender_name = snapshot.child("name").getValue().toString();
                    sender_url = snapshot.child("url").getValue().toString();
                    sender_prof = snapshot.child("prof").getValue().toString();

                    tvname.setText(sender_name);
                    Picasso.get().load(sender_url).into(imageView);
                    tvprof.setText(sender_prof);

                }else {
                    Toast.makeText(VideoCallinComing.this, "Cannot make call", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
         vcref  = FirebaseDatabase.getInstance().getReference("vc");

        referenceVc = database.getReference("vcref").child(sender_uid).child(receiver_uid);

        try {

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mp = MediaPlayer.create(getApplicationContext(),notification);
            mp.start();
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();

        }

        acceptbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String response = "yes";
                sendResponse(response);
                mp.stop();
                vcref.removeValue();
                referenceVc.removeValue();
            }
        });

        declinebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String response = "no";
                sendResponse(response);
                Intent intent = new Intent(VideoCallinComing.this,MessageActivity.class);
                startActivity(intent);
                vcref.removeValue();
                referenceVc.removeValue();
                mp.stop();
                finish();
            }
        });
    }

    private void checkCallstatus() {

        DatabaseReference cancelRef;
        cancelRef = database.getInstance().getReference("cancel");


        cancelRef.child(sender_uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    String response = snapshot.child("response").getValue().toString();

                    if (response.equals("no")){

                        Intent intent = new Intent(VideoCallinComing.this,MainActivity.class);
                        startActivity(intent);
                        mp.stop();
                        finish();


                    }else{

                    }

                }else {

                    // Toast.makeText(VideoCallOutgoing.this, "Not responding", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendResponse(String response) {

        if (response.equals("yes")){

            model.setKey(sender_name+receiver_uid);
            model.setResponse(response);
            referenceVc.child("res").setValue(model);
            joinmeeting();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    referenceVc.removeValue();
                }
            },3000);


        }else if (response.equals("no")){

            model.setKey(sender_name+receiver_uid);
            model.setResponse(response);
            referenceVc.child("res").setValue(model);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    referenceVc.removeValue();
                }
            },3000);

        }


    }

    private void joinmeeting() {


        try {

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(sender_name+receiver_uid)
                    .setWelcomePageEnabled(false)
                    .build();
            JitsiMeetActivity.launch(VideoCallinComing.this,options);
            finish();

        }catch (Exception e){

            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        String response = "no";
        sendResponse(response);
        Intent intent = new Intent(VideoCallinComing.this,MessageActivity.class);
        startActivity(intent);
        mp.stop();
        vcref.removeValue();
        finish();

    }
}