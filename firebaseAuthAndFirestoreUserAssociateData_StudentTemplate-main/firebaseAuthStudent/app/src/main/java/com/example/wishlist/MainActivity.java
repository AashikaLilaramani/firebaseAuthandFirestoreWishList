package com.example.wishlist;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Denna";
    private EditText nameET, emailET, passwordET;
    private TextView signUpResultTextView;
    private Button signInButton, signUpButton, signOutButton, showListButton, addItemButton;

    // create public static FirebaseHelper variable
    // this variable is accessible in all other activities by MainActivity.firebaseHelper
    // this will allow us to access auth and firestore anywhere we need it

    public static FirebaseHelper firebaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiate FirebaseHelper var
        firebaseHelper = new FirebaseHelper();

        // Make references to xml elements
        nameET = findViewById(R.id.nameTV);
        emailET = findViewById(R.id.emailTV);
        passwordET = findViewById(R.id.passwordTV);
        signUpResultTextView = findViewById(R.id.signUpResultTV);

        signInButton = findViewById(R.id.signInButton);
        signUpButton = findViewById(R.id.signUpButton);
        signOutButton = findViewById(R.id.signOutButton);
        showListButton = findViewById(R.id.showListButton);
        addItemButton = findViewById(R.id.addItemButton);


    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        updateIfLoggedIn();
    }

    public void updateIfLoggedIn(){
        // Create reference to current user using firebaseHelper variable
         FirebaseUser user = firebaseHelper.getmAuth().getCurrentUser();

        if (user != null) {
            signInButton.setVisibility(View.INVISIBLE);
            signUpButton.setVisibility(View.INVISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            showListButton.setVisibility(View.VISIBLE);
            addItemButton.setVisibility(View.VISIBLE);
            signUpResultTextView.setText(user.getEmail() + " is signed in");
            firebaseHelper.attachReadDataToUser();
        }
        else {
            signInButton.setVisibility(View.VISIBLE);
            signUpButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.INVISIBLE);
            showListButton.setVisibility(View.INVISIBLE);
            addItemButton.setVisibility(View.INVISIBLE);
            signUpResultTextView.setText("");
            nameET.setText("");
            emailET.setText("");
            passwordET.setText("");
            signUpResultTextView.setText("No one is signed in");
        }
    }

    public void signIn(View v) {
        // Note we don't care what they entered for name here
        // it could be blank

        // Get user data
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        // verify all user data is entered
        if (email.length() == 0 || password.length() == 0) {
            Toast.makeText(getApplicationContext(), "Enter all fields", Toast.LENGTH_SHORT).show();
        }

        // verify password is at least 6 char long (otherwise firebase will deny)
        else if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password must be at least 6 char long", Toast.LENGTH_SHORT).show();
        }
        else {

            // code to sign in user
            firebaseHelper.getmAuth().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful())
                            {
                                // updating MY var for the UID of current user
                                firebaseHelper.updateUid(firebaseHelper.getmAuth().getCurrentUser().getUid());
                                Log.i(TAG, email + " is signed in");

                                // this will help us with the asych method calls
                                firebaseHelper.attachReadDataToUser();

                                // lets further investigate why this method call is needed
                                firebaseHelper.attachReadDataToUser();

                                // we can do any other UI updating or change screens based on how our app
                                // should respond
                                updateIfLoggedIn();

                                // this is another way to create the intent from inside the onCompleteListener
                                Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
                                startActivity(intent);

                            }
                            else {
                                // sign in failed
                                Log.d(TAG, email + " failed to log in");
                            }


                        }
                    });

        }
    }

    public void signUp(View v) {
        // Make references to EditText in xml
        nameET = findViewById(R.id.nameTV);
        emailET = findViewById(R.id.emailTV);
        passwordET = findViewById(R.id.passwordTV);

        // Get user data
        String name = nameET.getText().toString();
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        Log.i(TAG, name + " " + email + " " + password);

        // verify all user data is entered
        if (name.length() == 0 || email.length() == 0 || password.length() == 0) {
            Toast.makeText(getApplicationContext(), "Enter all fields", Toast.LENGTH_SHORT).show();
        }

        // verify password is at least 6 char long (otherwise firebase will deny)
        else if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password must be at least 6 char long", Toast.LENGTH_SHORT).show();
        }
        else {
            // code to sign up user
        firebaseHelper.getmAuth().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        // user account was created in firebase auth
                        Log.i(TAG, email + "account created");
                        FirebaseUser user = firebaseHelper.getmAuth().getCurrentUser();

                        //update the firebaseHelper variable UID to equal the UID of the currently signed in user
                        firebaseHelper.updateUid(user.getUid());

                        // add a document to our database to represent this user
                        firebaseHelper.addUserToFirestore(name, user.getUid());

                        // lets further investigate why this method call is needed
                        firebaseHelper.attachReadDataToUser();

                        // choose whatever actions you want - update UI, switch to new screen, etc.
                        // ex: take the user to the screen where they can enter wishlist items
                        // getApplicationContext() will get the Activity we are currently in, that is sending
                        // the intent. similar to using "this"
                        Intent intent = new Intent(getApplicationContext(), AddItemActivity.class);
                        startActivity(intent);
                    }
                    else {
                        //user WASN'T created
                        Log.d(TAG, email + " sign up failed");
                    }
                }
            });


        }

        updateIfLoggedIn();
    }


    public void addData(View v) {
        Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
        startActivity(intent);
    }

    public void signOutUser(View v) {
        // firebaseHelper code to sign out
        // do I need getInstance()

        firebaseHelper.getmAuth().signOut();
        firebaseHelper.updateUid(null); // no " " used on null
        Log.i(TAG, "user logged out");
        
        nameET.setText("");
        emailET.setText("");
        passwordET.setText("");
        // refresh the UI for a new user to log in
        // without it, buttons would still be hidden (wouldn't be able to log back in)
        updateIfLoggedIn();
    }

    public void showList(View v) {
        Intent intent = new Intent(MainActivity.this, ViewListActivity.class);
        // use firebaseHelperCode to get List of data to display
        //ArrayList<WishListItem> myList = firebaseHelper.getWishListItems();
        //intent.putParcelableArrayListExtra( "LIST", myList);
        startActivity(intent);
    }

}
