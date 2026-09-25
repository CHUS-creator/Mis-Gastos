# MisGastos — Contexto del proyecto (léeme al iniciar sesión)

App Android (Kotlin + Jetpack Compose) para control de gastos del hogar, con
escaneo de tickets por OCR (ML Kit), autoaprendizaje por comercio, filtros,
comparación de precios entre comercios e importación/exportación de datos.

**Cómo usar este fichero**: es el punto de entrada para ponerse al día.
Léelo entero antes de tocar código. La sección [Mantenimiento](#mantenimiento)
indica cuándo actualizarlo; actualízalo tú mismo al cerrar cada hito.

Las preferencias personales del usuario están en `.vibe-preferences.md`.

## Puesta al día en 4 pasos

1. `git log --oneline -15` y `gh pr list` — qué ha pasado desde la última vez.
2. Lee este fichero entero (5 min).
3. Monta el entorno si la máquina es nueva (ver [Entorno](#entorno)).
4. `./gradlew test` — debe estar verde antes de empezar a cambiar nada.

## Arquitectura

```
app/src/main/java/com/misgastos/app/
├── data/
│   ├── entity/     # Transaction, LineItem, Budget, MerchantHint, MerchantTemplate
│   ├── dao/        # Flow-based DAOs (TransactionDao, LineItemDao, BudgetDao, ...)
│   ├── database/   # MisGastosDatabase (Room, version=4, migraciones 1→2→3→4)
│   └── repository/ # MisGastosRepository (única fuente de datos, Flow + suspend)
├── ocr/            # OcrRecognizer (ML Kit), ReceiptParser (heurístico, JVM puro)
├── viewmodel/      # MisGastosViewModel (MVVM + StateFlow, única instancia)
├── ui/
│   ├── screens/    # dashboard, expenses, income, budget, stats, scan (3), detail
│   ├── components/ # TransactionRow, SearchFilterBar, AddTransactionDialog, ...
│   └── nav/        # MisGastosNavHost, Screen (navegación inferior, 5 tabs)
└── util/           # DateUtils, Categories, DataExporter (CSV/JSON)
```

- `minSdk` 26, `targetSdk`/`compileSdk` 34, Java 17, AGP 8.13.2,
  Kotlin 2.0.20, Gradle 9.0 (wrapper). versionName 1.0.
- 100% local (Room, sin conexión ni permisos de red). Bilingüe es/en.
- MVVM estricto: la UI no toca DAOs; todo pasa por `MisGastosViewModel`.

### Modelo de datos (Room v4)

- `transactions`: id, type (INCOME/EXPENSE), amount, category, description,
  date (epoch ms), merchant, source (MANUAL/SCAN).
- `line_items`: name, price, quantity, FK a transactions (CASCADE). Solo se
  rellenan desde tickets escaneados.
- `merchant_hints`: merchant (PK) → categoría aprendida.
- `merchant_templates`: merchant (PK) → totalKeyword + dateFormat aprendidos.
- `budgets`: límite mensual por categoría.
- **Migraciones**: 1→2 (merchant, source, line_items), 2→3 (hints),
  3→4 (templates). Toda nueva tabla/columna necesita migración + versión.

### Flujo OCR (el corazón del proyecto)

```
foto/PDF → OcrRecognizer (ML Kit, latin) → texto crudo
        → lookupTemplate() (plantilla del comercio si existe)
        → ReceiptParser.parse(texto, template) → ParsedReceipt
        → applyMerchantHint() (categoría sugerida)
        → ReviewReceiptScreen (revisión manual SIEMPRE)
        → savePendingReceipt() → Transaction + LineItems
        → saveMerchantHint/Template() (aprende para la próxima)
```

- El usuario siempre revisa antes de guardar: el parser sugiere, no decide.
- `ReceiptParser` es JVM puro → testeable sin emulador. Mantenlo así.
- La app no ejecuta OCR de fotos en background ni procesa lotes (todavía).

## Comandos y CI

```bash
./gradlew test           # tests unitarios (JVM, sin emulador, ~15s con caché)
./gradlew assembleDebug  # APK debug (~63MB, incluye modelos ML Kit)
```

CI (`.github/workflows/ci.yml`, ~7 min): `test` + `assembleDebug` en cada
PR/push; sube el APK como artefacto.

## Entorno (máquina nueva / sandbox reiniciado)

1. JDK 17 Temurin: release de `adoptium/temurin17-binaries` (asset
   `OpenJDK17U-jdk_x64_linux_hotspot_*.tar.gz`), verificar checksum.
2. Android SDK: `commandlinetools-linux-*-latest.zip` de dl.google.com,
   aceptar licencias, instalar `platforms;android-34`, `build-tools;34.0.0`,
   `platform-tools`.
3. `local.properties` con `sdk.dir=<ruta>` (en `.gitignore`, no se sube).
4. Si `wget`/`curl` están bloqueados, Python `urllib` y `gh api`/`gh release
   download` suelen funcionar para descargas.

El sandbox no tiene emulador (sin KVM): la UI se valida en la tablet del
usuario; toda la lógica va cubierta por tests JVM.

## Convenciones

- Commits y PRs en español, claros y descriptivos.
- Ramas: `vibe/<slug>-<sesión>` creadas por el agente.
- Tests JUnit 4: estilo acumulador de fallos (`failures.add(...)`) con
  `assertTrue` que lista TODOS los fallos de una pasada, no solo el primero.
- Sin dependencias nuevas salvo necesidad justificada.
- Sin comentarios en el código salvo lo imprescindible (KDoc breve en tests).

## Reconocimiento de tickets (OCR)

### Corpus de regresión

`app/src/test/resources/receipts/` — texto OCR de tickets reales + verdad
esperada en `manifest.txt`:

```
archivo | total | fecha | comercio | num_lineas_producto
```

- `-` = campo no exigible con las heurísticas actuales (limitación
  documentada, no fallo).
- `-1` = no se exige recuento de líneas de producto.
- `ReceiptCorpusTest` (6 tests) valida total, fecha, comercio y líneas sobre
  todo el corpus en cada `./gradlew test`.

**Reglas del corpus:**
1. Nunca debilitar el manifiesto para que pase un test: se arregla el
   parser o se documenta la limitación con `-`.
2. Los tickets con datos personales se redactan (`[CLIENTE]`, `[NIF ...]`)
   antes de subirlos.
3. Fuentes de texto OCR válidas: biblioteca "TICKETS" de Mistral Studio
   (document library: search + open) y muestras del OCR de ML Kit de la app.
   El ruido de ambos motores es distinto; conviene tener de los dos.

### Añadir tickets nuevos

1. Subir PDF/foto a la biblioteca Mistral Studio "TICKETS".
2. Recuperar el texto con las herramientas de document library.
3. Crear `<comercio>_<fecha>.txt` + línea en `manifest.txt` (redactar datos
   personales).
4. `./gradlew test` → si falla algo nuevo, corregir el parser.

### Limitaciones conocidas del parser (actualizar)

- Gasolineras (Moeve) y algunas farmacias: comercio no deducible por
  heurísticas (cabecera "DATOS METROLOGICOS"); otras farmacias sí
  (findMerchant exige ≥3 letras y no empezar por dígito).
- Bershka: extrae las líneas (formato columnas multi-número), con
  nombres degradados por el OCR; Decathlon aún no (texto "EUR" detrás).
- Fechas mal OCR-adas (`"04 06.2026"`, `"10.06 2026"`): `mangledDateRegexes`
  las repara como ultimo recurso (dd + espacio + MM.yyyy y variantes).
- Precios con 3 decimales (ferretería `1,750`): `numberRegex` acepta 2-3
  decimales; el ticket no imprime total (el fallback da el máximo suelto,
  no fiable) y queda `-` en el manifiesto.
- Precios con texto "EUR" detras: se elimina el token de moneda antes de
  parsear la linea (Decathlon ya extrae lineas). Los nombres de linea se
  cortan en el primer numero incrustado (quita cantidades/refrids de cola).
- Restaurantes con `Cantidad Nombre Precio Total` en una línea: se exige
  el mínimo de líneas, el parser puede fusionar.
- El corpus es de OCR Mistral; falta cobertura de ruido propio de ML Kit
  (pendiente: capturar muestras reales desde la tablet).

## Mantenimiento de este fichero

Actualízalo (commit aparte o dentro del PR del hito) cuando:

- Se fusiona o abre un PR relevante → sección [Estado](#estado).
- Se añade tabla/columna Room → modelo de datos + migraciones.
- Se cambia el flujo OCR o se supera una limitación → limitaciones.
- Se añade función de usuario visible → primer párrafo y `ui/`.
- Se descubre un truco de entorno (p. ej. bloqueos de red) → Entorno.

Si al leerlo detectas que algo está desfasado (el código miente), corrígelo
en el mismo PR en que lo detectaste.

## Estado (actualizar al cerrar hitos)

- Fusionado en `main`: consolidación CI (#3).
- PRs abiertos (todos CI verde al redactarse): #4 filtros de
  periodo/comercio, #5 comparación de precios, #6 importación CSV/JSON,
  #7 corpus OCR de tickets reales + AGENTS.md, #8 banco de pruebas OCR
  (lotes de fotos desde galería, OCR ML Kit + parser, compartir texto
  separado por `====` para ampliar el corpus con ruido de ML Kit).
- Corpus OCR: 32 tickets (batch 1: 11 Mistral, batch 2: 19 Mistral,
  batch 3: 2 ML Kit del banco de pruebas, septiembre 2026). Parser:
  total 31/31 exigibles, fecha 31/31, comercio 24/24 exigibles, líneas
  dentro de rango en todos los exigibles.
- Ruido ML Kit (batch 3): bloques separados de descripciones e importes
  (líneas no emparejables sin geometría), dirección antes del logotipo
  (findMerchant prefiere logo corto tras dirección), "TOTAL (€)" separado
  de su importe (búsqueda de total en dos pasadas), keyword "a pagar".
- Pendiente implícito: ferretería (precios de 3 decimales sin total) y
  líneas con `-1` en comercios chinos (Ekomass/Mocasas: nombre "VARIOS").
