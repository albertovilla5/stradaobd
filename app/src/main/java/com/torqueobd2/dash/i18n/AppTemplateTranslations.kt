package com.torqueobd2.dash.i18n

internal class TemplateTranslationRule(
    val enFastCheck: String,
    val esFastCheck: String,
    enRegexPattern: String,
    val enOrder: IntArray,
    esRegexPattern: String,
    val esOrder: IntArray,
    val fr: String,
    val de: String,
    val pt: String,
    val it: String
) {
    val enRegex: Regex by lazy { Regex(enRegexPattern) }
    val esRegex: Regex by lazy { Regex(esRegexPattern) }
}

internal object AppTemplateTranslations {
    private val rules: List<TemplateTranslationRule> by lazy {
        val list = ArrayList<TemplateTranslationRule>(144)
        addChunk0(list)
        addChunk1(list)
        list
    }

    fun translate(language: AppLanguage, es: String, en: String): String? {
        for (rule in rules) {
            if (rule.enFastCheck.isNotEmpty() && !en.contains(rule.enFastCheck)) continue
            if (rule.esFastCheck.isNotEmpty() && !es.contains(rule.esFastCheck)) continue
            val mEn = rule.enRegex.matchEntire(en) ?: continue
            val mEs = rule.esRegex.matchEntire(es) ?: continue
            var out = when (language) {
                AppLanguage.FR -> rule.fr
                AppLanguage.DE -> rule.de
                AppLanguage.PT -> rule.pt
                AppLanguage.IT -> rule.it
                else -> return null
            }
            val captured = arrayOfNulls<String>(4)
            for (i in rule.enOrder.indices) {
                val ph = rule.enOrder[i]
                if (ph in 0..3) captured[ph] = mEn.groupValues[i + 1]
            }
            for (i in rule.esOrder.indices) {
                val ph = rule.esOrder[i]
                if (ph in 0..3 && captured[ph] == null) captured[ph] = mEs.groupValues[i + 1]
            }
            for (ph in 0..3) {
                val value = captured[ph] ?: continue
                out = out.replace("{$ph}", value)
            }
            return out
        }
        return null
    }

    private fun addChunk0(list: MutableList<TemplateTranslationRule>) {
        list.add(
            TemplateTranslationRule(
                enFastCheck = ". Make sure the ELM327 adapter is plugged into the car, its power LED is on, and the ignition is switched on.",
                esFastCheck = ". Verifica que el adaptador ELM327 esté conectado al coche, con el LED de alimentación encendido y la llave en contacto.",
                enRegexPattern = "^Could not connect to ([\\s\\S]*?)\\. Make sure the ELM327 adapter is plugged into the car, its power LED is on, and the ignition is switched on\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo conectar a ([\\s\\S]*?)\\. Verifica que el adaptador ELM327 esté conectado al coche, con el LED de alimentación encendido y la llave en contacto\\.\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de se connecter à {0}. Vérifiez que l'adaptateur ELM327 est branché à la voiture, que son voyant d'alimentation est allumé et que le contact est mis.",
                de = "Verbindung mit {0} fehlgeschlagen. Stelle sicher, dass der ELM327-Adapter im Fahrzeug eingesteckt ist, die Betriebs-LED leuchtet und die Zündung eingeschaltet ist.",
                pt = "Não foi possível conectar a {0}. Verifique se o adaptador ELM327 está ligado ao carro, com o LED de alimentação aceso e a ignição ligada.",
                it = "Impossibile connettersi a {0}. Verifica che l'adattatore ELM327 sia collegato all'auto, con il LED di alimentazione acceso e il quadro inserito."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " gauges without a matching sensor. You can assign them in the editor after importing.",
                esFastCheck = " indicadores sin sensor encontrado. Podrás asignarlos desde el editor después de importar.",
                enRegexPattern = "^([\\s\\S]*?) gauges without a matching sensor\\. You can assign them in the editor after importing\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) indicadores sin sensor encontrado\\. Podrás asignarlos desde el editor después de importar\\.\$",
                esOrder = intArrayOf(0),
                fr = "{0} jauges sans capteur correspondant. Vous pourrez les attribuer dans l'éditeur après l'importation.",
                de = "{0} Anzeigen ohne passenden Sensor. Du kannst sie nach dem Importieren im Editor zuweisen.",
                pt = "{0} indicadores sem sensor correspondente. Poderá atribuí-los no editor após importar.",
                it = "{0} indicatori senza sensore corrispondente. Potrai assegnarli nell'editor dopo l'importazione."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ". Trying fallback method (RFCOMM channel 1)...",
                esFastCheck = ". Probando método fallback (Canal RFCOMM 1)...",
                enRegexPattern = "^⚠️ Standard UUID connection failed: ([\\s\\S]*?)\\. Trying fallback method \\(RFCOMM channel 1\\)\\.\\.\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^⚠️ Falló conexión por UUID estándar: ([\\s\\S]*?)\\. Probando método fallback \\(Canal RFCOMM 1\\)\\.\\.\\.\$",
                esOrder = intArrayOf(0),
                fr = "⚠️ Échec de la connexion par UUID standard : {0}. Essai de la méthode de secours (canal RFCOMM 1)...",
                de = "⚠️ Verbindung über Standard-UUID fehlgeschlagen: {0}. Versuche Fallback-Methode (RFCOMM-Kanal 1)...",
                pt = "⚠️ Falhou a conexão por UUID padrão: {0}. Tentando método alternativo (Canal RFCOMM 1)...",
                it = "⚠️ Connessione tramite UUID standard fallita: {0}. Tentativo con metodo di fallback (Canale RFCOMM 1)..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ". Permanent trouble codes cannot be cleared by this operation.",
                esFastCheck = ". Los códigos permanentes no se borran con esta operación.",
                enRegexPattern = "^Clear confirmed by ([\\s\\S]*?)\\. Permanent trouble codes cannot be cleared by this operation\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Borrado confirmado por ([\\s\\S]*?)\\. Los códigos permanentes no se borran con esta operación\\.\$",
                esOrder = intArrayOf(0),
                fr = "Effacement confirmé par {0}. Les codes défauts permanents ne peuvent pas être effacés par cette opération.",
                de = "Löschen bestätigt durch {0}. Permanente Fehlercodes können mit diesem Vorgang nicht gelöscht werden.",
                pt = "Limpeza confirmada por {0}. Os códigos de falha permanentes não podem ser apagados com esta operação.",
                it = "Cancellazione confermata da {0}. I codici di errore permanenti non possono essere cancellati con questa operazione."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "\" and all its gauges will be deleted. Sensors will remain in your library.",
                esFastCheck = "» con todos sus indicadores. Los sensores seguirán en la biblioteca.",
                enRegexPattern = "^\"([\\s\\S]*?)\" and all its gauges will be deleted\\. Sensors will remain in your library\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Se eliminará «([\\s\\S]*?)» con todos sus indicadores\\. Los sensores seguirán en la biblioteca\\.\$",
                esOrder = intArrayOf(0),
                fr = "« {0} » et toutes ses jauges seront supprimés. Les capteurs resteront dans la bibliothèque.",
                de = "„{0}“ wird mit allen Anzeigen gelöscht. Die Sensoren bleiben in der Bibliothek erhalten.",
                pt = "«{0}» será eliminado com todos os seus indicadores. Os sensores permanecerão na biblioteca.",
                it = "«{0}» verrà eliminato con tutti i suoi indicatori. I sensori rimarranno nella libreria."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ". Read trouble codes again before retrying.",
                esFastCheck = ". Vuelve a leer las averías antes de reintentarlo.",
                enRegexPattern = "^Clear not confirmed\\. ([\\s\\S]*?)\\. Read trouble codes again before retrying\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se ha confirmado el borrado\\. ([\\s\\S]*?)\\. Vuelve a leer las averías antes de reintentarlo\\.\$",
                esOrder = intArrayOf(0),
                fr = "Effacement non confirmé. {0}. Relisez les codes défauts avant de réessayer.",
                de = "Löschen nicht bestätigt. {0}. Lies die Fehlercodes vor einem erneuten Versuch noch einmal aus.",
                pt = "Limpeza não confirmada. {0}. Volte a ler os códigos de falha antes de tentar novamente.",
                it = "Cancellazione non confermata. {0}. Rileggi i codici di errore prima di riprovare."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Waiting for Bluetooth, permissions, or scan completion to connect to ",
                esFastCheck = "Esperando Bluetooth, permisos o fin de búsqueda para conectar a ",
                enRegexPattern = "^Waiting for Bluetooth, permissions, or scan completion to connect to ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Esperando Bluetooth, permisos o fin de búsqueda para conectar a ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "En attente du Bluetooth, des autorisations ou de la fin de la recherche pour se connecter à {0}",
                de = "Warte auf Bluetooth, Berechtigungen oder Abschluss der Suche für Verbindung mit {0}",
                pt = "Aguardando Bluetooth, permissões ou fim da procura para conectar a {0}",
                it = "In attesa di Bluetooth, autorizzazioni o fine scansione per connettersi a {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " empty slots. Will be added as compact tiles.",
                esFastCheck = " ranuras vacías. Se añadirán como tarjetas compactas.",
                enRegexPattern = "^([\\s\\S]*?) selected · ([\\s\\S]*?) empty slots\\. Will be added as compact tiles\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^([\\s\\S]*?) seleccionados · ([\\s\\S]*?) ranuras vacías\\. Se añadirán como tarjetas compactas\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "{0} sélectionnés · {1} emplacements vides. Seront ajoutés sous forme de tuiles compactes.",
                de = "{0} ausgewählt · {1} freie Plätze. Werden als kompakte Kacheln hinzugefügt.",
                pt = "{0} selecionados · {1} posições vazias. Serão adicionados como cartões compactos.",
                it = "{0} selezionati · {1} slot vuoti. Verranno aggiunti come riquadri compatti."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Security error: Bluetooth connection permission denied (",
                esFastCheck = "Error de seguridad: Permiso de conexión Bluetooth denegado (",
                enRegexPattern = "^Security error: Bluetooth connection permission denied \\(([\\s\\S]*?)\\)\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error de seguridad: Permiso de conexión Bluetooth denegado \\(([\\s\\S]*?)\\)\\.\$",
                esOrder = intArrayOf(0),
                fr = "Erreur de sécurité : autorisation de connexion Bluetooth refusée ({0}).",
                de = "Sicherheitsfehler: Bluetooth-Verbindungsberechtigung verweigert ({0}).",
                pt = "Erro de segurança: permissão de conexão Bluetooth negada ({0}).",
                it = "Errore di sicurezza: autorizzazione di connessione Bluetooth negata ({0})."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " dashboard(s) and ",
                esFastCheck = "Copia restaurada: ",
                enRegexPattern = "^Backup restored: ([\\s\\S]*?) vehicle\\(s\\), ([\\s\\S]*?) dashboard\\(s\\) and ([\\s\\S]*?) alert\\(s\\)\\.\$",
                enOrder = intArrayOf(0, 1, 2),
                esRegexPattern = "^Copia restaurada: ([\\s\\S]*?) vehículo\\(s\\), ([\\s\\S]*?) pantalla\\(s\\) y ([\\s\\S]*?) aviso\\(s\\)\\.\$",
                esOrder = intArrayOf(0, 1, 2),
                fr = "Sauvegarde restaurée : {0} véhicule(s), {1} tableau(x) de bord et {2} alerte(s).",
                de = "Backup wiederhergestellt: {0} Fahrzeug(e), {1} Dashboard(s) und {2} Warnung(en).",
                pt = "Cópia restaurada: {0} veículo(s), {1} painel(éis) e {2} alerta(s).",
                it = "Backup ripristinato: {0} veicolo/i, {1} plancia/e e {2} avviso/i."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "No web browser found. Link copied to clipboard: ",
                esFastCheck = "No se encontró navegador web. Enlace copiado al portapapeles: ",
                enRegexPattern = "^No web browser found\\. Link copied to clipboard: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se encontró navegador web\\. Enlace copiado al portapapeles: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Aucun navigateur Web trouvé. Lien copié dans le presse-papiers : {0}",
                de = "Kein Webbrowser gefunden. Link in die Zwischenablage kopiert: {0}",
                pt = "Nenhum navegador web encontrado. Ligação copiada para a área de transferência: {0}",
                it = "Nessun browser web trovato. Link copiato negli appunti: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " sensors supported, ",
                esFastCheck = " sensores soportados, ",
                enRegexPattern = "^Scan complete: ([\\s\\S]*?) sensors supported, ([\\s\\S]*?) unavailable\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Escaneo completado: ([\\s\\S]*?) sensores soportados, ([\\s\\S]*?) no disponibles\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "Analyse terminée : {0} capteurs pris en charge, {1} indisponibles.",
                de = "Scan abgeschlossen: {0} Sensoren unterstützt, {1} nicht verfügbar.",
                pt = "Verificação concluída: {0} sensores suportados, {1} indisponíveis.",
                it = "Scansione completata: {0} sensori supportati, {1} non disponibili."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not restore adapter after diagnostics: ",
                esFastCheck = "No se pudo restaurar el adaptador tras el diagnóstico: ",
                enRegexPattern = "^Could not restore adapter after diagnostics: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo restaurar el adaptador tras el diagnóstico: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de restaurer l'adaptateur après le diagnostic : {0}",
                de = "Adapter konnte nach der Diagnose nicht wiederhergestellt werden: {0}",
                pt = "Não foi possível restaurar o adaptador após o diagnóstico: {0}",
                it = "Impossibile ripristinare l'adattatore dopo la diagnostica: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Do you want to permanently delete '",
                esFastCheck = "¿Deseas eliminar permanentemente '",
                enRegexPattern = "^Do you want to permanently delete '([\\s\\S]*?)' from the garage\\?\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^¿Deseas eliminar permanentemente '([\\s\\S]*?)' del garaje\\?\$",
                esOrder = intArrayOf(0),
                fr = "Voulez-vous supprimer définitivement '{0}' du garage ?",
                de = "Möchtest du '{0}' dauerhaft aus der Garage löschen?",
                pt = "Deseja eliminar permanentemente '{0}' da garagem?",
                it = "Vuoi eliminare definitivamente '{0}' dal garage?"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Android denied Bluetooth connection permission: ",
                esFastCheck = "Android denegó el permiso de conexión Bluetooth: ",
                enRegexPattern = "^Android denied Bluetooth connection permission: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Android denegó el permiso de conexión Bluetooth: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Android a refusé l'autorisation de connexion Bluetooth : {0}",
                de = "Android hat die Bluetooth-Verbindungsberechtigung verweigert: {0}",
                pt = "O Android negou a permissão de conexão Bluetooth: {0}",
                it = "Android ha negato l'autorizzazione di connessione Bluetooth: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Successfully imported ",
                esFastCheck = "Se importaron exitosamente ",
                enRegexPattern = "^Successfully imported ([\\s\\S]*?) PIDs from CSV file\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Se importaron exitosamente ([\\s\\S]*?) PIDs desde el archivo CSV\\.\$",
                esOrder = intArrayOf(0),
                fr = "{0} PIDs importés avec succès depuis le fichier CSV.",
                de = "{0} PIDs erfolgreich aus der CSV-Datei importiert.",
                pt = "Foram importados com sucesso {0} PIDs a partir do ficheiro CSV.",
                it = "Importati correttamente {0} PID dal file CSV."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "MIL indicator and monitors since last clear: ",
                esFastCheck = "Testigo MIL y monitores desde el último borrado: ",
                enRegexPattern = "^MIL indicator and monitors since last clear: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Testigo MIL y monitores desde el último borrado: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Témoin MIL et moniteurs depuis le dernier effacement : {0}",
                de = "MIL-Warnleuchte und Monitore seit dem letzten Löschen: {0}",
                pt = "Luz MIL e monitores desde a última limpeza: {0}",
                it = "Spia MIL e monitor dall'ultima cancellazione: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "No complete response from adapter to ",
                esFastCheck = "Sin respuesta completa del adaptador a ",
                enRegexPattern = "^No complete response from adapter to ([\\s\\S]*?) within ([\\s\\S]*?)ms\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Sin respuesta completa del adaptador a ([\\s\\S]*?) en ([\\s\\S]*?)ms\$",
                esOrder = intArrayOf(0, 1),
                fr = "Aucune réponse complète de l'adaptateur à {0} en {1} ms",
                de = "Keine vollständige Antwort vom Adapter auf {0} innerhalb von {1}ms",
                pt = "Sem resposta completa do adaptador a {0} em {1}ms",
                it = "Nessuna risposta completa dall'adattatore a {0} entro {1}ms"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": unrelated or incomplete command response",
                esFastCheck = ": respuesta ajena al comando o incompleta",
                enRegexPattern = "^ECU ([\\s\\S]*?): unrelated or incomplete command response\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): respuesta ajena al comando o incompleta\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : réponse à la commande non liée ou incomplète",
                de = "ECU {0}: nicht zugehörige oder unvollständige Befehlsantwort",
                pt = "ECU {0}: resposta ao comando não relacionada ou incompleta",
                it = "ECU {0}: risposta al comando non correlata o incompleta"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "; next attempt in ",
                esFastCheck = "; nuevo intento en ",
                enRegexPattern = "^Reconnecting to ([\\s\\S]*?): attempt ([\\s\\S]*?); next attempt in ([\\s\\S]*?)s\\.\$",
                enOrder = intArrayOf(0, 1, 2),
                esRegexPattern = "^Reconexión a ([\\s\\S]*?): intento ([\\s\\S]*?); nuevo intento en ([\\s\\S]*?)s\\.\$",
                esOrder = intArrayOf(0, 1, 2),
                fr = "Reconnexion à {0} : tentative {1} ; prochaine tentative dans {2} s.",
                de = "Wiederverbindung mit {0}: Versuch {1}; nächster Versuch in {2}s.",
                pt = "Reconexão a {0}: tentativa {1}; nova tentativa em {2}s.",
                it = "Riconnessione a {0}: tentativo {1}; nuovo tentativo tra {2}s."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": incomplete or out-of-order CAN sequence",
                esFastCheck = ": secuencia CAN incompleta o desordenada",
                enRegexPattern = "^ECU ([\\s\\S]*?): incomplete or out-of-order CAN sequence\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): secuencia CAN incompleta o desordenada\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : séquence CAN incomplète ou désordonnée",
                de = "ECU {0}: unvollständige oder ungeordnete CAN-Sequenz",
                pt = "ECU {0}: sequência CAN incompleta ou desordenada",
                it = "ECU {0}: sequenza CAN incompleta o fuori ordine"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Unsupported backup schema version (v",
                esFastCheck = "Versión de copia de seguridad no compatible (v",
                enRegexPattern = "^Unsupported backup schema version \\(v([\\s\\S]*?)\\)\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Versión de copia de seguridad no compatible \\(v([\\s\\S]*?)\\)\\.\$",
                esOrder = intArrayOf(0),
                fr = "Version de sauvegarde non prise en charge (v{0}).",
                de = "Nicht unterstützte Backup-Version (v{0}).",
                pt = "Versão de cópia de segurança não compatível (v{0}).",
                it = "Versione del backup non supportata (v{0})."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "📋 Paired Bluetooth devices detected: ",
                esFastCheck = "📋 Dispositivos Bluetooth emparejados detectados: ",
                enRegexPattern = "^📋 Paired Bluetooth devices detected: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^📋 Dispositivos Bluetooth emparejados detectados: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "📋 Appareils Bluetooth associés détectés : {0}",
                de = "📋 Gekoppelte Bluetooth-Geräte erkannt: {0}",
                pt = "📋 Dispositivos Bluetooth emparelhados detetados: {0}",
                it = "📋 Dispositivi Bluetooth associati rilevati: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Permission error reading bondedDevices: ",
                esFastCheck = "❌ Error de permisos al leer bondedDevices: ",
                enRegexPattern = "^❌ Permission error reading bondedDevices: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Error de permisos al leer bondedDevices: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ Erreur d'autorisation lors de la lecture de bondedDevices : {0}",
                de = "❌ Berechtigungsfehler beim Lesen von bondedDevices: {0}",
                pt = "❌ Erro de permissões ao ler bondedDevices: {0}",
                it = "❌ Errore di autorizzazione durante la lettura di bondedDevices: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " active · Available = recent ECU reading",
                esFastCheck = " activos · Disponible = lectura ECU reciente",
                enRegexPattern = "^([\\s\\S]*?) active · Available = recent ECU reading\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) activos · Disponible = lectura ECU reciente\$",
                esOrder = intArrayOf(0),
                fr = "{0} actifs · Disponible = lecture ECU récente",
                de = "{0} aktiv · Verfügbar = aktueller ECU-Messwert",
                pt = "{0} ativos · Disponível = leitura recente da ECU",
                it = "{0} attivi · Disponibile = lettura ECU recente"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not start background reading: ",
                esFastCheck = "No se pudo iniciar la lectura en segundo plano: ",
                enRegexPattern = "^Could not start background reading: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo iniciar la lectura en segundo plano: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de démarrer la lecture en arrière-plan : {0}",
                de = "Hintergrundmessung konnte nicht gestartet werden: {0}",
                pt = "Não foi possível iniciar a leitura em segundo plano: {0}",
                it = "Impossibile avviare la lettura in background: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Contact email copied to clipboard: ",
                esFastCheck = "Correo de contacto copiado al portapapeles: ",
                enRegexPattern = "^Contact email copied to clipboard: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Correo de contacto copiado al portapapeles: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "E-mail de contact copié dans le presse-papiers : {0}",
                de = "Kontakt-E-Mail in die Zwischenablage kopiert: {0}",
                pt = "E-mail de contacto copiado para a área de transferência: {0}",
                it = "Email di contatto copiata negli appunti: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "🚀 Full active connection established with ",
                esFastCheck = "🚀 Conexión completa y activa con ",
                enRegexPattern = "^🚀 Full active connection established with ([\\s\\S]*?)\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^🚀 Conexión completa y activa con ([\\s\\S]*?)\\.\$",
                esOrder = intArrayOf(0),
                fr = "🚀 Connexion complète et active établie avec {0}.",
                de = "🚀 Vollständige und aktive Verbindung mit {0} hergestellt.",
                pt = "🚀 Conexão completa e ativa estabelecida com {0}.",
                it = "🚀 Connessione completa e attiva stabilita con {0}."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " confirmed DTCs reported",
                esFastCheck = " DTC confirmados declarados",
                enRegexPattern = "^ECU ([\\s\\S]*?): MIL ([\\s\\S]*?); ([\\s\\S]*?) confirmed DTCs reported\$",
                enOrder = intArrayOf(0, 1, 2),
                esRegexPattern = "^ECU ([\\s\\S]*?): MIL ([\\s\\S]*?); ([\\s\\S]*?) DTC confirmados declarados\$",
                esOrder = intArrayOf(0, 1, 2),
                fr = "ECU {0} : MIL {1} ; {2} DTC confirmés signalés",
                de = "ECU {0}: MIL {1}; {2} bestätigte DTCs gemeldet",
                pt = "ECU {0}: MIL {1}; {2} DTCs confirmados declarados",
                it = "ECU {0}: MIL {1}; {2} DTC confermati segnalati"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid freeze frame code",
                esFastCheck = ": código del cuadro congelado inválido",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid freeze frame code\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): código del cuadro congelado inválido\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : code de trame figée invalide",
                de = "ECU {0}: ungültiger Freeze-Frame-Code",
                pt = "ECU {0}: código de quadro congelado inválido",
                it = "ECU {0}: codice freeze frame non valido"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid DTC count or length",
                esFastCheck = ": número de DTC o longitud inválidos",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid DTC count or length\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): número de DTC o longitud inválidos\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : nombre ou longueur de DTC invalide",
                de = "ECU {0}: ungültige DTC-Anzahl oder -Länge",
                pt = "ECU {0}: número ou comprimento de DTC inválido",
                it = "ECU {0}: conteggio o lunghezza DTC non validi"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": truncated multi-frame response",
                esFastCheck = ": respuesta multitrama truncada",
                enRegexPattern = "^ECU ([\\s\\S]*?): truncated multi-frame response\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): respuesta multitrama truncada\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : réponse multitrame tronquée",
                de = "ECU {0}: abgeschnittene Multi-Frame-Antwort",
                pt = "ECU {0}: resposta multitrama truncada",
                it = "ECU {0}: risposta multi-frame troncata"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid freeze frame data for ",
                esFastCheck = ": dato congelado inválido para ",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid freeze frame data for ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^ECU ([\\s\\S]*?): dato congelado inválido para ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "ECU {0} : données figées invalides pour {1}",
                de = "ECU {0}: ungültige Freeze-Frame-Daten für {1}",
                pt = "ECU {0}: dados congelados inválidos para {1}",
                it = "ECU {0}: dati freeze frame non validi per {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Exception searching for devices: ",
                esFastCheck = "❌ Excepción al buscar dispositivos: ",
                enRegexPattern = "^❌ Exception searching for devices: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Excepción al buscar dispositivos: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ Exception lors de la recherche d'appareils : {0}",
                de = "❌ Ausnahme bei der Gerätesuche: {0}",
                pt = "❌ Exceção ao procurar dispositivos: {0}",
                it = "❌ Eccezione durante la ricerca dei dispositivi: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "< Sensors supported by ECU: ",
                esFastCheck = "< Sensores soportados por la ECU: ",
                enRegexPattern = "^< Sensors supported by ECU: ([\\s\\S]*?) of ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^< Sensores soportados por la ECU: ([\\s\\S]*?) de ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "< Capteurs pris en charge par l'ECU : {0} sur {1}",
                de = "< Von der ECU unterstützte Sensoren: {0} von {1}",
                pt = "< Sensores suportados pela ECU: {0} de {1}",
                it = "< Sensori supportati dalla ECU: {0} di {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": unrecognized CAN frame type",
                esFastCheck = ": tipo de trama CAN no reconocido",
                enRegexPattern = "^ECU ([\\s\\S]*?): unrecognized CAN frame type\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): tipo de trama CAN no reconocido\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : type de trame CAN non reconnu",
                de = "ECU {0}: unbekannter CAN-Frame-Typ",
                pt = "ECU {0}: tipo de trama CAN não reconhecido",
                it = "ECU {0}: tipo di frame CAN non riconosciuto"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "No codes in responding ECUs: ",
                esFastCheck = "Sin códigos en las ECU que respondieron: ",
                enRegexPattern = "^No codes in responding ECUs: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Sin códigos en las ECU que respondieron: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Aucun code dans les ECU ayant répondu : {0}",
                de = "Keine Codes in antwortenden ECUs: {0}",
                pt = "Sem códigos nas ECUs que responderam: {0}",
                it = "Nessun codice nelle ECU che hanno risposto: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Enter a value greater than 0 in ",
                esFastCheck = "Introduce un valor mayor que 0 en ",
                enRegexPattern = "^Enter a value greater than 0 in ([\\s\\S]*?)\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Introduce un valor mayor que 0 en ([\\s\\S]*?)\\.\$",
                esOrder = intArrayOf(0),
                fr = "Entrez une valeur supérieure à 0 dans {0}.",
                de = "Gib in {0} einen Wert größer als 0 ein.",
                pt = "Introduza um valor superior a 0 em {0}.",
                it = "Inserisci un valore maggiore di 0 in {0}."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Invalid Bluetooth MAC address (",
                esFastCheck = "Dirección MAC Bluetooth inválida (",
                enRegexPattern = "^Invalid Bluetooth MAC address \\(([\\s\\S]*?)\\)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Dirección MAC Bluetooth inválida \\(([\\s\\S]*?)\\)\$",
                esOrder = intArrayOf(0),
                fr = "Adresse MAC Bluetooth invalide ({0})",
                de = "Ungültige Bluetooth-MAC-Adresse ({0})",
                pt = "Endereço MAC Bluetooth inválido ({0})",
                it = "Indirizzo MAC Bluetooth non valido ({0})"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Privacy Policy URL (HTTPS): ",
                esFastCheck = "URL de Política de Privacidad (HTTPS): ",
                enRegexPattern = "^Privacy Policy URL \\(HTTPS\\): ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^URL de Política de Privacidad \\(HTTPS\\): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "URL de la politique de confidentialité (HTTPS) : {0}",
                de = "Datenschutzrichtlinien-URL (HTTPS): {0}",
                pt = "URL da Política de Privacidade (HTTPS): {0}",
                it = "URL Informativa sulla Privacy (HTTPS): {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Fallback method also failed: ",
                esFastCheck = "❌ Falló también el método fallback: ",
                enRegexPattern = "^❌ Fallback method also failed: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Falló también el método fallback: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ La méthode de secours a également échoué : {0}",
                de = "❌ Fallback-Methode ebenfalls fehlgeschlagen: {0}",
                pt = "❌ O método alternativo também falhou: {0}",
                it = "❌ Anche il metodo di fallback è fallito: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "🔌 Looking up paired device ",
                esFastCheck = "🔌 Buscando dispositivo emparejado ",
                enRegexPattern = "^🔌 Looking up paired device ([\\s\\S]*?)\\.\\.\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^🔌 Buscando dispositivo emparejado ([\\s\\S]*?)\\.\\.\\.\$",
                esOrder = intArrayOf(0),
                fr = "🔌 Recherche de l'appareil associé {0}...",
                de = "🔌 Suche gekoppeltes Gerät {0}...",
                pt = "🔌 Procurando dispositivo emparelhado {0}...",
                it = "🔌 Ricerca del dispositivo associato {0}..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid multi-frame length",
                esFastCheck = ": longitud multitrama inválida",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid multi-frame length\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): longitud multitrama inválida\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : longueur multitrame invalide",
                de = "ECU {0}: ungültige Multi-Frame-Länge",
                pt = "ECU {0}: comprimento multitrama inválido",
                it = "ECU {0}: lunghezza multi-frame non valida"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Unrecognized header or format: ",
                esFastCheck = "Cabecera o formato no reconocido: ",
                enRegexPattern = "^Unrecognized header or format: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Cabecera o formato no reconocido: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "En-tête ou format non reconnu : {0}",
                de = "Unbekannter Header oder unbekanntes Format: {0}",
                pt = "Cabeçalho ou formato não reconhecido: {0}",
                it = "Intestazione o formato non riconosciuto: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not start OBD service: ",
                esFastCheck = "No se pudo iniciar el servicio OBD: ",
                enRegexPattern = "^Could not start OBD service: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo iniciar el servicio OBD: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de démarrer le service OBD : {0}",
                de = "OBD-Dienst konnte nicht gestartet werden: {0}",
                pt = "Não foi possível iniciar o serviço OBD: {0}",
                it = "Impossibile avviare il servizio OBD: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "' saved successfully.",
                esFastCheck = "' guardado correctamente.",
                enRegexPattern = "^Vehicle '([\\s\\S]*?)' saved successfully\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Vehículo '([\\s\\S]*?)' guardado correctamente\\.\$",
                esOrder = intArrayOf(0),
                fr = "Véhicule '{0}' enregistré avec succès.",
                de = "Fahrzeug '{0}' erfolgreich gespeichert.",
                pt = "Veículo '{0}' guardado com sucesso.",
                it = "Veicolo '{0}' salvato correttamente."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Init commands detected in CSV: ",
                esFastCheck = "Comandos Init detectados en CSV: ",
                enRegexPattern = "^Init commands detected in CSV: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Comandos Init detectados en CSV: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Commandes d'initialisation détectées dans le CSV : {0}",
                de = "Init-Befehle in CSV erkannt: {0}",
                pt = "Comandos de inicialização detetados no CSV: {0}",
                it = "Comandi di inizializzazione rilevati nel CSV: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Lost connection to ELM327: ",
                esFastCheck = "❌ Pérdida de conexión con ELM327: ",
                enRegexPattern = "^❌ Lost connection to ELM327: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Pérdida de conexión con ELM327: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ Perte de connexion avec l'ELM327 : {0}",
                de = "❌ Verbindung zum ELM327 verloren: {0}",
                pt = "❌ Perda de conexão com o ELM327: {0}",
                it = "❌ Connessione con ELM327 persa: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Successfully imported ",
                esFastCheck = " PIDs correctamente.",
                enRegexPattern = "^Successfully imported ([\\s\\S]*?) PIDs\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Se importaron ([\\s\\S]*?) PIDs correctamente\\.\$",
                esOrder = intArrayOf(0),
                fr = "{0} PIDs importés avec succès.",
                de = "{0} PIDs erfolgreich importiert.",
                pt = "Foram importados {0} PIDs com sucesso.",
                it = "Importati correttamente {0} PID."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": previous message truncated",
                esFastCheck = ": mensaje previo truncado",
                enRegexPattern = "^ECU ([\\s\\S]*?): previous message truncated\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): mensaje previo truncado\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : message précédent tronqué",
                de = "ECU {0}: vorherige Nachricht abgeschnitten",
                pt = "ECU {0}: mensagem anterior truncada",
                it = "ECU {0}: messaggio precedente troncato"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": no freeze frame data for ",
                esFastCheck = ": sin dato congelado para ",
                enRegexPattern = "^ECU ([\\s\\S]*?): no freeze frame data for ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^ECU ([\\s\\S]*?): sin dato congelado para ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "ECU {0} : aucune donnée figée pour {1}",
                de = "ECU {0}: keine Freeze-Frame-Daten für {1}",
                pt = "ECU {0}: sem dados congelados para {1}",
                it = "ECU {0}: nessun dato freeze frame per {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Adapter rejected command ",
                esFastCheck = "El adaptador rechazó el comando ",
                enRegexPattern = "^Adapter rejected command ([\\s\\S]*?): ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^El adaptador rechazó el comando ([\\s\\S]*?): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "L'adaptateur a rejeté la commande {0} : {1}",
                de = "Adapter hat Befehl {0} abgelehnt: {1}",
                pt = "O adaptador rejeitou o comando {0}: {1}",
                it = "L'adattatore ha rifiutato il comando {0}: {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Error processing CSV file: ",
                esFastCheck = "Error al procesar el archivo CSV: ",
                enRegexPattern = "^Error processing CSV file: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error al procesar el archivo CSV: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur lors du traitement du fichier CSV : {0}",
                de = "Fehler beim Verarbeiten der CSV-Datei: {0}",
                pt = "Erro ao processar o ficheiro CSV: {0}",
                it = "Errore durante l'elaborazione del file CSV: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": inconsistent DTC count",
                esFastCheck = ": contador DTC inconsistente",
                enRegexPattern = "^ECU ([\\s\\S]*?): inconsistent DTC count\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): contador DTC inconsistente\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : nombre de DTC incohérent",
                de = "ECU {0}: inkonsistente DTC-Anzahl",
                pt = "ECU {0}: contagem de DTC inconsistente",
                it = "ECU {0}: conteggio DTC incoerente"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not scan for devices: ",
                esFastCheck = "No se pudo buscar dispositivos: ",
                enRegexPattern = "^Could not scan for devices: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo buscar dispositivos: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de rechercher des appareils : {0}",
                de = "Gerätesuche fehlgeschlagen: {0}",
                pt = "Não foi possível procurar dispositivos: {0}",
                it = "Impossibile cercare dispositivi: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Follows device language (",
                esFastCheck = "Sigue el idioma del dispositivo (",
                enRegexPattern = "^Follows device language \\(([\\s\\S]*?)\\)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Sigue el idioma del dispositivo \\(([\\s\\S]*?)\\)\$",
                esOrder = intArrayOf(0),
                fr = "Suit la langue de l'appareil ({0})",
                de = "Folgt der Gerätesprache ({0})",
                pt = "Segue o idioma do dispositivo ({0})",
                it = "Segue la lingua del dispositivo ({0})"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Uses your device language (",
                esFastCheck = "Usa el idioma del dispositivo (",
                enRegexPattern = "^Uses your device language \\(([\\s\\S]*?)\\)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Usa el idioma del dispositivo \\(([\\s\\S]*?)\\)\$",
                esOrder = intArrayOf(0),
                fr = "Utilise la langue de l'appareil ({0})",
                de = "Verwendet die Gerätesprache ({0})",
                pt = "Usa o idioma do dispositivo ({0})",
                it = "Usa la lingua del dispositivo ({0})"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Active vehicle switched to '",
                esFastCheck = "Vehículo activo cambiado a '",
                enRegexPattern = "^Active vehicle switched to '([\\s\\S]*?)'\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Vehículo activo cambiado a '([\\s\\S]*?)'\\.\$",
                esOrder = intArrayOf(0),
                fr = "Véhicule actif changé pour '{0}'.",
                de = "Aktives Fahrzeug gewechselt zu '{0}'.",
                pt = "Veículo ativo alterado para '{0}'.",
                it = "Veicolo attivo cambiato in '{0}'."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Unexpected connection error: ",
                esFastCheck = "Error inesperado al conectar: ",
                enRegexPattern = "^Unexpected connection error: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error inesperado al conectar: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur de connexion inattendue : {0}",
                de = "Unerwarteter Verbindungsfehler: {0}",
                pt = "Erro inesperado ao conectar: {0}",
                it = "Errore di connessione imprevisto: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not save recording: ",
                esFastCheck = "No se pudo guardar la grabación: ",
                enRegexPattern = "^Could not save recording: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo guardar la grabación: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible d'enregistrer l'acquisition : {0}",
                de = "Aufzeichnung konnte nicht gespeichert werden: {0}",
                pt = "Não foi possível guardar a gravação: {0}",
                it = "Impossibile salvare la registrazione: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": truncated first frame",
                esFastCheck = ": primer segmento truncado",
                enRegexPattern = "^ECU ([\\s\\S]*?): truncated first frame\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): primer segmento truncado\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : première trame tronquée",
                de = "ECU {0}: abgeschnittener erster Frame",
                pt = "ECU {0}: primeiro segmento truncado",
                it = "ECU {0}: primo frame troncato"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Error querying devices: ",
                esFastCheck = "Error al consultar dispositivos: ",
                enRegexPattern = "^Error querying devices: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error al consultar dispositivos: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur lors de la recherche d'appareils : {0}",
                de = "Fehler beim Abfragen der Geräte: {0}",
                pt = "Erro ao consultar dispositivos: {0}",
                it = "Errore durante l'interrogazione dei dispositivi: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not delete session: ",
                esFastCheck = "No se pudo eliminar la sesión: ",
                enRegexPattern = "^Could not delete session: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo eliminar la sesión: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de supprimer la session : {0}",
                de = "Sitzung konnte nicht gelöscht werden: {0}",
                pt = "Não foi possível eliminar a sessão: {0}",
                it = "Impossibile eliminare la sessione: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": repeated first frame",
                esFastCheck = ": primer segmento repetido",
                enRegexPattern = "^ECU ([\\s\\S]*?): repeated first frame\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): primer segmento repetido\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : première trame répétée",
                de = "ECU {0}: wiederholter erster Frame",
                pt = "ECU {0}: primeiro segmento repetido",
                it = "ECU {0}: primo frame ripetuto"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connecting to ",
                esFastCheck = "Conectando a ",
                enRegexPattern = "^Connecting to ([\\s\\S]*?) \\(attempt ([\\s\\S]*?)\\)\\.\\.\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Conectando a ([\\s\\S]*?) \\(intento ([\\s\\S]*?)\\)\\.\\.\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "Connexion à {0} (tentative {1})...",
                de = "Verbinde mit {0} (Versuch {1})...",
                pt = "Conectando a {0} (tentativa {1})...",
                it = "Connessione a {0} (tentativo {1})..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not export backup: ",
                esFastCheck = "No se pudo exportar la copia: ",
                enRegexPattern = "^Could not export backup: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo exportar la copia: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible d'exporter la sauvegarde : {0}",
                de = "Backup konnte nicht exportiert werden: {0}",
                pt = "Não foi possível exportar a cópia de segurança: {0}",
                it = "Impossibile esportare il backup: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": truncated CAN segment",
                esFastCheck = ": segmento CAN truncado",
                enRegexPattern = "^ECU ([\\s\\S]*?): truncated CAN segment\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): segmento CAN truncado\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : segment CAN tronqué",
                de = "ECU {0}: abgeschnittenes CAN-Segment",
                pt = "ECU {0}: segmento CAN truncado",
                it = "ECU {0}: segmento CAN troncato"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": incomplete DTC pairs",
                esFastCheck = ": pares DTC incompletos",
                enRegexPattern = "^ECU ([\\s\\S]*?): incomplete DTC pairs\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): pares DTC incompletos\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : paires de DTC incomplètes",
                de = "ECU {0}: unvollständige DTC-Paare",
                pt = "ECU {0}: pares de DTC incompletos",
                it = "ECU {0}: coppie DTC incomplete"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Failed to restore backup: ",
                esFastCheck = "Error al restaurar copia: ",
                enRegexPattern = "^Failed to restore backup: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error al restaurar copia: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Échec de la restauration de la sauvegarde : {0}",
                de = "Fehler beim Wiederherstellen des Backups: {0}",
                pt = "Erro ao restaurar cópia de segurança: {0}",
                it = "Impossibile ripristinare il backup: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Loaded pack '",
                esFastCheck = "Cargado conjunto '",
                enRegexPattern = "^Loaded pack '([\\s\\S]*?)' \\(([\\s\\S]*?) PIDs\\)\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Cargado conjunto '([\\s\\S]*?)' \\(([\\s\\S]*?) PIDs\\)\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "Pack '{0}' chargé ({1} PIDs).",
                de = "Paket '{0}' geladen ({1} PIDs).",
                pt = "Conjunto '{0}' carregado ({1} PIDs).",
                it = "Pacchetto '{0}' caricato ({1} PID)."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Freeze frame · frame ",
                esFastCheck = "Datos congelados · cuadro ",
                enRegexPattern = "^Freeze frame · frame ([\\s\\S]*?): ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Datos congelados · cuadro ([\\s\\S]*?): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Données figées · trame {0} : {1}",
                de = "Freeze Frame · Rahmen {0}: {1}",
                pt = "Dados congelados · quadro {0}: {1}",
                it = "Freeze frame · fotogramma {0}: {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " · Contact: ",
                esFastCheck = "Desarrollador: ",
                enRegexPattern = "^Developer: ([\\s\\S]*?) · Contact: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Desarrollador: ([\\s\\S]*?) · Contacto: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Développeur : {0} · Contact : {1}",
                de = "Entwickler: {0} · Kontakt: {1}",
                pt = "Programador: {0} · Contacto: {1}",
                it = "Sviluppatore: {0} · Contatto: {1}"
            )
        )
    }

    private fun addChunk1(list: MutableList<TemplateTranslationRule>) {
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid CAN length",
                esFastCheck = ": longitud CAN inválida",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid CAN length\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): longitud CAN inválida\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : longueur CAN invalide",
                de = "ECU {0}: ungültige CAN-Länge",
                pt = "ECU {0}: comprimento CAN inválido",
                it = "ECU {0}: lunghezza CAN non valida"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid KWP length",
                esFastCheck = ": longitud KWP inválida",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid KWP length\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): longitud KWP inválida\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : longueur KWP invalide",
                de = "ECU {0}: ungültige KWP-Länge",
                pt = "ECU {0}: comprimento KWP inválido",
                it = "ECU {0}: lunghezza KWP non valida"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " codes in last query",
                esFastCheck = " códigos en la última consulta",
                enRegexPattern = "^([\\s\\S]*?) codes in last query\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) códigos en la última consulta\$",
                esOrder = intArrayOf(0),
                fr = "{0} codes lors de la dernière requête",
                de = "{0} Codes bei der letzten Abfrage",
                pt = "{0} códigos na última consulta",
                it = "{0} codici nell'ultima interrogazione"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Selected device: ",
                esFastCheck = "Dispositivo seleccionado: ",
                enRegexPattern = "^Selected device: ([\\s\\S]*?) \\(([\\s\\S]*?)\\)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Dispositivo seleccionado: ([\\s\\S]*?) \\(([\\s\\S]*?)\\)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Appareil sélectionné : {0} ({1})",
                de = "Ausgewähltes Gerät: {0} ({1})",
                pt = "Dispositivo selecionado: {0} ({1})",
                it = "Dispositivo selezionato: {0} ({1})"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": negative response ",
                esFastCheck = ": respuesta negativa ",
                enRegexPattern = "^ECU ([\\s\\S]*?): negative response ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^ECU ([\\s\\S]*?): respuesta negativa ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "ECU {0} : réponse négative {1}",
                de = "ECU {0}: negative Antwort {1}",
                pt = "ECU {0}: resposta negativa {1}",
                it = "ECU {0}: risposta negativa {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid CAN bytes",
                esFastCheck = ": bytes CAN inválidos",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid CAN bytes\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): bytes CAN inválidos\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : octets CAN invalides",
                de = "ECU {0}: ungültige CAN-Bytes",
                pt = "ECU {0}: bytes CAN inválidos",
                it = "ECU {0}: byte CAN non validi"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " sensors · ",
                esFastCheck = " sensores · ",
                enRegexPattern = "^([\\s\\S]*?)/([\\s\\S]*?) sensors · ([\\s\\S]*?) available\$",
                enOrder = intArrayOf(0, 1, 2),
                esRegexPattern = "^([\\s\\S]*?)/([\\s\\S]*?) sensores · ([\\s\\S]*?) disponibles\$",
                esOrder = intArrayOf(0, 1, 2),
                fr = "{0}/{1} capteurs · {2} disponibles",
                de = "{0}/{1} Sensoren · {2} verfügbar",
                pt = "{0}/{1} sensores · {2} disponíveis",
                it = "{0}/{1} sensori · {2} disponibili"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "WiFi connection error: ",
                esFastCheck = "Error al conectar WiFi: ",
                enRegexPattern = "^WiFi connection error: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error al conectar WiFi: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur de connexion Wi-Fi : {0}",
                de = "WLAN-Verbindungsfehler: {0}",
                pt = "Erro de conexão Wi-Fi: {0}",
                it = "Errore di connessione Wi-Fi: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " must be an integer",
                esFastCheck = " debe ser un número entero",
                enRegexPattern = "^([\\s\\S]*?) must be an integer\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) debe ser un número entero\$",
                esOrder = intArrayOf(0),
                fr = "{0} doit être un nombre entier",
                de = "{0} muss eine ganze Zahl sein",
                pt = "{0} deve ser um número inteiro",
                it = "{0} deve essere un numero intero"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " sensors · ",
                esFastCheck = " sensores · ",
                enRegexPattern = "^([\\s\\S]*?) sensors · ([\\s\\S]*?) available\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^([\\s\\S]*?) sensores · ([\\s\\S]*?) disponibles\$",
                esOrder = intArrayOf(0, 1),
                fr = "{0} capteurs · {1} disponibles",
                de = "{0} Sensoren · {1} verfügbar",
                pt = "{0} sensores · {1} disponíveis",
                it = "{0} sensori · {1} disponibili"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connected via WiFi to ",
                esFastCheck = "Conectado vía WiFi a ",
                enRegexPattern = "^Connected via WiFi to ([\\s\\S]*?):([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Conectado vía WiFi a ([\\s\\S]*?):([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Connecté via Wi-Fi à {0}:{1}",
                de = "Über WLAN verbunden mit {0}:{1}",
                pt = "Conectado via Wi-Fi a {0}:{1}",
                it = "Connesso tramite Wi-Fi a {0}:{1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not read CSV: ",
                esFastCheck = "No se pudo leer el CSV: ",
                enRegexPattern = "^Could not read CSV: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo leer el CSV: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible de lire le CSV : {0}",
                de = "CSV konnte nicht gelesen werden: {0}",
                pt = "Não foi possível ler o CSV: {0}",
                it = "Impossibile leggere il CSV: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Scan interrupted: ",
                esFastCheck = "❌ Escaneo interrumpido: ",
                enRegexPattern = "^❌ Scan interrupted: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Escaneo interrumpido: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ Analyse interrompue : {0}",
                de = "❌ Scan unterbrochen: {0}",
                pt = "❌ Verificação interrompida: {0}",
                it = "❌ Scansione interrotta: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "  ℹ️ ELM Diagnostic: ",
                esFastCheck = "  ℹ️ Diagnóstico ELM: ",
                enRegexPattern = "^  ℹ️ ELM Diagnostic: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^  ℹ️ Diagnóstico ELM: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "  ℹ️ Diagnostic ELM : {0}",
                de = "  ℹ️ ELM-Diagnose: {0}",
                pt = "  ℹ️ Diagnóstico ELM: {0}",
                it = "  ℹ️ Diagnostica ELM: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Delete profile for ",
                esFastCheck = "¿Eliminar la ficha de ",
                enRegexPattern = "^Delete profile for ([\\s\\S]*?)\\?\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^¿Eliminar la ficha de ([\\s\\S]*?)\\?\$",
                esOrder = intArrayOf(0),
                fr = "Supprimer la fiche de {0} ?",
                de = "Profil für {0} löschen?",
                pt = "Eliminar a ficha de {0}?",
                it = "Eliminare la scheda di {0}?"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Adapter rejected ",
                esFastCheck = "El adaptador rechazó ",
                enRegexPattern = "^Adapter rejected ([\\s\\S]*?): ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^El adaptador rechazó ([\\s\\S]*?): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "L'adaptateur a rejeté {0} : {1}",
                de = "Adapter hat {0} abgelehnt: {1}",
                pt = "O adaptador rejeitou {0}: {1}",
                it = "L'adattatore ha rifiutato {0}: {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Web Policy (HTTPS): ",
                esFastCheck = "Política Web (HTTPS): ",
                enRegexPattern = "^Web Policy \\(HTTPS\\): ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Política Web \\(HTTPS\\): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Politique Web (HTTPS) : {0}",
                de = "Web-Richtlinie (HTTPS): {0}",
                pt = "Política Web (HTTPS): {0}",
                it = "Informativa Web (HTTPS): {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "' deleted.",
                esFastCheck = "' eliminado.",
                enRegexPattern = "^Vehicle '([\\s\\S]*?)' deleted\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Vehículo '([\\s\\S]*?)' eliminado\\.\$",
                esOrder = intArrayOf(0),
                fr = "Véhicule '{0}' supprimé.",
                de = "Fahrzeug '{0}' gelöscht.",
                pt = "Veículo '{0}' eliminado.",
                it = "Veicolo '{0}' eliminato."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ OBD2 link lost: ",
                esFastCheck = "❌ Enlace OBD2 perdido: ",
                enRegexPattern = "^❌ OBD2 link lost: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^❌ Enlace OBD2 perdido: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "❌ Liaison OBD2 perdue : {0}",
                de = "❌ OBD2-Verbindung verloren: {0}",
                pt = "❌ Ligação OBD2 perdida: {0}",
                it = "❌ Collegamento OBD2 perso: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ". Retrying in ",
                esFastCheck = ". Reintentando en ",
                enRegexPattern = "^([\\s\\S]*?)\\. Retrying in ([\\s\\S]*?)s\\.\\.\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^([\\s\\S]*?)\\. Reintentando en ([\\s\\S]*?)s\\.\\.\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "{0}. Nouvelle tentative dans {1} s...",
                de = "{0}. Erneuter Versuch in {1}s...",
                pt = "{0}. Tentando novamente em {1}s...",
                it = "{0}. Nuovo tentativo tra {1}s..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": triggered by ",
                esFastCheck = ": originado por ",
                enRegexPattern = "^ECU ([\\s\\S]*?): triggered by ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^ECU ([\\s\\S]*?): originado por ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "ECU {0} : déclenché par {1}",
                de = "ECU {0}: ausgelöst durch {1}",
                pt = "ECU {0}: originado por {1}",
                it = "ECU {0}: attivato da {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not export: ",
                esFastCheck = "No se pudo exportar: ",
                enRegexPattern = "^Could not export: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo exportar: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible d'exporter : {0}",
                de = "Export fehlgeschlagen: {0}",
                pt = "Não foi possível exportar: {0}",
                it = "Impossibile esportare: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not import: ",
                esFastCheck = "No se pudo importar: ",
                enRegexPattern = "^Could not import: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo importar: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible d'importer : {0}",
                de = "Import fehlgeschlagen: {0}",
                pt = "Não foi possível importar: {0}",
                it = "Impossibile importare: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Saved sessions (",
                esFastCheck = "Sesiones guardadas (",
                enRegexPattern = "^Saved sessions \\(([\\s\\S]*?)\\)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Sesiones guardadas \\(([\\s\\S]*?)\\)\$",
                esOrder = intArrayOf(0),
                fr = "Sessions enregistrées ({0})",
                de = "Gespeicherte Sitzungen ({0})",
                pt = "Sessões guardadas ({0})",
                it = "Sessioni salvate ({0})"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connection error: ",
                esFastCheck = "Error de conexión: ",
                enRegexPattern = "^Connection error: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error de conexión: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur de connexion : {0}",
                de = "Verbindungsfehler: {0}",
                pt = "Erro de conexão: {0}",
                it = "Errore di connessione: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Error on line '",
                esFastCheck = "Error en línea '",
                enRegexPattern = "^Error on line '([\\s\\S]*?)': ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Error en línea '([\\s\\S]*?)': ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Erreur à la ligne '{0}' : {1}",
                de = "Fehler in Zeile '{0}': {1}",
                pt = "Erro na linha '{0}': {1}",
                it = "Errore alla riga '{0}': {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Could not save: ",
                esFastCheck = "No se pudo guardar: ",
                enRegexPattern = "^Could not save: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^No se pudo guardar: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Impossible d'enregistrer : {0}",
                de = "Speichern fehlgeschlagen: {0}",
                pt = "Não foi possível guardar: {0}",
                it = "Impossibile salvare: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " saved vehicles",
                esFastCheck = " vehículos guardados",
                enRegexPattern = "^([\\s\\S]*?) saved vehicles\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) vehículos guardados\$",
                esOrder = intArrayOf(0),
                fr = "{0} véhicules enregistrés",
                de = "{0} gespeicherte Fahrzeuge",
                pt = "{0} veículos guardados",
                it = "{0} veicoli salvati"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connecting to ",
                esFastCheck = "Conectando a ",
                enRegexPattern = "^Connecting to ([\\s\\S]*?):([\\s\\S]*?)\\.\\.\\.\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Conectando a ([\\s\\S]*?):([\\s\\S]*?)\\.\\.\\.\$",
                esOrder = intArrayOf(0, 1),
                fr = "Connexion à {0}:{1}...",
                de = "Verbinde mit {0}:{1}...",
                pt = "Conectando a {0}:{1}...",
                it = "Connessione a {0}:{1}..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": invalid DLC",
                esFastCheck = ": DLC inválido",
                enRegexPattern = "^ECU ([\\s\\S]*?): invalid DLC\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): DLC inválido\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : DLC invalide",
                de = "ECU {0}: ungültiger DLC",
                pt = "ECU {0}: DLC inválido",
                it = "ECU {0}: DLC non valido"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "DTC Diagnostics: ",
                esFastCheck = "Diagnóstico DTC: ",
                enRegexPattern = "^DTC Diagnostics: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Diagnóstico DTC: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Diagnostic DTC : {0}",
                de = "DTC-Diagnose: {0}",
                pt = "Diagnóstico DTC: {0}",
                it = "Diagnostica DTC: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = ": empty frame",
                esFastCheck = ": trama vacía",
                enRegexPattern = "^ECU ([\\s\\S]*?): empty frame\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^ECU ([\\s\\S]*?): trama vacía\$",
                esOrder = intArrayOf(0),
                fr = "ECU {0} : trame vide",
                de = "ECU {0}: leerer Frame",
                pt = "ECU {0}: trama vazia",
                it = "ECU {0}: frame vuoto"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connecting to ",
                esFastCheck = "Conectando a ",
                enRegexPattern = "^Connecting to ([\\s\\S]*?)\\.\\.\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Conectando a ([\\s\\S]*?)\\.\\.\\.\$",
                esOrder = intArrayOf(0),
                fr = "Connexion à {0}...",
                de = "Verbinde mit {0}...",
                pt = "Conectando a {0}...",
                it = "Connessione a {0}..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "GitHub Support: ",
                esFastCheck = "Soporte GitHub: ",
                enRegexPattern = "^GitHub Support: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Soporte GitHub: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Support GitHub : {0}",
                de = "GitHub-Support: {0}",
                pt = "Suporte GitHub: {0}",
                it = "Supporto GitHub: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Select up to ",
                esFastCheck = "Seleccionar hasta ",
                enRegexPattern = "^Select up to ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Seleccionar hasta ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Sélectionner jusqu'à {0}",
                de = "Bis zu {0} auswählen",
                pt = "Selecionar até {0}",
                it = "Seleziona fino a {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " samples · ",
                esFastCheck = " muestras · ",
                enRegexPattern = "^([\\s\\S]*?) · ([\\s\\S]*?) samples · ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1, 2),
                esRegexPattern = "^([\\s\\S]*?) · ([\\s\\S]*?) muestras · ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1, 2),
                fr = "{0} · {1} échantillons · {2}",
                de = "{0} · {1} Messwerte · {2}",
                pt = "{0} · {1} amostras · {2}",
                it = "{0} · {1} campioni · {2}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "❌ Command ",
                esFastCheck = "❌ Falló ",
                enRegexPattern = "^❌ Command ([\\s\\S]*?) failed: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^❌ Falló ([\\s\\S]*?): ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "❌ Échec de {0} : {1}",
                de = "❌ Befehl {0} fehlgeschlagen: {1}",
                pt = "❌ Falhou {0}: {1}",
                it = "❌ Comando {0} fallito: {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Edit gauge ",
                esFastCheck = "Editar indicador ",
                enRegexPattern = "^Edit gauge ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Editar indicador ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Modifier la jauge {0}",
                de = "Anzeige {0} bearbeiten",
                pt = "Editar indicador {0}",
                it = "Modifica indicatore {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Target slot ",
                esFastCheck = "Destino ranura ",
                enRegexPattern = "^Target slot ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Destino ranura ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Emplacement cible {0}",
                de = "Zielplatz {0}",
                pt = "Posição de destino {0}",
                it = "Slot di destinazione {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Missing byte ",
                esFastCheck = "Falta el byte ",
                enRegexPattern = "^Missing byte ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Falta el byte ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Octet {0} manquant",
                de = "Byte {0} fehlt",
                pt = "Falta o byte {0}",
                it = "Byte {0} mancante"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Developer: ",
                esFastCheck = "Desarrollador: ",
                enRegexPattern = "^Developer: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Desarrollador: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Développeur : {0}",
                de = "Entwickler: {0}",
                pt = "Programador: {0}",
                it = "Sviluppatore: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Connected to ",
                esFastCheck = "Conectado a ",
                enRegexPattern = "^Connected to ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Conectado a ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Connecté à {0}",
                de = "Verbunden mit {0}",
                pt = "Conectado a {0}",
                it = "Connesso a {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Regen code: ",
                esFastCheck = "Código reg.: ",
                enRegexPattern = "^Regen code: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Código reg\\.: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Code régén. : {0}",
                de = "Regen.-Code: {0}",
                pt = "Código reg.: {0}",
                it = "Codice rigen.: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Move ",
                esFastCheck = " después",
                enRegexPattern = "^Move ([\\s\\S]*?) down\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Mover ([\\s\\S]*?) después\$",
                esOrder = intArrayOf(0),
                fr = "Déplacer {0} vers le bas",
                de = "{0} nach unten verschieben",
                pt = "Mover {0} para baixo",
                it = "Sposta {0} dopo"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " sensors · ",
                esFastCheck = " sensores · ",
                enRegexPattern = "^([\\s\\S]*?) sensors · \$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) sensores · \$",
                esOrder = intArrayOf(0),
                fr = "{0} capteurs · ",
                de = "{0} Sensoren · ",
                pt = "{0} sensores · ",
                it = "{0} sensori · "
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Opening ",
                esFastCheck = "Abriendo ",
                enRegexPattern = "^Opening ([\\s\\S]*?)\\.\\.\\.\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Abriendo ([\\s\\S]*?)\\.\\.\\.\$",
                esOrder = intArrayOf(0),
                fr = "Ouverture de {0}...",
                de = "{0} wird geöffnet...",
                pt = "Abrindo {0}...",
                it = "Apertura di {0}..."
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Sensors · ",
                esFastCheck = "Sensores · ",
                enRegexPattern = "^Sensors · ([\\s\\S]*?)/([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Sensores · ([\\s\\S]*?)/([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Capteurs · {0}/{1}",
                de = "Sensoren · {0}/{1}",
                pt = "Sensores · {0}/{1}",
                it = "Sensori · {0}/{1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Gauge ",
                esFastCheck = "Indicador ",
                enRegexPattern = "^Gauge ([\\s\\S]*?) · ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Indicador ([\\s\\S]*?) · ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Jauge {0} · {1}",
                de = "Anzeige {0} · {1}",
                pt = "Indicador {0} · {1}",
                it = "Indicatore {0} · {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Querying ",
                esFastCheck = "Consultando ",
                enRegexPattern = "^Querying ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Consultando ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Interrogation de {0}",
                de = "Frage {0} ab",
                pt = "Consultando {0}",
                it = "Interrogazione di {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Adapter: ",
                esFastCheck = "Adaptador: ",
                enRegexPattern = "^Adapter: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Adaptador: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Adaptateur : {0}",
                de = "Adapter: {0}",
                pt = "Adaptador: {0}",
                it = "Adattatore: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Move ",
                esFastCheck = "Mover ",
                enRegexPattern = "^Move ([\\s\\S]*?) up\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Mover ([\\s\\S]*?) antes\$",
                esOrder = intArrayOf(0),
                fr = "Déplacer {0} vers le haut",
                de = "{0} nach oben verschieben",
                pt = "Mover {0} para cima",
                it = "Sposta {0} prima"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = " gauges",
                esFastCheck = " indicadores",
                enRegexPattern = "^([\\s\\S]*?) gauges\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^([\\s\\S]*?) indicadores\$",
                esOrder = intArrayOf(0),
                fr = "{0} jauges",
                de = "{0} Anzeigen",
                pt = "{0} indicadores",
                it = "{0} indicatori"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Disable ",
                esFastCheck = "Desactivar ",
                enRegexPattern = "^Disable ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Desactivar ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Désactiver {0}",
                de = "{0} deaktivieren",
                pt = "Desativar {0}",
                it = "Disattiva {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Vehicle: ",
                esFastCheck = "Vehículo: ",
                enRegexPattern = "^Vehicle: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Vehículo: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Véhicule : {0}",
                de = "Fahrzeug: {0}",
                pt = "Veículo: {0}",
                it = "Veicolo: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Increase ",
                esFastCheck = "Aumentar ",
                enRegexPattern = "^Increase ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Aumentar ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Augmenter {0}",
                de = "{0} erhöhen",
                pt = "Aumentar {0}",
                it = "Aumenta {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Garage · ",
                esFastCheck = "Garaje · ",
                enRegexPattern = "^Garage · ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Garaje · ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Garage · {0}",
                de = "Garage · {0}",
                pt = "Garagem · {0}",
                it = "Garage · {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Slot ",
                esFastCheck = "Ranura ",
                enRegexPattern = "^Slot ([\\s\\S]*?) · ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Ranura ([\\s\\S]*?) · ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Emplacement {0} · {1}",
                de = "Platz {0} · {1}",
                pt = "Posição {0} · {1}",
                it = "Slot {0} · {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Profile: ",
                esFastCheck = "Perfil: ",
                enRegexPattern = "^Profile: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Perfil: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Profil : {0}",
                de = "Profil: {0}",
                pt = "Perfil: {0}",
                it = "Profilo: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Decrease ",
                esFastCheck = "Reducir ",
                enRegexPattern = "^Decrease ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Reducir ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Diminuer {0}",
                de = "{0} verringern",
                pt = "Reduzir {0}",
                it = "Riduci {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Rename ",
                esFastCheck = "Renombrar ",
                enRegexPattern = "^Rename ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Renombrar ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Renommer {0}",
                de = "{0} umbenennen",
                pt = "Renomear {0}",
                it = "Rinomina {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Warning: ",
                esFastCheck = "Aviso: ",
                enRegexPattern = "^Warning: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Aviso: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Avertissement : {0}",
                de = "Warnung: {0}",
                pt = "Aviso: {0}",
                it = "Avviso: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Gauge ",
                esFastCheck = "Indicador ",
                enRegexPattern = "^Gauge ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Indicador ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Jauge {0}",
                de = "Anzeige {0}",
                pt = "Indicador {0}",
                it = "Indicatore {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Value · ",
                esFastCheck = "Valor · ",
                enRegexPattern = "^Value · ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Valor · ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Valeur · {0}",
                de = "Wert · {0}",
                pt = "Valor · {0}",
                it = "Valore · {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Enable ",
                esFastCheck = "Activar ",
                enRegexPattern = "^Enable ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Activar ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Activer {0}",
                de = "{0} aktivieren",
                pt = "Ativar {0}",
                it = "Attiva {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Year ",
                esFastCheck = "Año ",
                enRegexPattern = "^Year ([\\s\\S]*?) • ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0, 1),
                esRegexPattern = "^Año ([\\s\\S]*?) • ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0, 1),
                fr = "Année {0} • {1}",
                de = "Baujahr {0} • {1}",
                pt = "Ano {0} • {1}",
                it = "Anno {0} • {1}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Email: ",
                esFastCheck = "Correo: ",
                enRegexPattern = "^Email: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Correo: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "E-mail : {0}",
                de = "E-Mail: {0}",
                pt = "E-mail: {0}",
                it = "Email: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Edit: ",
                esFastCheck = "Editar: ",
                enRegexPattern = "^Edit: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Editar: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Modifier : {0}",
                de = "Bearbeiten: {0}",
                pt = "Editar: {0}",
                it = "Modifica: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Error: ",
                esFastCheck = "Error: ",
                enRegexPattern = "^Error: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Error: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Erreur : {0}",
                de = "Fehler: {0}",
                pt = "Erro: {0}",
                it = "Errore: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Field: ",
                esFastCheck = "Dato: ",
                enRegexPattern = "^Field: ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Dato: ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Champ : {0}",
                de = "Datenfeld: {0}",
                pt = "Campo: {0}",
                it = "Campo: {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Color ",
                esFastCheck = "Color ",
                enRegexPattern = "^Color ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Color ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Couleur {0}",
                de = "Farbe {0}",
                pt = "Cor {0}",
                it = "Colore {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Slot ",
                esFastCheck = "Ranura ",
                enRegexPattern = "^Slot ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Ranura ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Emplacement {0}",
                de = "Platz {0}",
                pt = "Posição {0}",
                it = "Slot {0}"
            )
        )
        list.add(
            TemplateTranslationRule(
                enFastCheck = "Key ",
                esFastCheck = "Tecla ",
                enRegexPattern = "^Key ([\\s\\S]*?)\$",
                enOrder = intArrayOf(0),
                esRegexPattern = "^Tecla ([\\s\\S]*?)\$",
                esOrder = intArrayOf(0),
                fr = "Touche {0}",
                de = "Taste {0}",
                pt = "Tecla {0}",
                it = "Tasto {0}"
            )
        )
    }

}
