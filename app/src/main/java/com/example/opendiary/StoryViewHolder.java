package com.example.opendiary;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

public class StoryViewHolder  extends RecyclerView.ViewHolder {

    ImageView imageView ;
    TextView textView;
    public StoryViewHolder(@NonNull View itemView) {
        super(itemView);
    }
    public void setStory(FragmentActivity activity,String postUri,
                         String name, long timeEnd, String timeUpload, String type, String caption, String url, String uid){

        imageView = itemView.findViewById(R.id.iv_story_f4);
        textView = itemView.findViewById(R.id.tv_unamestory);

        Picasso.get().load(url).into(imageView);
        textView.setText(name);

    }


}
