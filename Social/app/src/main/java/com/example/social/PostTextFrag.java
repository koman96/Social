package com.example.social;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;
import java.util.HashMap;
import de.hdodenhof.circleimageview.CircleImageView;


public class PostTextFrag extends Fragment {
    private DatabaseReference ref;

    public PostTextFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_post_text, container, false);
        define_layout(view);

        return view;
    }

    private void define_layout(View view) {
        CircleImageView postLogo = view.findViewById(R.id.postLogo);
        TextView postOwner = view.findViewById(R.id.postOwner);

        EditText myPostText = view.findViewById(R.id.myPostText);
        Button share = view.findViewById(R.id.share);

        //load my image and name
        SharedPreferences preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);
        String name = preferences.getString("name" ,"");
        String image = preferences.getString("image" ,"");

        if (!"default".equals(image) && ! "".equals(image) ) {
            Glide.with(getActivity() )
                    .load(image)
                    .centerCrop()
                    .into(postLogo);
        }
        postOwner.setText(name);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


        //events
        myPostText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = myPostText.getText().toString();

                if (! TextUtils.isEmpty(text.trim() )){
                    share.setBackgroundColor(getResources().getColor(R.color.lightViolet));
                    share.setEnabled(true);
                }
                else {
                    share.setBackgroundColor(getResources().getColor(R.color.silver));
                    share.setEnabled(false);
                }
            }
        });

        share.setOnClickListener(e->{
            String text = myPostText.getText().toString();

            ref = FirebaseDatabase.getInstance().getReference("posts").child(user.getUid() );
            String postId = ref.push().getKey();

            HashMap map = new HashMap();
            map.put("postText" ,text);
            map.put("postImage" ,"");
            map.put("postTime" , Calendar.getInstance().getTime().toString() );
            map.put("author" ,user.getUid() );
            map.put("postId" ,postId);
            map.put("authorName" ,preferences.getString("name" ,""));
            map.put("authorName" ,preferences.getString("image" ,"default"));

            ref.child(postId).setValue(map);

            //add post to my friends so they can see it
            ref = FirebaseDatabase.getInstance().getReference("friends");       //get my friends
            ref.child(user.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //
                    if (dataSnapshot.exists() ) {     //i have friends
                        DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("posts");

                        for(DataSnapshot single : dataSnapshot.getChildren() )
                            friendRef.child(single.getKey() ).child(postId).setValue(map);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            //go to main page
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.main_frag ,new MainFrag() )
                    .commit();
        });
    }
}