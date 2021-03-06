package com.tech42.sathish.firebasechat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tech42.sathish.firebasechat.FireChatHelper.ChatHelper;
import com.tech42.sathish.firebasechat.adapter.UsersChatAdapter;
import com.tech42.sathish.firebasechat.model.User;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public class RegisterActivity extends AppCompatActivity {

    private EditText displayname,email,password;
    private Button register,image;
    private ImageView image_avatar;
    private AlertDialog alertDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String imageEncoded;
    private static final int REQUEST_IMAGE_CAPTURE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = (EditText)findViewById(R.id.email);
        password = (EditText)findViewById(R.id.password);
        displayname = (EditText)findViewById(R.id.displayname);
        register = (Button)findViewById(R.id.register);
        image = (Button) findViewById(R.id.image);
        image_avatar = (ImageView) findViewById(R.id.img_avatar);

        // Get Instance for firebase authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Get Instance
        databaseReference = FirebaseDatabase.getInstance().getReference();

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // User registration in firebase
                onRegisterUser();
            }
        });

        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLaunchCamera();
            }
        });

    }


    // Show Alert Dialog
    private void showAlertDialog(String message){

        alertDialog = ChatHelper.buildAlertDialog("Error!",message,true,RegisterActivity.this);
        alertDialog.show();
    }

    // Get Displayname
    private String getUserDisplayName() {
        return displayname.getText().toString().trim();
    }

    // Get Email
    private String getUserEmail() {
        return email.getText().toString().trim();
    }

    // Get Password
    private String getUserPassword() {
        return password.getText().toString().trim();
    }

    // Get Password
    private String getImageUrl() {
        return imageEncoded;
    }

    // Check Email format
    private boolean isIncorrectEmail(String userEmail) {
        return !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches();
    }

    // Check Password Length
    private boolean isIncorrectPassword(String userPassword) {
        return !(userPassword.length() >= 6);
    }

    // User registration in firebase
    private void onRegisterUser() {
        if(getUserDisplayName().equals("") || getUserEmail().equals("") || getUserPassword().equals("")){
            showAlertDialog(getString(R.string.error_fields_empty));
        }
        else if(isIncorrectEmail(getUserEmail()) || isIncorrectPassword(getUserPassword())) {
            showAlertDialog(getString(R.string.error_incorrect_email_pass));
        }
        else {
            signUp(getUserEmail(), getUserPassword());
        }
    }

    // Signup method
    private void signUp(String email, String password) {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering..");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    onAuthSuccess(task.getResult().getUser());
                }else {
                    showAlertDialog(task.getException().getMessage());
                }
            }
        });
    }

    // Dismiss Alertdialog
    private void dismissAlertDialog() {
        alertDialog.dismiss();
    }

    // Login Successfully
    private void onAuthSuccess(FirebaseUser user) {
        createNewUser(user.getUid());
        goToMainActivity();
    }

    // To Create the new User
    private void createNewUser(String userId){
        User user = buildNewUser();
        databaseReference.child("users").child(userId).setValue(user);
    }

    // Go to next Activity
    private void goToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private User buildNewUser() {
        return new User(
                getUserDisplayName(),
                getUserEmail(),
                UsersChatAdapter.ONLINE,
                ChatHelper.generateRandomAvatarForUser(),
                new Date().getTime(),
                getImageUrl()
        );
    }



    /*-------------------- Image upload to the firebase ------------------------------*/

    public void onLaunchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            image_avatar.setImageBitmap(imageBitmap);
            encodeBitmapAndSaveToFirebase(imageBitmap);
        }
    }

    public void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }


}
