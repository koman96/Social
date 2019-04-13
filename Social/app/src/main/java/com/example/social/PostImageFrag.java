package com.example.social;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import de.hdodenhof.circleimageview.CircleImageView;

public class PostImageFrag extends Fragment {

    private Uri image;
    private DatabaseReference ref;

    public PostImageFrag() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public PostImageFrag(Uri image){
        this.image = image;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_post_image, container, false);
        
        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        CircleImageView postLogo = view.findViewById(R.id.postLogo);
        TextView postOwner = view.findViewById(R.id.postOwner);

        EditText myPostText = view.findViewById(R.id.myPostText);
        ImageView postImage = view.findViewById(R.id.postImage);
        Button share = view.findViewById(R.id.share);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //load my image and name
        SharedPreferences preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);
        String name = preferences.getString("name" ,"");
        String myImage = preferences.getString("image" ,"");

        if (!"default".equals(myImage) && ! "".equals(myImage) ) {
            Glide.with(getActivity())
                    .load(myImage)
                    .centerCrop()
                    .into(postLogo);
        }
        postOwner.setText(name);

        Glide.with(this).load(image).centerCrop().into(postImage);
        share.setBackgroundColor(getResources().getColor(R.color.lightViolet) );
        share.setEnabled(true);

        share.setOnClickListener(e->{
            //show progress dialog
            LinearLayout progressDialog = view.findViewById(R.id.progressDialog);
            progressDialog.setVisibility(View.VISIBLE);
            share.setBackgroundResource(R.color.darkSilver);
            share.setEnabled(false);

            String text = myPostText.getText().toString();

            ref = FirebaseDatabase.getInstance().getReference("posts");
            String postId = ref.child(user.getUid() ).push().getKey();
            String imageName = postId +".jpg";

            StorageReference imageRef = FirebaseStorage.getInstance().getReference("posts images").child(user.getUid() ).child(imageName);
            UploadTask uploadTask;
            //
            try {
                int quality = 15;

                MainActivity activity = (MainActivity) getActivity();
                float imageSize = activity.getImage_size(image);

                if (imageSize <= 0.5)
                    quality = 40;

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver() ,image);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                byte[] byteArray = stream.toByteArray();

                Toast.makeText(getActivity() ,"uploading image ..." ,Toast.LENGTH_LONG).show();

                uploadTask = imageRef.putBytes(byteArray);
                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return imageRef.getDownloadUrl();

                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();     //path of the image

                        HashMap map = new HashMap();
                        map.put("postText", text);
                        map.put("postImage", downloadUri.toString());
                        map.put("postTime", Calendar.getInstance().getTime().toString());
                        map.put("author", user.getUid());
                        map.put("postId" ,postId);
                        map.put("authorName" ,preferences.getString("name" ,""));
                        map.put("authorName" ,preferences.getString("image" ,"default"));

                        ref.child(user.getUid()).child(postId).setValue(map);

                        //add post to my friends so they can see it
                        ref = FirebaseDatabase.getInstance().getReference("friends");       //get my friends
                        ref.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //
                                if (dataSnapshot.exists()) {     //i have friends
                                    DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("posts");

                                    for (DataSnapshot single : dataSnapshot.getChildren())
                                        friendRef.child(single.getKey()).child(postId).setValue(map);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });

                        progressDialog.setVisibility(View.GONE);

                        //go to main page
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.main_frag, new MainFrag())
                                .commit();
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }
}