package edu.cftic.fichapp.actividades;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import edu.cftic.fichapp.R;
import edu.cftic.fichapp.helper.InternetDetector;
import edu.cftic.fichapp.helper.Utils;

public class EnviarMailActivity extends AppCompatActivity {


    FloatingActionButton sendFabButton;
    EditText edtToAddress, edtSubject, edtMessage, edtAttachmentData;
    private ImageView img11;
    Toolbar toolbar;
    GoogleAccountCredential mCredential;
    String emaildestion, emailtitulo, emailmensaje, emailfichero;

    ProgressDialog mProgress;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.MAIL_GOOGLE_COM
    };
    private InternetDetector internetDetector;
    private final int SELECT_PHOTO = 1;
    public String fileName = "";
    private Uri photo_uri;
    String email;
    String mlastErrorString= "valor inicial";

    @Override


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //->https://www.c-sharpcorner.com/article/java-mail-api-using-gmail-oauth-api-in-android/

    /*
        You need to set up your Android app key in Google Dev Console.
        https://stackoverflow.com/questions/25668152/gmail-api-access-using-android

        1-Choose your project, select API & Auth, then click Credentials
        2-Create new client id (though it has other client ids)
        3-Select installed app -> android
        4-Fill in your package name and SHA1 correctly
        5-Create new Key (though it has other client keys)
        6-Select Android key
        7-Fill in the SHA1;packageName like this: 45:B5:E4:6F:36:AD:0A:98:94:B4:02:66:2B:12:17:F2:56:26:A0:E0;
            com.example
        Your problem will be automatically solved. Be sure to create client id and key
        with both your debug keystore and release keystore
     */
        email = getIntent().getStringExtra("email");

        init();

    }

    private void init() {
        // Initializing Internet Checker
        internetDetector = new InternetDetector(getApplicationContext());

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        //DATOS PARA ENVIAR EL INFORME

        emaildestion = email;
        emailtitulo = "Informe";
        emailmensaje = "Adjunto se envia el fichero .pdf con el reporte mensual";

        // request to attacth the report pdf file to the e-mail
        Intent intent = new Intent(this, EnviarMailEnviarActivity.class);
        startActivityForResult(intent, Utils.REQUEST_INSERT_FILE_REPORT);
        // The execution will continue in onActivityResult for REQUEST_INSERT_FILE_REPORT(2)
    }


    private void showMessage(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
    private void getResultsFromApisinTemplate() {
        Log.i("MIAPP", "Lista de mCredential es : " + mCredential.getAllAccounts().toString());
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccountsinPlantilla();
        } else if (!internetDetector.checkMobileInternetConn()) {
            //        showMessage(view, "No network connection available.");
            Log.i("MIAPP", "get result api es :" + "sin Red");
            //    } else if (!Utils.isNotEmpty(edtToAddress)) {
            //        showMessage(view, "To address Required");
            //    } else if (!Utils.isNotEmpty(edtSubject)) {
            //        showMessage(view, "Subject Required");
            //    } else if (!Utils.isNotEmpty(edtMessage)) {
            //        showMessage(view, "Message Required");
        } else {
            new MakeRequestTask(this, mCredential).execute();
        }

    }

    // Method for Checking Google Play Service is Available
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    // Method to Show Info, If Google Play Service is Not Available.
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    // Method for Google Play Services Error Info
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                EnviarMailActivity.this,
                connectionStatusCode,
                Utils.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    // Storing Mail ID using Shared Preferences

    private void chooseAccountsinPlantilla() {

        if (Utils.checkPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApisinTemplate();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), Utils.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            ActivityCompat.requestPermissions(EnviarMailActivity.this,
                    new String[]{Manifest.permission.GET_ACCOUNTS}, Utils.REQUEST_PERMISSION_GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.REQUEST_PERMISSION_GET_ACCOUNTS:
                chooseAccountsinPlantilla();
                //chooseAccount(sendFabButton);
                break;
            case SELECT_PHOTO:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Utils.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                } else {
                    getResultsFromApisinTemplate();
                }
                break;
            case Utils.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApisinTemplate();
                    }
                }
                break;
            case Utils.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApisinTemplate();
                }
                break;
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    final Uri imageUri = data.getData();
                    fileName = getPathFromURI(imageUri);
                    emailfichero = fileName;
                }
                break;
            case Utils.REQUEST_INSERT_FILE_REPORT:
                String filepath = data.getStringExtra("MESSAGE");
                // check if file is present
                File file = new File(filepath);

                if (file.exists()) {
                    // IT WILL SEND THE MAIL WITH THE REPORT FILE
                    photo_uri = Uri.parse(filepath);
                    fileName = filepath;
                    Log.i("MIAPP", "fileName es : " + fileName);
                    emailfichero = fileName;

                } else { // REPORT FILE IS NOT SENT AND E-MAIL subjet and message body are changed.
                    emailtitulo = "Informe No disponible";
                    emailmensaje = "Contacte con el el administrador";
                }
                getResultsFromApisinTemplate();
                break;
        }
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, "", null, "");
        assert cursor != null;
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    // Async Task for sending Mail using GMail OAuth
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private View view = sendFabButton;
        private EnviarMailActivity activity;

        MakeRequestTask(EnviarMailActivity activity, GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.app_name))
                    .build();
            this.activity = activity;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                mlastErrorString = "Error"+ e;
                cancel(true);
                return null;
            }
        }

        private String getDataFromApi() throws IOException {
            String user = "me";
            String to = emaildestion;
            String from = mCredential.getSelectedAccountName();
            String subject = emailtitulo;
            String body = emailmensaje;
            MimeMessage mimeMessage;
            String response = "";
            try {
                mimeMessage = createEmail(to, from, subject, body);
                response = sendMessage(mService, user, mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return response;
        }

        // Method to send email

        private String sendMessage(Gmail service,
                                   String userId,
                                   MimeMessage email)
                throws MessagingException, IOException {
            Message message = createMessageWithEmail(email);
            // GMail's official method to send email with oauth2.0
            message = service.users().messages().send(userId, message).execute();

            System.out.println("Message id: " + message.getId());
            System.out.println(message.toPrettyString());
            return message.getId();
        }

        // Method to create email Params
        private MimeMessage createEmail(String to,
                                        String from,
                                        String subject,
                                        String bodyText) throws MessagingException {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            InternetAddress tAddress = new InternetAddress(to);
            InternetAddress fAddress = new InternetAddress(from);

            email.setFrom(fAddress);
            email.addRecipient(javax.mail.Message.RecipientType.TO, tAddress);
            email.setSubject(subject);

            // Create Multipart object and add MimeBodyPart objects to this object
            Multipart multipart = new MimeMultipart();

            // Changed for adding attachment and text
            BodyPart textBody = new MimeBodyPart();
            textBody.setText(bodyText);
            multipart.addBodyPart(textBody);

            if (!(activity.fileName.equals(""))) {
                // Create new MimeBodyPart object and set DataHandler object to this object
                MimeBodyPart attachmentBody = new MimeBodyPart();
                String filename = activity.fileName; // change accordingly
                DataSource source = new FileDataSource(filename);
                attachmentBody.setDataHandler(new DataHandler(source));
                attachmentBody.setFileName(filename);
                multipart.addBodyPart(attachmentBody);
            }

            //Set the multipart object to the message object
            email.setContent(multipart);
            return email;
        }

        private Message createMessageWithEmail(MimeMessage email)
                throws MessagingException, IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            email.writeTo(bytes);
            String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
            Message message = new Message();

            message.setRaw(encodedEmail);
            return message;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String output) {
            //        mProgress.hide();
            Log.i("MIAPP", "onPostExecute - he terminado de enviar el email");
            finish();
            mlastErrorString = "ENVIADO CORRECTAMENTE";
            Intent intent_reciver = new Intent("SERVICIO_TERMINADO_FICHAPP");
            intent_reciver.putExtra("CODIGO", mlastErrorString);
            sendBroadcast(intent_reciver);
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                    mlastErrorString = "Error en Google Services"+mLastError;
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Utils.REQUEST_AUTHORIZATION);
                    mlastErrorString = "Error en Autorizacion"+mLastError;
                } else {
                    Log.v("Error", mLastError + "");
                }
            } else {
                mlastErrorString = "Error"+mLastError;
            }
            Intent intent_reciver = new Intent("SERVICIO_TERMINADO_FICHAPP");
            intent_reciver.putExtra("CODIGO", mlastErrorString);
            sendBroadcast(intent_reciver);
            finish();
        }
    }
}
