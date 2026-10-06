package com.telefam.company.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.telefam.company.app.data.TelefamContactsFilter
import com.telefam.company.app.data.TelefamDirectoryUserEntity
import com.telefam.company.app.ui.TelefamComViewModel
import com.telefam.company.app.ui.components.TelefamComAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComContactsScreen(
    viewModel: TelefamComViewModel,
    onNavigateToProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val users by viewModel.directoryUsers.collectAsState()
    var selectedFilter by remember { mutableStateOf(TelefamContactsFilter.ALL) }
    
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.syncContacts()
    }

    LaunchedEffect(Unit) {
        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    val filteredUsers = when (selectedFilter) {
        TelefamContactsFilter.ALL -> users
        TelefamContactsFilter.FOLLOWERS -> users.filter { it.isFollowedBy }
        TelefamContactsFilter.FOLLOWING -> users.filter { it.isFollowing }
        TelefamContactsFilter.FRIENDS -> users.filter { it.isFriend }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedFilter.ordinal) {
                TelefamContactsFilter.values().forEachIndexed { index, filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter.label) }
                    )
                }
            }

            LazyColumn {
                items(filteredUsers, key = { it.email }) { user ->
                    ListItem(
                        headlineContent = { Text(user.fullName) },
                        supportingContent = { Text("@${user.username}") },
                        leadingContent = { 
                            TelefamComAvatar(photoPath = user.photoPath, name = user.fullName) 
                        },
                        trailingContent = {
                            Button(onClick = { viewModel.toggleFollowUser(user.email, !user.isFollowing) }) {
                                Text(if (user.isFollowing) "Unfollow" else "Follow")
                            }
                        },
                        modifier = Modifier.clickable { onNavigateToProfile(user.email) }
                    )
                }
            }
        }
    }
}
