package com.example.social;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class SignFrag extends Fragment {


    public SignFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sign, container, false);
        define_layout(view);

        return view;
    }


    private void define_layout(View view) {
        SignInButton google = view.findViewById(R.id.google_SignBtn);
        TextView textView = (TextView) google.getChildAt(0);    //for changing the google signIn text
        textView.setText("Sign in with Google");

        google.setOnClickListener(e -> {

            signIn_with_google();
        });
    }

    private void signIn_with_google(){
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getActivity() , gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 1);
    }

    private void firebase_authWith_google(Intent data){
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            // Google Sign In was successful, authenticate with Firebase
            GoogleSignInAccount account = task.getResult(ApiException.class);

            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(getActivity() , new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful() ) {
                                // Sign in success, update UI with the signed-in user's information

                                store_userData();
                            }
                            else
                                Toast.makeText(getActivity() ,"sign in failed, try again later" ,Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (ApiException e) {
            Toast.makeText(getActivity(), "Google sign in failed : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void show_mainPage(){
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_frag ,new MainFrag() ).commit();
    }

    private void store_userData(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid() );

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //
                SharedPreferences preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                if (dataSnapshot.exists() ){
                    //this user has old data    //signed out before ==> recover his data
                    User oldUser = dataSnapshot.getValue(User.class);

                    editor.putString("name" ,oldUser.getName() )
                            .putString("image" ,oldUser.getImage())
                            .putString("gender" ,oldUser.getGender())
                            .putString("birthDate" ,oldUser.getBirthDate())
                            .putString("country" ,oldUser.getCountry())
                            .apply();
                }
                else {
                    HashMap map = new HashMap();
                    map.put("name" ,user.getDisplayName() );
                    map.put("image" ,"default");
                    map.put("gender" ,"Male");
                    map.put("birthDate" ,"");
                    map.put("country" ,"");
                    map.put("id" ,user.getUid() );

                    ref.setValue(map);

                    editor.putString("name" ,user.getDisplayName() )
                            .putString("image" ,"default")
                            .putString("gender" ,"male")
                            .putString("birthDate" ,"")
                            .putString("country" ,"")
                            .apply();
                }

                Toast.makeText(getActivity() ,"signed in successfully" ,Toast.LENGTH_LONG).show();
                show_mainPage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {     //signed with Google
            firebase_authWith_google(data);
        }
    }
}