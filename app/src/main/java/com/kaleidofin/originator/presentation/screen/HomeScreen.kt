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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.kaleidofin.originator.presentation.component.DashboardFlowCard
import kotlinx.coroutines.launch

/**
 * HomeScreen - Backend-driven dashboard
 * 
 * Displays flows from GET /api/v1/dashboard/flows
 * Each flow rendered with:
 * - Custom colors from dashboardMeta.ui
 * - Icon from IconRegistry mapping
 * - Dynamic title/description
 * 
 * On click: Calls Runtime API with currentScreenId = "__START__"
 */
@Composable
fun HomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDynamicForm: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // TODO: Get applicationId from proper source (e.g., SharedPreferences, Auth state)
    val applicationId = remember { "placeholder-application-id" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            } else if (uiState.dashboardFlows.isEmpty()) {
                // Empty state - no flows available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No flows available",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Check back later for available workflows",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Render dashboard flows dynamically
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.dashboardFlows.size) { index ->
                        val flow = uiState.dashboardFlows[index]
                        DashboardFlowCard(
                            flow = flow,
                            onClick = {
                                // Start flow using Runtime API with "__START__" marker
                                viewModel.onFlowClick(
                                    flow = flow,
                                    applicationId = applicationId,
                                    onSuccess = { screenId ->
                                        // Navigate to dynamic form with resolved screenId
                                        onNavigateToDynamicForm(screenId)
                                    },
                                    onError = { error ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(error)
                                        }
                                    }
                                )
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

