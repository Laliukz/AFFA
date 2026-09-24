package com.lkz.affa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lkz.affa.core.network.extractor.VideoExtractorManager
import com.lkz.affa.core.network.scraper.AnimeAv1Scraper
import com.lkz.affa.core.network.scraper.JkAnimeScraper
import com.lkz.affa.core.network.scraper.TioAnimeScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ponytail: pantalla prueba sin ViewModel/StateFlow, solo verifica scraper+extractores en vivo
@Composable
fun TestExtractorScreen() {
    val tio = remember { TioAnimeScraper() }
    val jk = remember { JkAnimeScraper() }
    val av1 = remember { AnimeAv1Scraper() }
    val manager = remember { VideoExtractorManager() }
    val scope = rememberCoroutineScope()
    var src by remember { mutableIntStateOf(0) } // 0=Tio 1=JK 2=AV1
    var query by remember { mutableStateOf("naruto") }
    var log by remember { mutableStateOf("Listo. Busca y toca un resultado.") }
    var results by remember { mutableStateOf(listOf<com.lkz.affa.domain.model.AnimeSearchResult>()) }
    var loading by remember { mutableStateOf(false) }

    fun run(fn: suspend () -> String) {
        scope.launch {
            loading = true
            log = try { withContext(Dispatchers.IO) { fn() } } catch (e: Exception) { "Error: ${e.message}" }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            FilterChip(src == 0, { src = 0 }, { Text("Tio") })
            Spacer(Modifier.width(8.dp))
            FilterChip(src == 1, { src = 1 }, { Text("JK") })
            Spacer(Modifier.width(8.dp))
            FilterChip(src == 2, { src = 2 }, { Text("AV1") })
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            Button({ run {
                results = when (src) {
                    1 -> jk.search(query)
                    2 -> av1.search(query)
                    else -> tio.search(query)
                }
                "Search: ${results.size} resultados. Toca uno para extraer ep1."
            } }) { Text("Go") }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        Text(
            log,
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(results) { r ->
                ListItem(
                    headlineContent = { Text(r.title) },
                    supportingContent = { Text(r.slug) },
                    modifier = Modifier.clickable { run {
                        val d = when (src) {
                            1 -> jk.getAnimeDetails(r.slug)
                            2 -> av1.getAnimeDetails(r.slug)
                            else -> tio.getAnimeDetails(r.slug)
                        }
                        val ep = d.episodes.firstOrNull() ?: return@run "Sin episodios"
                        val servers = when (src) {
                            1 -> jk.getVideoServers(ep.url)
                            2 -> av1.getVideoServers(ep.url)
                            else -> tio.getVideoServers(ep.url)
                        }
                        if (servers.isEmpty()) return@run "Ep ${ep.title}: 0 servidores"
                        val ok = mutableListOf<Pair<String, String>>()
                        val failed = mutableListOf<String>()
                        for (s in servers) {
                            val tag = if (s.lang.isEmpty()) "-" else s.lang
                            if (!manager.supports(s)) continue // sin extractor: no se muestra
                            try {
                                val v = manager.extract(s)
                                if (v == null) failed.add("[$tag] ${s.name}: sin link")
                                else ok.add(tag to "${s.name} -> ${v.videoUrl}")
                            } catch (e: Exception) {
                                failed.add("[$tag] ${s.name}: ${e.message}")
                            }
                        }
                        if (ok.isEmpty()) return@run "Ep ${ep.title}: 0 reproducibles\nFallidos: ${failed.joinToString(", ").ifEmpty { "-" }}"
                        val total = ok.size
                        "Ep ${ep.title} ($total/${servers.size}):\n" +
                            ok.groupBy({ it.first }, { it.second }).entries.joinToString("\n") { "[${it.key}]\n" + it.value.joinToString("\n") } +
                            (if (failed.isNotEmpty()) "\nFallidos: " + failed.joinToString(", ") else "")
                    } }
                )
                HorizontalDivider()
            }
        }
    }
}
