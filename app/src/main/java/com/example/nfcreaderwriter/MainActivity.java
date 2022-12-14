package com.example.nfcreaderwriter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
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
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected ="No NFC Tag Detected";
    public static final String Write_Success ="Item added Successfully";
    public static final String Write_Error ="Error while adding the Item";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter[] writingTagFilters;
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
            editMessage = findViewById(R.id.editMessage);
            nfcContent = findViewById(R.id.nfcContent);
            activateButton = findViewById(R.id.activateButton);
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
            pendingIntent = PendingIntent.getActivity(this,0,new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writingTagFilters = new IntentFilter[] {tagDetected};

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
        private void write (String text, Tag tag) throws IOException, FormatException {
            NdefRecord[] records = { createRecord(text)};
            NdefMessage message = new NdefMessage(records);
            //Get an instance of Ndef for the tag
            Ndef ndef= Ndef.get(tag);
            //Enable I/O
            ndef.connect();
            //Write the Message
            ndef.writeNdefMessage(message);
            //Close the Connection
            ndef.close();

        }
        private  NdefRecord createRecord (String text) throws UnsupportedEncodingException{
            String lang = "en";
            byte[] textBytes = text.getBytes();
            byte[] langBytes = lang.getBytes("US-ASCII");
            int langLength = langBytes.length;
            int textLength = textBytes.length;
            byte[] payload = new byte[1 + langLength + textLength];

            //set status byte (see Ndef spec for acutal bits)
            payload[0] = (byte) langLength;

            //copy langbytes and textbytes into payload
            System.arraycopy(langBytes,0,payload,1,              langLength);
            System.arraycopy(textBytes,0,payload,1 + langLength, textLength);

            NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

            return  recordNFC;
        }
        //when detect a tag this is what happens
        @Override
        protected void onNewIntent (Intent intent){
            super.onNewIntent(intent);
            setIntent(intent);
            readFromIntent(intent);
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }
        }
        @Override
        public void onPause(){
            super.onPause();
            WriteModeOff();
        }
        @Override
        public void onResume(){
            super.onResume();
            WriteModeOn();
        }


        //Enable write
        private void WriteModeOn(){
            writeMode = true;
            nfcAdapter.enableForegroundDispatch(this,pendingIntent,writingTagFilters,null);
        }

        //Disable Write
        public void WriteModeOff(){
            writeMode = false;
            nfcAdapter.disableForegroundDispatch(this);

        }

    }
