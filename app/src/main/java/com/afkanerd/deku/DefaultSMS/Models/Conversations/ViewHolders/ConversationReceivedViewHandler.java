package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;


import android.text.Spannable;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.DAO.ConversationDao;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation;
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler;
import com.afkanerd.deku.DefaultSMS.Models.SMSDatabaseWrapper;
import com.afkanerd.deku.DefaultSMS.R;
import com.afkanerd.deku.E2EE.E2EEHandler;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ConversationReceivedViewHandler extends ConversationTemplateViewHandler {
    TextView receivedMessage;
    public TextView date;
    TextView timestamp;

    LinearLayoutCompat linearLayoutCompat;

    LinearLayoutCompat.LayoutParams layoutParams;


    public ConversationReceivedViewHandler(@NonNull View itemView) {
        super(itemView);
        receivedMessage = itemView.findViewById(R.id.message_received_text);
        date = itemView.findViewById(R.id.message_thread_received_date_text);
        timestamp = itemView.findViewById(R.id.received_message_date_segment);
        linearLayoutCompat = itemView.findViewById(R.id.conversation_received_linear_layout);

        layoutParams = (LinearLayoutCompat.LayoutParams) linearLayoutCompat.getLayoutParams();
        layoutParams.bottomMargin = Helpers.dpToPixel(16);
        linearLayoutCompat.setLayoutParams(layoutParams);
    }

    @Override
    public String getMessage_id() {
        return this.message_id;
    }

    @Override
    public String getText() {
        return receivedMessage.getText().toString();
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public void bind(Conversation conversation, String searchString, boolean secured) {
        this.id = conversation.getId();
        this.message_id = conversation.getMessage_id();
        String timestamp = Helpers.formatDateExtended(itemView.getContext(), Long.parseLong(conversation.getDate()));
        DateFormat dateFormat = new SimpleDateFormat("h:mm a");
        String txDate = dateFormat.format(new Date(Long.parseLong(conversation.getDate())));

        final String[] text = {conversation.getText()};
        if(secured) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (text[0] != null && E2EEHandler.isValidDefaultText(text[0])) {
                            String keystoreAlias = E2EEHandler.deriveKeystoreAlias(
                                    conversation.getAddress(), 0);
                            byte[] extractedText = E2EEHandler.extractTransmissionText(text[0]);
//                            text[0] = new String(E2EEHandler.decryptText(itemView.getContext(),
//                                    keystoreAlias, extractedText));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(searchString != null && !searchString.isEmpty() && text[0] != null) {
            Spannable spannable = Helpers.highlightSubstringYellow(itemView.getContext(),
                    text[0], searchString, false);
            receivedMessage.setText(spannable);
        }
        else
            Helpers.highlightLinks(receivedMessage, text[0],
                    itemView.getContext().getColor(R.color.primary_text_color));

        if(conversation.getSubscription_id() > 0) {
            String subscriptionName = SIMHandler.getSubscriptionName(itemView.getContext(),
                    conversation.getSubscription_id());
            if(subscriptionName != null && !subscriptionName.isEmpty())
                txDate += " • " + subscriptionName;
        }
        this.timestamp.setText(timestamp);

        this.date.setText(txDate);
    }

    @Override
    public View getContainerLayout() {
        return receivedMessage;
    }

    @Override
    public void activate() {
        receivedMessage.setBackgroundResource(R.drawable.received_messages_highlighted_drawable);
    }

    @Override
    public void deactivate() {
        receivedMessage.setBackgroundResource(R.drawable.received_messages_drawable);
    }

    @Override
    public void toggleDetails() {
        int visibility = this.date.getVisibility() == View.VISIBLE ?
                View.GONE : View.VISIBLE;
        this.date.setVisibility(visibility);
    }

    @Override
    public void hideDetails() {
        this.date.setVisibility(View.GONE);
    }

    @Override
    public void showDetails() {
        this.date.setVisibility(View.VISIBLE);
    }

    public static class TimestampConversationReceivedViewHandler extends ConversationReceivedViewHandler {
        public TimestampConversationReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class KeyReceivedViewHandler extends ConversationReceivedViewHandler {

        public KeyReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
        }

        @Override
        public void bind(Conversation conversation, String searchString, boolean secured) {
            super.bind(conversation, searchString, secured);
            receivedMessage.setTextAppearance(R.style.key_request_initiated);

            try {
                String keystoreAlias = E2EEHandler.deriveKeystoreAlias(conversation.getAddress(),
                        0);
                byte[] data = Base64.decode(conversation.getData(), Base64.DEFAULT);
                boolean isValidKey = E2EEHandler.isValidDefaultPublicKey(data);

                if(isValidKey) {
                    final byte[] extractedTransmissionKey = E2EEHandler.extractTransmissionKey(data);
                    final int[] keyType = {-1};
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                keyType[0] = E2EEHandler.getKeyType(itemView.getContext(), keystoreAlias,
                                        extractedTransmissionKey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    thread.join();

                    if (keyType[0] == E2EEHandler.REQUEST_KEY ) {
                        receivedMessage.setText(
                                itemView.getContext().getString(R.string.conversation_key_title_agree));

                        receivedMessage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (E2EEHandler.canCommunicateSecurely(itemView.getContext(), keystoreAlias))
                                                return;

                                            byte[] transmissionRequest =
                                                    E2EEHandler.buildForEncryptionRequest(
                                                            itemView.getContext(),
                                                            conversation.getAddress()).second;

                                            Conversation transmitConversation =
                                                    Conversation.buildForDataTransmission(conversation,
                                                            transmissionRequest);

                                            ConversationDao conversationDao =
                                                    conversation.getDaoInstance(itemView.getContext());
                                            long id = conversationDao.insert(transmitConversation);
                                            SMSDatabaseWrapper.send_data(itemView.getContext(),
                                                    transmitConversation);

                                            transmitConversation.setId(id);
                                            conversationDao.update(transmitConversation);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        });

                    } else receivedMessage.setText(
                            itemView.getContext().getString(R.string.conversation_threads_secured_content));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static class TimestampKeyReceivedViewHandler extends KeyReceivedViewHandler {
        public TimestampKeyReceivedViewHandler(@NonNull View itemView) {
            super(itemView);
            timestamp.setVisibility(View.VISIBLE);
        }
    }

    public static class TimestampKeyReceivedStartViewHandler extends TimestampConversationReceivedViewHandler {
        public TimestampKeyReceivedStartViewHandler(@NonNull View itemView) {
            super(itemView);

            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);
            timestamp.setVisibility(View.VISIBLE);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_mesages_start_view_drawable));
        }
    }

    public static class ConversationReceivedStartViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedStartViewHandler(@NonNull View itemView) {
            super(itemView);

            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_mesages_start_view_drawable));
        }
    }
    public static class ConversationReceivedEndViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedEndViewHandler(@NonNull View itemView) {
            super(itemView);
            layoutParams.bottomMargin = Helpers.dpToPixel(16);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_end_view_drawable));
        }
    }

    public static class ConversationReceivedMiddleViewHandler extends ConversationReceivedViewHandler {
        public ConversationReceivedMiddleViewHandler(@NonNull View itemView) {
            super(itemView);
            layoutParams.bottomMargin = Helpers.dpToPixel(1);
            linearLayoutCompat.setLayoutParams(layoutParams);

            receivedMessage.setBackground(
                    itemView.getContext().getDrawable(R.drawable.received_messages_middle_view_drawable));
        }
    }

}
