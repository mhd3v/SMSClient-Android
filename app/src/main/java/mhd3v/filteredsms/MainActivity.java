package mhd3v.filteredsms;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    private SectionsPageAdapter mSectionsPageAdapter;

    Tab1Fragment knownInstance;
    Tab2Fragment unknownInstance;

    Tab1Fragment.customAdapter knownAdapter;
    Tab2Fragment.customAdapter unknownAdapter;

    private ViewPager mViewPager;

    ArrayList<sms> knownSms = new ArrayList<>();
    ArrayList<sms> unknownSms = new ArrayList<>();
    ArrayList<sms> smsList = new ArrayList<>();

    boolean isContact;
    static boolean refreshInbox = false;
    static boolean active = false;
    static MainActivity inst;

    ProgressBar pb;

    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int READ_CONTACTS_PERMISSIONS_REQUEST = 1;

    public static MainActivity instance() {

        return inst;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED){
            getPermissionToReadSMS();

        }

        else
            refreshSmsInbox();

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager)findViewById(R.id.container);
        setupFragments(mViewPager);

        pb = (ProgressBar) findViewById(R.id.progressBar2);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        tb.setTitle("Filtered Messaging");

        tb.inflateMenu(R.menu.menu_main);

        tb.findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);

            }
        });

//        tb.findViewById(R.id.newmessagebutton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, NewMessage.class);
//                startActivity(intent);
//
//            }
//        });
    }

    void setupFragments(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());

        adapter.addFragment(new Tab1Fragment(), "Known");
        adapter.addFragment(new Tab2Fragment(), "Unknown");

        viewPager.setAdapter(adapter);
    }


    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_SMS)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},
                        READ_SMS_PERMISSIONS_REQUEST);
            }
        }


    }

    public void getPermissionToReadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                        READ_CONTACTS_PERMISSIONS_REQUEST);
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                getPermissionToReadContacts();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }

        }

        if (requestCode == READ_CONTACTS_PERMISSIONS_REQUEST ) {
            if (grantResults.length == 2 &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED ) {
                Toast.makeText(this, "Read Contacts permission granted", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                Toast.makeText(this, "Read Contacts permission denied", Toast.LENGTH_SHORT).show();
            }

        }

        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }

    public void refreshSmsInbox() {

        Cursor cursor = getContentResolver().query(Uri
                .parse("content://sms"), null, null, null, null);

        int indexBody = cursor.getColumnIndex("body");
        int indexAddress = cursor.getColumnIndex("address");
        if (indexBody < 0 || !cursor.moveToFirst()) return;


        String type = Integer.toString(cursor.getColumnIndex("type"));

        do {

            isContact = false;

            if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {

                //received messages

                boolean found = false;

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {
                        String date = cursor.getString(cursor
                                .getColumnIndex("date"));
                        smsList.get(i).addNewSenderMessage(cursor.getString(indexBody), date);
                        found = true;
                    }

                }
                if (found == false) {

                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));

                    sms newSms = new sms(cursor.getString(indexAddress), cursor.getString(cursor.getColumnIndex("thread_id")));
                    newSms.addNewSenderMessage(cursor.getString(indexBody), date);
                    smsList.add(newSms);

                    String contactName;
                    contactName = getContactName(this, newSms.sender);

                    if(isContact == true){

                        newSms.senderName = contactName;
                        knownSms.add(newSms);
                    }
                    else {

                        unknownSms.add(newSms);
                    }

                }

            }

            else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {

                //sent messages

                boolean found = false;

                for (int i = 0; i < smsList.size(); i++) {

                    if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                        String date = cursor.getString(cursor.getColumnIndex("date"));

                        smsList.get(i).addNewUserMessage(cursor.getString(indexBody), date);
                        found = true;
                    }
                }

                if (found == false) {
                    String date = cursor.getString(cursor
                            .getColumnIndex("date"));

                    sms newSms = new sms(cursor.getString(indexAddress),cursor.getString(cursor.getColumnIndex("thread_id")));

                    newSms.addNewUserMessage(cursor.getString(indexBody), date);

                    smsList.add(newSms);

                    String contactName;
                    contactName = getContactName(this, newSms.sender);

                    if(isContact == true){

                        newSms.senderName = contactName;
                        knownSms.add(newSms);
                    }
                    else {

                        unknownSms.add(newSms);
                    }

                }

            }

        } while (cursor.moveToNext());

    }

    public ArrayList<sms> getKnownSms() {

        return knownSms;
    }
    public ArrayList<sms> getUnknownSms() {

        return unknownSms;
    }

    public ArrayList<sms> getSmsList() {

        return smsList;
    }

    public String getContactName(Context context, String phoneNo) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNo));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return phoneNo;
        }
        String Name = phoneNo;
        if (cursor.moveToFirst()) {
            isContact = true;
            Name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return Name;
    }


    @Override
    public void onStart() {

        super.onStart();
        active = true;
        inst = this;

        if(refreshInbox){
            refreshOnExtraThread();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    public static MainActivity getInstance(){
        return inst;
    }

    void refreshFragments(){

        Log.d("mahad", "refreshing because refresh inbox true");

        ArrayList<sms> newKnownList = new ArrayList<>();
        ArrayList<sms> newUnknownList = new ArrayList<>();

        newKnownList.addAll(getKnownSms());
        newUnknownList.addAll(getUnknownSms());

        knownInstance.smsList.clear();
        knownInstance.smsList.addAll(newKnownList);
        knownAdapter.notifyDataSetChanged();

        unknownInstance.smsList.clear();
        unknownInstance.smsList.addAll(newUnknownList);
        unknownAdapter.notifyDataSetChanged();

        knownInstance.knownList.setVisibility(View.VISIBLE);
        unknownInstance.unknownList.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);

        refreshInbox = false;

    }

    public void setKnownInstance(Tab1Fragment knownInstance) {
        this.knownInstance = knownInstance;
    }

    public void setUnknownInstance(Tab2Fragment unknownInstance) {
        this.unknownInstance = unknownInstance;
    }

    public void setKnownAdapter(Tab1Fragment.customAdapter adapter) {

        knownAdapter = adapter;
    }

    public void setUnknownAdapter(Tab2Fragment.customAdapter adapter) {
        unknownAdapter = adapter;
    }

    public void refreshOnExtraThread() {

        new refreshInboxOnNewThread().execute();


    }

    public void fabClicked(View view) {
        Intent intent = new Intent(MainActivity.this, NewMessage.class);
        startActivity(intent);
    }

    class refreshInboxOnNewThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            knownInstance.knownList.setVisibility(View.GONE);
            unknownInstance.unknownList.setVisibility(View.GONE);
            pb.setVisibility(View.VISIBLE);

        }

        @Override
        protected Void doInBackground(Void... params) {

//            try {
//                Thread.sleep(1000); //waiting for db update
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

                knownSms.clear();
                smsList.clear();
                unknownSms.clear();


                Cursor cursor = getContentResolver().query(Uri
                        .parse("content://sms"), null, null, null, null);


                int indexBody = cursor.getColumnIndex("body");
                int indexAddress = cursor.getColumnIndex("address");


                cursor.moveToFirst();


                String type = Integer.toString(cursor.getColumnIndex("type"));


                do {

                    isContact = false;

                    if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("1")) {

                        //received messages

                        boolean found = false;

                        for (int i = 0; i < smsList.size(); i++) {

                            if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {
                                String date = cursor.getString(cursor
                                        .getColumnIndex("date"));
                                smsList.get(i).addNewSenderMessage(cursor.getString(indexBody), date);
                                found = true;
                            }

                        }
                        if (found == false) {

                            String date = cursor.getString(cursor
                                    .getColumnIndex("date"));

                            sms newSms = new sms(cursor.getString(indexAddress), cursor.getString(cursor.getColumnIndex("thread_id")));
                            newSms.addNewSenderMessage(cursor.getString(indexBody), date);
                            smsList.add(newSms);

                            String contactName;
                            contactName = getContactName(inst, newSms.sender);

                            if(isContact == true){

                                newSms.senderName = contactName;
                                knownSms.add(newSms);
                            }
                            else {

                                unknownSms.add(newSms);
                            }

                        }

                    }

                    else if (cursor.getString(Integer.parseInt(type)).equalsIgnoreCase("2")) {

                        ///sent messages

                        boolean found = false;

                        for (int i = 0; i < smsList.size(); i++) {

                            if (smsList.get(i).threadId.equals(cursor.getString(cursor.getColumnIndex("thread_id")))) {

                                String date = cursor.getString(cursor.getColumnIndex("date"));

                                smsList.get(i).addNewUserMessage(cursor.getString(indexBody), date);
                                found = true;
                            }
                        }

                        if (found == false) {
                            String date = cursor.getString(cursor
                                    .getColumnIndex("date"));

                            sms newSms = new sms(cursor.getString(indexAddress),cursor.getString(cursor.getColumnIndex("thread_id")));

                            newSms.addNewUserMessage(cursor.getString(indexBody), date);

                            smsList.add(newSms);

                            String contactName;
                            contactName = getContactName(inst, newSms.sender);

                            if(isContact == true){

                                newSms.senderName = contactName;
                                knownSms.add(newSms);
                            }
                            else {

                                unknownSms.add(newSms);
                            }

                        }

                    }

                } while (cursor.moveToNext());

                return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            refreshFragments();

        }
    }

}