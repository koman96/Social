package com.example.social;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FriendsFrag extends Fragment {

    private DatabaseReference ref;
    private FirebaseUser myUser;

    public FriendsFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        TextView friendsTxt = view.findViewById(R.id.friendsTxt);
        ListView friendsList = view.findViewById(R.id.friendsList);

        myUser = FirebaseAuth.getInstance().getCurrentUser();

        //check if i have friends
        ref = FirebaseDatabase.getInstance().getReference("friends");
        ref.child(myUser.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (! dataSnapshot.exists() ){
                    //i don't have friends yet
                    friendsTxt.setText("You don't have friends yet ,click 'Add Friends' from side bar to see other users");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        ArrayList<User> users = new ArrayList<>();
        ArrayList<String> friendSince = new ArrayList<>();

        FriendsAdapter adapter = new FriendsAdapter(users ,getActivity() ,friendSince);
        friendsList.setAdapter(adapter);

        ref.child(myUser.getUid() ).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //
                DatabaseReference friend = FirebaseDatabase.getInstance().getReference("users");
                friend.child(dataSnapshot.getKey() ).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                        //
                        if (dataSnapshot2.exists() ) {
                            User user = dataSnapshot2.getValue(User.class);
                            users.add(user);
                            friendSince.add(dataSnapshot.child("friendsSince").getValue().toString() );

                            adapter.notifyDataSetChanged();
                            friendsTxt.setText("Your Friends :");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                adapter.notifyDataSetChanged();

                if (users.size() == 0)
                    friendsTxt.setText("You don't have friends yet.");
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}