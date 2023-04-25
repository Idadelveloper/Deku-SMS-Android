package com.example.swob_deku.Models.Messages;


import android.content.Context;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import androidx.paging.PagingSource;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSPaging;

import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.functions.Function0;

public class SingleMessageViewModel extends ViewModel {
    String threadId;
    Context context;

    Lifecycle lifecycle;

    MutableLiveData mutableLiveData;
    LiveData liveData;

    Pager<Integer, SMS> pager;
    SMSPaging smsPaging;

    public Integer currentLimit = 12;
    Integer offset = 0;

    public boolean offsetStartedFromZero = true;

//    public LiveData<PagingData<SMS>> getMessages(Context context, String threadId, Lifecycle lifecycle){
//        this.threadId = threadId;
//        this.context = context;
//        this.lifecycle = lifecycle;
//
//        liveData = loadSMSThreads();
//        return liveData;
//    }

    public LiveData getMessages(Context context, String threadId, int offset){
        this.threadId = threadId;
        this.context = context;

        if((offset - 1) > 0) {
            offsetStartedFromZero = false;
//            offset = offset > 2 ? offset - (currentLimit/2) : offset;
//            Log.d(getClass().getName(), "Offset in getting messages: " + (offset -1));
//            offset -=1;
            this.offset = offset;
        }
        Log.d(getClass().getName(), "Loading data for offset: " + offset);
        this.mutableLiveData = new MutableLiveData(loadSMSThreads(offset, currentLimit));
        return mutableLiveData;
    }

    public void informNewItemChanges(String threadId) {
        this.threadId = threadId;
        informNewItemChanges();
    }

    public void informNewItemChanges() {
        offset = 0;
        this.mutableLiveData.setValue(loadSMSThreads(offset, currentLimit));
    }

    public void refresh() {
        if(offset != null) {
            offset += currentLimit;
            offset = _updateLiveData(offset);
        }
    }

    public int refreshDown() {
        int newSize = 0;
        if(!offsetStartedFromZero && offset != null) {
            Log.d(getClass().getName(), "Old offset: " + offset);
            int calculatedOffset = offset - currentLimit;
            int newLimit = currentLimit;
            if(calculatedOffset < 0) {
                newLimit = offset;
                offset = 0;
            }
            else offset = calculatedOffset;

            Log.d(getClass().getName(), "New offset: " + offset);
            newSize = _updateLiveDataDown(offset, newLimit);
        }
        return newSize;
    }

    private Integer _updateLiveData(int offset) {
        List newSMS = loadSMSThreads(offset, currentLimit);

        if (!newSMS.isEmpty()) {
            ArrayList sms = (ArrayList) mutableLiveData.getValue();

            sms.addAll(newSMS);

            Log.d(getClass().getName(), "Updating live data...: " + newSMS.size());
            mutableLiveData.setValue(sms);

            return offset;
        }

        return null;
    }

    private Integer _updateLiveDataDown(int offset, int limit) {
        List newSMS = loadSMSThreads(offset, limit);

        if (!newSMS.isEmpty()) {
            ArrayList sms = (ArrayList) mutableLiveData.getValue();

            ArrayList mergedList = new ArrayList();
            mergedList.addAll(newSMS);
            mergedList.addAll(sms);

            Log.d(getClass().getName(), "Updating live data...: " + newSMS.size());
            mutableLiveData.setValue(mergedList);

        }
        this.offset = offset == 0 ? null : offset;
        return newSMS.size();
    }

    private List loadSMSThreads(Integer _offset, int limit) {
        if(_offset == null)
            _offset = 0;
        return SMSPaging.fetchSMSFromHandlers(context, threadId, limit, _offset);
    }

//    private LiveData loadSMSThreads() {
//        // TODO: make 20
//        final int pageSize = 12;
//        // TODO: make 40
//        final int prefetchDistance = 0;
//        // TODO: make 30
//        final int initialLoad = pageSize;
//
//        PagingConfig pagingConfig = new PagingConfig(pageSize);
////        PagingConfig pagingConfig = new PagingConfig(pageSize, prefetchDistance,
////                true, initialLoad);
//        pager = new Pager<>(pagingConfig, new Function0<PagingSource<Integer, SMS>>() {
//            @Override
//            public PagingSource<Integer, SMS> invoke() {
//                smsPaging = new SMSPaging(context, threadId);
//                return smsPaging;
//            }
//        });
//
////        LiveData lliveData = PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
////        if(lliveData != null)
////            liveData = lliveData;
//
//        return PagingLiveData.cachedIn(PagingLiveData.getLiveData(pager), lifecycle);
//    }

}
