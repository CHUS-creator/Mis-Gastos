# MisGastos — Contexto del proyecto

App Android (Kotlin + Jetpack Compose) para control de gastos del hogar, con
escaneo de tickets por OCR (ML Kit) y autoaprendizaje por comercio.

Este fichero da contexto a los agentes de IA al iniciar una sesión. Las
preferencias personales del usuario están en `.vibe-preferences.md`.

## Arquitectura

```
app/src/main/java/com/misgastos/app/
├── data/          # Room: Transaction, Budget, LineItem, MerchantHint, MerchantTemplate
├── ocr/           # OcrRecognizer (ML Kit), ReceiptParser (heurístico JVM), ParsedReceipt
├── viewmodel/     # MisGastosViewModel (MVVM + StateFlow)
├── ui/            # Compose: screens (dashboard, expenses, income, budget, stats, scan, detail)
└── util/          # DateUtils, Categories, DataExporter (CSV/JSON)
```

- `minSdk` 26, `targetSdk`/`compileSdk` 34, Java 17, AGP 8.13.2, Kotlin 2.0.20.
- La app es 100% local (Room, sin conexión). Idiomas: español e inglés.

## Comandos

```bash
./gradlew test           # tests unitarios (JVM, sin emulador)
./gradlew assembleDebug  # APK debug
```

CI (`.github/workflows/ci.yml`): `test` + `assembleDebug` en cada PR/push.

## Entorno (sandbox o máquina nueva)

1. JDK 17 (Temurin; p. ej. desde releases de `adoptium/temurin17-binaries`).
2. Android SDK: cmdline-tools + `platforms;android-34` + `build-tools;34.0.0`.
3. `local.properties` con `sdk.dir=<ruta>` (está en `.gitignore`).
4. Gradle 9.0 vía wrapper del proyecto.

## Convenciones

- Commits y PRs en español, mensaje claro y descriptivo.
- Ramas de trabajo: `vibe/<slug>` (el sufijo de sesión lo añade el agente).
- Tests JUnit 4 estilo tabla/`assertTrue` con mensajes que listan los fallos.
- No añadir dependencias salvo necesidad justificada.

## Reconocimiento de tickets (OCR)

Flujo: foto/PDF → OCR → `ReceiptParser.parse()` → revisión manual →
guardado. El parser es JVM puro y se testea sin emulador.

### Corpus de regresión

`app/src/test/resources/receipts/` contiene tickets reales (texto OCR) y un
`manifest.txt` con la verdad esperada:

```
archivo | total | fecha | comercio | num_lineas_producto
```

- `-` = campo no exigible con las heurísticas actuales (limitación documentada).
- `-1` en líneas = no se exige recuento de líneas de producto.
- `ReceiptCorpusTest` valida total, fecha, comercio y líneas sobre todo el
  corpus. Nunca debilitar el manifiesto para "pase" un test: si un ticket
  falla, se arregla el parser o se documenta la limitación.

### Añadir tickets nuevos

1. Subir el PDF/foto a la biblioteca "TICKETS" de Mistral Studio.
2. Recuperar el texto OCR con las herramientas de document library
   (search + open).
3. Crear `<comercio>_<fecha>.txt` en el corpus y añadir la línea al manifiesto.
4. Ejecutar `./gradlew test` y corregir el parser si falla algo nuevo.

Nota: el corpus usa texto del OCR de Mistral; el OCR de ML Kit de la app
genera ruido distinto. Las muestras de ML Kit (p. ej. compartidas desde
`ReviewReceiptScreen`) también pueden añadirse al corpus con el mismo formato.

### Limitaciones conocidas del parser

- Gasolineras (Moeve) y farmacias: el comercio no es deducible por heurísticas
  (cabecera metrologica / nombre largo en mayúsculas).
- Bershka: el precio va en columnas en mitad de línea; no se extraen líneas
  de producto.

## Estado (actualizar al cerrar hitos)

- PRs abiertos: ver `gh pr list`. Históricos recientes: #4 filtros, #5
  comparación de precios, #6 importación CSV/JSON, #7 corpus OCR.
- El sandbox no tiene emulador (sin KVM): la UI se prueba en la tablet del
  usuario; toda la lógica (parsers, filtros, analyzer) queda cubierta por
  tests JVM locales.
