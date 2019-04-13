package com.example.social;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

class SuggestionsAdapter extends BaseAdapter {

    private ArrayList<User> users;
    private FragmentActivity activity;
    private FirebaseUser iam;
    private SharedPreferences preferences;
    private Boolean isSent ,heSent;

    public SuggestionsAdapter(ArrayList<User> users ,FragmentActivity activity){
        this.users = users;
        this.activity = activity;

        iam = FirebaseAuth.getInstance().getCurrentUser();
        preferences = activity.getSharedPreferences("myData" , Context.MODE_PRIVATE);
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
            isSent = heSent = false;
            view = activity.getLayoutInflater().inflate(R.layout.adding_friend_example, null);

            ImageView sugImage = view.findViewById(R.id.sugImage);
            TextView sugName = view.findViewById(R.id.sugName);
            Button add = view.findViewById(R.id.add);
            Button remove = view.findViewById(R.id.remove);

            User user = users.get(position);

            //check if i sent him friend request or he sent
            DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("friend requests");
            reqRef.child(user.getId() ).child(iam.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //
                    if (dataSnapshot.exists() ){    //i sent him before
                        isSent = true;
                        add.setText("Cancel Request");
                    }
                    else {
                        reqRef.child(iam.getUid() ).child(user.getId() ).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {

                                if (dataSnapshot2.exists() ){   //he sent request to me
                                    heSent = true;
                                    add.setText("Accept Request");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            //load sugName and image
            sugName.setText(user.getName());

            String image = user.getImage();
            if (!"default".equals(image)) {
                Glide.with(activity)
                        .load(image)
                        .centerCrop()
                        .into(sugImage);
            }

            //events
            remove.setOnClickListener(e -> {
                users.remove(user);
                notifyDataSetChanged();
            });

            add.setOnClickListener(e -> {
                //
                if (!isSent && !heSent) {
                    //send friendship request
                    HashMap map = new HashMap();
                    map.put("name" ,preferences.getString("name" ,"") );
                    map.put("image" ,preferences.getString("image" ,"") );
                    map.put("gender" ,preferences.getString("gender" ,"") );
                    map.put("birthDate" ,preferences.getString("birthDate" ,"") );
                    map.put("country" ,preferences.getString("country" ,"") );
                    map.put("id" ,iam.getUid() );

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("friend requests");
                    ref.child(user.id).child(iam.getUid()).setValue(map);

                    add.setText("Cancel Request");
                    isSent = true;

                    //send notification
                    Notification notification = new Notification();
                    notification.senderId = iam.getUid();
                    notification.senderName = preferences.getString("name" ,"");
                    notification.senderImage = preferences.getString("image" ,"default");
                    notification.type = "request";
                    notification.time = Calendar.getInstance().getTime().toString();

                    new MainActivity().send_notification(user.getId() ,notification);
                }
                else {
                    if (isSent) {   //cancel request
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("friend requests");
                        ref.child(user.id).child(iam.getUid()).removeValue();

                        add.setText("Add Friend");
                        isSent = false;
                    }
                    else {      //he sent and i accepted now
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("friend requests");
                        ref.child(iam.getUid()).child(user.id).removeValue();

                        HashMap map = new HashMap();
                        map.put("friendsSince" , Calendar.getInstance().getTime().toString() );

                        ref = FirebaseDatabase.getInstance().getReference("friends");
                        ref.child(iam.getUid() ).child(user.getId() ).setValue(map);
                        ref.child(user.getId() ).child(iam.getUid() ).setValue(map);

                        add.setText("Friends ✔");
                        add.setEnabled(false);

                        //send notification
                        Notification notification = new Notification();
                        notification.senderId = iam.getUid();
                        notification.senderName = preferences.getString("name" ,"");
                        notification.senderImage = preferences.getString("image" ,"default");
                        notification.type = "acceptance";
                        notification.time = Calendar.getInstance().getTime().toString();

                        new MainActivity().send_notification(user.getId() ,notification);
                    }
                }
            });

            sugImage.setOnClickListener(e->{
                goTo_profile(user);
            });

            sugName.setOnClickListener(e->{
                goTo_profile(user);
            });
        }

        return view;
    }

    private void goTo_profile(User user){
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new OtherProfileFrag(user) )
                .addToBackStack(null)
                .commit();
    }
}