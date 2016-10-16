package com.blikoon.rooster;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jivesoftware.smack.chat.ChatManager;

import java.util.List;

public class ContactListActivity extends AppCompatActivity implements AddReceiverDialog_Fragment.UserNameListener,
        ContactAdapter.mClickListener{

    private static final String TAG = "ContactListActivity";
    private FloatingActionButton fab;
    private RecyclerView contactsRecyclerView;
    private ContactAdapter mAdapter;
    private List<Contact> contacts;

    @Override
    public void mClick(Contact contact){
        Intent intent = new Intent(ContactListActivity.this
                ,ChatActivity.class);
        intent.putExtra("EXTRA_CONTACT_JID",contact.getJid());
        startActivity(intent);
    }

    @Override
    public void onFinishUserDialog(String user) {
        if(AllUtil.isValidId(user)){
            Contact contact = new Contact(user);
            contacts.add(contact);
            mAdapter.notifyItemInserted(contacts.size()-1);
            Toast.makeText(this, "Add " + user, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Wrong ID Xmpp", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        fab = (FloatingActionButton)findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditDialog();
            }
        });
        contactsRecyclerView = (RecyclerView) findViewById(R.id.contact_list_recycler_view);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        ContactModel model = ContactModel.get(getBaseContext());
        contacts = model.getContacts();
        Log.d("status Login : ",RoosterConnection.LoggedInState.LOGGED_IN.toString());
        mAdapter = new ContactAdapter(contacts,this);
        contactsRecyclerView.setAdapter(mAdapter);
    }

    private void showEditDialog() {
        FragmentManager fm = getSupportFragmentManager();
        AddReceiverDialog_Fragment editNameDialogFragment = AddReceiverDialog_Fragment.newInstance("Some Title");
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }
}
