package com.example.opendiary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView imageView;
    ImageButton sendbtn, cambtn, micbtn, vcbtn;
    TextView username, typingtv;
    EditText messageEt;
    UploadTask uploadTask;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference rootref1, rootref2, typingref, cancelRef;
    MessageMember messageMember;
    Boolean typingchecker = false;
    String receiver_name, receiver_uid, sender_uid, url, usertoken;
    DatabaseReference checkVideocallRef;
    String senderuid;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    String currentuid = user.getUid();

    MediaRecorder mediaRecorder;
    public static String filename = "recorded.3gp";
    String file = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + filename;


    Uri uri;
    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable screenshot in app
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_message);


        vcbtn = findViewById(R.id.btn_vc);

        checkIncoming();

        try {

            SharedPreferences sharedPreferences = getSharedPreferences("SharedPrefs",MODE_PRIVATE);




            final boolean isDarkModeOn = sharedPreferences.getBoolean("isDarkModeOn",false);


            if (isDarkModeOn){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

            }else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

            }
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();

        }


        cancelRef = database.getInstance().getReference("cancel");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            url = bundle.getString("u");
            receiver_name = bundle.getString("n");
            receiver_uid = bundle.getString("uid");
        } else {
            Toast.makeText(this, "user missing", Toast.LENGTH_SHORT).show();
        }

        cancelRef.removeValue();
        messageMember = new MessageMember();
        recyclerView = findViewById(R.id.rv_message);
        cambtn = findViewById(R.id.cam_sendmessage);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
        imageView = findViewById(R.id.iv_message);
        messageEt = findViewById(R.id.messageet);
        sendbtn = findViewById(R.id.imageButtonsend);
        username = findViewById(R.id.username_messageTv);
        micbtn = findViewById(R.id.btn_mic);
        typingtv = findViewById(R.id.typingstatus);

        Picasso.get().load(url).into(imageView);
        username.setText(receiver_name);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        sender_uid = user.getUid();

        rootref1 = database.getReference("Message").child(sender_uid).child(receiver_uid);
        rootref2 = database.getReference("Message").child(receiver_uid).child(sender_uid);
        typingref = database.getReference("typing");


        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

        mediaRecorder.setOutputFile(file);


        vcbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MessageActivity.this, VideoCallOutgoing.class);
                intent.putExtra("uid", receiver_uid);
                startActivity(intent);

            }
        });

        sendbtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                SendMessage();
            }
        });

        cambtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        micbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDialog();
            }
        });

        typingref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child(sender_uid).hasChild(receiver_uid)) {
                    typingtv.setVisibility(View.VISIBLE);

                } else {
                    typingtv.setVisibility(View.INVISIBLE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                Typing();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


    }

    private void Typing() {

        typingchecker = true;

        typingref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (typingchecker.equals(true)) {
                    if (snapshot.child(receiver_uid).hasChild(sender_uid)) {
                        typingchecker = false;
                    } else {
                        typingref.child(receiver_uid).child(sender_uid).setValue(true);
                        typingchecker = false;
                    }

                } else {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(MessageActivity.this);
        View view = inflater.inflate(R.layout.recoer_layout, null);
        TextView textView = view.findViewById(R.id.tv_record_status);
        Button start = view.findViewById(R.id.btn_start_rc);
        Button stop = view.findViewById(R.id.btn_stop_rc);
        Button send = view.findViewById(R.id.btn_send_rc);


        AlertDialog alertDialog = new AlertDialog.Builder(MessageActivity.this)
                .setView(view)
                .create();

        alertDialog.show();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                textView.setText("Audio recording.....");


            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                //   mediaRecorder.release();
                textView.setText("Recording Stopped");

            }
        });


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Uri audiofile = Uri.fromFile(new File(file));
                StorageReference storageReference = FirebaseStorage.getInstance().getReference("Audio files");
                final StorageReference reference = storageReference.child(System.currentTimeMillis() + filename);
                uploadTask = reference.putFile(audiofile);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        return reference.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();

                            Calendar cdate = Calendar.getInstance();
                            SimpleDateFormat currentdate = new SimpleDateFormat("dd-MMMM-yyyy");
                            final String savedate = currentdate.format(cdate.getTime());

                            Calendar ctime = Calendar.getInstance();
                            SimpleDateFormat currenttime = new SimpleDateFormat("HH:mm:ss");
                            final String savetime = currenttime.format(ctime.getTime());

                            String time = savedate + ":" + savetime;

                            long deletetime = System.currentTimeMillis();
                            messageMember.setDate(savedate);
                            messageMember.setTime(savetime);
                            messageMember.setAudio(downloadUri.toString());
                            messageMember.setReceiveruid(receiver_uid);
                            messageMember.setSenderuid(sender_uid);
                            messageMember.setType("a");
                            messageMember.setDelete(deletetime);

                            String id = rootref1.push().getKey();
                            rootref1.child(id).setValue(messageMember);

                            String id1 = rootref2.push().getKey();
                            rootref2.child(id1).setValue(messageMember);

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.dismiss();
                                    Toast.makeText(MessageActivity.this, "file sent", Toast.LENGTH_SHORT).show();
                                }
                            }, 1000);


                        } else {

                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == PICK_IMAGE || resultCode == RESULT_OK ||
                data != null || data.getData() != null) {
            uri = data.getData();

            String url = uri.toString();
            Intent intent = new Intent(MessageActivity.this, SendImage.class);
            intent.putExtra("u", url);
            intent.putExtra("n", receiver_name);
            intent.putExtra("ruid", receiver_uid);
            intent.putExtra("suid", sender_uid);
            startActivity(intent);


        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<MessageMember> options1 =
                new FirebaseRecyclerOptions.Builder<MessageMember>()
                        .setQuery(rootref1, MessageMember.class)
                        .build();

        FirebaseRecyclerAdapter<MessageMember, MessageViewHolder> firebaseRecyclerAdapter1 =
                new FirebaseRecyclerAdapter<MessageMember, MessageViewHolder>(options1) {
                    @Override
                    protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull MessageMember model) {

                        holder.Setmessage(getApplication(), model.getMessage(), model.getTime(), model.getDate(), model.getType(),
                                model.getSenderuid(), model.getReceiveruid(), model.getSendername(), model.getAudio(), model.getImage());

                        String audio = getItem(position).getAudio();
                        long delete = getItem(position).getDelete();
                        String type = getItem(position).getType();
                        String imageuri = getItem(position).getImage();
                        String date = getItem(position).getDate();
                        String time = getItem(position).getTime();
                        String sendername = getItem(position).getSendername();

                        holder.playsender.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                MediaPlayer mediaPlayer = new MediaPlayer();

                                holder.playsender.setImageResource(R.drawable.ic_baseline_pause_white);

                                try {
                                    mediaPlayer.setDataSource(audio);
                                    mediaPlayer.prepare();
                                    mediaPlayer.start();
                                    holder.playsender.setClickable(false);
                                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            holder.playsender.setImageResource(R.drawable.ic_baseline_white);
                                            mediaPlayer.stop();
                                            holder.playsender.setClickable(true);
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        });


                        holder.sendertv.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });
                        holder.iv_sender.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });
                        holder.playsender.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });

                        holder.receivertv.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });
                        holder.iv_receiver.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });
                        holder.playreceiver.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                createMessageDialog(delete, type, imageuri, date, time, sendername, audio);

                                return false;
                            }
                        });


                        holder.playreceiver.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                MediaPlayer mediaPlayer = new MediaPlayer();

                                holder.playreceiver.setImageResource(R.drawable.ic_baseline_pause_black);

                                try {
                                    mediaPlayer.setDataSource(audio);
                                    mediaPlayer.prepare();
                                    mediaPlayer.start();
                                    holder.playreceiver.setClickable(false);
                                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            holder.playreceiver.setImageResource(R.drawable.ic_baseline_black);
                                            mediaPlayer.stop();
                                            holder.playreceiver.setClickable(true);
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        });


                    }

                    @NonNull
                    @Override
                    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.message_layout, parent, false);

                        return new MessageViewHolder(view);
                    }
                };

        firebaseRecyclerAdapter1.startListening();
        recyclerView.setAdapter(firebaseRecyclerAdapter1);

    }

    private void createMessageDialog(long delete, String type, String imageuri, String date, String time, String sendername, String audio) {

        final Dialog dialog = new Dialog(MessageActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.message_options);

        TextView unsend = dialog.findViewById(R.id.unsend_id);
        TextView details = dialog.findViewById(R.id.details_id);
        TextView download = dialog.findViewById(R.id.option1_id);
        TextView opt2 = dialog.findViewById(R.id.option2_id);
        TextView datetv = dialog.findViewById(R.id.date_mo);
        TextView timetv = dialog.findViewById(R.id.time_mo);


        if (type.equals("t")) {
            download.setVisibility(View.GONE);
        } else {
            download.setVisibility(View.VISIBLE);
        }

        details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                datetv.setVisibility(View.VISIBLE);
                timetv.setVisibility(View.VISIBLE);
                datetv.setText("Date :" + date);
                timetv.setText("Time :" + time);

            }
        });

        unsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (type.equals("t")) {
                    Query rootref = rootref1.orderByChild("delete").equalTo(delete);
                    rootref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    Query rootrefdel = rootref2.orderByChild("delete").equalTo(delete);
                    rootrefdel.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    dialog.dismiss();


                } else if (type.equals("i")) {


                    Query rootref = rootref1.orderByChild("delete").equalTo(delete);
                    rootref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    Query rootrefdel = rootref2.orderByChild("delete").equalTo(delete);
                    rootrefdel.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(imageuri);
                    reference.delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();


                                    dialog.dismiss();
                                }
                            });
                } else if (type.equals("a")) {


                    Query rootref = rootref1.orderByChild("delete").equalTo(delete);
                    rootref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                    Query rootrefdel = rootref2.orderByChild("delete").equalTo(delete);
                    rootrefdel.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                dataSnapshot1.getRef().removeValue();

                                Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                    StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(audio);
                    reference.delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(MessageActivity.this, "deleted", Toast.LENGTH_SHORT).show();

                                    dialog.dismiss();

                                }
                            });

                }
            }
        });


        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                PermissionListener permissionListener = new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {

                        if (type.equals("i")) {

                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageuri));
                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                    DownloadManager.Request.NETWORK_MOBILE);
                            request.setTitle("Download");
                            request.setDescription("Downloading image....");
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sendername + System.currentTimeMillis() + ".jpg");
                            DownloadManager manager = (DownloadManager) MessageActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
                            manager.enqueue(request);

                            Toast.makeText(MessageActivity.this, "downloaded", Toast.LENGTH_SHORT).show();

                            dialog.dismiss();

                        } else if (type.equals("a")) {
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(audio));
                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                    DownloadManager.Request.NETWORK_MOBILE);
                            request.setTitle("Download");
                            request.setDescription("Downloading audio....");
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sendername + System.currentTimeMillis() + ".mp3");
                            DownloadManager manager = (DownloadManager) MessageActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
                            manager.enqueue(request);

                            Toast.makeText(MessageActivity.this, "downloaded", Toast.LENGTH_SHORT).show();


                            dialog.dismiss();

                        }

                    }

                    @Override
                    public void onPermissionDenied(List<String> deniedPermissions) {

                        Toast.makeText(MessageActivity.this, "error", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                };
                TedPermission.with(MessageActivity.this)
                        .setPermissionListener(permissionListener)
                        .setPermissions(Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE)
                        .check();


                dialog.dismiss();


            }
        });


        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.Bottomanim;
        dialog.getWindow().setGravity(Gravity.BOTTOM);


    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void SendMessage() {

        String message = messageEt.getText().toString();

        Calendar cdate = Calendar.getInstance();
        SimpleDateFormat currentdate = new SimpleDateFormat("dd-MMMM-yyyy");
        final String savedate = currentdate.format(cdate.getTime());

        Calendar ctime = Calendar.getInstance();
        SimpleDateFormat currenttime = new SimpleDateFormat("HH:mm:ss");
        final String savetime = currenttime.format(ctime.getTime());

        String time = savedate + ":" + savetime;

        if (message.isEmpty()) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
        } else {

            long deletetime = System.currentTimeMillis();
            messageMember.setDate(savedate);
            messageMember.setTime(savetime);
            messageMember.setMessage(message);
            messageMember.setReceiveruid(receiver_uid);
            messageMember.setSenderuid(sender_uid);
            messageMember.setType("t");
            messageMember.setDelete(deletetime);

            String id = rootref1.push().getKey();
            rootref1.child(id).setValue(messageMember);

            String id1 = rootref2.push().getKey();
            rootref2.child(id1).setValue(messageMember);


            sendNotification(receiver_uid, receiver_name, message);
            messageEt.setText("");


        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        typingref.child(receiver_uid).child(sender_uid).removeValue();
    }


    private void sendNotification(String receiver_uid, String receiver_name, String message) {

        FirebaseDatabase.getInstance().getReference().child(receiver_uid).child("token")
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
                        new FcmNotificationsSender(usertoken, "Social Media", receiver_name + ": " + message,
                                getApplicationContext(), MessageActivity.this);

                notificationsSender.SendNotifications();

            }
        }, 3000);

    }

    public void checkIncoming() {

        checkVideocallRef = database.getReference("vc");


        try {

            checkVideocallRef.child(currentuid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {

                        senderuid = snapshot.child("calleruid").getValue().toString();
                        Intent intent = new Intent(MessageActivity.this, VideoCallinComing.class);
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

}