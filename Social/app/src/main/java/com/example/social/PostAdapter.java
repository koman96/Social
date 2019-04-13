package com.example.social;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends BaseAdapter {

    private ArrayList<Post> posts;
    private FragmentActivity myContext;
    private FirebaseUser user;
    private User postUser;
    private SharedPreferences preferences;

    public PostAdapter( ArrayList<Post> posts ,FragmentActivity myContext){
        this.posts = posts;
        this.myContext = myContext;
        user = FirebaseAuth.getInstance().getCurrentUser();
        preferences = myContext.getSharedPreferences("myData", Context.MODE_PRIVATE);
    }

    public class MyHolder{
        CircleImageView postLogo;
        LinearLayout like ,comment ,share;
        TextView postOwner ,postTime ,postText ,likeTxt;
        ImageView postImage ,likeShape;
        Boolean liked = false;
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
            holder.likeShape = view.findViewById(R.id.likeShape);
            holder.likeTxt = view.findViewById(R.id.likeTxt);
            holder.comment = view.findViewById(R.id.comment);
            holder.share = view.findViewById(R.id.share);

            view.setTag(holder);
        }
        else
            holder = (MyHolder) view.getTag();

        Post post = posts.get(position);
        String authorId = post.author;

        if (authorId.equals(user.getUid())) {   //this is my post ==> load my name & image from shared preferences
            //
            String name = preferences.getString("name", "");
            String image = preferences.getString("image", "");

            if (!"default".equals(image) && !"".equals(image)) {
                Glide.with(myContext)
                        .load(image)
                        .centerCrop()
                        .into(holder.postLogo);
            }
            holder.postOwner.setText(name);
        }
        else {
            //get post's author name and image from database
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            ref.child(authorId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //
                    if (dataSnapshot.exists()) {
                        postUser = dataSnapshot.getValue(User.class);

                        holder.postOwner.setText(postUser.getName());
                        if (!"default".equals(postUser.getImage())) {
                            //load his image from server
                            Glide.with(myContext)
                                    .load(postUser.getImage())
                                    .centerCrop()
                                    .into(holder.postLogo);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        }


        //update post time
        try {
            Date postDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(post.postTime);
            Date currentDate = Calendar.getInstance().getTime();

            long diff = currentDate.getTime() - postDate.getTime();
            int seconds = (int) (diff / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            int days = hours / 24;

            String since = "";
            if (days >= 1.0) {
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
            } else {
                if (hours >= 1) {
                    if (hours == 1)
                        since = "1 hour ago";
                    else
                        since = String.valueOf(hours) + " hours ago";
                } else {
                    if (minutes >= 1) {
                        if (minutes == 1)
                            since = "1 minute ago";
                        else
                            since = String.valueOf(minutes) + " minutes ago";
                    } else {
                        if (seconds >= 1) {
                            if (seconds == 1)
                                since = "1 second ago";
                            else
                                since = String.valueOf(seconds) + " seconds ago";
                        }
                    }
                }
            }
            holder.postTime.setText(since);

        } catch (Exception e) {
            holder.postTime.setText("can't display post's time");
        }


        //update post text
        if ("".equals(post.postText))     //post has no text (image)
            holder.postText.setVisibility(View.GONE);
        else {
            if ("proPIC".equals(post.postText)) {
                String postHint;

                if (authorId.equals(user.getUid()))
                    postHint = "<b>You</b> have changed the profile picture";
                else
                    postHint = "<b>" + holder.postOwner.getText() + "</b> has changed the profile picture";

                postHint = String.valueOf(Html.fromHtml(postHint));

                holder.postText.setText(postHint);
                post.postText = postHint;
            } else
                holder.postText.setText(post.postText);
        }


        //update post image
        if ("".equals(post.postImage))
            holder.postImage.setVisibility(View.GONE);

        else {
            //download image from server
            Glide.with(myContext)
                    .load(post.postImage)
                    .centerCrop()
                    .into(holder.postImage);
        }

        //check if i liked it or not
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("post likes").child(post.author).child(post.postId);
        reference.child(user.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() ){
                    // liked it before
                    holder.likeShape.setImageResource(R.mipmap.liked);
                    holder.likeTxt.setText("Liked");
                    holder.likeTxt.setTextColor(myContext.getResources().getColor(R.color.violet));

                    holder.liked = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });


        //events
        holder.like.setOnClickListener(e -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("post likes").child(post.author).child(post.postId);

            if (holder.liked){
                //delete like
                ref.child(user.getUid() ).removeValue();

                holder.likeShape.setImageResource(R.mipmap.default_like);
                holder.likeTxt.setText("Like");
                holder.likeTxt.setTextColor(myContext.getResources().getColor(R.color.darkSilver));

                holder.liked = false;
            }
            else {
                //send like
                HashMap map = new HashMap();
                map.put("name" ,preferences.getString("name" ,""));
                map.put("image" ,preferences.getString("image" ,"default"));

                ref.child(user.getUid() ).setValue(map);

                holder.likeTxt.setText("Liked");
                holder.likeTxt.setTextColor(myContext.getResources().getColor(R.color.violet));
                holder.likeShape.setImageResource(R.mipmap.liked);

                holder.liked = true;
            }
        });

        holder.comment.setOnClickListener(e -> {
            ((MainActivity) myContext).show_postComments(post);
        });

        holder.share.setOnClickListener(e -> {
            //
        });

        holder.postLogo.setOnClickListener(e -> {
            goTo_profile(postUser ,authorId);
        });

        holder.postOwner.setOnClickListener(e -> {
            goTo_profile(postUser ,authorId);
        });

        return view;
    }

    private void goTo_profile(User postUser ,String autherId){
        // if the post author is me ==> go to my profile        else ==> go to his profile
        FragmentTransaction transaction = myContext.getSupportFragmentManager().beginTransaction();

        if (autherId.equals(user.getUid() ))   //it's me
            transaction.replace(R.id.main_frag ,new ProfileFrag() );
        else
            transaction.replace(R.id.main_frag ,new OtherProfileFrag(postUser) );

        transaction.addToBackStack(null).commit();
    }
}