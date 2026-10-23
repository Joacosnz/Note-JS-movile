# Plan de Implementación: Acción "Crear nota" al Seleccionar Texto en Android

Sí, Android permite agregar acciones personalizadas directamente en el menú contextual que aparece al seleccionar texto en cualquier aplicación mediante el intent del sistema `android.intent.action.PROCESS_TEXT` (además del menú compartir `android.intent.action.SEND`).

## Objetivo y Experiencia de Usuario

De acuerdo con tus preferencias, el flujo funcionará de la siguiente manera:
1. **Menú de selección de texto en Android**: Al seleccionar cualquier fragmento de texto en el navegador, lector o mensajería, aparecerá la opción **"Crear nota"** vinculada a **Note JS**.
2. **Apertura directa en el editor**: Al pulsar **"Crear nota"**, se abrirá **Note JS** directamente en el editor de una nueva nota.
3. **Título vacío y nota simple**: El campo de **título quedará completamente vacío** y todo el texto seleccionado se colocará directamente en el cuerpo de una **nota de texto simple**, sin agregar metadatos adicionales ni modificar etiquetas o carpetas.
4. **Soporte con la app ya abierta (`singleTop`)**: Si ya tenías una nota abierta en segundo plano y seleccionas texto en otra aplicación para pulsar **"Crear nota"**, el editor se reiniciará limpiamente con una nota nueva que contenga el texto recién seleccionado y el título en blanco.

## Cambios Técnicos Planificados

- **`AndroidManifest.xml` y `res/values/strings.xml`**:
  - Mantener y verificar los `<intent-filter>` para `android.intent.action.PROCESS_TEXT` y `android.intent.action.SEND` (`text/plain`) con la etiqueta **"Crear nota"** (`@string/process_text_create_note`).
- **`MainActivity.kt`**:
  - Al recibir `Intent.ACTION_PROCESS_TEXT` (o `Intent.ACTION_SEND`), extraer el texto seleccionado (`EXTRA_PROCESS_TEXT` / `EXTRA_TEXT`) y navegar a `Screen.Editor` asegurando que `initialSharedTitle` sea `null` para mantener el título vacío y todo el texto pase íntegro al contenido.
- **`ui/screens/NoteEditorScreen.kt`**:
  - Ajustar `LaunchedEffect(noteId, initialSharedText)` para que, al recibir un texto desde la selección del sistema (incluso si el editor ya estaba abierto en una nota nueva con `noteId = 0L`), se limpie el título (`title = ""`), se inicialice un único bloque `NoteBlock.Text` simple con el contenido seleccionado y se coloque el cursor al final del texto listo para editar o guardar.
