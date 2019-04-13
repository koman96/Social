package com.example.social;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;
import static android.app.Activity.RESULT_OK;

public class EditProfFrag extends Fragment {

    private static final int GALERY_PICK = 1;
    private static final int REQUEST_CODE = 11;
    private CircleImageView myImage;
    private Uri imageUri = null;
    private TextView dateTxt;
    private String new_name ,new_image ,new_gender ,new_birth ,new_country;

    public EditProfFrag() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_prof, container, false);

        define_layout(view);
        return view;
    }

    private void define_layout(View view) {
        myImage = view.findViewById(R.id.myImage);
        EditText newName = view.findViewById(R.id.newName);
        RadioButton male = view.findViewById(R.id.male);
        RadioButton female = view.findViewById(R.id.female);
        dateTxt = view.findViewById(R.id.dateTxt);
        Spinner countrySpin = view.findViewById(R.id.country);
        Button save = view.findViewById(R.id.save);

        //upload old data
        SharedPreferences preferences = getActivity().getSharedPreferences("myData" , Context.MODE_PRIVATE);
        String name = preferences.getString("name" ,"");
        String image = preferences.getString("image" ,"");
        String gender = preferences.getString("gender" ,"");
        String birthDate = preferences.getString("birthDate" ,"");
        String country = preferences.getString("country" ,"");

        newName.setText(name);
        if (!"default".equals(image) && !"".equals(image)){
            Glide.with(getActivity() )
                    .load(image)
                    .centerCrop()
                    .into(myImage);
        }
        if (gender.equals("Male"))
            male.setChecked(true);
        else
            female.setChecked(true);

        if (!"".equals(birthDate) )
            dateTxt.setText(birthDate);

        //get all countries in spinner
        Locale[] locale = Locale.getAvailableLocales();
        ArrayList<String> countries = new ArrayList<>();
        String rowCountry;

        for( Locale loc : locale ){
            rowCountry = loc.getDisplayCountry();
            if( rowCountry.length() > 0 && !countries.contains(country) ){
                countries.add( rowCountry );
            }
        }

        Collections.sort(countries, String.CASE_INSENSITIVE_ORDER);
        countrySpin.setAdapter( new ArrayAdapter<>(getActivity() ,R.layout.row_country ,countries) );

        if (! "".equals(country)){
            //
            if (countries.contains(country)) {
                int myCountryOrder = countries.indexOf(country);
                countrySpin.setSelection(myCountryOrder);
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid() );


        //events
        newName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s.toString().trim() )){
                    save.setEnabled(false);
                    save.setBackgroundResource(R.color.darkSilver);
                }
                else {
                    save.setEnabled(true);
                    save.setBackgroundResource(R.color.lightViolet);
                }
            }
        });

        myImage.setOnClickListener(e->{
            //open gallery to pic an image
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser( galleryIntent ,"SELECT IMAGE") ,GALERY_PICK);
        });

        dateTxt.setOnClickListener(e->{
            AppCompatDialogFragment dateFrag = new DateFrag();
            dateFrag.setTargetFragment(this ,REQUEST_CODE);
            dateFrag.show(getFragmentManager() ,null);
        });

        save.setOnClickListener(e->{
            new_name = newName.getText().toString();

            if (TextUtils.isEmpty(new_name.trim() ))
                Toast.makeText(getActivity() ,"Please enter your name" ,Toast.LENGTH_LONG).show();

            else{   //user name is valid

                new_gender = "Male";
                if (! male.isChecked() )
                    new_gender = "Female";

                new_birth = dateTxt.getText().toString();

                new_country = (String) countrySpin.getSelectedItem();

                if (imageUri != null) {     //user has selected image
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("profile pictures").child(user.getUid() );
                    String imageKey = reference.push().getKey();

                    StorageReference imageRef = FirebaseStorage.getInstance().getReference("profile pictures")
                            .child(user.getUid() ).child(imageKey);

                    //show progress bar
                    LinearLayout progressDialog = view.findViewById(R.id.progressDialog);
                    progressDialog.setVisibility(View.VISIBLE);
                    save.setEnabled(false);
                    save.setBackgroundResource(R.color.darkSilver);

                    //compress image
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();

                        int imageSize = bitmap.getByteCount()/1000;
                        Log.d("SIZE" ,String.valueOf(imageSize)+" KB");

                        bitmap.compress(Bitmap.CompressFormat.JPEG ,15 ,stream);
                        byte[] bytes = stream.toByteArray();

                        Toast.makeText(getActivity() ,"uploading image ..." ,Toast.LENGTH_LONG).show();

                        UploadTask uploadTask = imageRef.putBytes(bytes);

                        uploadTask.continueWithTask(task -> {
                            if (! task.isSuccessful())
                                throw task.getException();

                            return imageRef.getDownloadUrl();
                        }).addOnCompleteListener(task -> {
                            //
                            if (task.isSuccessful() ){
                                //image is uploaded
                                new_image = task.getResult().toString();
                                reference.child(imageKey).setValue(new_image);

                                preferences.edit().putString("name" ,new_name)
                                        .putString("image" ,new_image )
                                        .putString("gender" ,new_gender)
                                        .putString("birthDate" ,new_birth)
                                        .putString("country" ,new_country)
                                        .apply();

                                HashMap map = new HashMap();
                                map.put("name" ,new_name);
                                map.put("image" ,new_image);
                                map.put("gender" ,new_gender);
                                map.put("birthDate" ,new_birth);
                                map.put("country" ,new_country);
                                map.put("id" ,user.getUid());

                                ref.setValue(map);

                                DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
                                String postId = postsRef.child(user.getUid() ).push().getKey();

                                //create post "mojo has changed his profile pic"
                                HashMap postMap = new HashMap();
                                postMap.put("postText" ,"proPIC");
                                postMap.put("postImage" ,new_image);
                                postMap.put("postTime" ,Calendar.getInstance().getTime().toString() );
                                postMap.put("author" ,user.getUid() );
                                postMap.put("postId" ,postId);
                                map.put("authorName" ,preferences.getString("name" ,""));
                                map.put("authorName" ,preferences.getString("image" ,"default"));

                                postsRef.child(user.getUid() ).child(postId).setValue(postMap);

                                //add post to my friends
                                DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");
                                friendsRef.child(user.getUid() ).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        //
                                        if (dataSnapshot.exists() ) {     //i have friends
                                            for (DataSnapshot friend : dataSnapshot.getChildren() ){
                                                friend.getRef().child(postId).setValue(postMap);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });

                                progressDialog.setVisibility(View.GONE);

                                Toast.makeText(getActivity() ,"changes has been saved" ,Toast.LENGTH_LONG).show();
                                goto_mainPage();
                            }
                        });
                    }catch (Exception ex){
                        Toast.makeText(getActivity() ,"Couldn't compress the image" ,Toast.LENGTH_LONG).show();
                    }
                }
                else {    //didn't
                    new_image = "default";

                    preferences.edit().putString("name", new_name)
                            .putString("image", new_image)
                            .putString("gender", new_gender)
                            .putString("birthDate", new_birth)
                            .putString("country", new_country)
                            .apply();

                    HashMap map = new HashMap();
                    map.put("name", new_name);
                    map.put("image", new_image);
                    map.put("gender", new_gender);
                    map.put("birthDate", new_birth);
                    map.put("country", new_country);
                    map.put("id" ,user.getUid());

                    ref.setValue(map);

                    Toast.makeText(getActivity() ,"changes has been saved" ,Toast.LENGTH_LONG).show();
                    goto_mainPage();
                }
            }
        });
    }


    private void goto_mainPage(){
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frag ,new MainFrag() )
                .commit();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //image selected
        if (requestCode == GALERY_PICK && resultCode == RESULT_OK) {

            imageUri = data.getData();
            Glide.with(getActivity())
                    .load(imageUri)
                    .centerCrop()
                    .into(myImage);
        }

        //date selected
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            dateTxt.setText(data.getStringExtra("date") );
        }
    }


    //birthday picker
    public static class DateFrag extends AppCompatDialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity() ,this ,year ,month ,day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            String date = dayOfMonth +" / "+(month+1) +" / "+year;

            //send date back to fragment
            getTargetFragment().onActivityResult(getTargetRequestCode() ,RESULT_OK
                    ,new Intent().putExtra("date" ,date ));
        }
    }
}