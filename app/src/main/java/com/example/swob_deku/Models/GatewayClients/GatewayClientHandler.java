package com.example.swob_deku.Models.GatewayClients;

import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_FRIENDLY_NAME;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_HOST;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_ID;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_PASSWORD;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_PORT;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_USERNAME;
import static com.example.swob_deku.GatewayClientListingActivity.GATEWAY_CLIENT_VIRTUAL_HOST;
import static com.example.swob_deku.MessagesThreadsActivity.UNIQUE_WORK_MANAGER_NAME;

import android.content.Context;
import android.content.Intent;

import androidx.room.Room;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.swob_deku.Models.Datastore;
import com.example.swob_deku.Models.Migrations;
import com.example.swob_deku.Models.RMQ.RMQConnectionService;
import com.example.swob_deku.Models.RMQ.RMQWorkManager;

import java.util.concurrent.TimeUnit;

public class GatewayClientHandler {

    Datastore databaseConnector;
    Context context;

    public GatewayClientHandler(Context context) {
        this.context = context;
        databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration4To5())
                .build();
    }

    public void add(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.insert(gatewayClient);
            }
        });
        thread.start();
        thread.join();
    }

    public void delete(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.delete(gatewayClient);
            }
        });
        thread.start();
        thread.join();
    }

    public void update(GatewayClient gatewayClient) throws InterruptedException {
        gatewayClient.setDate(System.currentTimeMillis());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClientDAO.updateProjectNameAndProjectBinding(
                        gatewayClient.getProjectName(), gatewayClient.getProjectBinding(),
                        gatewayClient.getId());
            }
        });
        thread.start();
        thread.join();
    }

    public GatewayClient fetch(int id) throws InterruptedException {
        final GatewayClient[] gatewayClient = {new GatewayClient()};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GatewayClientDAO gatewayClientDAO = databaseConnector.gatewayClientDAO();
                gatewayClient[0] = gatewayClientDAO.fetch(id);
            }
        });
        thread.start();
        thread.join();

        return gatewayClient[0];
    }

    public Intent getIntent(int id) throws InterruptedException {
        GatewayClient gatewayClient = fetch(id);
        return getIntent(gatewayClient);
    }

    public Intent getIntent(GatewayClient gatewayClient) throws InterruptedException {
        Intent intent = new Intent(context, RMQConnectionService.class);
        intent.putExtra(GATEWAY_CLIENT_ID, gatewayClient.getId());
        intent.putExtra(GATEWAY_CLIENT_USERNAME, gatewayClient.getUsername());
        intent.putExtra(GATEWAY_CLIENT_PASSWORD, gatewayClient.getPassword());
        intent.putExtra(GATEWAY_CLIENT_HOST, gatewayClient.getHostUrl());
        intent.putExtra(GATEWAY_CLIENT_PORT, gatewayClient.getPort());
        intent.putExtra(GATEWAY_CLIENT_VIRTUAL_HOST, gatewayClient.getVirtualHost());
        intent.putExtra(GATEWAY_CLIENT_FRIENDLY_NAME, gatewayClient.getFriendlyConnectionName());

        return intent;
    }

    public void close() {
        databaseConnector.close();
    }


    public void startServices() throws InterruptedException {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        try {
            OneTimeWorkRequest gatewayClientListenerWorker = new OneTimeWorkRequest.Builder(RMQWorkManager.class)
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS
                    )
                    .addTag(GatewayClient.class.getName())
                    .build();

            WorkManager workManager = WorkManager.getInstance(context);
            workManager.enqueueUniqueWork(UNIQUE_WORK_MANAGER_NAME,
                    ExistingWorkPolicy.KEEP,
                    gatewayClientListenerWorker);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}