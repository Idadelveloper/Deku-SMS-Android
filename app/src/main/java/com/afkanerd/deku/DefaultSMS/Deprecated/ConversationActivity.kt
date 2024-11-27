package com.afkanerd.deku.DefaultSMS.Deprecated

class ConversationActivity() : CustomAppCompactActivity() {
//    var isContact: Boolean = false
//    var actionMode: ActionMode? = null
//    var conversationsRecyclerAdapter: ConversationsRecyclerAdapter? = null
//    var smsTextView: TextInputEditText? = null
//    var mutableLiveDataComposeMessage: MutableLiveData<String?> = MutableLiveData()
//
//    var linearLayoutManager: LinearLayoutManager? = null
//    var singleMessagesThreadRecyclerView: RecyclerView? = null
//
//    var searchPositions: MutableLiveData<List<Int>?>? = MutableLiveData()
//
//    var backSearchBtn: ImageButton? = null
//    var forwardSearchBtn: ImageButton? = null
//
//    var toolbar: Toolbar? = null
//
//    var firstScrollInitiated: Boolean = false
//
//    var searchPointerPosition: Int = 0
//    var searchFoundTextView: TextView? = null
//
//    var lifecycleOwner: LifecycleOwner? = null
//
//    var materialCardView: MaterialCardView? = null
//    var isShortCode: Boolean = false
//    var isDualSim: Boolean = false
//    var smsManager: SmsManager = SmsManager.getDefault()
//    var textInputEditText: TextInputEditText? = null
//    val fragmentManager = supportFragmentManager
//
//    val secureRequestFragmentModalTag = "secureRequestFragmentModalTag"
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_conversations)
//
//        toolbar = findViewById<View>(R.id.conversation_toolbar) as Toolbar
//        setSupportActionBar(toolbar)
//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
//
//        try {
//            configureActivityDependencies()
//            instantiateGlobals()
//            configureToolbars()
//            configureRecyclerView()
//            configureMessagesTextBox()
//
//            configureLayoutForMessageType()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        // Always call the superclass so it can restore the view hierarchy.
//        super.onRestoreInstanceState(savedInstanceState)
//
//        // Restore state members from saved instance.
//        smsTextView!!.setText(savedInstanceState.getString(DRAFT_TEXT))
//
//        savedInstanceState.remove(DRAFT_TEXT)
//        savedInstanceState.remove(DRAFT_ID)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        val layout = findViewById<TextInputLayout>(R.id.conversations_send_text_layout)
//        layout.requestFocus()
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                NativeSMSDB.Incoming.update_read(
//                    applicationContext, 1, threadId,
//                    null
//                )
//                conversationsViewModel?.updateInformation(
//                    applicationContext,
//                    if (isContact) contactName else null, defaultSubscriptionId.value
//                )
//                emptyDraft()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        try {
//            menuInflater.inflate(R.menu.conversations_menu, menu)
//            if (isShortCode) {
//                menu.findItem(R.id.conversation_main_menu_call).setVisible(false)
//                menu.findItem(R.id.conversation_main_menu_encrypt_lock).setVisible(false)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        CoroutineScope(Dispatchers.Default).launch {
//            databaseConnector?.let {
//                val threadedConversations =
//                    it.threadedConversationsDao()[threadId]
//                if (threadedConversations != null && threadedConversations.isIs_mute) {
//                    runOnUiThread {
//                        menu.findItem(R.id.conversations_menu_unmute).setVisible(true)
//                        menu.findItem(R.id.conversations_menu_mute).setVisible(false)
//                    }
//                }
//            }
//        }
//        return super.onCreateOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId) {
//            R.id.conversation_main_menu_call -> {
//                ThreadedConversationsHandler.call(applicationContext, address)
//                return true
//            }
//            R.id.conversation_main_menu_search -> {
//                val intent = Intent(applicationContext, SearchMessagesThreadsActivity::class.java)
//                intent.putExtra(Conversation.THREAD_ID, threadId)
//                startActivity(intent)
//            }
//            R.id.conversations_menu_block -> {
//                blockContact()
//                if (actionMode != null) actionMode!!.finish()
//                return true
//            }
//            R.id.conversations_menu_delete -> {
//                CoroutineScope(Dispatchers.Default).launch {
//                    databaseConnector?.threadedConversationsDao()
//                        ?.delete(applicationContext, listOf(threadId))
//                }
//                finish()
//            }
//            R.id.conversations_menu_mute -> {
//                CoroutineScope(Dispatchers.Default).launch {
//                    conversationsViewModel?.mute()
//                    databaseConnector?.let {
//                        val threadedConversations =
//                            it.threadedConversationsDao()[threadId]
//                        threadedConversations.isIs_mute = true
//
//                        it.threadedConversationsDao().update( applicationContext,
//                            threadedConversations )
//                        invalidateMenu()
//                        runOnUiThread {
//                            configureToolbars()
//                            Toast.makeText(
//                                applicationContext, getString(R.string.conversation_menu_muted),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                            if (actionMode != null) actionMode!!.finish()
//                        }
//                    }
//                }
//                return true
//            }
//            R.id.conversations_menu_unmute -> {
//                CoroutineScope(Dispatchers.Default).launch {
//                    conversationsViewModel?.unMute()
//                    databaseConnector?.let {
//                        val threadedConversations = it.threadedConversationsDao()[threadId]
//                        threadedConversations.isIs_mute = false
//                        it.threadedConversationsDao().update( applicationContext,
//                            threadedConversations )
//
//                        invalidateMenu()
//                        runOnUiThread {
//                            configureToolbars()
//                            Toast.makeText(
//                                applicationContext, getString(R.string.conversation_menu_unmuted),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                            if (actionMode != null) actionMode!!.finish()
//                        }
//                    }
//                }
//                return true
//            }
//            R.id.conversation_main_menu_encrypt_lock -> {
//                val fragmentManager = supportFragmentManager
//                val fragmentTransaction = fragmentManager.beginTransaction()
//                val secureRequestModal = ConversationSecureRequestModal() {
//                    address?.let {
//                        E2EEHandler.clear(applicationContext, it)
//                        val publicKey = E2EEHandler.generateKey(applicationContext, it)
//                        val txPublicKey = E2EEHandler.formatRequestPublicKey(publicKey,
//                            E2EEHandler.MagicNumber.REQUEST)
//                        sendDataMessage(txPublicKey)
//                    }
//                }
//                fragmentTransaction.add(secureRequestModal, "secure_request")
//                fragmentTransaction.commit()
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//    private fun sendDataMessage(data: ByteArray) {
//        val subscriptionId = SIMHandler.getDefaultSimSubscription(applicationContext)
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                val messageId = System.currentTimeMillis().toString()
//                val conversation = Conversation()
//                conversation.thread_id = threadId
//                conversation.address = address
//                conversation.isIs_key = true
//                conversation.message_id = messageId
//                conversation.data = Base64.encodeToString(data, Base64.DEFAULT)
//                conversation.subscription_id = subscriptionId
//                conversation.type = Telephony.Sms.MESSAGE_TYPE_OUTBOX
//                conversation.date = System.currentTimeMillis().toString()
//                conversation.status = Telephony.Sms.STATUS_PENDING
//
//                val id = conversationsViewModel?.insert(applicationContext, conversation)
//                SMSDatabaseWrapper.send_data(applicationContext, conversation)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
//        return super.onPrepareOptionsMenu(menu)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (((smsTextView != null) && (smsTextView!!.text != null) &&
//                    !smsTextView!!.text.toString().isEmpty())
//        ) {
//            try {
//                saveDraft(
//                    System.currentTimeMillis().toString(),
//                    smsTextView!!.text.toString()
//                )
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
//        // Save the user's current game state.
//        savedInstanceState.putString(DRAFT_TEXT, smsTextView!!.text.toString())
//        savedInstanceState.putString(DRAFT_ID, System.currentTimeMillis().toString())
//
//        // Always call the superclass so it can save the view hierarchy state.
//        super.onSaveInstanceState(savedInstanceState)
//    }
//
//    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<String>,
//                                             grantResults: IntArray ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == SEND_SMS_PERMISSION_REQUEST_CODE) {
//            if (grantResults.isNotEmpty()) {
//                Toast.makeText(this, "Let's do this!!", Toast.LENGTH_LONG).show()
//            } else {
//                Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//
//    private fun resetSearch() {
//        findViewById<View>(R.id.conversations_search_results_found).visibility = View.GONE
//        conversationsRecyclerAdapter!!.searchString = null
//        conversationsRecyclerAdapter!!.resetSearchItems(searchPositions!!.value)
//        searchPositions = MutableLiveData()
//    }
//
//    var defaultRegion: String? = null
//    @Throws(Exception::class)
//    private fun configureActivityDependencies() {
//        /**
//         * Address = This could come from Shared Intent, Contacts etc
//         * ThreadID = This comes from Thread screen and notifications
//         * ThreadID is the intended way of populating the messages
//         * ==> If not ThreadId do not populate, everything else should take the pleasure of finding
//         * and sending a threadID to this intent
//         */
//        defaultRegion = Helpers.getUserCountry( applicationContext )
//        if (intent.action != null && ((intent.action == Intent.ACTION_SENDTO) || (intent.action == Intent.ACTION_SEND))) {
//            val sendToString = intent.dataString
//            if (sendToString != null && (sendToString.contains("smsto:") ||
//                        sendToString.contains("sms:"))
//            ) {
//                val address = Helpers.getFormatCompleteNumber(sendToString, defaultRegion)
//                intent.putExtra(Conversation.ADDRESS, address)
//            }
//        }
//
//        if (!intent.hasExtra(Conversation.THREAD_ID) &&
//            !intent.hasExtra(Conversation.ADDRESS)
//        ) {
//            throw Exception("No threadId nor Address supplied for activity")
//        }
//        if (intent.hasExtra(Conversation.THREAD_ID)) {
//            threadId = intent.getStringExtra(Conversation.THREAD_ID)
//        }
//        if (intent.hasExtra(Conversation.ADDRESS)) {
//            address = intent.getStringExtra(Conversation.ADDRESS)
//        }
//
//        if (threadId == null)
//        threadId =
//            ThreadedConversationsHandler.get(applicationContext, address)
//                .thread_id
//        if (address == null) {
//            val thread = Thread {
//                databaseConnector?.let {
//                    val threadedConversations =
//                        it.threadedConversationsDao()[threadId]
//                    address = threadedConversations.address
//                }
//            }
//            thread.start()
//            thread.join()
//        }
//        contactName = Contacts.retrieveContactName( applicationContext,
//            Helpers.getFormatCompleteNumber(address, defaultRegion) )
//        if (contactName == null) {
//            contactName = Helpers.getFormatForTransmission(address, defaultRegion)
//        } else isContact = true
//
//        isShortCode = Helpers.isShortCode(address)
//
//        //        attachObservers();
//        isDualSim = SIMHandler.isDualSim(applicationContext)
//
//        //        if(isDualSim && getIntent().hasExtra(Conversation.SUBSCRIPTION_ID)) {
////            defaultSubscriptionId.setValue(getIntent()
////                    .getIntExtra(Conversation.SUBSCRIPTION_ID, -1));
////        }
//    }
//
//    private fun scrollRecyclerViewSearch(position: Int) {
//        if (position == -2) {
//            val text = "0/0 " + getString(R.string.conversations_search_results_found)
//            searchFoundTextView!!.text = text
//            return
//        }
//
//        conversationsRecyclerAdapter!!.refresh()
//        if (position != -3) singleMessagesThreadRecyclerView!!.scrollToPosition(position)
//        val text =
//            (if (searchPointerPosition == -1) 0 else searchPointerPosition + 1).toString() + "/" + searchPositions!!.value!!.size + " " + getString(
//                R.string.conversations_search_results_found
//            )
//        searchFoundTextView!!.text = text
//    }
//
//    @Throws(GeneralSecurityException::class, IOException::class)
//    private fun instantiateGlobals() {
//        searchFoundTextView = findViewById(R.id.conversations_search_results_found_counter_text)
//
//        backSearchBtn = findViewById(R.id.conversation_search_found_back_btn)
//        forwardSearchBtn = findViewById(R.id.conversation_search_found_forward_btn)
//
//        smsTextView = findViewById(R.id.conversation_send_text_input)
//        singleMessagesThreadRecyclerView = findViewById(R.id.single_messages_thread_recycler_view)
//        linearLayoutManager = LinearLayoutManager(this)
//        linearLayoutManager!!.stackFromEnd = false
//        linearLayoutManager!!.reverseLayout = true
//        singleMessagesThreadRecyclerView!!.setLayoutManager(linearLayoutManager)
//
//        conversationsRecyclerAdapter = ConversationsRecyclerAdapter()
//
//        conversationsViewModel = ViewModelProvider(this)
//            .get(ConversationsViewModel::class.java)
//        conversationsViewModel?.datastore = databaseConnector
//
//        backSearchBtn!!.setOnClickListener(object : View.OnClickListener {
//            override fun onClick(view: View) {
//                if (searchPointerPosition <= 0) searchPointerPosition =
//                    searchPositions!!.value!!.size
//                scrollRecyclerViewSearch(searchPositions!!.value!![--searchPointerPosition])
//            }
//        })
//
//        forwardSearchBtn!!.setOnClickListener(object : View.OnClickListener {
//            override fun onClick(view: View) {
//                if (searchPointerPosition >= searchPositions!!.value!!.size - 1) searchPointerPosition =
//                    -1
//                scrollRecyclerViewSearch(searchPositions!!.value!![++searchPointerPosition])
//            }
//        })
//
//        searchPositions!!.observe(this) {
//            if (!it.isNullOrEmpty()) {
//                searchPointerPosition = 0
//                scrollRecyclerViewSearch(
//                    if (firstScrollInitiated) searchPositions!!.value!![searchPointerPosition] else -3
//                )
//            } else {
//                conversationsRecyclerAdapter!!.searchString = null
//                scrollRecyclerViewSearch(-2)
//            }
//        }
//    }
//
//    @Throws(InterruptedException::class)
//    private fun configureRecyclerView() {
//        singleMessagesThreadRecyclerView!!.adapter = conversationsRecyclerAdapter
//        lifecycleOwner = this
//
//        conversationsRecyclerAdapter!!.addOnPagesUpdatedListener {
//            if (((conversationsRecyclerAdapter!!.getItemCount() < 1) && (
//                        getIntent().getAction() != null) &&
//                        !(getIntent().getAction() == Intent.ACTION_SENDTO) &&
//                        !(getIntent().getAction() == Intent.ACTION_SEND))
//            ) finish()
//            else if (((searchPositions != null) && (searchPositions!!.getValue() != null
//                        ) && !searchPositions!!.getValue()!!.isEmpty()
//                        && !firstScrollInitiated)
//            ) {
//                singleMessagesThreadRecyclerView!!.scrollToPosition(
//                    searchPositions!!.getValue()!!.get(searchPositions!!.getValue()!!.size - 1)
//                )
//                firstScrollInitiated = true
//            } else if (linearLayoutManager!!.findFirstCompletelyVisibleItemPosition() == 0) {
//                singleMessagesThreadRecyclerView!!.scrollToPosition(0)
//            }
//            null
//        }
//
//        if (intent.hasExtra(SEARCH_STRING)) {
//            conversationsViewModel?.threadId = threadId
//            findViewById<View>(R.id.conversations_search_results_found).visibility = View.VISIBLE
//            val searching = intent.getStringExtra(SEARCH_STRING)
//            CoroutineScope(Dispatchers.Default).launch { searchForInput(searching) }
//            configureSearchBox()
//
//            searchPositions!!.value = ArrayList(listOf(intent.getIntExtra(SEARCH_INDEX, 0)))
//
//            conversationsViewModel?.getSearch(applicationContext, threadId, searchPositions!!.value)
//                ?.observe(this) { conversationsRecyclerAdapter!!.submitData( lifecycle, it ) }
//
//        }
//
//        conversationsViewModel?.get(threadId)?.observe( this ) { value ->
//            value?.let {
//                conversationsRecyclerAdapter!!.submitData(lifecycle, value)
//
//                address?.let {
//                    if(fragmentManager.findFragmentByTag(secureRequestFragmentModalTag) == null) {
//                        if(E2EEHandler.hasPendingApproval(applicationContext, it))
//                            showSecureRequestModal()
//                    }
//                }
//            }
//        }
//
//        conversationsRecyclerAdapter!!.retryFailedMessage.observe( this ) {
//            val list: MutableList<Conversation> = ArrayList()
//            list.add(it)
//
//            showFailedRetryModal {
//                CoroutineScope(Dispatchers.Default).launch {
//                    conversationsViewModel?.deleteItems(applicationContext, list)
//                    try {
//                        databaseConnector?.let { db->
//                            val threadedConversations =
//                                db.threadedConversationsDao()[threadId]
//                            sendTextMessage( it.text, threadedConversations.subscription_id,
//                                it.message_id )
//                        }
//                    } catch (e: Exception) {
//                        Log.e(javaClass.name, "Exception sending failed message", e)
//                    }
//                }
//            }
//        }
//
//        conversationsRecyclerAdapter!!.retryFailedDataMessage.observe( this ) {
//            val list: MutableList<Conversation> = ArrayList()
//            list.add(it)
//            showFailedRetryModal {
//                CoroutineScope(Dispatchers.Default).launch {
//                    conversationsViewModel?.deleteItems(applicationContext, list)
//                    try {
//                        databaseConnector?.let { db ->
//                            val threadedConversations = db.threadedConversationsDao()[threadId]
////                            sendDataMessage(threadedConversations);
//                            TODO("Implement method to resend failed data messages")
//                        }
//                    } catch (e: Exception) {
//                        Log.e( javaClass.name, "Exception sending failed data message", e)
//                    }
//                }
//            }
//        }
//
//        conversationsRecyclerAdapter!!.mutableSelectedItems.observe( this,
//            object : Observer<HashMap<Long?, ConversationTemplateViewHandler?>?> {
//                override fun onChanged(selectedItems: HashMap<Long?, ConversationTemplateViewHandler?>?) {
//                    if (selectedItems == null || selectedItems.isEmpty()) {
//                        if (actionMode != null) {
//                            actionMode!!.finish()
//                        }
//                        return
//                    } else if (actionMode == null) {
//                        actionMode = startSupportActionMode(actionModeCallback)
//                    }
//                    if (selectedItems.size > 1 && actionMode != null) actionMode!!.invalidate()
//                    if (actionMode != null) actionMode!!.title = selectedItems.size.toString()
//                }
//            })
//
//    }
//
//    private fun showSecureRequestModal() {
//        val fragmentTransaction = fragmentManager.beginTransaction()
//
//        val secureRequestModal = contactName?.let {
//            ConversationsSecureRequestModalSheetFragment(it) {
//                address?.let { ad ->
//                    val publicKey = E2EEHandler.generateKey(applicationContext, ad)
//
//                    val isSelf = E2EEHandler.isSelf(applicationContext, ad)
//
//                    if(!isSelf) {
//                        val txPublicKey = E2EEHandler.formatRequestPublicKey(publicKey,
//                            E2EEHandler.MagicNumber.ACCEPT)
//
//                        // TODO: put a pending intent here that makes save on message delivered
//                        sendDataMessage(txPublicKey)
//                    } else {
//                        E2EEHandler.secureStorePeerPublicKey(applicationContext, address!!,
//                            publicKey, true)
//                    }
//                }
//            }
//        }
//        secureRequestModal?.let {
//            fragmentTransaction.add(secureRequestModal, secureRequestFragmentModalTag)
//            fragmentTransaction.commit()
//        }
//    }
//
//    private fun showFailedRetryModal(runnable: Runnable) {
//        val fragmentManager = supportFragmentManager
//        val fragmentTransaction = fragmentManager.beginTransaction()
//        val failedMessageRetryModal = FailedMessageRetryModal(runnable)
//        fragmentTransaction.add(failedMessageRetryModal, "failed_message_modal")
//        fragmentTransaction.commit()
//    }
//
//    private fun configureSearchBox() {
////        findViewById(R.id.conversations_pop_ups_layouts).setVisibility(View.VISIBLE);
//        findViewById<View>(R.id.conversations_search_results_found).visibility = View.VISIBLE
//        actionMode = startSupportActionMode(searchActionModeCallback)
//    }
//
//    private fun configureToolbars() {
//        title = "\n"
//        val contactTextView = findViewById<TextView>(R.id.conversation_contact_card_text_view)
//        contactTextView.text = abTitle
//
//        val avatarView =
//            findViewById<AvatarView>(R.id.conversation_contact_card_frame_avatar_initials)
//        val imageView = findViewById<ImageView>(R.id.conversation_contact_card_frame_avatar_photo)
//        val contactColor = Helpers.getColor(applicationContext, threadId)
//        contactName?.let {
//            if (isContact) {
//                avatarView.avatarInitials =
//                    if (it.contains(" ")) contactName else it.substring(0, 1)
//                avatarView.avatarInitialsBackgroundColor = contactColor
//                imageView.visibility = View.INVISIBLE
//            } else {
//                val drawable = getDrawable(R.drawable.baseline_account_circle_24)
//                drawable?.setColorFilter(contactColor, PorterDuff.Mode.SRC_IN)
//                imageView.setImageDrawable(drawable)
//                avatarView.visibility = View.INVISIBLE
//            }
//
//            materialCardView = findViewById(R.id.conversation_toolbar_contact_card)
//            materialCardView!!.setOnClickListener(object : View.OnClickListener {
//                override fun onClick(v: View) {
//                    Log.d(javaClass.name, "Yes contact clicked")
//                    val fragmentManager = supportFragmentManager
//                    val fragmentTransaction = fragmentManager.beginTransaction()
//
//                    val modalSheetFragment =
//                        ConversationsContactModalFragment( it,
//                            Helpers.getFormatForTransmission(address, defaultRegion))
//                    fragmentTransaction.add(modalSheetFragment, ConversationsContactModalFragment.TAG)
//                    fragmentTransaction.show(modalSheetFragment)
//                    fragmentTransaction.commitNow()
//                }
//            })
//        }
//
//        address?.let {
//            if(E2EEHandler.isSecured(applicationContext, it)) {
//                findViewById<View>(R.id.conversation_secured_text).visibility = View.VISIBLE
//            } else {
//                findViewById<View>(R.id.conversation_secured_text).visibility = View.GONE
//            }
//        }
//    }
//
//    private val abTitle: String
//        get() = if (isContact && !contactName.isNullOrBlank()) contactName!! else address!!
//
//    private fun emptyDraft() {
//        conversationsViewModel?.clearDraft(applicationContext)
//    }
//
//    fun getSMSCount(text: String?): String {
//        val messages: List<String> = smsManager.divideMessage(text)
//        val segmentCount = messages[messages.size - 1].length
//        return segmentCount.toString() + "/" + messages.size
//    }
//
//    private fun configureMessagesTextBox() {
//        if (mutableLiveDataComposeMessage.value == null ||
//            mutableLiveDataComposeMessage.value!!.isEmpty()
//        ) {
//            findViewById<View>(R.id.conversation_send_btn).visibility = View.INVISIBLE
//            findViewById<View>(R.id.conversation_secured_text_preview).visibility = View.GONE
//        }
//
//        val counterView = findViewById<TextView>(R.id.conversation_compose_text_counter)
//        val sendBtn = findViewById<View>(R.id.conversation_send_btn)
//
//        val dualSimCardName =
//            findViewById<MaterialTextView>(R.id.conversation_compose_dual_sim_send_sim_name)
//        mutableLiveDataComposeMessage.observe(this) {
//            if (!it.isNullOrBlank()) {
//                sendBtn.visibility = View.VISIBLE
//                if (isDualSim) dualSimCardName.visibility = View.VISIBLE
//
//                CoroutineScope(Dispatchers.Default).launch {
//                    val text = if(E2EEHandler.isSecured(applicationContext, address!!)) {
//                        encryptMessage(applicationContext, it, address!!).first
//                    } else it
//
//                    runOnUiThread {
//                        counterView.visibility = View.VISIBLE
//                        counterView.text = getSMSCount(text)
//                    }
//
//                    if(E2EEHandler.isSecured(applicationContext, address!!)) {
//                        findViewById<MaterialTextView>(R.id.conversation_secured_text_preview).apply {
//                            runOnUiThread {
//                                visibility = View.VISIBLE
//                                this.text = text
//                            }
//                        }
//                    }
//                }
//            } else {
//                sendBtn.visibility = View.GONE
//                dualSimCardName.visibility = View.GONE
//                counterView.visibility = View.GONE
//                findViewById<View>(R.id.conversation_secured_text_preview).visibility = View.GONE
//            }
//        }
//
//        smsTextView!!.setOnTouchListener { view, motionEvent ->
//            view.parent.requestDisallowInterceptTouchEvent(true)
//            if ((motionEvent.action and MotionEvent.ACTION_UP) != 0 &&
//                (motionEvent.actionMasked and MotionEvent.ACTION_UP) != 0
//            ) {
//                view.parent.requestDisallowInterceptTouchEvent(false)
//            }
//            false
//        }
//
//        findViewById<View>(R.id.conversation_send_btn).setOnClickListener {
//            try {
//                if (smsTextView!!.text != null && defaultSubscriptionId.value != null) {
//                    val text = smsTextView!!.text.toString()
//                    CoroutineScope(Dispatchers.Default).launch {
//                        var threadedConversations =
//                            Datastore.getDatastore(applicationContext)
//                                .threadedConversationsDao()[threadId]
//                        if (threadedConversations == null) {
//                            threadedConversations = ThreadedConversations()
//                            threadedConversations.setThread_id(threadId)
//                        }
//                        try {
//                            sendTextMessage(text, defaultSubscriptionId.value!!, null)
//                        } catch (e: NumberParseException) {
//                            e.printStackTrace()
//                        } catch (e: InterruptedException) {
//                            e.printStackTrace()
//                        }
//                    }
//                    smsTextView!!.text = null
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//        smsTextView!!.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
//            }
//
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//            }
//
//            override fun afterTextChanged(s: Editable) {
//                if(E2EEHandler.isSecured(applicationContext, address!!)) {
//                    runOnUiThread {
//                        findViewById<LinearProgressIndicator>(R.id.conversation_secured_progress)
//                            .visibility = View.VISIBLE
//                    }
//                }
//            }
//        })
//
//        RxTextView.textChanges(smsTextView!!)
//            .debounce(2, TimeUnit.SECONDS)
//            .subscribe {
//                if(E2EEHandler.isSecured(applicationContext, address!!)) {
//                    runOnUiThread {
//                        findViewById<LinearProgressIndicator>(R.id.conversation_secured_progress)
//                            .visibility = View.GONE
//                    }
//                }
//                runOnUiThread {
//                    mutableLiveDataComposeMessage.value = smsTextView!!.text.toString()
//                }
//            }
//
//
//        // Message has been shared from another app to send by SMS
//        if (intent.hasExtra(Conversation.SHARED_SMS_BODY)) {
//            smsTextView!!.setText(intent.getStringExtra(Conversation.SHARED_SMS_BODY))
//            intent.removeExtra(Conversation.SHARED_SMS_BODY)
//        }
//
//        try {
//            checkDrafts()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    @Throws(InterruptedException::class)
//    private fun checkDrafts() {
//        if (smsTextView!!.text == null || smsTextView!!.text.toString()
//                .isEmpty()
//        ) CoroutineScope(Dispatchers.Default).launch {
//            try {
//                val conversation =
//                    conversationsViewModel?.fetchDraft()
//                if (conversation != null) {
//                    runOnUiThread(object : Runnable {
//                        override fun run() {
//                            smsTextView!!.setText(conversation.text)
//                        }
//                    })
//                }
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private fun configureLayoutForMessageType() {
//        if (isShortCode) {
//            // Cannot reply to message
//            val smsLayout = findViewById<ConstraintLayout>(R.id.compose_message_include_layout)
//            smsLayout.visibility = View.INVISIBLE
//
//            val shortCodeSnackBar = Snackbar.make(
//                findViewById(R.id.conversation_coordinator_layout),
//                getString(R.string.conversation_shortcode_description), Snackbar.LENGTH_INDEFINITE
//            )
//
//            //            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext(), R.style.Theme_main);
//            val builder = AlertDialog.Builder(this)
//            builder.setMessage(getString(R.string.conversation_shortcode_learn_more_text))
//                .setNegativeButton(getString(R.string.conversation_shortcode_learn_more_ok),
//                    object : DialogInterface.OnClickListener {
//                        override fun onClick(dialog: DialogInterface, which: Int) {
//                        }
//                    })
//            val dialog = builder.create()
//            val onClickListener: View.OnClickListener = object : View.OnClickListener {
//                override fun onClick(v: View) {
//                    dialog.show()
//                }
//            }
//            shortCodeSnackBar.setAction(
//                getString(R.string.conversation_shortcode_action_button),
//                onClickListener
//            )
//            shortCodeSnackBar.show()
//        }
//    }
//
//    private fun blockContact() {
//        CoroutineScope(Dispatchers.Default).launch {
//            val threadedConversations =
//                Datastore.getDatastore(applicationContext)
//                    .threadedConversationsDao()[threadId]
//            threadedConversations.isIs_blocked = true
//            databaseConnector?.threadedConversationsDao()?.update(
//                applicationContext,
//                threadedConversations
//            )
//        }
//
//        val contentValues = ContentValues()
//        contentValues.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, address)
//        val uri = contentResolver.insert(
//            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
//            contentValues
//        )
//
//        Toast.makeText(
//            applicationContext, getString(R.string.conversations_menu_block_toast),
//            Toast.LENGTH_SHORT
//        ).show()
//        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
//        startActivity(telecomManager.createManageBlockedNumbersIntent(), null)
//    }
//
//    private fun shareItem() {
//        val entry: Set<Map.Entry<Long, ConversationTemplateViewHandler>> =
//            conversationsRecyclerAdapter!!.mutableSelectedItems.value!!.entries
//        val text = entry.iterator().next().value.text
//        val sendIntent = Intent()
//        sendIntent.setAction(Intent.ACTION_SEND)
//        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
//        sendIntent.setType("text/plain")
//
//        val shareIntent = Intent.createChooser(sendIntent, null)
//        // Only use for components you have control over
//        val excludedComponentNames = arrayOf(
//            ComponentName(
//                BuildConfig.APPLICATION_ID,
//                ThreadedConversationsActivity::class.java.name
//            )
//        )
//        shareIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponentNames)
//        startActivity(shareIntent)
//    }
//
//    private fun copyItem() {
//        val entry: Set<Map.Entry<Long, ConversationTemplateViewHandler>> =
//            conversationsRecyclerAdapter!!.mutableSelectedItems.value!!.entries
//        val text = entry.iterator().next().value.text
//        val clip = ClipData.newPlainText(text, text)
//        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
//        clipboard.setPrimaryClip(clip)
//
//        Toast.makeText(
//            applicationContext, getString(R.string.conversation_copied),
//            Toast.LENGTH_SHORT
//        ).show()
//    }
//
//    private fun replay() {
//        for (viewHandler: ConversationTemplateViewHandler in conversationsRecyclerAdapter!!.mutableSelectedItems.value!!.values) {
//            val conversation = Conversation()
//            conversation.id = viewHandler.id
//            conversation.message_id = viewHandler.message_id
//
//            CoroutineScope(Dispatchers.Default).launch {
//                GatewayServer.route(applicationContext, conversation)
//            }
//        }
//    }
//
//    @Throws(Exception::class)
//    private fun deleteItems() {
//        val conversationList: MutableList<Conversation> = ArrayList()
//        for (viewHandler: ConversationTemplateViewHandler in conversationsRecyclerAdapter!!.mutableSelectedItems.value!!.values) {
//            val conversation = Conversation()
//            conversation.id = viewHandler.id
//            conversation.message_id = viewHandler.message_id
//            conversationList.add(conversation)
//        }
//        CoroutineScope(Dispatchers.Default).launch {
//            conversationsViewModel?.deleteItems(applicationContext, conversationList)
//        }
//    }
//
//    private fun searchForInput(search: String?) {
//        conversationsRecyclerAdapter!!.searchString = search
//        try {
//            searchPositions!!.postValue(conversationsViewModel?.search(search))
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//    }
//
//    @Throws(InterruptedException::class)
//    private fun viewDetailsPopUp() {
//        val entry: Set<Map.Entry<Long, ConversationTemplateViewHandler>> =
//            conversationsRecyclerAdapter!!.mutableSelectedItems.value!!.entries
//        val messageId = entry.iterator().next().value.message_id
//
//        val detailsBuilder = StringBuilder()
//        val builder = AlertDialog.Builder(this)
//            .setTitle(getString(R.string.conversation_menu_view_details_title))
//            .setMessage(detailsBuilder)
//
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                val conversation = conversationsViewModel?.fetch(messageId)
//                runOnUiThread {
//                    detailsBuilder.append(getString(R.string.conversation_menu_view_details_type))
//                        .append(
//                            if (conversation?.text!!.isNotEmpty())
//                                getString(R.string.conversation_menu_view_details_type_text)
//                            else getString( R.string.conversation_menu_view_details_type_data )
//                        )
//                        .append("\n")
//                        .append(
//                            if (conversation.type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX)
//                                getString( R.string.conversation_menu_view_details_from )
//                            else getString(R.string.conversation_menu_view_details_to)
//                        )
//                        .append(conversation.address)
//                        .append("\n")
//                        .append(getString(R.string.conversation_menu_view_details_sent))
//                        .append(
//                            if (conversation.type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX)
//                                Helpers.formatLongDate( conversation.date_sent!!.toLong() )
//                            else Helpers.formatLongDate( conversation.date!!.toLong() )
//                        )
//                    if (conversation.type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX) {
//                        detailsBuilder.append("\n")
//                            .append(getString(R.string.conversation_menu_view_details_received))
//                            .append(Helpers.formatLongDate(conversation.date!!.toLong()))
//                    }
//
//                    val dialog = builder.create()
//                    dialog.show()
//                }
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private val searchActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
//        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
//            Objects.requireNonNull(supportActionBar)?.hide()
//
//            val viewGroup = layoutInflater.inflate(
//                R.layout.layout_conversation_search_bar,
//                null
//            )
//            mode.customView = viewGroup
//
//            //            MenuInflater inflater = mode.getMenuInflater();
////            inflater.inflate(R.menu.conversations_menu_search, menu);
////
////            MenuItem searchItem = menu.findItem(R.id.conversations_search_active);
////            searchItem.expandActionView();
//            val searchString = intent.getStringExtra(SEARCH_STRING)
//            intent.removeExtra(SEARCH_STRING)
//
//            textInputEditText = viewGroup.findViewById(R.id.conversation_search_input)
//            textInputEditText!!.addTextChangedListener(object : TextWatcher {
//                override fun beforeTextChanged(
//                    s: CharSequence,
//                    start: Int,
//                    count: Int,
//                    after: Int
//                ) {
//                }
//
//                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
//                }
//
//                override fun afterTextChanged(editable: Editable) {
//                    if (editable != null && editable.length > 1) {
//                        conversationsRecyclerAdapter!!.searchString = editable.toString()
//                        conversationsRecyclerAdapter!!.resetSearchItems(searchPositions!!.value)
//                        CoroutineScope(Dispatchers.Default).launch{
//                            searchForInput(editable.toString())
//                        }
//                    } else {
//                        conversationsRecyclerAdapter!!.searchString = null
//                        if (actionMode != null) actionMode!!.finish()
//                    }
//                }
//            })
//            textInputEditText!!.setText(searchString)
//            return true
//        }
//
//        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
//            return false // Return false if nothing is done.
//        }
//
//        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//            return false
//        }
//
//        // Called when the user exits the action mode.
//        override fun onDestroyActionMode(mode: ActionMode) {
//            actionMode = null
//            toolbar!!.visibility = View.VISIBLE
//            resetSearch()
//        }
//    }
//
//    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
//        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
//            Objects.requireNonNull(supportActionBar)?.hide()
//            val inflater = mode.menuInflater
//            inflater.inflate(R.menu.conversations_menu_item_selected, menu)
//            return true
//        }
//
//        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
//            conversationsRecyclerAdapter?.mutableSelectedItems?.value?.let {
//                if(it.size > 1) {
//                    menu.clear()
//                    mode.menuInflater.inflate(R.menu.conversations_menu_items_selected, menu)
//                    return true
//                }
//            }
//            return false // Return false if nothing is done.
//        }
//
//        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//            val id = item.itemId
//            if (R.id.conversations_menu_copy == id) {
//                copyItem()
//                if (actionMode != null) actionMode!!.finish()
//                return true
//            }
//            else if (R.id.conversations_menu_share == id) {
//                shareItem()
//                if (actionMode != null) actionMode!!.finish()
//                return true
//            }
//            else if (R.id.conversations_menu_delete == id ||
//                R.id.conversations_menu_delete_multiple == id
//            ) {
//                try {
//                    deleteItems()
//                    if (actionMode != null) actionMode!!.finish()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                return true
//            }
//            else if (R.id.conversations_menu_replay == id) {
//                replay()
//                if (actionMode != null) actionMode!!.finish()
//                return true
//            }
//            else if(R.id.conversations_menu_view_details == id) {
//                viewDetailsPopUp()
//                if (actionMode != null) actionMode!!.finish()
//                return true
//            }
//            return false
//        }
//
//        // Called when the user exits the action mode.
//        override fun onDestroyActionMode(mode: ActionMode) {
//            supportActionBar?.show()
//            actionMode = null
//            conversationsRecyclerAdapter!!.resetAllSelectedItems()
//        }
//    }
//
//    companion object {
//        val IMAGE_URI: String = "IMAGE_URI"
//        val SEARCH_STRING: String = "SEARCH_STRING"
//        val SEARCH_INDEX: String = "SEARCH_INDEX"
//        val SEND_SMS_PERMISSION_REQUEST_CODE: Int = 1
//
//        val DRAFT_TEXT: String = "DRAFT_TEXT"
//        val DRAFT_ID: String = "DRAFT_ID"
//    }
}