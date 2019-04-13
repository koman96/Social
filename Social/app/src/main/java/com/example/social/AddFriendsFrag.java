package com.example.social;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
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

public class AddFriendsFrag extends Fragment {

    private ListView sugListView;
    private ArrayList<User> users;
    private SuggestionsAdapter adapter;
    private FirebaseUser iam;


    public AddFriendsFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_friends, container, false);

        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        sugListView = view.findViewById(R.id.sugListView);
        iam = FirebaseAuth.getInstance().getCurrentUser();

        //load other users (not friends with me)
        users = new ArrayList<>();
        adapter = new SuggestionsAdapter(users ,getActivity() );
        sugListView.setAdapter(adapter);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //check if this user is not me and not my friend
                String userId = dataSnapshot.getKey();

                if (! userId.equals(iam.getUid() )) {

                    DatabaseReference myFriends = FirebaseDatabase.getInstance().getReference("friends");
                    myFriends.child(iam.getUid() ).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            if (! dataSnapshot2.exists() ){
                                // this is a stranger that i could add
                                Collections.reverse(users);
                                User user = dataSnapshot.getValue(User.class);

                                users.add(user);
                                Collections.reverse(users);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
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