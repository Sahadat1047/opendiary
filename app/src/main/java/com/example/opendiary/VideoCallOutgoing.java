package com.example.opendiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

public class VideoCallOutgoing extends AppCompatActivity {

    ImageView imageView;
    TextView tvname,tvprof;
    FloatingActionButton declinebtn;
    String receiver_url,receive_prof,receiver_name,receiver_token,reciver_uid,sender_uid;
    DatabaseReference reference,reference_response,videocallref;
    VcModel model;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    VideoCallModel videoCallModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_outgoing);
        model = new VcModel();

        videoCallModel  = new VideoCallModel();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        sender_uid = user.getUid();
        imageView = findViewById(R.id.iv_og_vc);
        tvname = findViewById(R.id.name_vc_og);
        tvprof = findViewById(R.id.prof_og_vc);
        declinebtn = findViewById(R.id.decline_vc_og);



        Bundle bundle = getIntent().getExtras();
        if (bundle!= null){
            reciver_uid = bundle.getString("uid");

        }else {
            Toast.makeText(this, "Data missing", Toast.LENGTH_SHORT).show();
        }

        reference = database.getReference("All Users").child(reciver_uid);
        videocallref = FirebaseDatabase.getInstance().getReference("vc");
        reference_response = database.getReference("vcref").child(sender_uid).child(reciver_uid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    receiver_name = snapshot.child("name").getValue().toString();
                    receiver_url = snapshot.child("url").getValue().toString();
                    receive_prof = snapshot.child("prof").getValue().toString();

                    tvname.setText(receiver_name);
                    Picasso.get().load(receiver_url).into(imageView);
                    tvprof.setText(receive_prof);

                }else {
                    Toast.makeText(VideoCallOutgoing.this, "Cannot make call", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sendCallInvitation();

        checkResponse();

        declinebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelVC();
            }

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        cancelVC();
    }

    private void cancelVC() {
        DatabaseReference cancelRef;
        cancelRef = database.getInstance().getReference("cancel");

        model.setResponse("no");
        cancelRef.child(sender_uid).setValue(model);
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(VideoCallOutgoing.this,MessageActivity.class);
        startActivity(intent);
        reference_response.removeValue();
        videocallref.removeValue();
        finish();


    }

    private void checkResponse() {



        reference_response.child("res").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    String key = snapshot.child("key").getValue().toString();
                    String response = snapshot.child("response").getValue().toString();

                    if (response.equals("yes")){

                        joinmeeting(key);
                        Toast.makeText(VideoCallOutgoing.this, "Call Accepted", Toast.LENGTH_SHORT).show();

                    }else  if (response.equals("no")){
                        Toast.makeText(VideoCallOutgoing.this, "Call denied", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(VideoCallOutgoing.this,MessageActivity.class);
                        startActivity(intent);
                        finish();
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

    private void joinmeeting(String key) {

        try {

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(key)
                    .setWelcomePageEnabled(false)
                    .build();
            JitsiMeetActivity.launch(VideoCallOutgoing.this,options);
            finish();

        }catch (Exception e){

            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void sendCallInvitation() {

        FirebaseDatabase.getInstance().getReference("Token").child(reciver_uid).child("token").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                receiver_token = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        videoCallModel.setCalleruid(sender_uid);
        videocallref.child(reciver_uid).setValue(videoCallModel);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                FcmNotificationsSender notificationsSender =
                        new FcmNotificationsSender(receiver_token,"Social Book","Incoming Video Call",
                                getApplicationContext(),VideoCallOutgoing.this);

                notificationsSender.SendNotifications();

            }
        },1000);


    }
}