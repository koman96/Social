package com.example.social;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseUser iam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        check_authority();
    }

    private void check_authority() {
        iam = FirebaseAuth.getInstance().getCurrentUser();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (iam == null)
            transaction.replace(R.id.main_frag ,new SignFrag() ).commit();
        else
            transaction.replace(R.id.main_frag, new MainFrag()).commit();
    }

    public void signOut(){
        getSharedPreferences("myData" ,Context.MODE_PRIVATE).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this ,gso);
        mGoogleSignInClient.signOut();
    }

    public void reloadApp(){
        finish();
        startActivity(getIntent() );
    }

    public void delete_account(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //delete my friends
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");
        friendsRef.child(user.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //
                if (dataSnapshot.exists() ){
                    for (DataSnapshot friend : dataSnapshot.getChildren() ){
                        //
                        DatabaseReference friendRef = friendsRef.child(friend.getKey() ).child(user.getUid() );
                        friendRef.removeValue();
                        friend.getRef().removeValue();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("friend requests");
        reqRef.child(user.getUid() ).removeValue();

        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference("notifications");
        notRef.child(user.getUid() ).removeValue();

        DatabaseReference picRef = FirebaseDatabase.getInstance().getReference("profile pictures");
        picRef.child(user.getUid() ).removeValue();

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("posts");
        postRef.child(user.getUid() ).removeValue();

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        myRef.child(user.getUid() ).removeValue();

        signOut();
        reloadApp();
    }

    public float getImage_size(Uri image){
        float dataSize = 0;
        try {
            String scheme = image.getScheme();

            if(scheme.equals(ContentResolver.SCHEME_CONTENT))
            {
                InputStream fileInputStream = getContentResolver().openInputStream(image);
                dataSize = fileInputStream.available();
            }
            else if(scheme.equals(ContentResolver.SCHEME_FILE))
            {
                File f = new File(image.getPath() );
                dataSize = f.length();
            }
        }catch (Exception e){
            Toast.makeText(this ,"couldn't get image size" ,Toast.LENGTH_LONG).show();
        }
        return dataSize/(1024*1024);
    }

    public void check_friendReq(){
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("friend requests");
        if (iam == null)
            iam = FirebaseAuth.getInstance().getCurrentUser();

        reqRef.child(iam.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() ){
                    //some one want to be my friend !!
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_frag ,new RequestsFrag() )
                            .addToBackStack(null)
                            .commit();
                }
                else
                    Toast.makeText(getApplicationContext() ,"you don't have any requests yet" ,Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void send_notification(String receiver ,Notification notification){
        //
        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference("notifications").child(receiver);
        String notificationId = notRef.push().getKey();

        notification.notificationId = notificationId;
        notRef.child(notificationId).setValue(notification);
    }

    public void goto_profile(User user){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new OtherProfileFrag(user))
                .addToBackStack(null)
                .commit();
    }

    public void goto_myProfile(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new ProfileFrag())
                .addToBackStack(null)
                .commit();
    }

    public void show_notifications(ArrayList<Notification> notifications){
        if (notifications.size() == 0)
            Toast.makeText(this ,"You don't have notifications yet" ,Toast.LENGTH_LONG).show();

        else {
            //make all notifications seen and show them
            iam = FirebaseAuth.getInstance().getCurrentUser();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications").child(iam.getUid() );

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //
                    if (dataSnapshot.exists() ) {   //i have notifications
                        for (DataSnapshot row : dataSnapshot.getChildren() )
                            row.child("seen").getRef().setValue(true);
                    }

                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_frag, new NotificationsFragment(notifications))
                            .addToBackStack(null)
                            .commit();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    public String getSince(String time){
        String since = "";
        //
        return since;
    }

    public byte[] compress_image(Uri imageUri ,int quality) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver() ,imageUri);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    public void show_postComments(Post post){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new PostFragment(post))
                .addToBackStack(null)
                .commit();
    }
}