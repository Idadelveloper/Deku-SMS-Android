package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.provider.Telephony;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.swob_deku.ArchivedMessagesActivity;
import com.example.swob_deku.Models.Contacts.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.ViewHolders.ReceivedMessagesViewHolder;
import com.example.swob_deku.Models.Messages.ViewHolders.SentMessagesViewHolder;
import com.example.swob_deku.Models.Messages.ViewHolders.TemplateViewHolder;
import com.example.swob_deku.Models.SMS.Conversations;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.Security.SecurityHelpers;
import com.example.swob_deku.R;
import com.example.swob_deku.RouterActivity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MessagesThreadRecyclerAdapter extends RecyclerView.Adapter<TemplateViewHolder> {

    private final AsyncListDiffer<Conversations> mDiffer = new AsyncListDiffer<>(this, Conversations.DIFF_CALLBACK);

    Context context;
    Boolean isSearch = false;
    String searchString = "";
    RouterActivity routerActivity;
    ArchivedMessagesActivity archivedMessagesActivity;

    WorkManager workManager;
    LiveData<List<WorkInfo>> workers;

    public MutableLiveData<HashMap<String, TemplateViewHolder>> selectedItems = new MutableLiveData<>();

    final int MESSAGE_TYPE_SENT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
    final int MESSAGE_TYPE_INBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;
    final int MESSAGE_TYPE_DRAFT = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_DRAFT;
    final int MESSAGE_TYPE_OUTBOX = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX;
    final int MESSAGE_TYPE_FAILED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED;
    final int MESSAGE_TYPE_QUEUED = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_QUEUED;

    private final int RECEIVED_VIEW_TYPE = 1;
    private final int RECEIVED_UNREAD_VIEW_TYPE = 2;
    private final int RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE = 3;
    private final int RECEIVED_ENCRYPTED_VIEW_TYPE = 4;

    private final int SENT_VIEW_TYPE = 5;
    private final int SENT_UNREAD_VIEW_TYPE = 6;
    private final int SENT_ENCRYPTED_UNREAD_VIEW_TYPE = 7;
    private final int SENT_ENCRYPTED_VIEW_TYPE = 8;


    public MessagesThreadRecyclerAdapter(Context context) {
       this.context = context;
    }

    public MessagesThreadRecyclerAdapter(Context context, Boolean isSearch,
                                         String searchString) {
        this.context = context;
        this.isSearch = isSearch;
        this.searchString = searchString;
    }

    public MessagesThreadRecyclerAdapter(Context context, Boolean isSearch,
                                         String searchString, RouterActivity routerActivity) {
        this.context = context;
        this.isSearch = isSearch;
        this.searchString = searchString;
        this.routerActivity = routerActivity;

        workManager = WorkManager.getInstance(context);
    }

    public MessagesThreadRecyclerAdapter(Context context, Boolean isSearch,
                                         String searchString, ArchivedMessagesActivity archivedMessagesActivity) {
        this.context = context;
        this.isSearch = isSearch;
        this.searchString = searchString;
        this.archivedMessagesActivity = archivedMessagesActivity;

        workManager = WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);

        View view = inflater.inflate(R.layout.messages_threads_layout, parent, false);
        Log.d(getClass().getName(), "Running for creating view holder");
        if(viewType == (RECEIVED_UNREAD_VIEW_TYPE))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderUnread(view);
        else if(viewType == (SENT_UNREAD_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderUnread(view);

        else if(viewType == (RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderEncryptedUnread(view);
         else if(viewType == (SENT_ENCRYPTED_UNREAD_VIEW_TYPE ))
            return new SentMessagesViewHolder.SentViewHolderEncryptedUnread(view);

        else if(viewType == (RECEIVED_VIEW_TYPE))
            return new ReceivedMessagesViewHolder.ReceivedViewHolderRead(view);
        else if(viewType == (SENT_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderRead(view);

        else if(viewType == (SENT_ENCRYPTED_VIEW_TYPE))
            return new SentMessagesViewHolder.SentViewHolderEncryptedRead(view);

        return new ReceivedMessagesViewHolder.ReceivedViewHolderEncryptedRead(view);
    }

    public boolean isContact(String address) {
        String addressInPhone = Contacts.retrieveContactName(context, address);
        return !addressInPhone.isEmpty() && !addressInPhone.equals("null");
    }

    @Override
    public int getItemViewType(int position) {
        Conversations conversations = mDiffer.getCurrentList().get(position);
        SMS.SMSMetaEntity smsMetaEntity = conversations.getNewestMessage();
        Log.d(getClass().getName(), "Running for getting item type: " + position);

        String snippet = conversations.SNIPPET;
        int type = smsMetaEntity.getNewestType();

        if(SecurityHelpers.containersWaterMark(snippet) || SecurityHelpers.isKeyExchange(snippet)) {
            if(!smsMetaEntity.getNewestIsRead()) {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_UNREAD_VIEW_TYPE;
                else
                    return RECEIVED_ENCRYPTED_UNREAD_VIEW_TYPE;
            }
            else {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_ENCRYPTED_VIEW_TYPE;
                else
                    return RECEIVED_ENCRYPTED_VIEW_TYPE;
            }
        }
        else {
            if(!smsMetaEntity.getNewestIsRead()) {
                if(type != MESSAGE_TYPE_INBOX)
                    return SENT_UNREAD_VIEW_TYPE;
                else
                    return RECEIVED_UNREAD_VIEW_TYPE;
            }else {
                if(type != MESSAGE_TYPE_INBOX) {
                    return SENT_VIEW_TYPE;
                }
            }
        }

        return RECEIVED_UNREAD_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Conversations conversation = mDiffer.getCurrentList().get(position);
        holder.id = conversation.THREAD_ID;
//
        SMS.SMSMetaEntity smsMetaEntity = conversation.getNewestMessage();
        String address = smsMetaEntity.getAddress();
        long smsDate = smsMetaEntity.getNewestDateTime();

        if(isContact(address)) {
            address = Contacts.retrieveContactName(context, address);
//            if(!address.isEmpty()) {
//                holder.contactInitials.setAvatarInitials(address.substring(0, 1));
//                holder.contactInitials.setAvatarInitialsBackgroundColor(Helpers.generateColor(address));
//            }
        }
        else {
//            Drawable drawable = holder.contactPhoto.getDrawable();
//            drawable.setColorFilter(Helpers.generateColor(address), PorterDuff.Mode.SRC_IN);
//            holder.contactPhoto.setImageDrawable(drawable);
        }
        holder.address.setText(address);

        String date = Helpers.formatDate(context, smsDate);
        holder.date.setText(date);
        holder.snippet.setText(conversation.SNIPPET);

//                SecurityECDH securityECDH = new SecurityECDH(context);
//                if(securityECDH.hasSecretKey(conversation.SNIPPET) ) {
//                    holder.encryptedLock.setVisibility(View.VISIBLE);
//                }

//
//        if(routerActivity != null && !conversation.routingUrls.isEmpty()) {
//            holder.state.setText(conversation.getRouterStatus());
//            holder.routingURLText.setVisibility(View.VISIBLE);
//            holder.routingUrl.setVisibility(View.VISIBLE);
//
//            StringBuilder routingUrl = new StringBuilder();
//            for(int i=0;i<conversation.routingUrls.size(); ++i) {
//                if (routingUrl.length() > 0)
//                    routingUrl.append(", ");
//                routingUrl.append(conversation.routingUrls.get(i));
//            }
//            holder.routingUrl.setText(routingUrl);
//        }
//        else {
//            holder.routingURLText.setVisibility(View.GONE);
//            holder.routingUrl.setVisibility(View.GONE);
//        }

//        View.OnClickListener onClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(selectedItems.getValue() != null && !selectedItems.getValue().isEmpty()) {
//                    HashMap<String, TemplateViewHolder> items = selectedItems.getValue();
//                    if(items.containsKey(holder.id)) {
//                        holder.unHighlight();
//                        items.remove(holder.id);
//                    }
//                    else {
//                        items = selectedItems.getValue();
//                        holder.highlight();
//                        items.put(holder.id, holder);
//                    }
//                    selectedItems.postValue(items);
//                }
//                else {
//                    Intent singleMessageThreadIntent = new Intent(context, SMSSendActivity.class);
//                    singleMessageThreadIntent.putExtra(SMS.SMSMetaEntity.ADDRESS, conversation.getAddress());
//                    singleMessageThreadIntent.putExtra(SMS.SMSMetaEntity.THREAD_ID, conversation.getThreadId());
//
//                    if (searchString != null && !searchString.isEmpty()) {
//                        int calculatedOffset = SMSHandler.calculateOffset(context, conversation.getThreadId(), conversation.getId());
//                        singleMessageThreadIntent
//                                .putExtra(SMS.SMSMetaEntity.ID, conversation.getId())
//                                .putExtra(SMSSendActivity.SEARCH_STRING, searchString)
//                                .putExtra(SMSSendActivity.SEARCH_OFFSET, calculatedOffset)
//                                .putExtra(SMSSendActivity.SEARCH_POSITION, absolutePosition);
//                    }
//
//                    context.startActivity(singleMessageThreadIntent);
//                }
//            }
//        };
//
//        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                HashMap<String, TemplateViewHolder> items = selectedItems.getValue();
//                if(items != null && !items.isEmpty()) {
//                    if(items.containsKey(holder.id)) {
//                        holder.unHighlight();
//                        items.remove(holder.id);
//                    }
//                    else {
//                        items = selectedItems.getValue();
//                        holder.highlight();
//                        items.put(holder.id, holder);
//                    }
//                    selectedItems.postValue(items);
//                } else {
//                    items = new HashMap<>();
//                    holder.highlight();
//                    items.put(holder.id, holder);
//                }
//                selectedItems.setValue(items);
//                return true;
//            }
//        };
//
//        holder.layout.setOnClickListener(onClickListener);
//        holder.layout.setOnLongClickListener(onLongClickListener);
    }

    public void resetAllSelectedItems() {
        HashMap<String, TemplateViewHolder> items = selectedItems.getValue();
        if(items != null) {
            for (Map.Entry<String, TemplateViewHolder> entry : items.entrySet()) {
                entry.getValue().unHighlight();
            }
        }
        selectedItems.setValue(new HashMap<>());
    }

    @Override
    public int getItemCount() {
        int itemCount = mDiffer.getCurrentList().size();
        return itemCount;
    }

    public void submitList(List<Conversations> list) {
        if(routerActivity != null) {
            workManagerFactories();
        }
        mDiffer.submitList(list);
    }

    public void submitList(List<SMS> list, String searchString) {
        this.searchString = searchString;
//        mDiffer.submitList(list);
    }

    private String getSMSFromWorkInfo(WorkInfo workInfo) {
        String[] tags = Helpers.convertSetToStringArray(workInfo.getTags());
        String messageId = "";
        for(int i = 0; i< tags.length; ++i) {
            if (tags[i].contains("swob.work.id")) {
                tags = tags[i].split("\\.");
                messageId = tags[tags.length - 1];
                return messageId;
            }
        }
        return messageId;
    }

    private void workManagerFactories() {
//
//        WorkQuery workQuery = WorkQuery.Builder
//                .fromTags(Collections.singletonList(IncomingTextSMSBroadcastReceiver.TAG_NAME))
//                .addStates(Arrays.asList(
//                        WorkInfo.State.ENQUEUED,
//                        WorkInfo.State.FAILED,
//                        WorkInfo.State.CANCELLED,
//                        WorkInfo.State.SUCCEEDED,
//                        WorkInfo.State.RUNNING))
//                .build();
//
//        workers = workManager.getWorkInfosLiveData(workQuery);
//        workers.observe(routerActivity, new Observer<List<WorkInfo>>() {
//            @Override
//            public void onChanged(List<WorkInfo> workInfos) {
//                if(workInfos.isEmpty())
//                    return;
//
//                List<SMS> smsList = new ArrayList<>(mDiffer.getCurrentList());
//
//                for(WorkInfo workInfo: workInfos) {
//                    String messageId = getSMSFromWorkInfo(workInfo);
//                    for(int i=0;i<mDiffer.getCurrentList().size();++i)
//                        if(mDiffer.getCurrentList().get(i).id.equals(messageId)) {
//                            SMS sms = smsList.get(i);
//                            conversation.routerStatus = workInfo.getState().name();
//                            smsList.set(i, sms);
//                            break;
//                        }
//                }
//                mDiffer.submitList(smsList);
//                notifyDataSetChanged();
//            }
//        });
    }

}
