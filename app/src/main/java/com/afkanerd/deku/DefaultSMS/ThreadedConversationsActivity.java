package com.afkanerd.deku.DefaultSMS;

import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.ARCHIVED_MESSAGE_TYPES;
import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.BLOCKED_MESSAGE_TYPES;
import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.DRAFTS_MESSAGE_TYPES;
import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT;
import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.MUTED_MESSAGE_TYPE;
import static com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment.UNREAD_MESSAGE_TYPES;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NotificationManagerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afkanerd.deku.DefaultSMS.DAO.ThreadedConversationsDao;
import com.afkanerd.deku.DefaultSMS.Fragments.ThreadedConversationsFragment;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationRecyclerAdapter;
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.QueueListener.GatewayClients.GatewayClientHandler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ThreadedConversationsActivity extends CustomAppCompactActivity implements ThreadedConversationsFragment.ViewModelsInterface {
    public static final String UNIQUE_WORK_MANAGER_NAME = BuildConfig.APPLICATION_ID;
    FragmentManager fragmentManager = getSupportFragmentManager();

    ActionBar ab;

    HashMap<String, ThreadedConversationRecyclerAdapter> messagesThreadRecyclerAdapterHashMap = new HashMap<>();

    String ITEM_TYPE = "";

    ThreadedConversations threadedConversations = new ThreadedConversations();

    MaterialToolbar toolbar;

    NavigationView navigationView;

    ThreadedConversationsDao threadedConversationsDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_threads);

        toolbar = findViewById(R.id.conversation_threads_toolbar);
        setSupportActionBar(toolbar);
        ab = getSupportActionBar();

        threadedConversationsDao = threadedConversations.getDaoInstance(getApplicationContext());

        threadedConversationsViewModel = new ViewModelProvider(this).get(
                ThreadedConversationsViewModel.class);
        threadedConversationsViewModel.threadedConversationsDao = threadedConversationsDao;


        fragmentManagement();
        configureBroadcastListeners();
        configureNavigationBar();

        configureNotifications();
    }

    public void configureNavigationBar() {
        navigationView = findViewById(R.id.conversations_threads_navigation_view);
        View view = getLayoutInflater().inflate(R.layout.header_navigation_drawer, null);
        TextView textView = view.findViewById(R.id.conversations_threads_navigation_view_version_number);
        textView.setText(BuildConfig.VERSION_NAME);

        navigationView.addHeaderView(view);

        MenuItem inboxMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_inbox);
        MenuItem draftMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_drafts);
        MenuItem encryptedMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_encrypted);
        MenuItem unreadMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_unread);
        MenuItem blockedMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_blocked);
        MenuItem mutedMenuItem = navigationView.getMenu().findItem(R.id.navigation_view_menu_muted);

        threadedConversationsViewModel.folderMetrics.observe(this, new Observer<List<Integer>>() {
            @Override
            public void onChanged(List<Integer> integers) {
//                inboxMenuItem.setTitle(getString(R.string.conversations_navigation_view_inbox)
//                        + "(" + integers.get(0) + ")");
                draftMenuItem.setTitle(getString(R.string.conversations_navigation_view_drafts)
                        + "(" + integers.get(0) + ")");

                encryptedMenuItem.setTitle(getString(R.string.homepage_fragment_tab_encrypted)
                        + "(" + integers.get(1) + ")");

                unreadMenuItem.setTitle(getString(R.string.conversations_navigation_view_unread)
                        + "(" + integers.get(2) + ")");

                blockedMenuItem.setTitle(getString(R.string.conversations_navigation_view_blocked)
                        + "(" + integers.get(3) + ")");

                mutedMenuItem.setTitle(getString(R.string.conversation_menu_muted_label)
                        + "(" + integers.get(4) + ")");
            }
        });

        DrawerLayout drawerLayout = findViewById(R.id.conversations_drawer);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                String messageType = "";
                String label = "";
                String noContent = "";
                int defaultMenu = -1;
                int actionModeMenu = -1;
                if(item.getItemId() == R.id.navigation_view_menu_inbox) {
                    fragmentManagement();
                    drawerLayout.close();
                    return true;
                } else if(item.getItemId() == R.id.navigation_view_menu_drafts) {
                    messageType = DRAFTS_MESSAGE_TYPES;
                    label = getString(R.string.conversations_navigation_view_drafts);
                    noContent = getString(R.string.homepage_draft_no_message);
                    defaultMenu = R.menu.drafts_menu;
                    actionModeMenu = R.menu.conversations_threads_menu_items_selected;
                } else if(item.getItemId() == R.id.navigation_view_menu_encrypted) {
                    messageType = ENCRYPTED_MESSAGES_THREAD_FRAGMENT;
                    label = getString(R.string.conversations_navigation_view_encryption);
                    noContent = getString(R.string.homepage_encryption_no_message);
                    defaultMenu = R.menu.conversations_threads_menu;
                    actionModeMenu = R.menu.conversations_threads_menu_items_selected;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_unread) {
                    messageType = UNREAD_MESSAGE_TYPES;
                    label = getString(R.string.conversations_navigation_view_unread);
                    noContent = getString(R.string.homepage_unread_no_message);
                    defaultMenu = R.menu.read_menu;
                    actionModeMenu = R.menu.conversations_threads_menu_items_selected;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_archive) {
                    messageType = ARCHIVED_MESSAGE_TYPES;
                    label = getString(R.string.conversations_navigation_view_archived);
                    noContent = getString(R.string.homepage_archive_no_message);
                    defaultMenu = R.menu.archive_menu;
                    actionModeMenu = R.menu.archive_menu_items_selected;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_blocked) {
                    messageType = BLOCKED_MESSAGE_TYPES;
                    label = getString(R.string.conversation_menu_block);
                    noContent = getString(R.string.homepage_blocked_no_message);
                    defaultMenu = R.menu.blocked_conversations;
                    actionModeMenu = R.menu.blocked_conversations_items_selected;
                }
                else if(item.getItemId() == R.id.navigation_view_menu_muted) {
                    messageType = MUTED_MESSAGE_TYPE;
                    label = getString(R.string.conversation_menu_muted_label);
                    noContent = getString(R.string.homepage_muted_no_muted);
                    defaultMenu = R.menu.muted_menu;
                    actionModeMenu = R.menu.muted_menu_items_selected;
                }
                else return false;

                Bundle bundle = new Bundle();
                bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_TYPE,
                        messageType);
                bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_LABEL, label);
                bundle.putString(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_NO_CONTENT,
                        noContent);
                bundle.putInt(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_DEFAULT_MENU,
                        defaultMenu);
                bundle.putInt(ThreadedConversationsFragment.MESSAGES_THREAD_FRAGMENT_DEFAULT_ACTION_MODE_MENU,
                        actionModeMenu);
                fragmentManager.beginTransaction().replace(R.id.view_fragment,
                                ThreadedConversationsFragment.class, bundle, null)
                        .setReorderingAllowed(true)
                        .commit();
                drawerLayout.close();
                return true;
            }
        });
    }

    private void fragmentManagement() {
        fragmentManager.beginTransaction().replace(R.id.view_fragment,
                        ThreadedConversationsFragment.class, null, "HOMEPAGE_TAG")
                .setReorderingAllowed(true)
                .commit();
    }


    private void cancelAllNotifications() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.cancelAll();
    }

    public void onNewMessageClick(View view) {
        Intent intent = new Intent(this, ComposeNewMessageActivity.class);
        startActivity(intent);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu){
//        getMenuInflater().inflate(R.menu.conversations_threads_menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }


    @Override
    protected void onPause() {
        super.onPause();
//        setViewModel(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
               startServices();
            }
        });
    }

    @Override
    public ThreadedConversationsViewModel getThreadedConversationsViewModel() {
        return threadedConversationsViewModel;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        threadedConversations.close();
    }

    ArrayList<String> notificationsChannelIds = new ArrayList<>();
    ArrayList<String> notificationsChannelNames = new ArrayList<>();
    private void createNotificationChannel() {
        notificationsChannelIds.add(getString(R.string.incoming_messages_channel_id));
        notificationsChannelNames.add(getString(R.string.incoming_messages_channel_name));

        notificationsChannelIds.add(getString(R.string.running_gateway_clients_channel_id));
        notificationsChannelNames.add(getString(R.string.running_gateway_clients_channel_name));

        notificationsChannelIds.add(getString(R.string.foreground_service_failed_channel_id));
        notificationsChannelNames.add(getString(R.string.foreground_service_failed_channel_name));

        createNotificationChannelIncomingMessage();

        createNotificationChannelRunningGatewayListeners();

        createNotificationChannelReconnectGatewayListeners();
    }

    private void createNotificationChannelIncomingMessage() {
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(
                notificationsChannelIds.get(0), notificationsChannelNames.get(0), importance);
        channel.setDescription(getString(R.string.incoming_messages_channel_description));
        channel.enableLights(true);
        channel.setLightColor(R.color.logo_primary);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannelRunningGatewayListeners() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(
                notificationsChannelIds.get(1), notificationsChannelNames.get(1), importance);
        channel.setDescription(getString(R.string.running_gateway_clients_channel_description));
        channel.setLightColor(R.color.logo_primary);
        channel.setLockscreenVisibility(Notification.DEFAULT_ALL);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannelReconnectGatewayListeners() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(
                notificationsChannelIds.get(2), notificationsChannelNames.get(2), importance);
        channel.setDescription(getString(R.string.running_gateway_clients_channel_description));
        channel.setLightColor(R.color.logo_primary);
        channel.setLockscreenVisibility(Notification.DEFAULT_ALL);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void configureNotifications(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    createNotificationChannel();
                }
            });
        }
    }

    private void startServices() {
        GatewayClientHandler gatewayClientHandler = new GatewayClientHandler(getApplicationContext());
        try {
            gatewayClientHandler.startServices(getApplicationContext());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}