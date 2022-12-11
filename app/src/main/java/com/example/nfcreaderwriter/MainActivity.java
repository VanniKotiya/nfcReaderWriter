package com.example.nfcreaderwriter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected ="No NFC Tag Detected";
    public static final String Write_Success ="Item added Successfully";
    public static final String Write_Error ="Error while adding the Item";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters;
    boolean writeMode;
    Tag myTag;
    Context context;

    TextView editMessage;
    TextView nfcContent;
    Button activateButton;



    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            editMessage = (TextView) findViewById(R.id.editMessage);
            nfcContent = (TextView) findViewById(R.id.nfcContent);
            activateButton = (Button) findViewById(R.id.activateButton);
            context = this;

                activateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            if (myTag== null){
                                Toast.makeText(context, Error_Detected,Toast.LENGTH_LONG).show();
                            }else{
                                write("PlainText | "+ editMessage.getText().toString(),myTag);
                                Toast.makeText(context, Write_Success,Toast.LENGTH_LONG).show();
                            }
                        }catch(IOException e){
                            Toast.makeText(context, Write_Error,Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }catch (FormatException e){
                            Toast.makeText(context, Write_Error,Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                });
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter == null){
                Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
                finish();
            }
            readFromIntent(getIntent());
            pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[] {tagDetected};

        }
        private void readFromIntent(Intent intent){
            String action = intent.getAction();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                    ||NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                    ||NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
                Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage[] msg = null;
                if (rawMsg != null){
                    msg = new NdefMessage[rawMsg.length];
                    for (int i =0; i < rawMsg.length;i++){
                        msg[i] = (NdefMessage) rawMsg[i];
                    }
                }
                buildTagViews(msg);
            }
        }
        private void buildTagViews(NdefMessage[] msg){
            if (msg == null || msg.length == 0) return;

            String text = "";
//            String tagId = new String(msg[0].getRecords()[0].getPayload());
            byte[] payload = msg[0].getRecords()[0].getPayload();
            String textEncoding = ((payload[0]&128)== 0)? "UTF-8" :"UTF-16";// get text encoding
            int languageCodeLength = payload[0] & 0063; // get language code ex. "en"
            //String languageCode = new String(payload,1,languageCodeLength,"US-ASCII");

            try {
                //Get text
                text = new String(payload,languageCodeLength+1,payload.length-languageCodeLength-1, textEncoding);
            }catch (UnsupportedEncodingException e){
                Log.e("UnsupportedEncoding",e.toString());
            }

            nfcContent.setText("NFC Content " +text);
        }
    }
