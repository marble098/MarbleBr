package com.marble098.marblebr.feature.vault

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.vault.CredentialFields
import com.marble098.marblebr.vault.CredentialVault
import com.marble098.marblebr.vault.SavedCredential
import com.marble098.marblebr.vault.StrongPasswordGenerator
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun CredentialVaultSection(vault: CredentialVault, unlocked: Boolean, onUnlock: () -> Unit) {
    val strings = LocalMarbleStrings.current
    Card(modifier = Modifier.fillMaxWidth().testTag("credential_vault")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.credentialVaultTitle, style = MaterialTheme.typography.titleMedium)
            Text(strings.credentialVaultDescription, style = MaterialTheme.typography.bodySmall)
            if (!unlocked) {
                Text(strings.credentialLockRequired, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onUnlock, modifier = Modifier.testTag("credential_unlock")) {
                    Text(strings.credentialUnlock)
                }
            } else {
                UnlockedCredentials(vault)
            }
        }
    }
}

@Composable
private fun UnlockedCredentials(vault: CredentialVault) {
    val strings = LocalMarbleStrings.current
    val context = LocalContext.current
    val activity = remember(context) { context.activityOrNull() }
    // Do not allow screenshots or recents previews while decrypted credentials are visible.
    DisposableEffect(activity) {
        val window = activity?.window
        val alreadySecure = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val flow = remember(vault) { vault.observeCredentials() }
    val credentials by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SavedCredential?>(null) }
    var showingForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavedCredential?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val filtered = credentials.filter {
        it.fields.site.contains(query, true) || it.fields.username.contains(query, true)
    }

    Button(onClick = { editing = null; showingForm = true }, modifier = Modifier.testTag("credential_add")) {
        Text(strings.credentialAdd)
    }
    if (credentials.isNotEmpty()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(strings.vaultSearchHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text(strings.credentialEmpty, style = MaterialTheme.typography.bodySmall)
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    filtered.forEach { credential ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(credential.fields.site, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                Text(credential.fields.username, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        copySensitive(context, credential.fields.username)
                        message = strings.credentialCopied
                    }) { Text(strings.credentialCopyUser) }
                    TextButton(onClick = {
                        copySensitive(context, credential.fields.password)
                        message = strings.credentialCopied
                    }) { Text(strings.credentialCopyPassword) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editing = credential; showingForm = true }) { Text(strings.credentialEdit) }
                    TextButton(onClick = { pendingDelete = credential }) { Text(strings.vaultDelete) }
                }
            }
        }
    }
    if (showingForm) {
        CredentialForm(
            initial = editing?.fields,
            saving = saving,
            onDismiss = { if (!saving) showingForm = false },
            onSave = { fields ->
                if (!CredentialVault.isValidSite(fields.site) || (fields.username.isBlank() && fields.password.isBlank())) {
                    message = strings.credentialInvalid
                } else {
                    saving = true
                    scope.launch {
                        val result = runCatching { vault.save(fields, editing?.id) }
                        saving = false
                        if (result.isSuccess) {
                            showingForm = false
                            message = null
                        } else {
                            message = strings.credentialFailed
                        }
                    }
                }
            },
        )
    }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.credentialDelete) },
            text = { Text(strings.credentialDeleteBody) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { vault.delete(target.id) }
                }) { Text(strings.vaultDelete) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(strings.cancel) } },
        )
    }
}

@Composable
private fun CredentialForm(
    initial: CredentialFields?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CredentialFields) -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var site by remember { mutableStateOf(initial?.site.orEmpty()) }
    var user by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var revealed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.credentialAdd else strings.credentialEdit) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(site, { site = it }, label = { Text(strings.credentialSite) }, singleLine = true)
                OutlinedTextField(user, { user = it }, label = { Text(strings.credentialUsername) }, singleLine = true)
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text(strings.credentialPassword) },
                    singleLine = true,
                    visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { revealed = !revealed }) {
                            Text(if (revealed) "◉" else "○")
                        }
                    },
                )
                TextButton(onClick = { password = StrongPasswordGenerator.generate(); revealed = true }) {
                    Text(strings.credentialGenerate)
                }
                OutlinedTextField(notes, { notes = it }, label = { Text(strings.credentialNotes) }, maxLines = 4)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = { onSave(CredentialFields(site, user, password, notes)) }) {
                Text(strings.credentialSave)
            }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(strings.cancel) } },
    )
}

private fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

/** Clear only our own clip; never erase a newer item the user copied elsewhere. */
internal fun copySensitive(context: Context, value: String) {
    if (value.isEmpty()) return
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val label = "marblebr-${UUID.randomUUID()}"
    val clip = ClipData.newPlainText(label, value)
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    clipboard.setPrimaryClip(clip)
    Handler(Looper.getMainLooper()).postDelayed({
        runCatching {
            val current = clipboard.primaryClip
            if (current?.description?.label?.toString() == label) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }, 30_000L)
}
