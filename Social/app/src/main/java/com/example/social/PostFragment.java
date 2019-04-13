package com.example.social;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import de.hdodenhof.circleimageview.CircleImageView;
import static android.app.Activity.RESULT_OK;

public class PostFragment extends Fragment {

    private ListView postComments;
    private Post post;
    private ArrayList<Comment> comments;
    private FirebaseUser iam;
    private Boolean liked = false;
    private DatabaseReference ref;
    private SharedPreferences preferences;
    private static final int GALERY_PICK = 1;
    private MainActivity mainActivity;


    public PostFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ValidFragment")
    public PostFragment(Post post){
        this.post = post;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_post, container, false);
        
        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        iam = FirebaseAuth.getInstance().getCurrentUser();
        postComments = view.findViewById(R.id.postComments);

        View header = getActivity().getLayoutInflater().inflate(R.layout.post_layout ,postComments ,false);
        postComments.addHeaderView(header ,null ,false);

        CircleImageView postLogo = header.findViewById(R.id.postLogo);
        LinearLayout like = header.findViewById(R.id.like) ,comment = header.findViewById(R.id.comment) ,share = header.findViewById(R.id.share)
                ,commentTools = view.findViewById(R.id.commentTools);

        TextView postOwner = header.findViewById(R.id.postOwner) ,postTime = header.findViewById(R.id.postTime) ,postText = header.findViewById(R.id.postText)
                ,likeTxt = header.findViewById(R.id.likeTxt);

        ImageView postImage = header.findViewById(R.id.postImage) ,likeShape = header.findViewById(R.id.likeShape) ,
                photoComment = view.findViewById(R.id.photoComment) ,send = view.findViewById(R.id.send);

        EditText txtComment = view.findViewById(R.id.txtComment);

        //load post
        postOwner.setText(post.authorName);

        if (! "default".equals(post.authorImage)){
            Glide.with(getActivity())
                    .load(post.authorImage)
                    .centerCrop()
                    .into(postLogo);
        }

        mainActivity = (MainActivity) getActivity();
        preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);

        String since = mainActivity.getSince(post.postTime);

        postTime.setText(since);

        if (! "".equals(post.postText))
            postText.setText(post.postText);
        else
            postText.setVisibility(View.GONE);

        if (! "".equals(post.authorImage))
            Glide.with(getActivity()).load(post.postImage).centerCrop().into(postImage);
        else
            postImage.setVisibility(View.GONE);


        //check if iam liked this post before
        ref = FirebaseDatabase.getInstance().getReference("post likes").child(post.author).child(post.postId);
        ref.child(iam.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //
                if (dataSnapshot.exists() ) {     //i liked it before
                    liked = true;
                    likeTxt.setText("Liked");
                    likeTxt.setTextColor(getActivity().getResources().getColor(R.color.violet));
                    likeShape.setImageResource(R.mipmap.liked);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        load_postComments();


        //events
        like.setOnClickListener(e->{
            ref = FirebaseDatabase.getInstance().getReference("post likes").child(post.author).child(post.postId);

            if (liked){
                //delete like
                ref.child(iam.getUid() ).removeValue();

                likeShape.setImageResource(R.mipmap.default_like);
                likeTxt.setText("Like");
                likeTxt.setTextColor(getActivity().getResources().getColor(R.color.darkSilver));

                liked = false;
            }
            else {
                //send like
                HashMap map = new HashMap();
                map.put("name" ,preferences.getString("name" ,""));
                map.put("image" ,preferences.getString("image" ,"default"));

                ref.child(iam.getUid() ).setValue(map);

                likeTxt.setText("Liked");
                likeTxt.setTextColor(getActivity().getResources().getColor(R.color.violet));
                likeShape.setImageResource(R.mipmap.liked);

                liked = true;
            }
        });

        comment.setOnClickListener(e->{
            //scroll to bottom and show comment tools
            int lastPos = postComments.getLastVisiblePosition();

            if (lastPos >= 1)
                postComments.setSelection(1);

            commentTools.setVisibility(View.VISIBLE);
        });

        txtComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = txtComment.getText().toString();

                if (! TextUtils.isEmpty(text.trim() ))
                    send.setVisibility(View.VISIBLE);

                else
                    send.setVisibility(View.GONE);
            }
        });

        send.setOnClickListener(e->{
            Comment newComment = new Comment();
            newComment.authorId = iam.getUid();
            newComment.authorImage = preferences.getString("image" ,"default");
            newComment.authorName = preferences.getString("name" ,"");
            newComment.commentTxt = txtComment.getText().toString();

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("post comments").child(post.author).child(post.postId);
            reference.push().setValue(newComment);
        });

        photoComment.setOnClickListener(e->{
            //open galery to select image
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser( galleryIntent ,"SELECT IMAGE") ,GALERY_PICK);
        });
    }

    private void load_postComments(){
        comments = new ArrayList<>();
        CommentAdapter adapter = new CommentAdapter(comments ,getActivity() );
        postComments.setAdapter(adapter);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("post comments").child(post.postId);
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //
                Comment comment = dataSnapshot.getValue(Comment.class);
                comments.add(comment);
                adapter.notifyDataSetChanged();
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
            Toast.makeText(getActivity() ,"uploading image .." ,Toast.LENGTH_LONG).show();

            float imageSize = mainActivity.getImage_size(imageUri);
            int quality = 15;

            if (imageSize <= 0.5)
                quality = 40;

            try {
                byte[] imageBytes = mainActivity.compress_image(imageUri ,quality);

                DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference("post comments").child(post.author).child(post.postId);
                String commentId = dataRef.push().getKey();

                StorageReference stoRef = FirebaseStorage.getInstance().getReference("comment images").child(post.author).child(post.postId);
                UploadTask uploadTask;
                uploadTask = stoRef.child(commentId).putBytes(imageBytes);

                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return stoRef.getDownloadUrl();

                }).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();     //path of the image

                        Comment comment = new Comment();
                        comment.authorId = iam.getUid();
                        comment.authorImage = preferences.getString("image" ,"default");
                        comment.authorName = preferences.getString("name" ,"");
                        comment.commentImage = downloadUri.toString();

                        dataRef.child(commentId).setValue(comment);
                        Toast.makeText(getActivity() ,"comment is added" ,Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}