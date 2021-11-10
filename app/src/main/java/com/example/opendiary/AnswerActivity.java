package com.example.opendiary;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AnswerActivity extends AppCompatActivity {


    String uid,que,postkey;
    EditText editText;
    Button button;
    AnswerMember member;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference Allquestions,ntref;
    String name,url,time,usertoken;
    DatabaseReference checkVideocallRef;
    String senderuid,userid;

    NewMember newMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        checkIncoming();
        newMember = new NewMember();
        member = new AnswerMember();
        editText = findViewById(R.id.answer_et);
        button = findViewById(R.id.btn_answer_submit);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            uid = bundle.getString("u");
            postkey = bundle.getString("p");
        }else {
            Toast.makeText(this, "Error ", Toast.LENGTH_SHORT).show();
        }

        Allquestions = database.getReference("AllQuestions").child(postkey).child("Answer");
        ntref = database.getReference("notification").child(uid);

        button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                saveAnswer();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void saveAnswer(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userid = user.getUid();
        String answer = editText.getText().toString();
        if (answer != null){

            Calendar cdate = Calendar.getInstance();
            SimpleDateFormat currentdate = new SimpleDateFormat("dd-MMMM-yyyy");
            final  String savedate = currentdate.format(cdate.getTime());

            Calendar ctime = Calendar.getInstance();
            SimpleDateFormat currenttime = new SimpleDateFormat("HH:mm:ss");
            final String savetime = currenttime.format(ctime.getTime());

            time = savedate +":"+ savetime;

            member.setAnswer(answer);
            member.setTime(time);
            member.setName(name);
            member.setUid(userid);
            member.setUrl(url);

            String id = Allquestions.push().getKey();
            Allquestions.child(id).setValue(member);


            newMember.setName(name);
            newMember.setText("Replied To your Question: " + answer);
            newMember.setSeen("no");
            newMember.setUid(userid);
            newMember.setUrl(url);


            String key = ntref.push().getKey();
            ntref.child(key).setValue(newMember);

            sendNotification(uid,name,answer);

            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "Please  write answer", Toast.LENGTH_SHORT).show();
        }



    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userid = user.getUid();
        FirebaseFirestore d = FirebaseFirestore.getInstance();
        DocumentReference reference;
        reference = d.collection("user").document(userid);

        reference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.getResult().exists()){
                             url = task.getResult().getString("url");
                             name = task.getResult().getString("name");

                        }else {
                            Toast.makeText(AnswerActivity.this, "error", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

    }

    public void checkIncoming() {

        checkVideocallRef = database.getReference("vc");


        try {

            checkVideocallRef.child(userid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        senderuid = snapshot.child("calleruid").getValue().toString();
                        Intent intent = new Intent(AnswerActivity.this, VideoCallinComing.class);
                        intent.putExtra("uid", senderuid);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {


                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } catch (Exception e) {

            //   Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

    }

        private void sendNotification(String uid, String name, String answer){

        FirebaseDatabase.getInstance().getReference().child(uid).child("token")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        usertoken = snapshot.getValue(String.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                FcmNotificationsSender notificationsSender =
                        new FcmNotificationsSender(usertoken,"Social Media",name+" Commented on your post: " + answer,
                                getApplicationContext(),AnswerActivity.this);

                notificationsSender.SendNotifications();

            }
        },3000);

    }
}