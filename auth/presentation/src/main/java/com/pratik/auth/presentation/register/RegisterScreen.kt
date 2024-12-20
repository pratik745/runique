package com.pratik.auth.presentation.register

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratik.auth.domain.UserDataValidator.Companion.MIN_PASSWORD_LENGTH
import com.pratik.auth.presentation.R
import com.pratik.core.presentation.designsystem.CheckIcon
import com.pratik.core.presentation.designsystem.CrossIcon
import com.pratik.core.presentation.designsystem.EmailIcon
import com.pratik.core.presentation.designsystem.Poppins
import com.pratik.core.presentation.designsystem.RuniqueDarkRed
import com.pratik.core.presentation.designsystem.RuniqueGreen
import com.pratik.core.presentation.designsystem.RuniqueTheme
import com.pratik.core.presentation.designsystem.components.GradientBackground
import com.pratik.core.presentation.designsystem.components.RuniqueActionButton
import com.pratik.core.presentation.designsystem.components.RuniquePasswordTextField
import com.pratik.core.presentation.designsystem.components.RuniqueTextField
import com.pratik.core.presentation.ui.ObserveAsEvent
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel


@Composable
fun RegisterScreenRoot(
    onSignInClick: ()->Unit,
    onSuccessfulRegistration: ()->Unit,
    viewModel: RegisterViewModel = koinViewModel()
){
    ObserveEvents(
        registerEvent = viewModel.events,
        onSuccessfulRegistration = onSuccessfulRegistration
    )
    RegisterScreen(
        state = viewModel.state.collectAsState().value,
        onAction = viewModel::onAction,
        onSignInClick = onSignInClick
    )
}

@Composable
private fun ObserveEvents(
    registerEvent: Flow<RegisterEvent>,
    onSuccessfulRegistration: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    ObserveAsEvent(flow = registerEvent) { event ->
        when (event) {
            is RegisterEvent.Error -> {
                keyboard?.hide()
                Toast.makeText(
                    context,
                    event.error.asString(context),
                    Toast.LENGTH_LONG
                ).show()
            }

            RegisterEvent.RegistrationSuccess -> {
                keyboard?.hide()
                Toast.makeText(
                    context,
                    R.string.registration_successful,
                    Toast.LENGTH_LONG
                ).show()
                onSuccessfulRegistration()
            }
        }
    }
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction)-> Unit,
    onSignInClick: () -> Unit
) {
    GradientBackground {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.create_account),
                style = MaterialTheme.typography.headlineMedium
            )
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontFamily = Poppins,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ){
                    append(stringResource(id = R.string.already_have_an_account) + " ")
                    pushStringAnnotation(
                        tag = "clickable_text",
                        annotation = stringResource(id = R.string.login)
                    )
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = Poppins
                        )
                    ){
                        append(stringResource(id = R.string.login))
                    }
                }
            }

            ClickableText(
                text = annotatedString,
                onClick = { offset ->
                    annotatedString.getStringAnnotations(
                        tag = "clickable_text",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let {
                        onSignInClick()
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))
            RuniqueTextField(
                state = state.email ,
                hint = stringResource(id = R.string.example_email),
                title = stringResource(id = R.string.email),
                additionalInfo = stringResource(id = R.string.must_be_a_valid_email),
                startIcon = EmailIcon,
                endIcon = if(state.isEmailValid) CheckIcon else null,
                keyBoardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            RuniquePasswordTextField(
                state = state.password,
                hint = stringResource(id = R.string.enter_password),
                title = stringResource(id = R.string.password) ,
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = {
                    onAction(RegisterAction.OnTogglePasswordVisibilityClick)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            PasswordRequirement(
                text = stringResource(id = R.string.at_least_x_char,MIN_PASSWORD_LENGTH),
                isValid = state.passwordValidationState.hasMinLength
            )
            Spacer(modifier = Modifier.height(4.dp))
            PasswordRequirement(
                text = stringResource(id = R.string.at_least_one_number),
                isValid = state.passwordValidationState.hasNumber
            )
            Spacer(modifier = Modifier.height(4.dp))
            PasswordRequirement(
                text = stringResource(id = R.string.contains_lower_case_char),
                isValid = state.passwordValidationState.hasLowerCaseCharacter
            )
            Spacer(modifier = Modifier.height(4.dp))
            PasswordRequirement(
                text = stringResource(id = R.string.contains_upper_case_char),
                isValid = state.passwordValidationState.hasUpperCaseCharacter
            )
            Spacer(modifier = Modifier.height(32.dp))
            RuniqueActionButton(
                text = stringResource(id = R.string.register),
                isLoading = state.isRegistering,
                enabled = (state.canRegister && state.isEmailValid),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onAction(RegisterAction.OnRegisterClick)
                }
            )
        }
    }
}

@Composable
fun PasswordRequirement(
    text: String,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isValid) CheckIcon else CrossIcon,
            contentDescription = null,
            tint = if (isValid) RuniqueGreen else RuniqueDarkRed
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    RuniqueTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {},
            onSignInClick = {}
        )
    }
}