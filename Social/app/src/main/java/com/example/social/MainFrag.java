package com.example.social;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import de.hdodenhof.circleimageview.CircleImageView;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class MainFrag extends Fragment {

    private FirebaseUser user;
    private DatabaseReference ref;
    private static final int GALERY_PICK = 1;
    private ArrayList<Post> posts;
    private PostAdapter adapter;
    private ListView postsListview;
    private ArrayList<Notification> notifications;
    private int unSeenNotifications = 0;
    private TextView notifText;

    public MainFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        define_main_layout(view);

        return view;
    }

    private void define_main_layout(View view){
        ImageView menu_icon = view.findViewById(R.id.menu_icon);
        RelativeLayout notif_icon = view.findViewById(R.id.notif_icon);
        notifText = notif_icon.findViewById(R.id.notif_count);
        ImageView mess_icon = view.findViewById(R.id.mess_icon);
        ImageView prof_icon = view.findViewById(R.id.prof_icon);
        ImageView newFeed_icon = view.findViewById(R.id.newFeed_icon);

        DrawerLayout drawer = view.findViewById(R.id.drawer);
        postsListview = view.findViewById(R.id.postsListview);

        View header = getLayoutInflater().inflate(R.layout.main_header ,postsListview ,false);
        postsListview.addHeaderView(header ,null ,false);

        CircleImageView myLogo = view.findViewById(R.id.myLogo);
        TextView myPost = view.findViewById(R.id.myPost);
        ImageView addPhoto = view.findViewById(R.id.addPhoto);

        Button edit_prof = view.findViewById(R.id.edit_prof);
        Button friends_btn = view.findViewById(R.id.friends_btn);
        Button requests_btn = view.findViewById(R.id.requests_btn);
        Button addFriends_btn = view.findViewById(R.id.addFriends_btn);
        Button signOut_btn = view.findViewById(R.id.signOut_btn);
        Button deleteAcc_btn = view.findViewById(R.id.deleteAcc_btn);

        MainActivity activity = (MainActivity) getActivity();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        //load my image
        SharedPreferences preferences = getActivity().getSharedPreferences("myData" , MODE_PRIVATE);
        String image = preferences.getString("image" ,"");

        if (! "default".equals(image) && ! "".equals(image) ) {
            //load from server
                    Glide.with(getActivity() )
                            .load(image)
                            .centerCrop()
                            .into(myLogo);
        }

        user = FirebaseAuth.getInstance().getCurrentUser();

        load_myNotifications();
        load_myPosts();

        //events
        menu_icon.setOnClickListener(v -> {
            if (drawer.isDrawerOpen(Gravity.START) )
                drawer.closeDrawer(Gravity.START);
            else
                drawer.openDrawer(Gravity.START);
        });

        addPhoto.setOnClickListener(e->{
            //open gallery to pic an image
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser( galleryIntent ,"SELECT IMAGE") ,GALERY_PICK);
        });


        myLogo.setOnClickListener(e->{
            //go to my profile
            transaction.replace(R.id.main_frag ,new ProfileFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        myPost.setOnClickListener(e->{  //create post view
            transaction.replace(R.id.main_frag ,new PostTextFrag() )
                    .addToBackStack(null)
                    .commit();
        });

        newFeed_icon.setOnClickListener(e->{
            //scroll to top ==> what is new ?
            postsListview.setSelection(0);
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

        notif_icon.setOnClickListener(e->{
            activity.show_notifications(notifications);
        });
    }

    private void load_myPosts(){
        if (user != null){

            posts = new ArrayList<>();
            adapter = new PostAdapter(posts ,getActivity() );
            postsListview.setAdapter(adapter);

            ref = FirebaseDatabase.getInstance().getReference("posts").child(user.getUid() );
            ref.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //a post is created by me or one of my friends
                    Collections.reverse(posts);

                    Post post = dataSnapshot.getValue(Post.class);
                    posts.add(post);

                    Collections.reverse(posts);
                    adapter.notifyDataSetChanged();
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

    private void load_myNotifications(){
        notifications = new ArrayList<>();

        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference("notifications");
        notRef.child(user.getUid()).addChildEventListener(new ChildEventListener() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode ,resultCode ,data);

        if (requestCode == GALERY_PICK && resultCode == RESULT_OK){
            Uri imageUri = data.getData();

            //create a post with image
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frag ,new PostImageFrag(imageUri) )
                    .addToBackStack(null)
                    .commit();
        }
    }
}