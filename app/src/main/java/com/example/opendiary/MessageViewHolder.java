package com.example.opendiary;

import android.app.Application;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class MessageViewHolder extends RecyclerView.ViewHolder {

    TextView sendertv,receivertv;
    ImageView iv_sender,iv_receiver;
    ImageButton playsender,playreceiver;
    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);

    }
    public void Setmessage(Application application ,  String message, String time, String date,String type,
                           String senderuid,String receiveruid, String sendername, String audio, String image){

        sendertv = itemView.findViewById(R.id.sender_tv);
        receivertv = itemView.findViewById(R.id.receiver_tv);

        playreceiver = itemView.findViewById(R.id.play_message_receiver);
        playsender = itemView.findViewById(R.id.play_message_sender);
        LinearLayout llsender = itemView.findViewById(R.id.llsender);
        LinearLayout llreceiver = itemView.findViewById(R.id.llreceiver);

        iv_receiver = itemView.findViewById(R.id.iv_receiver);
        iv_sender = itemView.findViewById(R.id.iv_sender);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user.getUid();

        if(currentUid.equals(senderuid)){

            if(type.equals("i")){
                receivertv.setVisibility(View.GONE);
                sendertv.setVisibility(View.GONE);
                iv_sender.setVisibility(View.GONE);
                iv_sender.setVisibility(View.VISIBLE);
                Picasso.get().load(image).into(iv_sender);
                llreceiver.setVisibility(View.GONE);
                llsender.setVisibility(View.GONE);
            }else if (type.equals("t")){

                receivertv.setVisibility(View.GONE);
                sendertv.setVisibility(View.VISIBLE);
                sendertv.setText(message);
                llreceiver.setVisibility(View.GONE);
                llsender.setVisibility(View.GONE);
                iv_sender.setVisibility(View.GONE);
                iv_receiver.setVisibility(View.GONE);

            }else if (type.equals("a")){

                receivertv.setVisibility(View.GONE);
                sendertv.setVisibility(View.GONE);
                llreceiver.setVisibility(View.GONE);
                llsender.setVisibility(View.VISIBLE);
                iv_sender.setVisibility(View.GONE);
                iv_receiver.setVisibility(View.GONE);
            }

        }else if (currentUid.equals(receiveruid)){

            if(type.equals("i")){
                receivertv.setVisibility(View.GONE);
                sendertv.setVisibility(View.GONE);
                iv_sender.setVisibility(View.GONE);
                iv_receiver.setVisibility(View.VISIBLE);
                Picasso.get().load(image).into(iv_receiver);
                llreceiver.setVisibility(View.GONE);
                llsender.setVisibility(View.GONE);
            }else if (type.equals("t")){

                receivertv.setVisibility(View.VISIBLE);
                sendertv.setVisibility(View.GONE);
                receivertv.setText(message);
                llreceiver.setVisibility(View.GONE);
                llsender.setVisibility(View.GONE);
                iv_sender.setVisibility(View.GONE);
                iv_receiver.setVisibility(View.GONE);

            }else if (type.equals("a")){

                receivertv.setVisibility(View.GONE);
                sendertv.setVisibility(View.GONE);
                llreceiver.setVisibility(View.VISIBLE);
                llsender.setVisibility(View.GONE);
                iv_sender.setVisibility(View.GONE);
                iv_receiver.setVisibility(View.GONE);
            }



        }


    }



}
