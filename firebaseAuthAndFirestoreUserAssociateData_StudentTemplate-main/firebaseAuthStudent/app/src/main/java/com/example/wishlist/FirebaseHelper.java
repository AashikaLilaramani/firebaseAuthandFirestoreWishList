package com.example.wishlist;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The purpose of this class is to hold ALL the code to communicate with Firebase.  This class
 * will connect with Firebase auth and Firebase firestore.  Each class that needs to verify
 * authentication OR access data from the database will reference a variable of this class and
 * call a method of this class to handle the task.  Essentially this class is like a "gopher" that
 * will go and do whatever the other classes want or need it to do.  This allows us to keep all
 * our other classes clean of the firebase code and also avoid having to update firebase code
 * in many places.  This is MUCH more efficient and less error prone.
 */
public class FirebaseHelper {
    public final String TAG = "Denna";
    private static String uid = null;            // var will be updated for currently signed in user

    // create two instance vars for firebaseAuth and firebaseFirestore that will give me access to
    // my project on Firebase. the json file is what links this app to that project
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<WishListItem> myItems = new ArrayList<>();

    public FirebaseHelper() {
        // set up mAuth and db by calling the getInstance() method and this will create the
        // connection to the instance of Auth and Firestore for this project
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // we need to connect the data reading to logged in user if applicable
        attachReadDataToUser();

    }

    public FirebaseAuth getmAuth() {
        return mAuth;
    }

    public void attachReadDataToUser() {
        // This method will do an initial read of the database when we login or create a user
        if (mAuth.getCurrentUser() != null)
        {
            // redundant but guarantees that the UID var is correct
            uid = mAuth.getUid();
            readData(new FirestoreCallback() {
                @Override
                public void onCallback(ArrayList<WishListItem> myList) {
                    Log.i(TAG, "Inside attachReadDataToUser, onCallback");
                }
            });
        }
        else
        {
            Log.i(TAG, "No one is logged in");
        }
    }


    public void addUserToFirestore(String name, String newUID) {
        // Create a new user with their name
        // Use a simple HashMap
        Map<String, Object> user = new HashMap<>();
        user.put("name", name); // adding the data that is in the parameter name

        // Add a new document to the collection called users with a docID = UID of the authorized user.
        // By passing in newUID as a parameter to document, we are able to tell the document
        // what we want its docID to be equal to
        db.collection("users").document(newUID)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, name + " 's user account added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error adding user account", e);
                    }
                });
    }

    public void addData(WishListItem w) {
        // this is the method that is called from the activity. it receives the WishListItem we want to add
        // to Firestore. this method is overloaded with a private addData method that incorporates the
        // interface to handle the async method calls.
        addData(w, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<WishListItem> myList) {
                Log.i(TAG, "inside addData, onCallBack");
            }
        });
    }

    private void addData(WishListItem w, FirestoreCallback firestoreCallback)
    {
        db.collection("users").document(uid).collection("myWishList")
                .add(w)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // this will add the object and return a reference to the newly created document
                        // in the parameter documentReference. we want to set the instance variable of this
                        // WishListItem docID to equal the actual docID of the document.

                        // we will call update method on this document to set its docID variable
                        db.collection("users").document(uid).collection("myWishList")
                                .document(documentReference.getId()).update("docID", documentReference.getId());
                        Log.i(TAG, "just added " + w.getItemName());
                        readData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.i(TAG, "Eroor adding document", e);
                    }
                });
    }

    public ArrayList<WishListItem> getWishListItems() {
        return myItems;
    }
    
    public void editData(WishListItem w) {
        // edit WishListItem w to the database
        // this method is overloaded and incorporates the interface to handle the async calls
        editData(w, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<WishListItem> myList) {
                Log.i(TAG, "Inside editData, onCallback" + myList.toString());
            }
        });

    }

    private void editData(WishListItem w, FirestoreCallback firestoreCallback)
    {
        String docId = w.getDocID();
        db.collection("users").document(uid).collection("myWishList")
                .document(docId)
                .set(w)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Success updating document");
                        readData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error updating document", e);
                    }
                });
    }

    public void deleteData(WishListItem w) {
        // delete item w from database
        // this method is overloaded and incorporates the interface to handle the async calls
        deleteData(w, new FirestoreCallback() {
            @Override
            public void onCallback(ArrayList<WishListItem> myList) {
                Log.i(TAG, "Inside deleteData, onCallBack" + myList.toString());
            }
        });
    }

    private void deleteData(WishListItem w, FirestoreCallback firestoreCallback)
    {
        // delete item w from database
        String docId = w.getDocID();
        db.collection("users").document(uid).collection("myWishList")
                .document(docId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, w.getItemName() + "successfully deleted");
                        readData(firestoreCallback);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error deleting document", e);
                    }
                });
    }

    public void updateUid(String uid) {

    }

    /* https://www.youtube.com/watch?v=0ofkvm97i0s
    This video is good!!!   Basically he talks about what it means for tasks to be asynchronous
    and how you can create an interface and then using that interface pass an object of the interface
    type from a callback method and access it after the callback method.  It also allows you to delay
    certain things from occuring until after the onSuccess is finished.
     */

    private void readData(FirestoreCallback firestoreCallback) {
        // This is necessary to avoid the issues we have ran into with data displaying before we
        // returned from the async get method

        // clear out the AL of data
        myItems.clear();
        db.collection("users").document(uid).collection("myWishList")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            // iterate through ALL the items in the Query
                            // each item is a DocumentSnapshot
                            for (DocumentSnapshot doc: task.getResult())
                            {
                                // convert the snapshot into a WishListItem object
                                WishListItem w = doc.toObject(WishListItem.class);
                                myItems.add(w);
                            }
                            // I am done getting all the data
                            Log.i(TAG, "Success reading all data: " + myItems.toString());
                            firestoreCallback.onCallback(myItems);
                        }
                        else
                        {
                            Log.d(TAG, "Error getting documents", task.getException());
                        }
                    }

                });
    }

//https://stackoverflow.com/questions/48499310/how-to-return-a-documentsnapshot-as-a-result-of-a-method/48500679#48500679
    public interface FirestoreCallback {
        // we use the arraylist of the data type we are working with in firebase
        void onCallback(ArrayList<WishListItem> myList);
    }
}

