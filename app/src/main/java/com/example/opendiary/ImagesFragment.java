package com.example.opendiary;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;


import com.squareup.picasso.Picasso;

public class ImagesFragment  extends RecyclerView.ViewHolder {
    ImageView imageView;
    public ImagesFragment(@NonNull View itemView) {

        super(itemView);
    }

    public void SetImage(FragmentActivity activity, String name, String url,String postUri,String time,
                        String uid,String type,String desc){


        imageView = itemView.findViewById(R.id.iv_post_ind);

            Picasso.get().load(postUri).into(imageView);



        }
    }


