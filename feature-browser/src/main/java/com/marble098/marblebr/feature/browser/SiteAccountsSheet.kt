package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.model.WebAppContainer

@Composable
internal fun SiteAccountsSheet(
    accounts: List<WebAppContainer>,
    activeId: String?,
    onSelect: (WebAppContainer) -> Unit,
    onCreate: (String) -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.accountsTitle, style = MaterialTheme.typography.titleLarge)
        Text(strings.accountsDescription, style = MaterialTheme.typography.bodyMedium)
        accounts.forEach { account ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(account) }
                    .padding(vertical = 10.dp).testTag("site_account_${account.id}"),
            ) {
                Text(account.displayName, modifier = Modifier.weight(1f))
                if (account.id == activeId) Text(strings.accountCurrent, color = MaterialTheme.colorScheme.primary)
                else Text(strings.accountOpen, color = MaterialTheme.colorScheme.primary)
            }
        }
        Button(onClick = { adding = true }, modifier = Modifier.testTag("site_account_add")) {
            Text(strings.accountCreate)
        }
        Text(strings.accountSessionWarning, style = MaterialTheme.typography.bodySmall)
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(strings.accountCreate) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(strings.accountName) },
                    singleLine = true,
                    modifier = Modifier.testTag("site_account_name"),
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim()) }) {
                    Text(strings.accountCreate)
                }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(strings.cancel) } },
        )
    }
}
