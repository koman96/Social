package com.example.social;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends BaseAdapter {

    private ArrayList<Comment> comments;
    private FragmentActivity activity;

    public CommentAdapter (ArrayList<Comment> comments , FragmentActivity activity){
        this.comments = comments;
        this.activity = activity;
    }

    public class MyHolder{
        CircleImageView userImage;
        TextView userName ,commentTxt;
        ImageView commentImage;
    }

    @Override
    public int getCount() {
        return comments.size();
    }

    @Override
    public Object getItem(int position) {
        return comments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final MyHolder holder;

        if (view == null){
            view = activity.getLayoutInflater().inflate(R.layout.comment_row ,null);
            holder = new MyHolder();

            holder.userImage = view.findViewById(R.id.userImage);
            holder.userName = view.findViewById(R.id.userName);
            holder.commentTxt = view.findViewById(R.id.commentTxt);
            holder.commentImage = view.findViewById(R.id.commentImage);

            view.setTag(holder);
        }
        else
            holder = (MyHolder) view.getTag();

        Comment comment = comments.get(position);
        holder.userName.setText(comment.authorName);

        if (! "default".equals(comment.authorImage))
            Glide.with(activity).load(comment.authorImage).centerCrop().into(holder.userImage);

        if (! "".equals(comment.commentTxt)) {
            holder.commentTxt.setVisibility(View.VISIBLE);
            holder.commentTxt.setText(comment.commentTxt);
        }

        if (! "".equals(comment.commentImage)){
            holder.commentImage.setVisibility(View.VISIBLE);
            Glide.with(activity).load(comment.commentImage).centerCrop().into(holder.commentImage);
        }

        holder.userImage.setOnClickListener(e->{
            MainActivity mainActivity = (MainActivity) activity;

            if (comment.authorId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid() ))
                mainActivity.goto_myProfile();

            else {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(comment.authorId);
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() ){

                            User user = dataSnapshot.getValue(User.class);
                            mainActivity.goto_profile(user);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }
        });

        return view;
    }
}