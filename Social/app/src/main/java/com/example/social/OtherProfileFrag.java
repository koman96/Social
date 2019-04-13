package com.example.social;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class OtherProfileFrag extends Fragment {

    private DatabaseReference ref;
    private Boolean friendOfMe = false;
    private Boolean iSentReq = false;
    private Boolean heSentReq = false;
    private ListView otherPosts;
    private User user;
    private FirebaseUser iam;
    private TextView notifText;
    private ArrayList<Notification> notifications;
    private int unSeenNotifications = 0;

    public OtherProfileFrag() {
        // Required empty public constructor
    }


    @SuppressLint("ValidFragment")
    public OtherProfileFrag(User user){
        this.user = user;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_other_profile, container, false);
        
        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        ImageView menu_icon = view.findViewById(R.id.menu_icon);
        RelativeLayout notif_icon = view.findViewById(R.id.notif_icon);
        notifText = notif_icon.findViewById(R.id.notif_count);
        ImageView mess_icon = view.findViewById(R.id.mess_icon);
        ImageView prof_icon = view.findViewById(R.id.prof_icon);
        ImageView newFeed_icon = view.findViewById(R.id.newFeed_icon);

        DrawerLayout drawer = view.findViewById(R.id.drawer);
        otherPosts = view.findViewById(R.id.userPosts);

        View header = getActivity().getLayoutInflater().inflate(R.layout.otherprofile_header ,otherPosts ,false);
        otherPosts.addHeaderView(header ,null ,false);

        CircleImageView otherImage = view.findViewById(R.id.otherImage);
        TextView otherName = view.findViewById(R.id.otherName);

        LinearLayout addFriend = view.findViewById(R.id.addFriend);
        ImageView addFriendImg = view.findViewById(R.id.addFriendImg);
        TextView addFriendTxt = view.findViewById(R.id.addFriendTxt);
        LinearLayout sendMess = view.findViewById(R.id.sendMess);
        ImageView sendMessImg = view.findViewById(R.id.sendMessImg);
        TextView sendMessTxt = view.findViewById(R.id.sendMessTxt);

        TextView gender = view.findViewById(R.id.gender);
        TextView birth = view.findViewById(R.id.birth);
        TextView country = view.findViewById(R.id.country);

        Button edit_prof = view.findViewById(R.id.edit_prof);
        Button friends_btn = view.findViewById(R.id.friends_btn);
        Button requests_btn = view.findViewById(R.id.requests_btn);
        Button addFriends_btn = view.findViewById(R.id.addFriends_btn);
        Button signOut_btn = view.findViewById(R.id.signOut_btn);
        Button deleteAcc_btn = view.findViewById(R.id.deleteAcc_btn);

        //load his data
        otherName.setText(user.getName());

        if (! "default".equals(user.getImage()) ){
            Glide.with(getActivity() )
                    .load(user.getImage() )
                    .centerCrop()
                    .into(otherImage);
        }

        gender.setText(user.getGender());
        birth.setText(user.getBirthDate());
        country.setText(user.getCountry());

        //test if this user is friend of me
        iam = FirebaseAuth.getInstance().getCurrentUser();

        ref = FirebaseDatabase.getInstance().getReference("friends").child(iam.getUid() ).child(user.getId());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //
                if (dataSnapshot.exists() ){    //he is my friend !!
                    friendOfMe = true;
                    addFriendImg.setImageResource(R.mipmap.friends);
                    addFriendTxt.setText("Remove Friend");
                }
                else {  //check if i sent him a friend request before or he did
                    DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("friend requests");
                    ref1.child(user.getId()).child(iam.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                            //
                            if (dataSnapshot1.exists() ){    //i sent
                                addFriendTxt.setText("Cancel Request");
                                iSentReq = true;
                            }
                            else {  //check if he sent
                                DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("friend requests");
                                ref2.child(iam.getUid() ).child(user.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                                        if (dataSnapshot2.exists() ){    //he sent
                                            addFriendTxt.setText("Approve for friend request");
                                            heSentReq = true;
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
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });


        load_myNotifications();
        load_user_posts();


        //events
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        MainActivity activity = (MainActivity) getActivity();

        newFeed_icon.setOnClickListener(e->{
            transaction.replace(R.id.main_frag ,new MainFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        menu_icon.setOnClickListener(e->{
            if (drawer.isDrawerOpen(Gravity.START) )
                drawer.closeDrawer(Gravity.START);
            else
                drawer.openDrawer(Gravity.START);
        });

        prof_icon.setOnClickListener(e->{
            transaction.replace(R.id.main_frag ,new ProfileFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        edit_prof.setOnClickListener(e->{
            transaction.replace(R.id.main_frag ,new EditProfFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        friends_btn.setOnClickListener(e->{
            transaction.replace(R.id.main_frag ,new FriendsFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        requests_btn.setOnClickListener(e->{
            activity.check_friendReq();
        });

        addFriends_btn.setOnClickListener(e->{
            //
            transaction.replace(R.id.main_frag ,new AddFriendsFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        signOut_btn.setOnClickListener(e->{
            //
            activity.signOut();
            activity.reloadApp();
        });

        deleteAcc_btn.setOnClickListener(e->{
            //
            new AlertDialog.Builder(getActivity() )
                    .setMessage("Delete your information ,friends ... elc ?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        activity.delete_account();
                    })
                    .setNegativeButton("No", (dialog, which) -> {

                        dialog.dismiss();
                    })
                    .show();
        });

        addFriend.setOnClickListener(e->{
            //
            SharedPreferences preferences = activity.getSharedPreferences("myData" ,Context.MODE_PRIVATE);

            if (friendOfMe){
                //delete him from my friends
                new AlertDialog.Builder(getActivity() )
                        .setMessage("Remove this Friend ?")
                        .setPositiveButton("Yes", (dialog1, which) -> {
                            //
                            dialog1.dismiss();

                            ref = FirebaseDatabase.getInstance().getReference("friends");
                            ref.child(iam.getUid() ).child(user.getId()).removeValue();      //delete him from my list
                            ref.child(user.getId()).child(iam.getUid() ).removeValue();      //delete me from his list

                            addFriendTxt.setText("Add Friend");
                            addFriendImg.setImageResource(R.mipmap.add_user);
                            friendOfMe = false;
                        })
                        .setNegativeButton("No", (dialog12, which) -> dialog12.dismiss()).show();
            }
            else if (iSentReq) {
                //  cancel friend request
                new AlertDialog.Builder(getActivity())
                        .setMessage("Cancel friendship request ?")
                        .setPositiveButton("Yes", (dialog1, which) -> {
                            //
                            dialog1.dismiss();
                            ref = FirebaseDatabase.getInstance().getReference("friend requests");
                            ref.child(user.getId()).child(iam.getUid()).removeValue();

                            addFriendTxt.setText("Add Friend");
                            addFriendImg.setImageResource(R.mipmap.add_user);
                            iSentReq = false;
                        })
                        .setNegativeButton("No", (dialog12, which) -> dialog12.dismiss()).show();
            }
            else if (heSentReq) {
                //approval of request
                new AlertDialog.Builder(getActivity())
                        .setMessage("This user has sent you a friend request")
                        .setPositiveButton("Accept", (dialog1, which) -> {
                            //
                            dialog1.dismiss();

                            ref = FirebaseDatabase.getInstance().getReference("friend requests");
                            ref.child(iam.getUid()).child(user.getId()).removeValue();

                            HashMap map = new HashMap();
                            map.put("friendsSince" , Calendar.getInstance().getTime().toString() );

                            ref = FirebaseDatabase.getInstance().getReference("friends");
                            ref.child(iam.getUid()).child(user.getId()).setValue(map);
                            ref.child(user.getId()).child(iam.getUid()).setValue(map);

                            addFriendTxt.setText("Remove Friend");
                            addFriendImg.setImageResource(R.mipmap.friends);
                            friendOfMe = true;

                            //send notification
                            Notification notification = new Notification();
                            notification.senderId = iam.getUid();
                            notification.senderName = preferences.getString("name" ,"");
                            notification.senderImage = preferences.getString("image" ,"default");
                            notification.type = "acceptance";
                            notification.time = Calendar.getInstance().getTime().toString();

                            activity.send_notification(user.getId() ,notification);
                        })
                        .setNegativeButton("Reject", (dialog12, which) -> {
                            ref = FirebaseDatabase.getInstance().getReference("friend requests");
                            ref.child(iam.getUid()).child(user.getId()).removeValue();

                            addFriendTxt.setText("Add Friend");
                            heSentReq = false;
                        })
                        .setNeutralButton("Later", (dialog, which) -> dialog.dismiss()).show();
            }
            else {
                //send friendship request
                HashMap map = new HashMap();
                map.put("name" ,preferences.getString("name" ,"") );
                map.put("image" ,preferences.getString("image" ,"") );
                map.put("gender" ,preferences.getString("gender" ,"") );
                map.put("birthDate" ,preferences.getString("birthDate" ,"") );
                map.put("country" ,preferences.getString("country" ,"") );
                map.put("id" ,iam.getUid() );

                ref = FirebaseDatabase.getInstance().getReference("friend requests");
                ref.child(user.getId()).child(iam.getUid() ).setValue(map);

                addFriendTxt.setText("Cancel Request");
                iSentReq = true;

                //send notification
                Notification notification = new Notification();
                notification.senderId = iam.getUid();
                notification.senderName = preferences.getString("name" ,"");
                notification.senderImage = preferences.getString("image" ,"default");
                notification.type = "request";
                notification.time = Calendar.getInstance().getTime().toString();

                activity.send_notification(user.getId() ,notification);
            }
        });

        notif_icon.setOnClickListener(e->{
            activity.show_notifications(notifications);
        });
    }

    private void load_myNotifications(){
        notifications = new ArrayList<>();

        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference("notifications");
        notRef.child(iam.getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //
                Notification notification = dataSnapshot.getValue(Notification.class);
                Collections.reverse(notifications);

                notifications.add(notification);
                Collections.reverse(notifications);

                if (! notification.seen) {
                    unSeenNotifications++ ;

                    notifText.setVisibility(View.VISIBLE);
                    notifText.setText(String.valueOf(unSeenNotifications));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void load_user_posts() {
        ArrayList<Post> posts = new ArrayList<>();
        UserPostsAdapter adapter = new UserPostsAdapter(posts ,getActivity() ,user.getId());
        otherPosts.setAdapter(adapter);

        ref = FirebaseDatabase.getInstance().getReference("posts").child(user.getId());
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //check if the post created by him
                String authorId = dataSnapshot.child("author").getValue().toString();

                if (authorId.equals(user.getId()) ){
                    Collections.reverse(posts);
                    Post post = dataSnapshot.getValue(Post.class);

                    posts.add(post);
                    Collections.reverse(posts);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}