package com.afkanerd.deku.DefaultSMS.BroadcastReceivers

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Base64
import android.util.Log
import android.util.Pair
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afkanerd.deku.Datastore
import com.afkanerd.deku.DefaultSMS.BuildConfig
import com.afkanerd.deku.DefaultSMS.Deprecated.ConversationActivity
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler
import com.afkanerd.deku.DefaultSMS.Models.NativeSMSDB
import com.afkanerd.deku.DefaultSMS.Models.NotificationsHandler
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.Router.GatewayServers.GatewayServer
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.Ratchets
import com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal.States
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IncomingTextSMSBroadcastReceiver : BroadcastReceiver() {
    /*
    - address received might be different from how address is saved.
    - how it received is the trusted one, but won't match that which has been saved.
    - when message gets stored it's associated to the thread - so matching is done by android
    - without country code, can't know where message is coming from. Therefore best assumption is
    - service providers do send in country code.
    - How is matched to users stored without country code?
     */
    var executorService: ExecutorService = Executors.newFixedThreadPool(4)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val regIncomingOutput =
                            NativeSMSDB.Incoming.register_incoming_text(context, intent)
                    if (regIncomingOutput != null) {
                        val messageId = regIncomingOutput[NativeSMSDB.MESSAGE_ID]
                        val body = regIncomingOutput[NativeSMSDB.BODY]
                        val threadId = regIncomingOutput[NativeSMSDB.THREAD_ID]
                        val address = regIncomingOutput[NativeSMSDB.ADDRESS]
                        val date = regIncomingOutput[NativeSMSDB.DATE]
                        val dateSent = regIncomingOutput[NativeSMSDB.DATE_SENT]
                        val subscriptionId = regIncomingOutput[NativeSMSDB.SUBSCRIPTION_ID].toInt()

                        val conversation =
                                insertConversation(context, address, messageId, threadId, body,
                                        subscriptionId, date, dateSent)

                        CoroutineScope(Dispatchers.Default).launch {
                            GatewayServer.route(context, conversation)
                        }
                    }
                } catch (e: IOException) {
                    Log.e(javaClass.name, "Exception Incoming message broadcast", e)
                }
            }
        } else if (intent.action == SMS_SENT_BROADCAST_INTENT) {
            executorService.execute(object : Runnable {
                override fun run() {
                    val id = intent.getStringExtra(NativeSMSDB.ID)!!

                    val conversation = Datastore.getDatastore(context).conversationDao()
                            .getMessage(id)

                    if (resultCode == Activity.RESULT_OK) {
                        NativeSMSDB.Outgoing.register_sent(context, id)
                        conversation.status = Telephony.TextBasedSmsColumns.STATUS_NONE
                        conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                    } else {
                        try {
                            NativeSMSDB.Outgoing.register_failed(context, id, resultCode)
                            conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
                            conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
                            conversation.error_code = resultCode

                        } catch (e: Exception) {
                            Log.e(javaClass.name,
                                    "Exception with sent message broadcast", e)
                        } finally {
                            conversation.thread_id?.let {
                                notifyMessageFailedToSend(context, conversation)
                            }
                        }
                    }
                    Datastore.getDatastore(context).conversationDao()
                        ._update(conversation)
                }
            })
        }
        else if (intent.action == SMS_DELIVERED_BROADCAST_INTENT) {
            executorService.execute(Runnable {
                val id = intent.getStringExtra(NativeSMSDB.ID)!!
                val conversation = Datastore.getDatastore(context).conversationDao().getMessage(id)

                if (resultCode == Activity.RESULT_OK) {
                    NativeSMSDB.Outgoing.register_delivered(context, id)
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_COMPLETE
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                } else {
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
                    conversation.error_code = resultCode
                }
                Datastore.getDatastore(context).conversationDao()._update(conversation)
            })
        }
        else if (intent.action == IncomingDataSMSBroadcastReceiver.DATA_SENT_BROADCAST_INTENT) {
            executorService.execute {
                val id = intent.getStringExtra(NativeSMSDB.ID)!!
                val conversation = Datastore.getDatastore(context).conversationDao().getMessage(id)

                if (resultCode == Activity.RESULT_OK) {
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_NONE
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                } else {
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
                    conversation.error_code = resultCode
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
                }
                Datastore.getDatastore(context).conversationDao()._update(conversation)
            }
        }
        else if (intent.action == IncomingDataSMSBroadcastReceiver.DATA_DELIVERED_BROADCAST_INTENT) {
            executorService.execute {
                val id = intent.getStringExtra(NativeSMSDB.ID)!!
                val conversation = Datastore.getDatastore(context).conversationDao().getMessage(id)

                if (resultCode == Activity.RESULT_OK) {
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_COMPLETE
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                } else {
                    conversation.status = Telephony.TextBasedSmsColumns.STATUS_FAILED
                    conversation.error_code = resultCode
                    conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_FAILED
                }
                Datastore.getDatastore(context).conversationDao()._update(conversation)
            }
        }
    }

    private fun insertConversation(context: Context, address: String, messageId: String,
                           threadId: String, body: String, subscriptionId: Int, date: String,
                           dateSent: String): Conversation {
        val conversation = Conversation()
        conversation.message_id = messageId
        conversation.thread_id = threadId
        conversation.type = Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX
        conversation.address = address
        conversation.subscription_id = subscriptionId
        conversation.date = date
        conversation.date_sent = dateSent

        val text = try {
            val res = processEncryptedIncoming(context, address, body)
            conversation.isIs_encrypted = res.second
            res.first
        } catch (e: Throwable) {
            e.printStackTrace()
            body
        }
        conversation.text = text

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val threadedConversations = Datastore.getDatastore(context)
                        .threadedConversationsDao()
                        .insertThreadAndConversation(context, conversation)
                if (!threadedConversations.isIs_mute)
                    NotificationsHandler.sendIncomingTextMessageNotification(context, conversation)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return conversation
    }


    private fun processEncryptedIncoming(context: Context, address: String, text: String):
            Pair<String?, Boolean> {
        var text = text
        var encrypted = false
        if (E2EEHandler.isValidMessage(Base64.decode(text, Base64.DEFAULT))) {
            val payload = E2EEHandler.extractMessageFromPayload(Base64.decode(text, Base64.DEFAULT))

            val isSelf = E2EEHandler.isSelf(context, address)
            val keypair = E2EEHandler.fetchKeypair(context, address, isSelf)
            val peerPublicKey = if(isSelf) keypair.second else
                Base64.decode(E2EEHandler.secureFetchPeerPublicKey(context, address), Base64.DEFAULT)
            var states = E2EEHandler.fetchStates(context, address, isSelf)
            if(states.isBlank()) {
                val bobState = States()
                val SK = E2EEHandler.calculateSharedSecret(context, address, peerPublicKey)
                Ratchets.ratchetInitBob(bobState, SK, keypair)
                states = bobState.serializedStates
            }
            val receivingState = States(states)
            if(BuildConfig.DEBUG)
                println(states)
            val decryptedText = Ratchets.ratchetDecrypt(receivingState, payload.first,
                payload.second, keypair.second)
            text = String(decryptedText, Charsets.UTF_8)
            encrypted = true

            if(BuildConfig.DEBUG)
                Toast.makeText(context, "Decryption happened!", Toast.LENGTH_LONG).show()

            E2EEHandler.storeState(context, receivingState.serializedStates, address, isSelf)
        }

        return Pair(text, encrypted)
    }

    companion object {
        var SMS_SENT_BROADCAST_INTENT: String = BuildConfig.APPLICATION_ID + ".SMS_SENT_BROADCAST_INTENT"
        var SMS_DELIVERED_BROADCAST_INTENT: String = BuildConfig.APPLICATION_ID + ".SMS_DELIVERED_BROADCAST_INTENT"
    }

    private fun notifyMessageFailedToSend(context: Context, conversation: Conversation) {
        val notificationIntent = Intent(context, ConversationActivity::class.java).apply {
            putExtra(Conversation.THREAD_ID, conversation.thread_id)
            putExtra(Conversation.SUBSCRIPTION_ID, conversation.subscription_id)
        }

        val pendingIntent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE)

        val content = context
                .getString(R.string
                        .message_failed_send_notification_description_a_message_failed_to_send_to) +
                " ${conversation.address}"
        val notification =
                NotificationCompat.Builder(context,
                        context.getString(R.string.message_failed_channel_id))
                        .setContentTitle(context
                                .getString(R.string.message_failed_channel_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setPriority(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentText(content)
                        .setContentIntent(pendingIntent)
                        .build()


        val notificationId = context.getString(R.string.message_failed_notification_id).toInt()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
    }

}