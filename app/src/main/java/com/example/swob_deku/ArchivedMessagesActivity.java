package com.example.swob_deku;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.swob_deku.Models.Archive.Archive;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.Archive.ArchivedViewModel;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Router.RouterViewModel;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.HashMap;
import java.util.List;

public class ArchivedMessagesActivity extends AppCompatActivity {

    public MessagesThreadRecyclerAdapter archivedThreadRecyclerAdapter;

    ArchivedViewModel archivedViewModel;
    Toolbar myToolbar;
    ActionBar ab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived_messages);

        myToolbar = (Toolbar) findViewById(R.id.messages_archived_toolbar);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ab = getSupportActionBar();
        ab.setTitle(R.string.archived_messages_toolbar_title);

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);


        RecyclerView archivedMessagesRecyclerView = findViewById(R.id.messages_archived_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        archivedMessagesRecyclerView.setLayoutManager(linearLayoutManager);

        // TODO: search - and goto message in adapter
        archivedThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter(
                this, R.layout.messages_threads_layout, true, "", this);

        archivedMessagesRecyclerView.setAdapter(archivedThreadRecyclerAdapter);

        archivedViewModel = new ViewModelProvider(this).get(
                ArchivedViewModel.class);

        try {
            archivedViewModel.getMessages(getApplicationContext()).observe(this,
                    new Observer<List<SMS>>() {
                        @Override
                        public void onChanged(List<SMS> smsList) {
                            archivedThreadRecyclerAdapter.submitList(smsList);
                            if(!smsList.isEmpty())
                                findViewById(R.id.messages_archived_no_messages).setVisibility(View.GONE);
                            else {
                                findViewById(R.id.messages_archived_no_messages).setVisibility(View.VISIBLE);
                                archivedMessagesRecyclerView.smoothScrollToPosition(0);
                            }
                        }
                    });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        archivedThreadRecyclerAdapter.selectedItems.observe(this, new Observer<HashMap<String, MessagesThreadRecyclerAdapter.ViewHolder>>() {
            @Override
            public void onChanged(HashMap<String, MessagesThreadRecyclerAdapter.ViewHolder> stringViewHolderHashMap) {
                highlightListener(stringViewHolderHashMap.size());
            }
        });

        myToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.archive_unarchive) {
                    try {
                        String[] ids = archivedThreadRecyclerAdapter.selectedItems.getValue()
                                .keySet().toArray(new String[0]);

                        long[] longArr = new long[ids.length];
                        for (int i = 0; i < ids.length; i++)
                            longArr[i] = Long.parseLong(ids[i]);

                        ArchiveHandler.removeMultipleFromArchive(getApplicationContext(), longArr);
                        archivedThreadRecyclerAdapter.resetAllSelectedItems();
                        archivedViewModel.informChanges();
                        return true;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                else if(item.getItemId() == R.id.archive_delete) {
                    try {
                        String[] ids = archivedThreadRecyclerAdapter.selectedItems.getValue()
                                .keySet().toArray(new String[0]);

                        SMSHandler.deleteThreads(getApplicationContext(), ids);
                        archivedThreadRecyclerAdapter.resetAllSelectedItems();
                        archivedViewModel.informChanges();
                        return true;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home
                && archivedThreadRecyclerAdapter.selectedItems.getValue() != null &&
                !archivedThreadRecyclerAdapter.selectedItems.getValue().isEmpty()) {
            archivedThreadRecyclerAdapter.resetAllSelectedItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.archive_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void highlightListener(int size){
        Menu menu = myToolbar.getMenu();
        Log.d(getLocalClassName(), "Size: " + size);
        if(size < 1) {
            menu.setGroupVisible(R.id.archive_menu, false);
            ab.setTitle(R.string.archived_messages_toolbar_title);
            ab.setHomeAsUpIndicator(null);
        } else {
            menu.setGroupVisible(R.id.archive_menu, true);
            ab.setHomeAsUpIndicator(R.drawable.baseline_cancel_24);
            ab.setTitle(String.valueOf(size));
        }
    }
}