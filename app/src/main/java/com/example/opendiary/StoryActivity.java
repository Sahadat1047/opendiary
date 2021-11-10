package com.example.opendiary;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Calendar;

public class StoryActivity extends AppCompatActivity {

    ImageView imageView;
    Button button;
    EditText editText;
    ProgressBar progressBar;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference Allstory,userstory;
    UploadTask uploadTask;
    String posturi,url,name,type,currenUid;
    FirebaseStorage firebaseStorage;
    StorageReference storageReference;
    StoryMember storyMember;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference documentReference;
    DatabaseReference checkVideocallRef;
    String senderuid;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    String currentuid = user.getUid();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        storyMember = new StoryMember();
        button = findViewById(R.id.btn_storyUp);
        editText = findViewById(R.id.et_story_Up);
        progressBar = findViewById(R.id.pb_storyUp);
        imageView = findViewById(R.id.iv_storyup);

        checkIncoming();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currenUid = user.getUid();

        documentReference = db.collection("user").document(currenUid);

        storageReference =firebaseStorage.getInstance().getReference("story");

        Allstory = database.getReference("All story");
        userstory = database.getReference("story").child(currenUid);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            posturi = bundle.getString("u");
        }else {
            Toast.makeText(this, "no uri received", Toast.LENGTH_SHORT).show();
        }

        Picasso.get().load(posturi).into(imageView);

        button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                uploadStory();
            }
        });

    }
    private String getFileExt(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType((contentResolver.getType(uri)));
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void uploadStory(){

        String caption = editText.getText().toString();
        Calendar cdate = Calendar.getInstance();
        SimpleDateFormat currentdate = new SimpleDateFormat("dd-MMMM-yyyy");
        final  String savedate = currentdate.format(cdate.getTime());

        Calendar ctime = Calendar.getInstance();
        SimpleDateFormat currenttime = new SimpleDateFormat("HH:mm:ss");
        final String savetime = currenttime.format(ctime.getTime());

        String time = savedate +":"+ savetime;

        Uri uri = Uri.parse(posturi);
        if (TextUtils.isEmpty(caption) || uri != null){


            progressBar.setVisibility(View.VISIBLE);
            final StorageReference reference = storageReference.child(System.currentTimeMillis()+ "."+getFileExt(uri));
            uploadTask = reference.putFile(uri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw  task.getException();
                    }

                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    if (task.isSuccessful()){
                        Uri downloadUri = task.getResult();


                        long timeend = System.currentTimeMillis()+8640000;

                        storyMember.setCaption(caption);
                        storyMember.setName(name);
                        storyMember.setPostUri(downloadUri.toString());
                        storyMember.setUrl(url);
                        storyMember.setTimeEnd(timeend);
                        storyMember.setUid(currenUid);
                        storyMember.setTimeUpload(time);

                       // user story
                        String key = userstory.push().getKey();
                        userstory.child(key).setValue(storyMember);

                        // all story f4

                        Allstory.child(currenUid).setValue(storyMember);

                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StoryActivity.this, "Story uploaded", Toast.LENGTH_SHORT).show();


                    }

                }
            });

        }else {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
        }


    }


    @Override
    protected void onStart() {
        super.onStart();

        documentReference.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                        if (task.getResult().exists()){
                            name = task.getResult().getString("name");
                            url = task.getResult().getString("url");


                        }else {
                            Toast.makeText(StoryActivity.this, "Error", Toast.LENGTH_SHORT).show();

                        }

                    }
                });

    }

    public void checkIncoming(){

        checkVideocallRef = database.getReference("vc");


        try {

            checkVideocallRef.child(currentuid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()){

                        senderuid = snapshot.child("calleruid").getValue().toString();
                        Intent intent = new Intent(StoryActivity.this,VideoCallinComing.class);
                        intent.putExtra("uid",senderuid );
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }else {


                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }catch (Exception e){

            //   Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }


    }
}























