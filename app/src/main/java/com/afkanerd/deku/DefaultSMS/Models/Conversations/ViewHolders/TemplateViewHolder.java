package com.afkanerd.deku.DefaultSMS.Models.Conversations.ViewHolders;

import static android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.RECEIVED_ENCRYPTED_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.RECEIVED_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.RECEIVED_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.SENT_ENCRYPTED_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.SENT_ENCRYPTED_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.SENT_UNREAD_VIEW_TYPE;
import static com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversationRecyclerAdapter.SENT_VIEW_TYPE;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.afkanerd.deku.DefaultSMS.Commons.Helpers;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import io.getstream.avatarview.AvatarView;

public class TemplateViewHolder extends RecyclerView.ViewHolder {

    public String id;
    public long messageId;
    public TextView snippet;
    public TextView address;
    public TextView date;
    public AvatarView contactInitials;
    public TextView youLabel;

    public ConstraintLayout layout;

    public MaterialCardView materialCardView;

    View itemView;

    public TemplateViewHolder(@NonNull View itemView) {
        super(itemView);
        this.itemView = itemView;

        snippet = itemView.findViewById(R.id.messages_thread_text);
        address = itemView.findViewById(R.id.messages_thread_address_text);
        date = itemView.findViewById(R.id.messages_thread_date);
        layout = itemView.findViewById(R.id.messages_threads_layout);
        youLabel = itemView.findViewById(R.id.message_you_label);
        contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
        materialCardView = itemView.findViewById(R.id.messages_threads_cardview);
    }

    public void bind(ThreadedConversations conversation, View.OnClickListener onClickListener,
                     View.OnLongClickListener onLongClickListener) {
        this.id = String.valueOf(conversation.getThread_id());

        if(conversation.getAvatar_initials() != null) {
            this.contactInitials.setAvatarInitials(conversation.getAvatar_initials());
            this.contactInitials.setAvatarInitialsBackgroundColor(
                    Helpers.generateColor( conversation.getAddress() ));
//            this.contactInitials.setAvatarInitialsBackgroundColor(Helpers.generateColor(address));
        }
        if(conversation.getContact_name() != null) {
            this.address.setText(conversation.getContact_name());
        } else this.address.setText(conversation.getAddress());

        this.date.setText(conversation.getFormatted_datetime());
        this.snippet.setText(conversation.getSnippet());
        this.materialCardView.setOnClickListener(onClickListener);
        this.materialCardView.setOnLongClickListener(onLongClickListener);
        // TODO: investigate new Avatar first before anything else
//        this.contactInitials.setPlaceholder(itemView.getContext().getDrawable(R.drawable.round_person_24));
    }

    public static int getViewType(int position, List<ThreadedConversations> items) {
        ThreadedConversations threadedConversations = items.get(position);
        String snippet = threadedConversations.getSnippet();
        int type = threadedConversations.getType();

//        if(SecurityHelpers.containersWaterMark(snippet) || SecurityHelpers.isKeyExchange(snippet)) {
//            if(!threadedConversations.isIs_read()) {
//                return type == MESSAGE_TYPE_INBOX ?
//                        RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE : SENT_ENCRYPTED_UNREAD_VIEW_TYPE;
//            }
//            else {
//                return type == MESSAGE_TYPE_INBOX ?
//                        RECEIVED_ENCRYPTED_VIEW_TYPE : SENT_ENCRYPTED_VIEW_TYPE;
//            }
//        }
        if(!threadedConversations.isIs_read()) {
            return type == MESSAGE_TYPE_INBOX ?
                    RECEIVED_UNREAD_VIEW_TYPE : SENT_UNREAD_VIEW_TYPE;
        }
        else {
            return type == MESSAGE_TYPE_INBOX ?
                    RECEIVED_VIEW_TYPE : SENT_VIEW_TYPE;
        }
    }

    public void setCardOnClickListener(View.OnClickListener onClickListener) {
        this.materialCardView.setOnClickListener(onClickListener);
    }

    public static class ReadViewHolder extends TemplateViewHolder{
        public ReadViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setMaxLines(1);
        }
    }

    public static class UnreadViewHolder extends TemplateViewHolder{
        public UnreadViewHolder(@NonNull View itemView) {
            super(itemView);
            address.setTypeface(Typeface.DEFAULT_BOLD);
            address.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            snippet.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            date.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));
        }
    }

    public static class UnreadEncryptedViewHolder extends TemplateViewHolder.UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        }
    }

    public static class ReadEncryptedViewHolder extends TemplateViewHolder.ReadViewHolder {
        public ReadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        }
    }

    public void highlight(){
        materialCardView.setBackgroundResource(R.drawable.received_messages_drawable);
//        this.setIsRecyclable(false);
    }

    public void unHighlight(){
        materialCardView.setBackgroundResource(0);
//        this.setIsRecyclable(true);
    }

}
