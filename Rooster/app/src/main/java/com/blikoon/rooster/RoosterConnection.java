package com.blikoon.rooster;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by gakwaya on 4/28/2016.
 */

public class RoosterConnection implements ConnectionListener,ChatMessageListener {

    private static final String TAG = "RoosterConnection";
    private static final String tesID = "asneiya31@xmpp.jp";
    private  final Context mApplicationContext;
    private  final String mUsername;
    private  final String mPassword;
    private  final String mServiceName;
    private  final String jid;
    private XMPPTCPConnection mConnection;
    private BroadcastReceiver uiThreadMessageReceiver;//Receives messages from the ui thread.
    private NotificationManager mNotification;
    private int notifyID = 21;
    private int numMsg = 0;
    private NotificationCompat.Builder mNotifyBuilder;


    public static enum ConnectionState
    {
        CONNECTED ,AUTHENTICATED, CONNECTING ,DISCONNECTING ,DISCONNECTED;
    }

    public static enum LoggedInState
    {
        LOGGED_IN , LOGGED_OUT;
    }


    public RoosterConnection( Context context)
    {
        Log.d(TAG,"RoosterConnection Constructor called.");
        mApplicationContext = context.getApplicationContext();
        mNotification = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyBuilder =  new NotificationCompat.Builder(context)
                        .setContentTitle("SMS Pulsa")
                        .setContentText("You've received new messages.")
                        .setSmallIcon(R.mipmap.ic_launcher);

        jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_jid",null);
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString("xmpp_password",null);

        if( jid != null)
        {
            mUsername = jid.split("@")[0];
            mServiceName = jid.split("@")[1];
        }else
        {
            mUsername ="";
            mServiceName="";
        }
    }


    public void connect() throws IOException, XMPPException,SmackException
    {
        Log.d(TAG, "Connecting to server " + mServiceName);
        XMPPTCPConnectionConfiguration.Builder builder=
                XMPPTCPConnectionConfiguration.builder();
        builder.setServiceName(mServiceName);
        builder.setUsernameAndPassword(mUsername, mPassword);
        builder.setPort(5222);
        //builder.setRosterLoadedAtLogin(true);
        builder.setResource("Rooster");

        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver();

        mConnection = new XMPPTCPConnection(builder.build());
        mConnection.addConnectionListener(this);
        mConnection.connect();
        mConnection.login();

        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mConnection);
        reconnectionManager.setEnabledPerDefault(true);
        reconnectionManager.enableAutomaticReconnection();
        Roster roster = Roster.getInstanceFor(mConnection);
        Presence presence = roster.getPresence(tesID);
        if(presence.isAvailable()){
            Log.d(tesID," available");
        }
        else {
            Log.e(tesID,"ora ono jee");
        }
    }

    private void setupUiThreadBroadCastMessageReceiver() {
        uiThreadMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                    //Check if the Intents purpose is to send the message.
                    String action = intent.getAction();
                    if (action.equals(RoosterConnectionService.SEND_MESSAGE)) {
                        Bundle bundle = intent.getExtras();
                        if(bundle!=null){
                            //Send the message.
                            sendMessage(intent.getStringExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY),
                                    intent.getStringExtra(RoosterConnectionService.BUNDLE_TO));
                        }
                    }
                    else if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
                        Bundle myBundle = intent.getExtras();
                        SmsMessage [] messages = null;
                        String strMessage = "";

                        if (myBundle != null)
                        {
                            Object [] pdus = (Object[]) myBundle.get("pdus");

                            messages = new SmsMessage[pdus.length];

                            for (int i = 0; i < messages.length; i++)
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    String format = myBundle.getString("format");
                                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                                }
                                else {
                                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                                }
                                strMessage += "SMS From: " + messages[i].getOriginatingAddress();
                                strMessage += " : ";
                                strMessage += messages[i].getMessageBody();
                                //show notification
                                mNotifyBuilder.setContentText(messages[i].getMessageBody())
                                        .setNumber(numMsg++);
                                mNotification.notify(notifyID,mNotifyBuilder.build());
                                strMessage += "\n";
                            }

                            Log.e(" SMS >>", strMessage);
                            Toast.makeText(context, strMessage, Toast.LENGTH_SHORT).show();
                            sendMessage(strMessage,tesID);
                        }
                    }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(RoosterConnectionService.SEND_MESSAGE);
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter);
    }


    private void sendMessage ( String body ,String toJid)
    {
        Log.d(TAG,"Sending message to :"+ toJid);
        Chat chat = ChatManager.getInstanceFor(mConnection)
                .createChat(toJid,this);
        try
        {
            if(mConnection.isAuthenticated()){
                chat.sendMessage(body);
            }
        }catch (SmackException.NotConnectedException e)
        {
            e.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void processMessage(Chat chat, Message message) {

        if(message.getBody()!=null){

            Log.d(TAG,"message.getBody() :"+message.getBody());
            Log.d(TAG,"message.getFrom() :"+message.getFrom());

            String from = message.getFrom();
            String contactJid="";
            if ( from.contains("/"))
            {
                contactJid = from.split("/")[0];
                Log.d(TAG,"The real jid is :" +contactJid);
            }else
            {
                contactJid=from;
            }

            //Bundle up the intent and send the broadcast.
            Intent intent = new Intent(RoosterConnectionService.NEW_MESSAGE);
            intent.setPackage(mApplicationContext.getPackageName());
            intent.putExtra(RoosterConnectionService.BUNDLE_FROM_JID,contactJid);
            intent.putExtra(RoosterConnectionService.BUNDLE_MESSAGE_BODY,message.getBody());
            mApplicationContext.sendBroadcast(intent);
            Log.d(TAG,"Received message from :"+contactJid+" broadcast sent.");
        }

    }


    public void disconnect()
    {
        Log.d(TAG,"Disconnecting from serser "+ mServiceName);
        try
        {
            if (mConnection.isAuthenticated())
            {
                mConnection.disconnect();
            }

        }catch (Exception e)
        {
            RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
            e.printStackTrace();
        }
        mConnection = null;
        // Unregister the message broadcast receiver.
        if( uiThreadMessageReceiver != null)
        {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver);
            uiThreadMessageReceiver = null;
        }

    }


    @Override
    public void connected(XMPPConnection connection) {
        RoosterConnectionService.sConnectionState=ConnectionState.CONNECTED;
        Log.d(TAG,"Connected Successfully");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean arg0) {
        RoosterConnectionService.sConnectionState=ConnectionState.AUTHENTICATED;
        ChatManager.getInstanceFor(mConnection).createChat(tesID,this);
        Log.d(TAG,"Authenticated Successfully");
        showContactListActivityWhenAuthenticated();

    }

    @Override
    public void connectionClosed() {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"Connectionclosed()");

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        RoosterConnectionService.sConnectionState=ConnectionState.DISCONNECTED;
        Log.d(TAG,"ConnectionClosedOnError, error "+ e.toString());

    }

    @Override
    public void reconnectingIn(int seconds) {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTING;
        Log.d(TAG,"ReconnectingIn() ");

    }

    @Override
    public void reconnectionSuccessful() {
        RoosterConnectionService.sConnectionState = ConnectionState.CONNECTED;
        Log.d(TAG,"ReconnectionSuccessful()");

    }

    @Override
    public void reconnectionFailed(Exception e) {
        RoosterConnectionService.sConnectionState = ConnectionState.DISCONNECTED;
        Log.d(TAG,"ReconnectionFailed()");

    }

    private void showContactListActivityWhenAuthenticated()
    {
        Intent i = new Intent(RoosterConnectionService.UI_AUTHENTICATED);
        i.setPackage(mApplicationContext.getPackageName());
        mApplicationContext.sendBroadcast(i);
        Log.d(TAG,"Sent the broadcast that we are authenticated");
    }
}
