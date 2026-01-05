package com.kaleidofin.originator.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaleidofin.originator.presentation.component.PrimaryButton
import com.kaleidofin.originator.presentation.viewmodel.ForgotPasswordViewModel
import com.kaleidofin.originator.ui.theme.SuccessGreen

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ForgotPasswordTopBar(onBackClick = onNavigateBack)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isSuccess) {
                    // Success State
                    SuccessView(onNavigateToLogin = onNavigateToLogin)
                } else {
                    // Form State
                    FormView(
                        uiState = uiState,
                        onUserNameChange = viewModel::updateUserName,
                        onEmailChange = viewModel::updateEmail,
                        onSendClick = viewModel::sendPasswordReset
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordTopBar(onBackClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Text(
                    text = "Forget password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun FormView(
    uiState: com.kaleidofin.originator.presentation.ui.state.ForgotPasswordUiState,
    onUserNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    // Instructional text
    Text(
        text = "Please enter the registered email to receive the Username and Password",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    )

    // User Name field
    OutlinedTextField(
        value = uiState.userName,
        onValueChange = onUserNameChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        label = { Text("User Name") },
        placeholder = { Text("Enter User Name") },
        visualTransformation = VisualTransformation.None,
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = "User icon"
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )

    // Email Address field
    OutlinedTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        label = { Text("Email Address") },
        placeholder = { Text("Enter Email Address") },
        visualTransformation = VisualTransformation.None,
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = "Email icon"
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )

    // Error message
    uiState.error?.let { errorMessage ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Error",
                tint = Color(0xFFB00020),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = errorMessage,
                color = Color(0xFFB00020),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // SEND button
    PrimaryButton(
        text = if (uiState.isLoading) "SENDING" else "SEND",
        onClick = onSendClick,
        enabled = uiState.isFormValid,
        isLoading = uiState.isLoading
    )
}

@Composable
private fun SuccessView(onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = SuccessGreen,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success message
        Text(
            text = "User name password sent to your registered email id",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // LOGIN button
        PrimaryButton(
            text = "LOGIN",
            onClick = onNavigateToLogin,
            enabled = true,
            isLoading = false
        )
    }
}

