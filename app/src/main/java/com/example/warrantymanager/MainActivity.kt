package com.example.warrantymanager

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import coil.compose.AsyncImage
import com.example.warrantymanager.data.*
import com.example.warrantymanager.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { WarrantyApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyApp() {
    val context = LocalContext.current
    val dao = remember { DatabaseProvider.get(context).dao() }
    val items by dao.observeItems().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ItemEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("All items") }
    val filtered = items.filter {
        val text = "${it.name} ${it.brand} ${it.model} ${it.category} ${it.serialNumber}".lowercase()
        text.contains(search.lowercase()) && (tab == "All items" || it.category == tab)
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF245B9E), secondary = androidx.compose.ui.graphics.Color(0xFF168477))) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Warranty Manager", fontWeight = FontWeight.Bold) }, actions = {
                TextButton(onClick = { BackupRestore.export(context, dao) }) { Text("Backup") }
            }) },
            floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add item") } }
        ) { padding ->
            Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Items", items.size.toString(), Modifier.weight(1f))
                    SummaryCard("Categories", items.map { it.category }.distinct().size.toString(), Modifier.weight(1f))
                    SummaryCard("Expiring", "See list", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search items, brand, model or serial") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All items", "Electronics", "Appliances", "Vehicles", "Other").forEach { category ->
                        FilterChip(selected = tab == category, onClick = { tab = category }, label = { Text(category) })
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (items.isEmpty()) "No items yet. Tap + to register your first purchase." else "No matching items.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filtered, key = { it.id }) { item ->
                            ItemCard(item, onClick = { selected = item })
                        }
                    }
                }
            }
        }
    }
    if (showAdd) AddItemDialog(onDismiss = { showAdd = false }, onSave = { item, warranty ->
        scope.launch {
            val id = dao.insertItem(item)
            if (warranty.endDate.isNotBlank()) {
                val warrantyId = dao.insertWarranty(warranty.copy(itemId = id))
                scheduleReminders(context, id, item.name, warranty.endDate, warranty.reminderDays)
            }
        }
        showAdd = false
    })
    selected?.let { item ->
        ItemDetailDialog(item, dao, onDismiss = { selected = null })
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ItemCard(item: ItemEntity, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val firstPhoto = item.photoUris.split("|").firstOrNull().orEmpty()
            if (firstPhoto.isNotBlank()) AsyncImage(Uri.parse(firstPhoto), null, Modifier.size(64.dp))
            else Surface(Modifier.size(64.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Box(contentAlignment = Alignment.Center) { Text(item.category.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(listOf(item.brand, item.model).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                Text("${item.category}  •  ${item.purchaseDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(onDismiss: () -> Unit, onSave: (ItemEntity, WarrantyEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var serial by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var price by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var warrantyType by remember { mutableStateOf("Manufacturer warranty") }
    var warrantyEnd by remember { mutableStateOf("") }
    var warrantyProvider by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf("") }
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = uris.joinToString("|") { it.toString() }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Register purchase") }, text = {
        Column(Modifier.heightIn(max = 540.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.foundation.rememberScrollState().let { scroll ->
                Column(Modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Item name *") }, singleLine = true)
                    OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, singleLine = true)
                    OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        OutlinedTextField(category, {}, Modifier.menuAnchor().fillMaxWidth(), readOnly = true, label = { Text("Category") })
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            listOf("Electronics", "Appliances", "Vehicles", "Furniture", "Tools", "Other").forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { category = option; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(serial, { serial = it }, label = { Text("Serial number / IMEI") })
                    OutlinedTextField(purchaseDate, { purchaseDate = it }, label = { Text("Purchase date (YYYY-MM-DD)") })
                    OutlinedTextField(price, { price = it }, label = { Text("Purchase price (LKR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("Store / supplier") })
                    Text("Warranty / guarantee", fontWeight = FontWeight.Bold)
                    OutlinedTextField(warrantyType, { warrantyType = it }, label = { Text("Type") })
                    OutlinedTextField(warrantyProvider, { warrantyProvider = it }, label = { Text("Warranty provider") })
                    OutlinedTextField(warrantyEnd, { warrantyEnd = it }, label = { Text("Expiry date (YYYY-MM-DD)") })
                    OutlinedTextField(terms, { terms = it }, label = { Text("Terms and conditions") })
                    OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text(if (photoUris.isBlank()) "Add receipt / product photos" else "Photos selected: ${photoUris.split("|").size}") }
                }
            }
        }
    }, confirmButton = {
        TextButton(enabled = name.isNotBlank(), onClick = {
            onSave(ItemEntity(name = name.trim(), brand = brand, model = model, category = category, serialNumber = serial, purchaseDate = purchaseDate, purchasePrice = price, supplier = supplier, photoUris = photoUris),
                WarrantyEntity(itemId = 0, type = warrantyType, provider = warrantyProvider, startDate = purchaseDate, endDate = warrantyEnd, terms = terms))
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailDialog(item: ItemEntity, dao: WarrantyDao, onDismiss: () -> Unit) {
    val warranties by dao.observeWarranties(item.id).collectAsStateWithLifecycle(emptyList())
    val claims by dao.observeClaims(item.id).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var claimText by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(item.name) }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${item.brand} ${item.model}".trim(), fontWeight = FontWeight.SemiBold)
            Text("Category: ${item.category}")
            Text("Purchased: ${item.purchaseDate}  •  Price: ${item.currency} ${item.purchasePrice}")
            Text("Serial / IMEI: ${item.serialNumber.ifBlank { "Not recorded" }}")
            Text("Supplier: ${item.supplier.ifBlank { "Not recorded" }}")
            item.photoUris.split("|").filter { it.isNotBlank() }.forEach { uri -> AsyncImage(Uri.parse(uri), null, Modifier.fillMaxWidth().height(130.dp)) }
            Divider()
            Text("Warranty records", fontWeight = FontWeight.Bold)
            if (warranties.isEmpty()) Text("No warranty dates recorded.")
            warranties.forEach { w ->
                Text("${w.type} • ${w.provider}\nExpiry: ${w.endDate.ifBlank { "Not recorded" }}\n${w.terms}")
            }
            Divider()
            Text("Claims / repairs", fontWeight = FontWeight.Bold)
            claims.forEach { Text("${it.date}: ${it.description} — ${it.outcome}") }
            OutlinedTextField(claimText, { claimText = it }, label = { Text("Add claim / repair note") })
            Button(enabled = claimText.isNotBlank(), onClick = {
                scope.launch { dao.insertClaim(ClaimEntity(itemId = item.id, date = LocalDate.now().toString(), description = claimText)); claimText = "" }
            }) { Text("Save claim note") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

fun scheduleReminders(context: android.content.Context, itemId: Long, itemName: String, expiry: String, reminderDays: String) {
    try {
        val date = LocalDate.parse(expiry)
        val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        reminderDays.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { days ->
            if (days >= 0) ReminderScheduler.schedule(context, itemId, itemName, millis, days)
        }
    } catch (_: Exception) { }
}

object BackupRestore {
    fun export(context: android.content.Context, dao: WarrantyDao) {
        // Backup is provided by the dedicated JSON export screen in the next project iteration.
        android.widget.Toast.makeText(context, "Use Android Settings > System > Backup for device backup. File export is planned for the next release.", android.widget.Toast.LENGTH_LONG).show()
    }
}
