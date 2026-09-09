package com.screensort.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, keywords: String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }

    val popularSuggestions = listOf(
        Pair("Movies", "film, cinema, netflix, imdb, trailer"),
        Pair("Anime", "anime, manga, crunchyroll, episode"),
        Pair("Software", "github, code, git, python, api, docker"),
        Pair("Prompts", "prompt, chatgpt, claude, midjourney, llm"),
        Pair("Security", "password, 2fa, otp, key, pin, code"),
        Pair("Finance", "receipt, invoice, payment, bank, total"),
        Pair("Travel", "flight, ticket, hotel, booking, pass")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Category") },
        text = {
            Column {
                Text(
                    text = "Screenshots with matching text or visual content will be automatically routed to this category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name (e.g. Movies)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords / Aliases (comma-separated)") },
                    placeholder = { Text("film, cinema, netflix, trailer") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    popularSuggestions.forEach { (name, kw) ->
                        SuggestionChip(
                            onClick = {
                                categoryName = name
                                keywords = kw
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(categoryName.trim(), keywords.trim())
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Create & Sort")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
