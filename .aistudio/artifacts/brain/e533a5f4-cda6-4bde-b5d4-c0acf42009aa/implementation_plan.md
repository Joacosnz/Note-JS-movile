# Plan de Implementación: Optimización para Dispositivos de Gama Baja y Antiguos

Optimizar la aplicación **Note JS** para dispositivos de bajos recursos (RAM limitada, procesadores modestos y versiones antiguas de Android) mediante optimizaciones a nivel de compilación (R8/Shrinking), gestión eficiente de memoria en imágenes (Coil) y prevención de recomposiciones innecesarias en listas y cuadrículas de Jetpack Compose.

---

## 1. User Intent

El stack tecnológico de la aplicación (Jetpack Compose, Firebase Firestore, ML Kit Document Scanner/OCR, Media3 y Room) ofrece gran potencia pero puede exigir alta memoria RAM y CPU en terminales antiguos o de gama de entrada. El objetivo es recortar drásticamente el consumo de RAM, reducir el tamaño del APK y mejorar los cuadros por segundo (FPS) en el desplazamiento de listas sin perder funcionalidades.

---

## 2. Solution Overview

El plan implementa las siguientes optimizaciones clave respetando las respuestas y preferencias del usuario:

1. **R8 y Shrinking en `app/build.gradle.kts`:**
   - Activar `isMinifyEnabled = true` e `isShrinkResources = true` en `buildTypes { release { ... } }`.
   - Utilizar el archivo de optimización estándar de Android (`proguard-android-optimize.txt`) junto con `proguard-rules.pro`.
   - **Restricción Crítica de Plataforma:** Preservar intacta la configuración de firmado (`signingConfig = signingConfigs.getByName("release")`) y los keystores existentes (`debug.keystore`).

2. **Reglas ProGuard en `app/proguard-rules.pro`:**
   - Definir directivas `-keep` y `-dontwarn` para proteger las clases y métodos de Firebase (Firestore, Auth, Storage), ML Kit (Text Recognition, Document Scanner), entidades de Room, modelos serializables con Moshi (`com.example.model.**`) y corrutinas de Kotlin.

3. **Optimización Global de Imágenes con Coil en `MainApplication.kt`:**
   - Hacer que `MainApplication` implemente `ImageLoaderFactory`.
   - Proveer un `ImageLoader` personalizado con:
     - Formato de píxel ligero: `bitmapConfig(Bitmap.Config.RGB_565)` (reduce el consumo de memoria en bitmaps a la mitad en comparación con `ARGB_8888`).
     - Límite estricto de caché en RAM: `maxSizePercent(0.15)` (máximo 15% del heap disponible).
     - Desactivación precautoria de hardware bitmaps: `allowHardware(false)` para máxima compatibilidad y estabilidad en GPUs y APIs antiguas.
     - `crossfade(true)` para transiciones suaves de carga.

4. **Optimización de Listas y Cuadrículas Lazy en Jetpack Compose:**
   - Asegurar que todas las llamadas `items(...)` e `itemsIndexed(...)` a lo largo de las vistas principales posean un `key` estable y único (por ejemplo `key = { it.id }` o identificador unívoco).
   - Esto permite que el runtime de Compose omita la recomposición de elementos inalterados cuando cambia o se añade una nota, reduciendo la carga sobre el recolector de basura (GC).

---

## 3. User Experience & Workflows

### Flujo de Renderizado y Memoria Optimizado

```
+-------------------------------------------------------------+
|                     Dispositivo Android                     |
|                   (Gama baja / RAM < 3GB)                  |
+-------------------------------------------------------------+
                              |
       +----------------------+----------------------+
       |                                             |
       v                                             v
[ Carga de Imágenes (Coil) ]              [ Listas Compose (Lazy) ]
       |                                             |
       +--> Bitmap.Config.RGB_565                    +--> items(..., key = { it.id })
       |    (50% ahorro de RAM)                      |    (Evita recomposiciones)
       |                                             |
       +--> Max 15% Memory Cache                     +--> LazyStaggeredGrid / LazyColumn
       |    (Evita OutOfMemoryErrors)                |    (Reciclado eficiente)
       |                                             |
       +--> allowHardware(false)                     +--> Desplazamiento fluido
            (GPU/Render estable en APIs viejas)           (Sin tirones de frame drops)
                              |
                              v
             [ APK Optimizado con R8 + Shrinking ]
             - Clases muertas eliminadas
             - Recursos sin usar removidos
             - Reglas ProGuard protegiendo Firebase y ML Kit
```

---

## 4. Implementation Details & Technical Scope

### A. Configuración de Compilación (`app/build.gradle.kts`)
- En el bloque `buildTypes.release`:
  - `isMinifyEnabled = true`
  - `isShrinkResources = true`
  - `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`
  - Mantener exactamente la asignación `signingConfig = signingConfigs.getByName("release")`.

### B. Reglas de Preservación de Código (`app/proguard-rules.pro`)
- Reglas para **Firebase SDK** (`com.google.firebase.**`).
- Reglas para **ML Kit** Vision & Document Scanner (`com.google.mlkit.**`, `com.google.android.gms.vision.**`).
- Reglas para **Room Database** y DAOs (`androidx.room.**`, `com.example.data.local.**`).
- Reglas para **Modelos de datos Moshi** (`com.example.model.**`, `@com.squareup.moshi.JsonClass`).
- Reglas para **Media3 / ExoPlayer** (`androidx.media3.**`).

### C. Configuración de Coil en `MainApplication.kt`
- Declarar `class MainApplication : Application(), ImageLoaderFactory`.
- Implementar `override fun newImageLoader(): ImageLoader`:
  ```kotlin
  ImageLoader.Builder(this)
      .bitmapConfig(Bitmap.Config.RGB_565)
      .memoryCache {
          MemoryCache.Builder(this)
              .maxSizePercent(0.15)
              .build()
      }
      .allowHardware(false)
      .crossfade(true)
      .build()
  ```

### D. Claves Estables en Listas Lazy
- Revisar y garantizar `key`:
  - **`HomeScreen.kt`**: Listas de Archivadas, Papelera, pestañas y filtros por etiqueta (`TagFilterRow.kt`).
  - **`KeepNotesGrid.kt`**: Secciones `FIJADAS` y `OTRAS` con `key = { it.id }` y `item` con claves estables.
  - **`RemindersScreen.kt`**: Lista de recordatorios del calendario (`key = { it.id }`).
  - **`FoldersScreen.kt`**: Lista de carpetas (`key = { it.name }` o id).
  - **`PdfLibraryScreen.kt`**: Documentos PDF (`key = { it.uri.toString() }`).

---

## 5. Critical Constraints & Verifications

- **Seguridad de Firma:** No modificar bajo ninguna circunstancia `debug.keystore`, `debug.keystore.base64`, ni eliminar los bloques `signingConfigs`.
- **Compatibilidad R8:** Las reglas ProGuard deben evitar fallos de instanciación por reflexión en Firebase y Moshi al generar variantes release o debug minificadas.
- **Verificación:** Ejecutar `compile_applet` tras aplicar los cambios para garantizar que el proyecto compile limpiamente sin errores de sintaxis, dependencias o tipos.
