package com.blikoon.rooster;

/**
 * Created by gakwaya on 4/16/2016.
 */
public class Contact {
    private String jid;

    public Contact(String contactJid )
    {
        jid = contactJid;
    }

    public String getJid()
    {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }
}
