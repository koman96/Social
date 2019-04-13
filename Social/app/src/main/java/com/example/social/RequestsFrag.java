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
import java.util.ArrayList;
import java.util.Collections;

public class RequestsFrag extends Fragment {

    private DatabaseReference ref;
    private FirebaseUser iam;

    public RequestsFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        TextView requestsTxt = view.findViewById(R.id.requestsTxt);
        ListView requestsList = view.findViewById(R.id.requestsList);

        iam = FirebaseAuth.getInstance().getCurrentUser();
        ref = FirebaseDatabase.getInstance().getReference("friend requests");

        ArrayList<User> users = new ArrayList<>();
        RequestsAdapter adapter = new RequestsAdapter(users ,getActivity() );
        requestsList.setAdapter(adapter);

        ref.child(iam.getUid() ).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //
                Collections.reverse(users);
                User user = dataSnapshot.getValue(User.class);
                users.add(user);

                Collections.reverse(users);
                adapter.notifyDataSetChanged();
                requestsTxt.setText("Your friendship requests :");
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                User user = dataSnapshot.getValue(User.class);
                users.remove(user);
                adapter.notifyDataSetChanged();

                if (users.size() == 0)
                    requestsTxt.setText("You don't have friendship requests");

            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
}