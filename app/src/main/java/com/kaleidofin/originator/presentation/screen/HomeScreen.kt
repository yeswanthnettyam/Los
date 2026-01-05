package com.kaleidofin.originator.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaleidofin.originator.presentation.component.ActionCard
import com.kaleidofin.originator.presentation.component.KaleidofinLogo
import com.kaleidofin.originator.presentation.component.PrimaryButton
import com.kaleidofin.originator.presentation.component.SuccessBanner
import com.kaleidofin.originator.presentation.viewmodel.HomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import com.kaleidofin.originator.R

@Composable
fun HomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDynamicForm: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value

    Scaffold(
        topBar = {
            HomeTopBar(onBackClick = onNavigateBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(22.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Loading...")
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = androidx.compose.ui.graphics.Color(0xFFB00020)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = "Retry",
                            onClick = { viewModel.retry() }
                        )
                    }
                }
            } else {
                val layout = uiState.layout
                val columns = layout?.columns ?: 2
                val spacing = layout?.spacingDp?.dp ?: 16.dp
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.actions) { action ->
                        ActionCard(
                            action = action,
                            onClick = {
                                // Navigate based on action type
                                if (action.actionType == "NAVIGATION") {
                                    onNavigateToDynamicForm(action.actionTarget)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onBackClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status bar spacer
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            
            // Toolbar content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = { /* menu */ },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                KaleidofinLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.splash_logo),
                    maxWidthFraction = 0.5f,
                    minHeight = 42.dp,
                    maxHeight = 50.dp
                )
            }
        }
    }
}

