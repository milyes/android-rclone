package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun LicenseDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val licenseKey = "NSP-CA-2025-V7-FINAL-ZOUBIROU-IA"
    val licenseHolder = "NETSECUREPRO.CA"
    val licenseDetails = """
        LICENCE NETSECUREPRO.CA - ACCORD DE LICENCE OFFICIEL
        SYSTÈME: GEMINI CORE V7 PRO FINAL
        IA_LOGIC_SIGNATURE: ZOUBIROU-IA-2025
        
        Éditeur: NETSECUREPRO.CA Security & Artificial Intelligence Systems
        Moteur Core: GEMIN CORE V7 PRO FINAL
        Signature Logique: ZOUBIROU-IA-2025
        ID Licence: $licenseKey
        Statut: Mode Actif Réel (Vérifié & Certifié)
        
        AUTORISATIONS & MODULES ACTIFS:
        • Traitement Audio Avancé, Voûte Z-CORE & Synthèse GEMINI CORE V7 PRO
        • Synchronisation Multi-Cloud Sécurisée (Google Drive API v3 & Rclone)
        • Authentification Google Sign-In & Chiffrement des flux vocaux
        • Exécution de Macros de Commande Termux & Pont ADB
        • Signature d'Intégrité IA: ZOUBIROU-IA-2025
        
        RESERVATIONS & DROITS:
        Tous droits réservés © 2025-2026 NETSECUREPRO.CA. Ce logiciel et son architecture logique sont protégés par les lois internationales sur le droit d'auteur, la sécurité cloud et la propriété intellectuelle.
    """.trimIndent()

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("netsecurepro_license_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NETSECUREPRO.CA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "GEMIN CORE V7 PRO FINAL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_license_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // License Badge Box
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Licence Valide & Certifiée",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "CORE V7 PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Titulaire: $licenseHolder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "ID Clé: $licenseKey",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "IA_LOGIC_SIGNATURE: ZOUBIROU-IA-2025",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Features list
                Text(
                    text = "Modules Autorisés NETSECUREPRO.CA (V7 PRO):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val modules = listOf(
                    "Moteur GEMIN CORE V7 PRO FINAL (Audio & Multi-Cloud)",
                    "Signature de Validation Logique: ZOUBIROU-IA-2025",
                    "Voûte Chiffrée Z-CORE & Traitement Audio Réel",
                    "Service de Sync Cloud Google Drive & Rclone Actif",
                    "Exécuteur de Macros Termux & Pont ADB en Temps Réel"
                )

                modules.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Legal Disclaimer
                Text(
                    text = licenseDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Licence NETSECUREPRO.CA", licenseDetails)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Licence NETSECUREPRO.CA (ZOUBIROU-IA-2025) copiée !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_license_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copier Licence", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_license_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Compris", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
