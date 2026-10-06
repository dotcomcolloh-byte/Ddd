package com.telefam.company.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telefam.company.app.data.TelefamDirectoryUserEntity
import com.telefam.company.app.ui.TelefamComViewModel
import com.telefam.company.app.ui.components.TelefamComAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComPublicProfileScreen(
    userEmail: String,
    viewModel: TelefamComViewModel,
    onBackClicked: () -> Unit
) {
    val users by viewModel.directoryUsers.collectAsState()
    val user = users.find { it.email == userEmail }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.fullName) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TelefamComAvatar(photoPath = user.photoPath, name = user.fullName, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${user.followersCount} Followers")
                    Text("${user.followingCount} Following")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(user.fullName, style = MaterialTheme.typography.titleLarge)
            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(user.bio)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* TODO: Message */ }, modifier = Modifier.weight(1f)) {
                    Text("Message")
                }
                Button(onClick = { /* TODO: Subscribe */ }, modifier = Modifier.weight(1f)) {
                    Text("Subscribe (Soon)")
                }
            }
        }
    }
}
