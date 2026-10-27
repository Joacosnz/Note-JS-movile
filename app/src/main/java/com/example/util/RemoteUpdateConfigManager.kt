package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AppUpdateConfigState(
    val firebaseDatabaseName: String = FirebaseInitializer.NOTE_JS_DATABASE_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val minRequiredVersionCode: Int = BuildConfig.VERSION_CODE,
    val latestVersionCode: Int = BuildConfig.VERSION_CODE,
    val minRequiredVersionName: String = BuildConfig.VERSION_NAME,
    val latestVersionName: String = BuildConfig.VERSION_NAME,
    val isMandatoryUpdate: Boolean = false,
    val isOptionalUpdateAvailable: Boolean = false,
    val isOptionalDismissedInSession: Boolean = false,
    val isSimulationPreview: Boolean = false,
    val updateTitle: String = "Actualización Obligatoria Disponible",
    val updateMessage: String = "Tu versión actual de Note JS ya no es compatible. Actualiza a la versión más reciente para seguir utilizando todas las funciones y mantener tus notas sincronizadas con NoteJsDataBase.",
    val releaseNotes: List<String> = listOf(
        "Optimización de memoria RAM (RGB_565 y caché inteligente)",
        "Sincronización mejorada con NoteJsDataBase",
        "Mayor fluidez en listas y cuadrículas de notas",
        "Correcciones de estabilidad para dispositivos de gama baja"
    ),
    val updateUrl: String = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}",
    val isFetching: Boolean = false,
    val lastFetchTimeMillis: Long = 0L,
    val statusMessage: String = "Conectado a ${FirebaseInitializer.NOTE_JS_DATABASE_NAME}"
) {
    val shouldShowDialog: Boolean
        get() = isSimulationPreview || isMandatoryUpdate || (isOptionalUpdateAvailable && !isOptionalDismissedInSession)
}

/**
 * Gestor centralizado de Firebase Remote Config vinculado a "NoteJsDataBase".
 * Permite administrar remotamente el estado de versión de la aplicación y mostrar
 * alertas de actualización obligatoria u opcional sin necesidad de republicar en Play Store.
 */
class RemoteUpdateConfigManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _updateState = MutableStateFlow(createInitialState())
    val updateState: StateFlow<AppUpdateConfigState> = _updateState.asStateFlow()

    private var namedRemoteConfig: FirebaseRemoteConfig? = null
    private var defaultRemoteConfig: FirebaseRemoteConfig? = null

    init {
        initializeRemoteConfig()
    }

    private fun createInitialState(): AppUpdateConfigState {
        val (verCode, verName) = getInstalledVersionInfo()
        return AppUpdateConfigState(
            firebaseDatabaseName = FirebaseInitializer.NOTE_JS_DATABASE_NAME,
            currentVersionCode = verCode,
            currentVersionName = verName,
            minRequiredVersionCode = verCode,
            latestVersionCode = verCode,
            minRequiredVersionName = verName,
            latestVersionName = verName,
            updateUrl = "https://play.google.com/store/apps/details?id=${appContext.packageName}"
        )
    }

    private fun getInstalledVersionInfo(): Pair<Int, String> {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode
            }
            val name = pkgInfo.versionName ?: BuildConfig.VERSION_NAME
            code to name
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE to BuildConfig.VERSION_NAME
        }
    }

    private fun initializeRemoteConfig() {
        scope.launch {
            try {
                val noteJsApp = FirebaseInitializer.getNoteJsApp(appContext)
                val defaultApp = FirebaseInitializer.ensureInitialized(appContext)

                val minIntervalSeconds = if (BuildConfig.DEBUG) 0L else 900L
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minIntervalSeconds)
                    .setFetchTimeoutInSeconds(15L)
                    .build()

                if (noteJsApp != null) {
                    val rcNamed = FirebaseRemoteConfig.getInstance(noteJsApp)
                    rcNamed.setConfigSettingsAsync(configSettings).await()
                    rcNamed.setDefaultsAsync(R.xml.remote_config_defaults).await()
                    attachRealtimeListener(rcNamed, FirebaseInitializer.NOTE_JS_DATABASE_NAME)
                    namedRemoteConfig = rcNamed
                }

                if (defaultApp != null && defaultApp.name != noteJsApp?.name) {
                    val rcDefault = FirebaseRemoteConfig.getInstance(defaultApp)
                    rcDefault.setConfigSettingsAsync(configSettings).await()
                    rcDefault.setDefaultsAsync(R.xml.remote_config_defaults).await()
                    attachRealtimeListener(rcDefault, defaultApp.name)
                    defaultRemoteConfig = rcDefault
                } else if (namedRemoteConfig == null && defaultApp != null) {
                    val rcDefault = FirebaseRemoteConfig.getInstance(defaultApp)
                    rcDefault.setConfigSettingsAsync(configSettings).await()
                    rcDefault.setDefaultsAsync(R.xml.remote_config_defaults).await()
                    attachRealtimeListener(rcDefault, FirebaseInitializer.NOTE_JS_DATABASE_NAME)
                    namedRemoteConfig = rcDefault
                }

                evaluateCurrentConfig()
                fetchAndActivate(forceImmediate = false)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo inicializar Firebase Remote Config en NoteJsDataBase", e)
                _updateState.update {
                    it.copy(
                        isFetching = false,
                        statusMessage = "Modo local activo (${FirebaseInitializer.NOTE_JS_DATABASE_NAME})"
                    )
                }
            }
        }
    }

    private fun attachRealtimeListener(rc: FirebaseRemoteConfig, instanceLabel: String) {
        try {
            rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.i(TAG, "Actualización en tiempo real detectada en $instanceLabel: ${configUpdate.updatedKeys}")
                    rc.activate().addOnCompleteListener {
                        evaluateCurrentConfig()
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Listener de Remote Config ($instanceLabel) reportó aviso: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adjuntar ConfigUpdateListener en $instanceLabel", e)
        }
    }

    /**
     * Descarga y activa los parámetros más recientes desde Firebase Remote Config (NoteJsDataBase)
     * y verifica también si existe configuración en la colección remota de NoteJsDataBase.
     */
    fun fetchAndActivate(forceImmediate: Boolean = false, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            _updateState.update {
                it.copy(
                    isFetching = true,
                    statusMessage = "Consultando estado en ${FirebaseInitializer.NOTE_JS_DATABASE_NAME}..."
                )
            }
            var success = false
            try {
                val rcList = listOfNotNull(namedRemoteConfig, defaultRemoteConfig).distinct()
                for (rc in rcList) {
                    try {
                        if (forceImmediate) {
                            val fastSettings = FirebaseRemoteConfigSettings.Builder()
                                .setMinimumFetchIntervalInSeconds(0L)
                                .setFetchTimeoutInSeconds(15L)
                                .build()
                            rc.setConfigSettingsAsync(fastSettings).await()
                        }
                        rc.fetchAndActivate().await()
                        success = true
                    } catch (inner: Exception) {
                        Log.w(TAG, "Fallo parcial en fetchAndActivate: ${inner.message}")
                    }
                }

                evaluateCurrentConfig()
                checkNoteJsDatabaseFirestoreFallback()

                _updateState.update {
                    it.copy(
                        isFetching = false,
                        lastFetchTimeMillis = System.currentTimeMillis(),
                        statusMessage = if (it.isMandatoryUpdate) {
                            "Actualización obligatoria requerida (v${it.minRequiredVersionName})"
                        } else if (it.isOptionalUpdateAvailable) {
                            "Nueva versión disponible (v${it.latestVersionName})"
                        } else {
                            "Versión al día en ${FirebaseInitializer.NOTE_JS_DATABASE_NAME}"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error al obtener Remote Config desde NoteJsDataBase", e)
                _updateState.update {
                    it.copy(
                        isFetching = false,
                        statusMessage = "Verificación completada (valores en caché de ${FirebaseInitializer.NOTE_JS_DATABASE_NAME})"
                    )
                }
            } finally {
                onComplete?.invoke(success)
            }
        }
    }

    private fun evaluateCurrentConfig() {
        val rc = namedRemoteConfig ?: defaultRemoteConfig ?: return
        val rcFallback = defaultRemoteConfig

        val (currentCode, currentName) = getInstalledVersionInfo()

        val minCodePrimary = listOf(
            rc.getLong("min_required_version_code"),
            rc.getLong("min_version_code"),
            rc.getLong("force_update_version_code"),
            rcFallback?.getLong("min_required_version_code") ?: 0L,
            rcFallback?.getLong("min_version_code") ?: 0L
        ).maxOrNull()?.toInt()?.takeIf { it > 0 } ?: currentCode

        val latestCodePrimary = listOf(
            rc.getLong("latest_version_code"),
            rcFallback?.getLong("latest_version_code") ?: 0L,
            minCodePrimary.toLong()
        ).maxOrNull()?.toInt()?.takeIf { it > 0 } ?: currentCode

        val minVersionName = firstNonBlank(
            rc.getString("min_required_version_name"),
            rc.getString("required_version_name"),
            rcFallback?.getString("min_required_version_name"),
            default = currentName
        )

        val latestVersionName = firstNonBlank(
            rc.getString("latest_version_name"),
            rc.getString("latest_version"),
            rcFallback?.getString("latest_version_name"),
            default = minVersionName
        )

        val explicitForceFlag = rc.getBoolean("force_update_required") ||
            rc.getBoolean("mandatory_update") ||
            rc.getBoolean("actualizacion_obligatoria") ||
            (rcFallback?.getBoolean("force_update_required") == true) ||
            (rcFallback?.getBoolean("mandatory_update") == true) ||
            (rcFallback?.getBoolean("actualizacion_obligatoria") == true)

        val explicitOptionalFlag = rc.getBoolean("update_available") ||
            (rcFallback?.getBoolean("update_available") == true)

        val isCodeOutdatedMandatory = currentCode < minCodePrimary
        val isNameOutdatedMandatory = compareSemanticVersions(currentName, minVersionName) < 0

        val isMandatory = explicitForceFlag || isCodeOutdatedMandatory || isNameOutdatedMandatory

        val isCodeOutdatedOptional = currentCode < latestCodePrimary
        val isNameOutdatedOptional = compareSemanticVersions(currentName, latestVersionName) < 0
        val isOptional = !isMandatory && (explicitOptionalFlag || isCodeOutdatedOptional || isNameOutdatedOptional)

        val title = firstNonBlank(
            rc.getString("update_title"),
            rc.getString("force_update_title"),
            rc.getString("titulo_actualizacion"),
            rcFallback?.getString("update_title"),
            default = if (isMandatory) "Actualización Obligatoria Disponible" else "Nueva Actualización Disponible"
        )

        val message = firstNonBlank(
            rc.getString("update_message"),
            rc.getString("force_update_message"),
            rc.getString("mensaje_actualizacion"),
            rcFallback?.getString("update_message"),
            default = "Se requiere actualizar Note JS a la versión más reciente para seguir utilizando la aplicación con las últimas mejoras de rendimiento, seguridad y sincronización con NoteJsDataBase."
        )

        val rawNotes = firstNonBlank(
            rc.getString("update_release_notes"),
            rc.getString("release_notes"),
            rcFallback?.getString("update_release_notes"),
            default = "Optimización de memoria RAM (RGB_565 y caché inteligente)|Sincronización mejorada con NoteJsDataBase|Mayor fluidez en listas y cuadrículas de notas|Correcciones de estabilidad para dispositivos de gama baja"
        )
        val parsedNotes = rawNotes
            .split("|", "\n")
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotEmpty() }

        val url = firstNonBlank(
            rc.getString("update_url"),
            rc.getString("store_url"),
            rc.getString("play_store_url"),
            rcFallback?.getString("update_url"),
            default = "https://play.google.com/store/apps/details?id=${appContext.packageName}"
        )

        val dbName = firstNonBlank(
            rc.getString("firebase_database_name"),
            default = FirebaseInitializer.NOTE_JS_DATABASE_NAME
        )

        _updateState.update { current ->
            current.copy(
                firebaseDatabaseName = dbName,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                minRequiredVersionCode = minCodePrimary,
                latestVersionCode = latestCodePrimary,
                minRequiredVersionName = if (isMandatory && minVersionName == currentName && minCodePrimary > currentCode) {
                    "Código $minCodePrimary+"
                } else {
                    minVersionName
                },
                latestVersionName = latestVersionName,
                isMandatoryUpdate = isMandatory,
                isOptionalUpdateAvailable = isOptional,
                updateTitle = title,
                updateMessage = message,
                releaseNotes = if (parsedNotes.isNotEmpty()) parsedNotes else current.releaseNotes,
                updateUrl = url
            )
        }
    }

    /**
     * Consulta complementaria en la base de datos nombrada "NoteJsDataBase" (o la configurada)
     * por si el administrador definió un documento `app_config/version_control` en Firestore.
     */
    private suspend fun checkNoteJsDatabaseFirestoreFallback() {
        try {
            val app = FirebaseInitializer.getNoteJsApp(appContext) ?: return
            val dbInstances = buildList {
                runCatching {
                    add(FirebaseFirestore.getInstance(app, FirebaseInitializer.NOTE_JS_DATABASE_NAME))
                }
                runCatching {
                    val configuredId = appContext.getString(R.string.firestore_database_id)
                    if (configuredId.isNotBlank()) {
                        add(FirebaseFirestore.getInstance(app, configuredId))
                    }
                }
                runCatching {
                    add(FirebaseFirestore.getInstance(app))
                }
            }.distinct()

            for (db in dbInstances) {
                val doc = try {
                    db.collection("app_config").document("version_control").get().await()
                } catch (_: Exception) {
                    null
                }
                if (doc != null && doc.exists()) {
                    val (currentCode, currentName) = getInstalledVersionInfo()
                    val remoteMinCode = (doc.getLong("min_required_version_code")
                        ?: doc.getLong("min_version_code"))?.toInt()
                    val remoteMinName = doc.getString("min_required_version_name")
                    val remoteForce = doc.getBoolean("force_update_required")
                        ?: doc.getBoolean("actualizacion_obligatoria")
                        ?: false
                    val remoteTitle = doc.getString("update_title")
                    val remoteMessage = doc.getString("update_message")
                    val remoteUrl = doc.getString("update_url")

                    _updateState.update { state ->
                        val effectiveMinCode = maxOf(state.minRequiredVersionCode, remoteMinCode ?: state.minRequiredVersionCode)
                        val effectiveMinName = if (!remoteMinName.isNullOrBlank() &&
                            compareSemanticVersions(state.minRequiredVersionName, remoteMinName) < 0
                        ) {
                            remoteMinName
                        } else {
                            state.minRequiredVersionName
                        }
                        val mandatoryByDoc = remoteForce ||
                            (currentCode < effectiveMinCode) ||
                            (compareSemanticVersions(currentName, effectiveMinName) < 0)

                        state.copy(
                            minRequiredVersionCode = effectiveMinCode,
                            minRequiredVersionName = effectiveMinName,
                            isMandatoryUpdate = state.isMandatoryUpdate || mandatoryByDoc,
                            updateTitle = remoteTitle?.takeIf { it.isNotBlank() } ?: state.updateTitle,
                            updateMessage = remoteMessage?.takeIf { it.isNotBlank() } ?: state.updateMessage,
                            updateUrl = remoteUrl?.takeIf { it.isNotBlank() } ?: state.updateUrl
                        )
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Silencioso si la colección app_config no existe o requiere reglas distintas
        }
    }

    fun dismissOptionalUpdateForSession() {
        _updateState.update {
            it.copy(
                isOptionalDismissedInSession = true,
                isSimulationPreview = false
            )
        }
    }

    /**
     * Activa una vista previa interactiva de la alerta de actualización obligatoria
     * para verificar el diseño y comportamiento desde Ajustes.
     */
    fun triggerSimulationPreview() {
        _updateState.update {
            it.copy(
                isSimulationPreview = true,
                minRequiredVersionName = "1.8.0",
                minRequiredVersionCode = it.currentVersionCode + 1,
                updateTitle = "Actualización Obligatoria Disponible",
                updateMessage = "Tu versión actual de Note JS (${it.currentVersionName}) debe actualizarse a la versión 1.8.0 para mantener la compatibilidad con ${FirebaseInitializer.NOTE_JS_DATABASE_NAME} y aplicar las optimizaciones de memoria RAM."
            )
        }
    }

    fun dismissSimulationPreview() {
        _updateState.update {
            it.copy(isSimulationPreview = false)
        }
        evaluateCurrentConfig()
    }

    /**
     * Abre Google Play Store en la ficha de la aplicación o el enlace configurado en Remote Config.
     */
    fun openStoreForUpdate(context: Context) {
        val url = _updateState.value.updateUrl.ifBlank {
            "https://play.google.com/store/apps/details?id=${context.packageName}"
        }
        try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (marketIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(marketIntent)
                return
            }
        } catch (_: Exception) {
        }

        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo abrir el enlace de actualización: $url", e)
        }
    }

    private fun firstNonBlank(vararg values: String?, default: String): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim() ?: default
    }

    companion object {
        private const val TAG = "RemoteUpdateConfigMgr"

        @Volatile
        private var INSTANCE: RemoteUpdateConfigManager? = null

        fun getInstance(context: Context): RemoteUpdateConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteUpdateConfigManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Compara dos cadenas de versión semántica (ej. "1.7.10" vs "1.8.0").
         * Retorna < 0 si v1 < v2, 0 si son iguales, > 0 si v1 > v2.
         */
        fun compareSemanticVersions(v1: String, v2: String): Int {
            val clean1 = v1.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val clean2 = v2.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            if (clean1 == clean2) return 0

            val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }
            return 0
        }
    }
}
