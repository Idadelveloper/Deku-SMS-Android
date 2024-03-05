package com.afkanerd.deku.E2EE;

import android.os.Bundle;
import android.provider.Telephony;
import android.util.Base64;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.Models.ThreadingPoolExecutor;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    protected ThreadedConversations threadedConversations;
    View securePopUpRequest;

    protected String keystoreAlias;

    protected boolean isSelf = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    boolean isEncrypted = false;

    /**
     *
     * @param text
     * @param subscriptionId
     * @param threadedConversations
     * @param messageId
     * @param _mk
     * @throws NumberParseException
     * @throws InterruptedException
     */
    @Override
    public void sendTextMessage(final String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId,
                                final byte[] _mk, boolean _isSelf) throws NumberParseException, InterruptedException {
        if(threadedConversations.is_secured && !isEncrypted) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[][] cipherText = E2EEHandler.encrypt(getApplicationContext(),
                                keystoreAlias, text.getBytes(StandardCharsets.UTF_8), isSelf);
                        String encryptedText = E2EEHandler.buildTransmissionText(cipherText[0]);
                        isEncrypted = true;
                        sendTextMessage(encryptedText, subscriptionId, threadedConversations,
                                messageId, cipherText[1], isSelf);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            isEncrypted = false;
            super.sendTextMessage(text, subscriptionId, threadedConversations, messageId, _mk,
                    _isSelf);
        }
    }

    @Override
    public void informSecured(boolean secured) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                threadedConversations.is_secured = secured;
                if(secured && securePopUpRequest != null) {
                    securePopUpRequest.setVisibility(View.GONE);
                    TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
                    layout.setPlaceholderText(getString(R.string.send_message_secured_text_box_hint));
                }

            }
        });
    }

    protected void sendDataMessage(ThreadedConversations threadedConversations) {
        final int subscriptionId = SIMHandler.getDefaultSimSubscription(getApplicationContext());
        ThreadingPoolExecutor.executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String,  byte[]> transmissionRequestKeyPair =
                            E2EEHandler.buildForEncryptionRequest(getApplicationContext(),
                                    threadedConversations.getAddress(), null);

                    final String messageId = String.valueOf(System.currentTimeMillis());
                    Conversation conversation = new Conversation();
                    conversation.setThread_id(threadedConversations.getThread_id());
                    conversation.setAddress(threadedConversations.getAddress());
                    conversation.setIs_key(true);
                    conversation.setMessage_id(messageId);
                    conversation.setData(Base64.encodeToString(transmissionRequestKeyPair.second,
                            Base64.DEFAULT));
                    conversation.setSubscription_id(subscriptionId);
                    conversation.setType(Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                    conversation.setDate(String.valueOf(System.currentTimeMillis()));
                    conversation.setStatus(Telephony.Sms.STATUS_PENDING);

                    long id = conversationsViewModel.insert(getApplicationContext(), conversation);
                    SMSDatabaseWrapper.send_data(getApplicationContext(), conversation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void showSecureRequestPopUpMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.conversation_secure_popup_request_menu_title));

        View conversationSecurePopView = View.inflate(getApplicationContext(),
                R.layout.conversation_secure_popup_menu, null);
        builder.setView(conversationSecurePopView);

        Button yesButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_send);
        Button cancelButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_cancel);
        TextView descriptionText = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_text_description);
        String descriptionTextRevised = descriptionText.getText()
                .toString()
                .replaceAll("\\[contact name]", threadedConversations.getContact_name() == null ?
                        threadedConversations.getAddress() : threadedConversations.getContact_name());
        descriptionText.setText(descriptionTextRevised);

        AlertDialog dialog = builder.create();

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataMessage(threadedConversations);
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSecureRequestPopUpMenu();
            }
        });
    }

    public void setEncryptionThreadedConversations(ThreadedConversations threadedConversations) {
        this.threadedConversations = threadedConversations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
        setSecurePopUpRequest();
//        if(!SettingsHandler.alertNotEncryptedCommunicationDisabled(getApplicationContext()))
//            securePopUpRequest.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.conversation_main_menu_encrypt_lock == item.getItemId()) {
            if(securePopUpRequest != null) {
                securePopUpRequest.setVisibility(securePopUpRequest.getVisibility() == View.VISIBLE ?
                        View.GONE : View.VISIBLE);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(threadedConversations != null) {
            ThreadingPoolExecutor.executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        keystoreAlias = E2EEHandler.deriveKeystoreAlias(
                                threadedConversations.getAddress(), 0);
                        threadedConversations.is_secured =
                                E2EEHandler.canCommunicateSecurely(getApplicationContext(),
                                        keystoreAlias);
                        if(threadedConversations.is_secured) {
                            isSelf = E2EEHandler.isSelf(getApplicationContext(), keystoreAlias);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextInputLayout layout = findViewById(R.id.conversations_send_text_layout);
                                    layout.setPlaceholderText(getString(R.string.send_message_secured_text_box_hint));
                                }
                            });
                        }
                    } catch (IOException | GeneralSecurityException | NumberParseException |
                             InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
