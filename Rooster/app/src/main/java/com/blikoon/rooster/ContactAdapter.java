package com.blikoon.rooster;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by neobyte on 10/15/2016.
 */

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactHolder>
{
    private List<Contact> mContacts;
    private mClickListener klik;

    public interface mClickListener {
        public void mClick(Contact contact);
    }

    public ContactAdapter(List<Contact> contactList, mClickListener klik)
    {
        this.klik = klik;
        mContacts = contactList;
    }

    @Override
    public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater
                .inflate(R.layout.list_item_contact, parent,
                        false);
        return new ContactHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactHolder holder, int position) {
        Contact contact = mContacts.get(position);
        holder.bindContact(contact, klik);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public class ContactHolder extends RecyclerView.ViewHolder
    {
        private TextView contactTextView;
        private Contact mContact;
        public ContactHolder (final View itemView)
        {
            super(itemView);
            contactTextView = (TextView) itemView.findViewById(R.id.contact_jid);
        }


        public void bindContact(final Contact contact, final mClickListener mKlik)
        {
            mContact = contact;
            if (mContact == null)
            {
                Log.d(TAG,"Trying to work on a null Contact object ,returning.");
                return;
            }
            contactTextView.setText(mContact.getJid());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mKlik.mClick(contact);
                }
            });
        }
    }
}