package com.example.social;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserPostsAdapter extends BaseAdapter {

    private ArrayList<Post> posts;
    private FragmentActivity myContext;
    private String id;
    private User user;

    public UserPostsAdapter(ArrayList<Post> posts ,FragmentActivity myContext ,String id){
        this.posts = posts;
        this.myContext = myContext;
        this.id = id;
    }

    public class MyHolder{
        CircleImageView postLogo;
        TextView postOwner ,postTime ,postText ,like ,comment ,share;
        ImageView postImage;
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Object getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final MyHolder holder;

        if (view == null) {
            view = myContext.getLayoutInflater().inflate(R.layout.post_layout, null);
            holder = new MyHolder();

            holder.postLogo = view.findViewById(R.id.postLogo);
            holder.postOwner = view.findViewById(R.id.postOwner);
            holder.postTime = view.findViewById(R.id.postTime);

            holder.postText = view.findViewById(R.id.postText);
            holder.postImage = view.findViewById(R.id.postImage);

            holder.like = view.findViewById(R.id.like);
            holder.comment = view.findViewById(R.id.comment);
            holder.share = view.findViewById(R.id.share);

            view.setTag(holder);
        }
        else
            holder = (MyHolder) view.getTag();

        Post post = posts.get(position);

        //get post's author name and image
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //
                if (dataSnapshot.exists() ) {
                    user = dataSnapshot.getValue(User.class);

                    if (! "default".equals(user.getImage() )) {
                        Glide.with(myContext)
                                .load(user.getImage() )
                                .centerCrop()
                                .into(holder.postLogo);
                    }

                    holder.postOwner.setText(user.getName() );
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });


        //update post time
        try {
            Date postDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy" , Locale.ENGLISH).parse(post.postTime);
            Date currentDate = Calendar.getInstance().getTime();

            long diff = currentDate.getTime() - postDate.getTime();
            int seconds = (int) (diff / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            int days = hours / 24;

            String since = "";
            if (days >= 1.0){
                if (days == 1)
                    since = "1 day ago";

                else {  //more than 1 day

                    if (days < 7)
                        since = String.valueOf(days) + " days ago";

                    else {
                        int weeks = days / 7;

                        if (weeks == 1)
                            since = "1 week ago";

                        else {
                            if (weeks < 4)
                                since = String.valueOf(weeks) + " weeks ago";

                            else {  //1 month or more
                                int months = weeks / 4;

                                if (months == 1)
                                    since = "1 month ago";

                                else {
                                    int years = months / 12;

                                    if (years < 1)
                                        since = String.valueOf(months) + " months ago";
                                    else {
                                        if (years == 1)
                                            since = "1 year ago";
                                        else
                                            since = String.valueOf(years) + " years ago";
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                if (hours >= 1){
                    if (hours == 1)
                        since = "1 hour ago";
                    else
                        since = String.valueOf(hours) + " hours ago";
                }
                else {
                    if (minutes >= 1){
                        if (minutes == 1)
                            since = "1 minute ago";
                        else
                            since = String.valueOf(minutes) + " minutes ago";
                    }
                    else {
                        if (seconds >= 1){
                            if (seconds == 1)
                                since = "1 second ago";
                            else
                                since = String.valueOf(seconds) + " seconds ago";
                        }
                    }
                }
            }
            holder.postTime.setText(since);

        } catch (Exception e) {     holder.postTime.setText("can't display post's time");      }


        //update post text
        if ("".equals(post.postText))     //post has no text (image)
            holder.postText.setVisibility(View.GONE);
        else{
            if ("proPIC".equals(post.postText )){

                String postHint = "<b>" + holder.postOwner.getText() + "</b> has changed the profile picture";
                holder.postText.setText(Html.fromHtml(postHint) );
            }
            else    //normal post
                holder.postText.setText(post.postText);
        }


        //update post image
        if ("".equals(post.postImage))
            holder.postImage.setVisibility(View.GONE);

        else    //upload image from server
            Glide.with(myContext)
                    .load(post.postImage )
                    .centerCrop()
                    .into(holder.postImage);

        //events
        holder.like.setOnClickListener(e -> {
            //
        });

        holder.comment.setOnClickListener(e -> {
            //
        });

        holder.share.setOnClickListener(e -> {
            //
        });

        return view;
    }
}