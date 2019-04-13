package com.example.social;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
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

public class NotificationAdapter extends BaseAdapter {

    private ArrayList<Notification> notifications;
    private FragmentActivity activity;
    private MainActivity mainActivity;

    public NotificationAdapter(ArrayList<Notification> notifications ,FragmentActivity activity){
        this.notifications = notifications;
        this.activity = activity;
        mainActivity = (MainActivity) activity;
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public Object getItem(int position) {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null){
            view = activity.getLayoutInflater().inflate(R.layout.notification_row ,null);

            CircleImageView senderImage = view.findViewById(R.id.senderImage);
            TextView text = view.findViewById(R.id.text);
            TextView since = view.findViewById(R.id.since);

            Notification notification = notifications.get(position);
            if (! "default".equals(notification.senderImage))
                Glide.with(activity).load(notification.senderImage).centerCrop().into(senderImage);


            if (notification.clicked)
                view.setBackgroundResource(R.color.white);

            String sender = String.valueOf( Html.fromHtml("<b>"+ notification.senderName +"</b>"));
            String body = notification.text;

            if (body.isEmpty() ) {
                if ("acceptance".equals(notification.type))
                    body = sender + " has accepted your friendship request";

                else if ("request".equals(notification.type))
                    body = sender + " has sent you a friendship request";
            }
            else
                body = sender +" "+ body;

            text.setText(body);


            //update post time
            try {
                Date postDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(notification.time);
                Date currentDate = Calendar.getInstance().getTime();

                long diff = currentDate.getTime() - postDate.getTime();
                int seconds = (int) (diff / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                int days = hours / 24;

                String duration = "";
                if (days >= 1.0) {
                    if (days == 1)
                        duration = "1 day ago";

                    else {  //more than 1 day

                        if (days < 7)
                            duration = String.valueOf(days) + " days ago";

                        else {
                            int weeks = days / 7;

                            if (weeks == 1)
                                duration = "1 week ago";

                            else {
                                if (weeks < 4)
                                    duration = String.valueOf(weeks) + " weeks ago";

                                else {  //1 month or more
                                    int months = weeks / 4;

                                    if (months == 1)
                                        duration = "1 month ago";

                                    else {
                                        int years = months / 12;

                                        if (years < 1)
                                            duration = String.valueOf(months) + " months ago";
                                        else {
                                            if (years == 1)
                                                duration = "1 year ago";
                                            else
                                                duration = String.valueOf(years) + " years ago";
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (hours >= 1) {
                        if (hours == 1)
                            duration = "1 hour ago";
                        else
                            duration = String.valueOf(hours) + " hours ago";
                    } else {
                        if (minutes >= 1) {
                            if (minutes == 1)
                                duration = "1 minute ago";
                            else
                                duration = String.valueOf(minutes) + " minutes ago";
                        } else {
                            if (seconds >= 1) {
                                if (seconds == 1)
                                    duration = "1 second ago";
                                else
                                    duration = String.valueOf(seconds) + " seconds ago";
                            }
                        }
                    }
                }
                since.setText(duration);

            } catch (Exception e) {
                since.setText("can't display notification's time");
            }

            view.setOnClickListener(e->{
                if (! notification.clicked) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(notification.notificationId);

                    ref.child("seen").setValue(true);
                    ref.child("clicked").setValue(true);
                }

                if ("acceptance".equals(notification.type)){
                    // go to profile of the one who accepted my request
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(notification.senderId);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                //
                                User user = dataSnapshot.getValue(User.class);
                                mainActivity.goto_profile(user);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                }
                else if ("request".equals(notification.type))
                    mainActivity.check_friendReq();

                else {
                    //post reaction
                }
            });
        }

        return view;
    }
}