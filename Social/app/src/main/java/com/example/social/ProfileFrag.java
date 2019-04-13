package com.example.social;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.Collections;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFrag extends Fragment {

    private ListView myPostsList;
    private FirebaseUser iam;
    private TextView notifText;
    private ArrayList<Notification> notifications;
    private int unSeenNotifications = 0;

    public ProfileFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
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
        myPostsList = view.findViewById(R.id.myPosts);

        View header = getLayoutInflater().inflate(R.layout.myprofile_header ,myPostsList ,false);
        myPostsList.addHeaderView(header ,null ,false);

        CircleImageView image = view.findViewById(R.id.myImage);
        TextView name = view.findViewById(R.id.myName);

        TextView gender = view.findViewById(R.id.gender);
        TextView birth = view.findViewById(R.id.birth);
        TextView country = view.findViewById(R.id.country);

        Button edit_prof = view.findViewById(R.id.edit_prof);
        Button friends_btn = view.findViewById(R.id.friends_btn);
        Button requests_btn = view.findViewById(R.id.requests_btn);
        Button addFriends_btn = view.findViewById(R.id.addFriends_btn);
        Button signOut_btn = view.findViewById(R.id.signOut_btn);
        Button deleteAcc_btn = view.findViewById(R.id.deleteAcc_btn);

        iam = FirebaseAuth.getInstance().getCurrentUser();
        //get my info
        SharedPreferences preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);
        String myName = preferences.getString("name" ,"");
        String myImage = preferences.getString("image" ,"");
        String myGender = preferences.getString("gender" ,"");
        String myBirth = preferences.getString("birthDate" ,"");
        String myCountry = preferences.getString("country" ,"");

        name.setText(myName);

        if (! "default".equals(myImage)){
            Glide.with(getActivity())
                    .load(myImage)
                    .centerCrop()
                    .into(image);
        }
        gender.setText(myGender);
        birth.setText(myBirth);
        country.setText(myCountry);


        load_myNotifications();
        load_myPosts();


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
            myPostsList.setSelection(0);
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

    private void load_myPosts() {

        FirebaseUser iam = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");

        ArrayList<Post> myPosts = new ArrayList<>();
        MyPostsAdapter adapter = new MyPostsAdapter(myPosts ,getActivity() );
        myPostsList.setAdapter(adapter);

        postsRef.child(iam.getUid() ).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                if (iam.getUid().equals(dataSnapshot.child("author").getValue().toString() )){  //this post is created by me
                    Collections.reverse(myPosts);

                    Post post = dataSnapshot.getValue(Post.class);
                    myPosts.add(post);

                    Collections.reverse(myPosts);   //last appears first
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                //
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}