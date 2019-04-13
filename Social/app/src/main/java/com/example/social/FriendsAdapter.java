package com.example.social;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsAdapter extends BaseAdapter {

    private ArrayList<User> users;
    private FragmentActivity activity;
    private ArrayList<String> friendSince;
    private FirebaseUser iam;

    public FriendsAdapter (ArrayList<User> users ,FragmentActivity activity ,ArrayList<String> friendSince){
        this.users = users;
        this.activity = activity;
        this.friendSince = friendSince;
        iam = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.friend_layout, null);

            CircleImageView userImage = view.findViewById(R.id.userImage);
            TextView userName = view.findViewById(R.id.userName);
            TextView duration = view.findViewById(R.id.duration);

            User currentUser = users.get(position);

            //load user name and image
            userName.setText(currentUser.getName() );

            if (! "default".equals(currentUser.getImage() )){
                Glide.with(activity)
                        .load(currentUser.getImage() )
                        .centerCrop()
                        .into(userImage);
            }

            try {
                Date friendSin = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(friendSince.get(position));
                Date currentDate = Calendar.getInstance().getTime();

                long diff = currentDate.getTime() - friendSin.getTime();
                int minutes = (int) (diff/1000/60);
                int hours = minutes / 60;
                int days = hours / 24;

                String since = "";

                if (days >= 1.0) {
                    if (days == 1)
                        since = "1 day";

                    else {  //more than 1 day

                        if (days < 7)
                            since = String.valueOf(days) + " days";

                        else {
                            int weeks = days / 7;

                            if (weeks == 1)
                                since = "1 week";

                            else {
                                if (weeks < 4)
                                    since = String.valueOf(weeks) + " weeks";

                                else {  //1 month or more
                                    int months = weeks / 4;

                                    if (months == 1)
                                        since = "1 month";

                                    else {
                                        int years = months / 12;

                                        if (years < 1)
                                            since = String.valueOf(months) + " months";
                                        else {
                                            if (years == 1)
                                                since = "1 year";
                                            else
                                                since = String.valueOf(years) + " years";
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (hours >= 1) {
                        if (hours == 1)
                            since = "1 hour";
                        else
                            since = String.valueOf(hours) + " hours";
                    }
                    else {
                        if (minutes > 1)
                            since = String.valueOf(minutes) + " minutes";
                        else
                            since = "1 minute";
                    }
                }
                duration.append(since);

            }catch (Exception e){
                duration.setText("couldn't get friendship duration");
            }


            //events
            userImage.setOnClickListener(e->{
                go_to_profile(currentUser);
            });

            userName.setOnClickListener(e->{
                go_to_profile(currentUser);
            });
        }
        return view;
    }

    private void go_to_profile(User user){
        //
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new OtherProfileFrag(user) )
                .addToBackStack(null)
                .commit();
    }
}