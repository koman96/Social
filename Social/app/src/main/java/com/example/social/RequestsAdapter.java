package com.example.social;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class RequestsAdapter extends BaseAdapter {

    private ArrayList<User> users;
    private FragmentActivity activity;
    private DatabaseReference ref;
    private FirebaseUser myData;

    public RequestsAdapter( ArrayList<User> users ,FragmentActivity activity){
        this.users = users;
        this.activity = activity;
        myData = FirebaseAuth.getInstance().getCurrentUser();
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
            view = activity.getLayoutInflater().inflate(R.layout.request_row, null);

            ImageView userImage = view.findViewById(R.id.userImage);
            TextView userName = view.findViewById(R.id.userName);
            Button confirm = view.findViewById(R.id.confirm);
            Button reject = view.findViewById(R.id.reject);

            User user = users.get(position);
            userName.setText(user.getName() );

            //load image
            if (! "default".equals(user.getImage() )){
                Glide.with(activity)
                        .load(user.getImage() )
                        .centerCrop()
                        .into(userImage);
            }

            //events
            userName.setOnClickListener(e->{
                go_to_profile(user);
            });

            userImage.setOnClickListener(e->{
                go_to_profile(user);
            });

            confirm.setOnClickListener(e->{
                //add friends
                HashMap map = new HashMap();
                map.put("friendsSince" , Calendar.getInstance().getTime().toString() );

                ref = FirebaseDatabase.getInstance().getReference("friends");
                ref.child(myData.getUid() ).child(user.getId() ).setValue(map);
                ref.child(user.getId() ).child(myData.getUid() ).setValue(map);

                //remove request
                ref = FirebaseDatabase.getInstance().getReference("friend requests");
                ref.child(myData.getUid() ).child(user.getId() ).removeValue();

                confirm.setText("Friends ✔");
                confirm.setEnabled(false);
                reject.setVisibility(View.GONE);


                SharedPreferences preferences = activity.getSharedPreferences("myData" , Context.MODE_PRIVATE);

                //send notification
                Notification notification = new Notification();
                notification.senderId = myData.getUid();
                notification.senderName = preferences.getString("name" ,"");
                notification.senderImage = preferences.getString("image" ,"default");
                notification.type = "acceptance";
                notification.time = Calendar.getInstance().getTime().toString();

                new MainActivity().send_notification(user.getId() ,notification);
            });

            reject.setOnClickListener(e->{
                //delete request
                ref = FirebaseDatabase.getInstance().getReference("friend requests")
                        .child(myData.getUid() ).child(user.getId() );
                ref.removeValue();

                users.remove(user);
                notifyDataSetChanged();
            });
        }

        return view;
    }

    private void go_to_profile(User user) {
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new OtherProfileFrag(user) )
                .addToBackStack(null)
                .commit();
    }
}