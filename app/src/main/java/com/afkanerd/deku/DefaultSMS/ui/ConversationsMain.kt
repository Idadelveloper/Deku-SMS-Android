package com.afkanerd.deku.DefaultSMS.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ConversationsViewModel
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.SearchViewModel
import com.afkanerd.deku.DefaultSMS.AdaptersViewModels.ThreadedConversationsViewModel
import com.afkanerd.deku.DefaultSMS.BuildConfig
import com.afkanerd.deku.DefaultSMS.Commons.Helpers
import com.afkanerd.deku.DefaultSMS.Deprecated.ThreadedConversationsActivity
import com.afkanerd.deku.DefaultSMS.HomeScreen
import com.afkanerd.deku.DefaultSMS.Models.Contacts
import com.afkanerd.deku.DefaultSMS.Models.Conversations.Conversation
import com.afkanerd.deku.DefaultSMS.Models.E2EEHandler
import com.afkanerd.deku.DefaultSMS.Models.SIMHandler
import com.afkanerd.deku.DefaultSMS.Models.SMSHandler.sendDataMessage
import com.afkanerd.deku.DefaultSMS.Models.SMSHandler.sendTextMessage
import com.afkanerd.deku.DefaultSMS.R
import com.afkanerd.deku.DefaultSMS.SearchThreadScreen
import com.afkanerd.deku.DefaultSMS.ui.Components.ChatCompose
import com.afkanerd.deku.DefaultSMS.ui.Components.ConvenientMethods
import com.afkanerd.deku.DefaultSMS.ui.Components.ConversationPositionTypes
import com.afkanerd.deku.DefaultSMS.ui.Components.ConversationStatusTypes
import com.afkanerd.deku.DefaultSMS.ui.Components.ConversationsCard
import com.afkanerd.deku.DefaultSMS.ui.Components.FailedMessageOptionsModal
import com.afkanerd.deku.DefaultSMS.ui.Components.SearchCounterCompose
import com.afkanerd.deku.DefaultSMS.ui.Components.SearchTopAppBarText
import com.afkanerd.deku.DefaultSMS.ui.Components.SecureRequestAcceptModal
import com.example.compose.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

private fun copyItem(context: Context, text: String) {
    val clip = ClipData.newPlainText(text, text)
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(clip)

    Toast.makeText(
        context, context.getString(R.string.conversation_copied),
        Toast.LENGTH_SHORT
    ).show()
}

private fun sendSMS(
    context: Context,
    text: String,
    messageId: String,
    threadId: String,
    address: String,
    conversationsViewModel: ConversationsViewModel
) {
    val subscriptionId = SIMHandler.getDefaultSimSubscription(context)

    val conversation = Conversation()
    conversation.text = text
    conversation.message_id = messageId
    conversation.thread_id = threadId
    conversation.subscription_id = subscriptionId
    conversation.type = Telephony.Sms.MESSAGE_TYPE_OUTBOX
    conversation.date = System.currentTimeMillis().toString()
    conversation.address = address
    conversation.status = Telephony.Sms.STATUS_PENDING
    conversation.isRead = true

    sendTextMessage(
        context = context,
        text = text,
        address = address,
        conversation = conversation,
        conversationsViewModel = conversationsViewModel,
        messageId = null,
    )
}


private fun getContentType(
    index: Int,
    conversation: Conversation,
    conversations: List<Conversation>
): ConversationPositionTypes {
    if(conversations.size < 2) {
        return ConversationPositionTypes.NORMAL_TIMESTAMP
    }
    if(index == 0) {
        if(Helpers.isSameHour(conversation.date!!.toLong(),
                conversations[index + 1].date!!.toLong())) {
            if(conversation.type == conversations[index + 1].type) {
                if(Helpers.isSameMinute(conversation.date!!.toLong(),
                        conversations[index + 1].date!!.toLong())) {
                    return ConversationPositionTypes.END
                }
            }
            return ConversationPositionTypes.NORMAL
        }
    }
    else if(index == conversations.size - 1) {
        if(conversation.type == conversations[index - 1].type) {
            if(Helpers.isSameMinute(conversation.date!!.toLong(),
                    conversations[index - 1].date!!.toLong())) {
                return ConversationPositionTypes.START_TIMESTAMP
            }
        }
        return ConversationPositionTypes.NORMAL_TIMESTAMP
    }
    else {
        if(Helpers.isSameHour(conversation.date!!.toLong(),
                conversations[index + 1].date!!.toLong())) {
            if(conversation.type == conversations[index - 1].type) {
                if(Helpers.isSameMinute(conversation.date!!.toLong(),
                        conversations[index - 1].date!!.toLong())) {
                    if(Helpers.isSameMinute(conversation.date!!.toLong(),
                            conversations[index + 1].date!!.toLong())) {
                        return ConversationPositionTypes.MIDDLE
                    }
                    return ConversationPositionTypes.START
                } else {
                    if(Helpers.isSameMinute(conversation.date!!.toLong(),
                            conversations[index + 1].date!!.toLong())) {
                        return ConversationPositionTypes.END
                    }
                    return ConversationPositionTypes.NORMAL
                }
            }
        } else {
            if(conversation.type == conversations[index + 1].type) {
                if(Helpers.isSameMinute(conversation.date!!.toLong(),
                        conversations[index - 1].date!!.toLong())) {
                    return ConversationPositionTypes.START_TIMESTAMP
                }
            }
        }
    }
    return ConversationPositionTypes.NORMAL
}

@Preview
@Composable
private fun ConversationCrudBottomBar(
    viewModel: ConversationsViewModel = ConversationsViewModel(),
    items: List<Conversation> = emptyList(),
    onCompleted: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    BottomAppBar (
        actions = {
            Row {
                IconButton(onClick = {
                    CoroutineScope(Dispatchers.Default).launch {
                        onCancel?.let { it() }
                    }
                }) {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_selected_messages))
                }

                Text(
                    viewModel.selectedItems.size.toString(),
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                Spacer(Modifier.weight(1f))

                if(viewModel.selectedItems.size < 2) {
                    IconButton(onClick = {
                        val conversation = items.firstOrNull {
                            it.message_id in viewModel.selectedItems
                        }
                        copyItem(context, conversation?.text!!)
                        onCompleted?.invoke()
                    }) {
                        Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy_message))
                    }

                    IconButton(onClick = {
                        TODO("Implement forward message")
                    }) {
                        Icon(painter= painterResource(id= R.drawable.rounded_forward_24),
                            stringResource(R.string.forward_message)
                        )
                    }

                    IconButton(onClick = {
                        val conversation = items.firstOrNull {
                            it.message_id in viewModel.selectedItems
                        }
                        shareItem(context, conversation?.text!!)
                        onCompleted?.let { it() }
                    }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share_message))
                    }
                }

                IconButton(onClick = {
                    CoroutineScope(Dispatchers.Default).launch {
                        val conversations = items.filter {
                            it.message_id in viewModel.selectedItems
                        }
                        viewModel.deleteItems(context, conversations)
                        onCompleted?.let { it() }
                    }
                }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete_message))
                }
            }

        }
    )
}


private fun shareItem(context: Context, text: String) {
    val sendIntent = Intent().apply {
        setAction(Intent.ACTION_SEND)
        putExtra(Intent.EXTRA_TEXT, text)
        setType("text/plain")
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    // Only use for components you have control over
    val excludedComponentNames = arrayOf(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            ThreadedConversationsActivity::class.java.name
        )
    )
    shareIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponentNames)
    context.startActivity(shareIntent)
}

private fun call(context: Context, address: String) {
    val callIntent = Intent(Intent.ACTION_DIAL).apply {
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        setData(Uri.parse("tel:$address"));
    }
    context.startActivity(callIntent);
}

@Composable
private fun MainDropDownMenu(
    expanded: Boolean = true,
    searchCallback: (() -> Unit)? = null,
    blockCallback: (() -> Unit)? = null,
    deleteCallback: (() -> Unit)? = null,
    muteCallback: (() -> Unit)? = null,
    isMute: Boolean = false,
    isBlocked: Boolean = false,
    dismissCallback: ((Boolean) -> Unit)? = null,
) {
    var expanded = expanded
    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentSize(Alignment.TopEnd)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { dismissCallback?.let{ it(false) }},
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text=stringResource(R.string.conversations_menu_search_title),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    searchCallback?.let{
                        dismissCallback?.let { it(false) }
                        it()
                    }
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text=if(isBlocked) stringResource(R.string.conversations_menu_unblock)
                        else stringResource(R.string.conversation_menu_block),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    blockCallback?.let {
                        dismissCallback?.let { it(false) }
                        it()
                    }
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text=stringResource(R.string.conversation_menu_delete),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    deleteCallback?.let {
                        dismissCallback?.let { it(false) }
                        it()
                    }
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text= if(isMute) stringResource(R.string.conversation_menu_unmute)
                        else stringResource(R.string.conversation_menu_mute),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                onClick = {
                    muteCallback?.let {
                        dismissCallback?.let { it(false) }
                        it()
                    }
                }
            )
        }
    }
}

fun backHandler(
    context: Context,
    viewModel: ConversationsViewModel,
    navController: NavController
) {
    if(viewModel.text.isNotBlank()) {
        CoroutineScope(Dispatchers.Default).launch {
            viewModel.insertDraft(context)
        }
    }

    if(!viewModel.selectedItems.isEmpty()) {
        viewModel.selectedItems.clear()
    }
    else navController.popBackStack()
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Conversations(
    viewModel: ConversationsViewModel = ConversationsViewModel(),
    searchViewModel: SearchViewModel = SearchViewModel(),
    threadConversationsViewModel: ThreadedConversationsViewModel = ThreadedConversationsViewModel(),
    navController: NavController,
    _items: List<Conversation>? = null
) {
    val context = LocalContext.current
    var isSecured by remember {
        mutableStateOf(
            if(viewModel.address.isBlank()) false
            else E2EEHandler.isSecured(context, viewModel.address)
        )
    }

    var showSecureRequestModal by rememberSaveable { mutableStateOf(false) }
    var showSecureAgreeModal by rememberSaveable {
        mutableStateOf(
            if(viewModel.address.isBlank()) false
            else E2EEHandler.hasPendingApproval(context, viewModel.address)
        )
    }
    var showFailedRetryModal by rememberSaveable { mutableStateOf(false) }

    val items: List<Conversation>? = _items ?: viewModel
        .getLiveData(context)
        ?.observeAsState(emptyList())
        ?.value

    val selectedItems = remember { viewModel.selectedItems }

    val listState = rememberLazyListState()
    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var rememberMenuExpanded by remember { mutableStateOf( false) }
    val searchIndexes = remember { mutableStateListOf<Int>() }

    var searchQuery by remember { mutableStateOf(viewModel.searchQuery) }
    var searchIndex by remember { mutableIntStateOf(0) }

    var isMute by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val defaultRegion = Helpers.getUserCountry( context )

    LaunchedEffect(items) {
        if(searchQuery.isNotBlank()) {
            CoroutineScope(Dispatchers.Default).launch {
                items?.forEachIndexed { index, it ->
                    it.text?.let { text ->
                        if(it.text!!.contains(other=searchQuery, ignoreCase=true)
                            && !searchIndexes.contains(index))
                            searchIndexes.add(index)
                    }
                }
            }
        }

        if(searchIndexes.isNotEmpty() && searchIndex == 0)
            listState.animateScrollToItem(searchIndexes.first())

        CoroutineScope(Dispatchers.Default).launch {
            threadConversationsViewModel.get(context, viewModel.threadId)?.let {
                isMute = it.isIs_mute
                isBlocked = it.isIs_blocked
            }

        }
    }

    LaunchedEffect(true){
        if(searchQuery.isBlank())
            listState.animateScrollToItem(0)


        CoroutineScope(Dispatchers.Default).launch {
            Contacts.retrieveContactName(
                context,
                Helpers.getFormatCompleteNumber(viewModel.address, defaultRegion)
            )?.let { viewModel.contactName = it }

            if(viewModel.contactName.isNullOrBlank())
                viewModel.contactName = viewModel.address

        }

        CoroutineScope(Dispatchers.Default).launch {
            viewModel.fetchDraft(context)?.let {
                viewModel.clearDraft(context)
                viewModel.text = it.text!!
            }
        }
    }

    BackHandler {
        if(searchQuery.isNotBlank()) searchQuery = ""
        else
        backHandler(
            context = context,
            viewModel = viewModel,
            navController = navController,
        )
    }

    MainDropDownMenu(
        rememberMenuExpanded,
        isMute = isMute,
        isBlocked = isBlocked,
        searchCallback = {
            searchViewModel.threadId = viewModel.threadId
            navController.navigate(SearchThreadScreen)
        },
        blockCallback = {
            if(isBlocked) {
                val ids = listOf(viewModel.threadId)
                CoroutineScope(Dispatchers.Default).launch {
                    threadConversationsViewModel.unblock(context, ids)
                }
            }
            else
                ConvenientMethods.blockContact(context, viewModel.threadId, viewModel.address)
        },
        deleteCallback = {
            val ids = listOf(viewModel.threadId)
            CoroutineScope(Dispatchers.Default).launch{
                threadConversationsViewModel.delete(context, ids)
            }
            backHandler(
                context = context,
                viewModel = viewModel,
                navController = navController,
            )
        },
        muteCallback = {
            CoroutineScope(Dispatchers.Default).launch {
                threadConversationsViewModel.get(context, viewModel.threadId)?.let {
                    if(it.isIs_mute) viewModel.unMute(context)
                    else viewModel.mute(context)
                }
            }
        }
    ) {
        rememberMenuExpanded = false
    }

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if(searchQuery.isBlank()) {
                        Text(
                            text= viewModel.contactName!!,
                            maxLines =1,
                            overflow = TextOverflow.Ellipsis)

                        if(isSecured) {
                            Text(
                                text= stringResource(R.string.secured),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines =1,
                                overflow = TextOverflow.Ellipsis)
                        }

                    } else {
                        SearchTopAppBarText(
                            searchQuery,
                            cancelCallback = { searchQuery = "" }
                        ) {
                            searchIndexes.clear()
                            searchQuery = it
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if(searchQuery.isNotBlank()) searchQuery = ""
                        else
                        backHandler(
                            context = context,
                            viewModel = viewModel,
                            navController = navController,
                        )
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.go_back))
                    }

                },
                actions = {
                    if(searchQuery.isBlank()) {
                        IconButton(onClick = {
                            call(context, viewModel.address)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = stringResource(R.string.call)
                            )
                        }

                        IconButton(onClick = {
                            showSecureRequestModal = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.EnhancedEncryption,
                                contentDescription = stringResource(
                                    R.string
                                        .request_secure_communication)
                            )
                        }

                        IconButton(onClick = {
                            rememberMenuExpanded = !rememberMenuExpanded
                        }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.open_menu)
                            )
                        }

                    }
                },
                scrollBehavior = scrollBehaviour
            )
        },
        bottomBar = {
            if(!selectedItems.isEmpty()) {
                ConversationCrudBottomBar(
                    viewModel,
                    items!!,
                    onCompleted = { selectedItems.clear() }
                ) {
                    selectedItems.clear()
                }
            }
            else if(searchQuery.isNotBlank()) {
                SearchCounterCompose(
                    index = (searchIndex + 1).toString(),
                    total=searchIndexes.size.toString(),
                    forwardClick = {
                        if(searchIndex + 1 >= searchIndexes.size)
                            searchIndex = 0
                        else searchIndex += 1
                        coroutineScope.launch {
                            listState.animateScrollToItem(searchIndexes[searchIndex])
                        }
                    },
                    backwardClick = {
                        if(searchIndex - 1 < 0)
                            searchIndex = searchIndexes.size - 1
                        else searchIndex -= 1
                        coroutineScope.launch {
                            listState.animateScrollToItem(searchIndexes[searchIndex])
                        }
                    }
                )
            }
            else ChatCompose(
                value=viewModel.text,
                valueChanged = {
                    viewModel.text = it

                    CoroutineScope(Dispatchers.Default).launch {
                        if(it.isEmpty()) viewModel.clearDraft(context)
                        else viewModel.insertDraft(context)
                    }
                }
            ) {
                sendSMS(
                    context=context,
                    text=viewModel.text,
                    threadId= viewModel.threadId,
                    messageId = System.currentTimeMillis().toString(),
                    address= viewModel.address,
                    conversationsViewModel = viewModel
                )
                viewModel.text = ""
                CoroutineScope(Dispatchers.Default).launch {
                    viewModel.clearDraft(context)
                }
                coroutineScope.launch { listState.animateScrollToItem(0) }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true,
            ) {
                itemsIndexed(
                    items = items!!,
                    key = { index, conversation -> conversation.id }
                ) { index, conversation ->
                    var showDate by remember { mutableStateOf(index == 0) }

                    ConversationsCard(
                        text= if(conversation.text.isNullOrBlank()) ""
                        else conversation.text!!,
                        timestamp =
                        if(!conversation.date.isNullOrBlank())
                            Helpers.formatDateExtended(context,
                                conversation.date!!.toLong())
                        else "1730062120",
                        type= conversation.type,
                        status = ConversationStatusTypes.fromInt(conversation.status)!!,
                        position = getContentType(index, conversation, items),
                        date =
                        if(!conversation.date.isNullOrBlank()) deriveMetaDate(conversation)
                        else "1730062120",
                        showDate = showDate,
                        onClickCallback = {
                            if (selectedItems.isNotEmpty()) {
                                if (selectedItems.contains(conversation.message_id))
                                    selectedItems.remove(conversation.message_id)
                                else
                                    selectedItems.add(conversation.message_id!!)
                            }
                            else if(conversation.type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
                                viewModel.retryDeleteItem.add(conversation)
                                showFailedRetryModal = true
                            }
                            else {
                                showDate = !showDate
                            }
                        },
                        onLongClickCallback = {
                            selectedItems.add(conversation.message_id!!)
                        },
                        isSelected = selectedItems.contains(conversation.message_id),
                        isKey = conversation.isIs_key,
                    )

                    val checkIsSecured by remember {
                        derivedStateOf {
                            conversation.isIs_key &&
                                    conversation.type == Telephony.TextBasedSmsColumns
                                        .MESSAGE_TYPE_INBOX
                        }
                    }

                    if(checkIsSecured) {
                        LaunchedEffect(true) {
                            coroutineScope.launch{
                                showSecureAgreeModal = E2EEHandler
                                    .hasPendingApproval(context, viewModel.address)
                            }
                        }
                    }
                }
            }

            val showScrollBottom by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0
                }
            }

            if(showScrollBottom) {
                Button(
                    onClick = {
                        searchQuery = ""
                        searchIndexes.clear()
                        searchIndex = 0

                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape)
                        .size(50.dp)
                    ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.down_to_latest_content),
                        tint= MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        if(showFailedRetryModal) {
            FailedMessageOptionsModal(
                retryCallback = {
                    CoroutineScope(Dispatchers.Default).launch {
                        viewModel.deleteItems(context, viewModel.retryDeleteItem)
                        sendSMS(
                            context=context,
                            text=viewModel.retryDeleteItem.first().text!!,
                            threadId= viewModel.threadId,
                            messageId = System.currentTimeMillis().toString(),
                            address= viewModel.address,
                            conversationsViewModel = viewModel
                        )
                        viewModel.retryDeleteItem = arrayListOf()
                    }
                },
                deleteCallback = {
                    CoroutineScope(Dispatchers.Default).launch {
                        viewModel.deleteItems(context, viewModel.retryDeleteItem)
                        viewModel.retryDeleteItem = arrayListOf()
                    }
                },
            ){
                showFailedRetryModal = false
            }
        }

        if(showSecureRequestModal) {
            SecureRequestAcceptModal(
                viewModel=viewModel,
                isSecureRequest = true,
            ){
                showSecureRequestModal = false
            }
        }

        else if(showSecureAgreeModal) {
            SecureRequestAcceptModal(
                viewModel=viewModel,
                isSecureRequest = false,
            ){
                showSecureAgreeModal = false
                isSecured = E2EEHandler.isSecured(context, viewModel.address)
            }
        }

    }

}

private fun deriveMetaDate(conversation: Conversation): String{
    val dateFormat: DateFormat = SimpleDateFormat("h:mm a");
    return dateFormat.format(Date(conversation.date!!.toLong()));
}

@Preview
@Composable
fun PreviewConversations() {
    AppTheme(darkTheme = true) {
        Surface(Modifier.safeDrawingPadding()) {
            var conversations: MutableList<Conversation> =
                remember { mutableListOf( ) }
            var isSend = false
            val address = "+123456789"
            val threadId = "1"
            for(i in 0..1) {
                val conversation = Conversation()
                conversation.id = i.toLong()
                conversation.text = stringResource(
                    R.string
                        .settings_add_gateway_server_protocol_meta_description)
                conversation.type = if(!isSend) Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX
                else Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                conversations.add(conversation)
                isSend = !isSend
            }
            Conversations(navController = rememberNavController(), _items=conversations)
        }
    }
}
