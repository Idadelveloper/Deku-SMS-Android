package com.afkanerd.deku.DefaultSMS.Models.NativeConversationDB;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afkanerd.deku.DefaultSMS.BroadcastReceivers.IncomingTextSMSBroadcastReceiver;
import com.afkanerd.deku.DefaultSMS.Models.Transmissions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NativeSMSDB {
    public static String BROADCAST_NATIVE_SMS_DB = "BROADCAST_NATIVE_SMS_DB";
    public static String BROADCAST_THREAD_ID_INTENT = "BROADCAST_THREAD_ID_INTENT";
    public static String BROADCAST_CONVERSATION_ID_INTENT = "BROADCAST_CONVERSATION_ID_INTENT";

    public static String BROADCAST_STATUS_CHANGED_ACTION = "BROADCAST_STATUS_CHANGED_ACTION";
    public static String BROADCAST_NEW_MESSAGE_ACTION = "BROADCAST_NEW_MESSAGE_ACTION";

    public static String ID = "ID";

    public static int THREAD_ID = 0;
    public static int MESSAGE_ID = 1;

    public static int BODY = 2;

    public static int ADDRESS = 3;
    public static int SUBSCRIPTION_ID = 4;

    public static Cursor fetchAll(Context context) {
        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null);
    }
    public static Cursor fetchByThreadId(Context context, String threadId) {
        return context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                null,
                Telephony.Sms.THREAD_ID + "=?",
                new String[]{threadId},
                null);
    }

    public static Cursor fetchByMessageId(@NonNull Context context, String id) {
        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                null,
                Telephony.Sms._ID + "=?",
                new String[]{id},
                null);
    }

    public static Cursor fetchAndBuildConversation(Context context) {
        return context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.TextBasedSmsColumns.ADDRESS,
                        Telephony.TextBasedSmsColumns.BODY,
                        Telephony.TextBasedSmsColumns.READ,
                        Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.DATE
                },
                "thread_id IS NOT NULL) GROUP BY(thread_id",
                null,
                "date DESC");
    }

    /*
     * Places which require playing with Native SMS DB
     * Outgoing:
     *  Actions:
     *      - Manual
     *          - From send message
     *          - From notifications - X
     *  Broadcast:
     *      - state changes
     *          - Auto
     *              - pending - X
     *              - sent - X
     *              - failed - X
     *              - delivered -X
     *
     * Incoming:
     *  Actions:
     *      - Manual:
     *          - Read status
     */

    private static String[] broadcastNewMessage(Context context, Uri uri) {
        /**
         * Threads ID
         * Message ID
         */

        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.Sms._ID},
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            String threadId = cursor.getString(
                    cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            String messageId = cursor.getString(
                    cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
            cursor.close();

            Intent broadcastIntent = new Intent(BROADCAST_NEW_MESSAGE_ACTION);
            broadcastIntent.putExtra(BROADCAST_THREAD_ID_INTENT, threadId);
            broadcastIntent.putExtra(BROADCAST_CONVERSATION_ID_INTENT, messageId);
            context.sendBroadcast(broadcastIntent);

            return new String[]{threadId, messageId};
        }

        return null;
    }

    private static String[] broadcastStateChanged(Context context, String messageId) {
        /**
         * Threads ID
         * Message ID
         */
        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{
                        Telephony.TextBasedSmsColumns.THREAD_ID,
                        Telephony.Sms._ID},
                Telephony.Sms._ID + "=?",
                new String[]{messageId},
                null);

        if (cursor.moveToFirst()) {
            String threadId = cursor.getString(
                    cursor.getColumnIndexOrThrow(Telephony.TextBasedSmsColumns.THREAD_ID));
            cursor.close();

            Intent broadcastIntent = new Intent(BROADCAST_STATUS_CHANGED_ACTION);
            broadcastIntent.putExtra(BROADCAST_THREAD_ID_INTENT, threadId);
            broadcastIntent.putExtra(BROADCAST_CONVERSATION_ID_INTENT, messageId);
            context.sendBroadcast(broadcastIntent);

            return new String[]{threadId, messageId};
        }

        return null;
    }

    public static class Outgoing {

        private static int update_status(Context context, int statusCode, long messageId, int errorCode) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.TextBasedSmsColumns.STATUS, statusCode);

            if(statusCode == Telephony.Sms.STATUS_NONE)
                contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                        Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);

            if(statusCode == Telephony.Sms.STATUS_FAILED || errorCode > -1)
                contentValues.put(Telephony.TextBasedSmsColumns.ERROR_CODE, errorCode);

            try {
                return context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        "_id=?",
                        new String[]{Long.toString(messageId)});
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        public static String[] send_text(Context context, String destinationAddress,
                                                 String text, int subscriptionId, Bundle bundle) throws Exception {
            String[] pendingOutputs = register_pending(context, destinationAddress, text, subscriptionId);
            long messageId = Long.parseLong(pendingOutputs[MESSAGE_ID]);
            PendingIntent[] pendingIntents = getPendingIntents(context, messageId, bundle);
            Transmissions.sendTextSMS(destinationAddress, text,
                    pendingIntents[0], pendingIntents[1], subscriptionId);
            return pendingOutputs;
        }


        public static String[] send_data(Context context, String destinationAddress, byte[] data,
                                 int subscriptionId, Bundle bundle) throws Exception {
            String[] pendingOutputs = register_pending(context, destinationAddress,
                    Base64.encodeToString(data, Base64.DEFAULT), subscriptionId);
            long messageId = Long.parseLong(pendingOutputs[MESSAGE_ID]);
            PendingIntent[] pendingIntents = getPendingIntents(context, messageId, bundle);
            Transmissions.sendDataSMS(destinationAddress, data,
                    pendingIntents[0], pendingIntents[1], subscriptionId);
            return pendingOutputs;
        }

        private static String[] register_pending(Context context, String destinationAddress,
                                                String text, int subscriptionId) {
            long messageId = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();

            contentValues.put(Telephony.Sms._ID, messageId);

            contentValues.put(Telephony.TextBasedSmsColumns.TYPE,
                    Telephony.TextBasedSmsColumns.MESSAGE_TYPE_OUTBOX);

            contentValues.put(Telephony.TextBasedSmsColumns.STATUS,
                    Telephony.TextBasedSmsColumns.STATUS_PENDING);

            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);

            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, destinationAddress);

            contentValues.put(Telephony.TextBasedSmsColumns.BODY, text);

            contentValues.put(Telephony.TextBasedSmsColumns.DATE_SENT,
                    String.valueOf(System.currentTimeMillis()));

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                return broadcastNewMessage(context, uri);

            } catch (Exception e) {
                throw e;
            }
        }

        public static String[] register_failed(Context context, long messageId, int errorCode) {
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_FAILED,
                            messageId, errorCode);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static String[] register_delivered(@NonNull Context context, long messageId) {
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_COMPLETE,
                            messageId, -1);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static String[] register_sent(Context context, long messageId) {
            int numberChanged =
                    update_status(context, Telephony.TextBasedSmsColumns.STATUS_NONE,
                            messageId, -1);
            return broadcastStateChanged(context, String.valueOf(messageId));
        }

        public static PendingIntent[] getPendingIntents(Context context, long messageId, Bundle bundle) {
            Intent sentIntent = new Intent(IncomingTextSMSBroadcastReceiver.SMS_SENT_BROADCAST_INTENT);
            sentIntent.setPackage(context.getPackageName());
            sentIntent.putExtra(ID, messageId);

            Intent deliveredIntent = new Intent(IncomingTextSMSBroadcastReceiver.SMS_DELIVERED_BROADCAST_INTENT);
            deliveredIntent.setPackage(context.getPackageName());
            deliveredIntent.putExtra(SMSMetaEntity.ID, messageId);

            if(bundle != null) {
                sentIntent.putExtras(bundle);
                deliveredIntent.putExtras(bundle);
            }

            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context,
                    (int) messageId,
                    sentIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context,
                    (int) messageId,
                    deliveredIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            return new PendingIntent[]{sentPendingIntent, deliveredPendingIntent};
        }


    }


    public static class Incoming {

        public static int update_read(Context context, int read, String thread_id) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Telephony.TextBasedSmsColumns.READ, read);

            try {
                return context.getContentResolver().update(
                        Telephony.Sms.CONTENT_URI,
                        contentValues,
                        "thread_id=?",
                        new String[]{thread_id});
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }

        public static String[] register_incoming_text(Context context, Intent intent) throws IOException {
            long messageId = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();

            Bundle bundle = intent.getExtras();
            int subscriptionId = bundle.getInt("subscription", -1);

            String address = "";
            StringBuilder bodyBuffer = new StringBuilder();

            for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                address = currentSMS.getDisplayOriginatingAddress();

                String text_message = currentSMS.getDisplayMessageBody();

                bodyBuffer.append(text_message);
            }
            String body = bodyBuffer.toString();

            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                Log.d(NativeSMSDB.class.getName(), "URI: " + uri.toString());
                String[] broadcastOutputs = broadcastNewMessage(context, uri);
                String[] returnString = new String[5];
                returnString[THREAD_ID] = broadcastOutputs[THREAD_ID];
                returnString[MESSAGE_ID] = broadcastOutputs[MESSAGE_ID];
                returnString[BODY] = body;
                returnString[ADDRESS] = address;
                returnString[SUBSCRIPTION_ID] = String.valueOf(subscriptionId);
                return returnString;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        public static String[] register_incoming_data(Context context, Intent intent) throws IOException {
            long messageId = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();

            Bundle bundle = intent.getExtras();
            int subscriptionId = bundle.getInt("subscription", -1);

            String address = "";
            ByteArrayOutputStream dataBodyBuffer = new ByteArrayOutputStream();

            for (SmsMessage currentSMS : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                address = currentSMS.getDisplayOriginatingAddress();

                byte[] data_message = currentSMS.getUserData();

                dataBodyBuffer.write(data_message);
            }

            String body = Base64.encodeToString(dataBodyBuffer.toByteArray(), Base64.DEFAULT);
            contentValues.put(Telephony.Sms._ID, messageId);
            contentValues.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            contentValues.put(Telephony.TextBasedSmsColumns.BODY, body);
            contentValues.put(Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID, subscriptionId);
            contentValues.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

            try {
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.CONTENT_URI,
                        contentValues);
                String[] broadcastOutputs = broadcastNewMessage(context, uri);
                String[] returnString = new String[4];
                returnString[THREAD_ID] = broadcastOutputs[THREAD_ID];
                returnString[MESSAGE_ID] = broadcastOutputs[MESSAGE_ID];
                returnString[BODY] = body;
                returnString[ADDRESS] = address;
                returnString[SUBSCRIPTION_ID] = String.valueOf(subscriptionId);
                return returnString;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}
