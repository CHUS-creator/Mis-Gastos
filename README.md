# MisGastos

App Android para control de gastos del hogar.

## Características

- **Registro de gastos e ingresos** con importe, categoría, descripción y fecha.
- **Escaneo de tickets con OCR** (ML Kit): fotografía el ticket y la app extrae total, fecha, comercio y líneas de producto para revisarlas y guardarlas.
- **Autoaprendizaje por comercio**: la app recuerda la categoría y el formato de ticket de cada comercio para acelerar futuros escaneos.
- **Detalle y edición de transacciones**, incluyendo las líneas de producto de los tickets escaneados.
- **Dashboard** con balance total, ingresos y gastos del mes.
- **Presupuestos mensuales por categoría** con barras de progreso y alertas (snackbar) al superar el 80% o el 100% del límite.
- **Estadísticas**: desglose de gastos por categoría del mes en curso.
- **Exportación de datos** a CSV y JSON.
- **Almacenamiento local** con Room (funciona sin conexión).
- **Navegación inferior** con 5 pantallas: Inicio, Gastos, Ingresos, Presupuestos, Estadísticas.
- **Bilingüe**: español e inglés.

## Stack tecnológico

- Kotlin + Jetpack Compose
- Material 3
- Room (base de datos local SQLite)
- Navigation-Compose
- ViewModel + StateFlow (arquitectura MVVM)
- CameraX 1.4.2+ + ML Kit Text Recognition (OCR de tickets, compatible con páginas de 16 KB)
- Coil (carga de imágenes)
- KSP para la generación de código de Room
- JUnit 4 (tests unitarios)

## Cómo abrir el proyecto

1. Clona el repositorio:

   ```bash
   git clone https://github.com/CHUS-creator/Mis-Gastos.git
   ```

2. Ábrelo en **Android Studio** (Hedgehog o superior).
3. Espera a que Gradle sincronice y descargue las dependencias (incluye el Android SDK si no está configurado).
4. Conecta un dispositivo o emulador Android (API 26+) y pulsa **Run**.

## Ejecutar los tests

```bash
./gradlew test
```

Los tests unitarios cubren el parser de tickets (`ReceiptParser`) y las utilidades de fechas (`DateUtils`).

## Configuración

- `minSdk`: 26 (Android 8.0)
- `targetSdk`: 34
- `compileSdk`: 34
- Java 17

## Estructura del proyecto

```
app/src/main/java/com/misgastos/app/
├── data/
│   ├── entity/        # Transaction, Budget, LineItem, MerchantHint, MerchantTemplate
│   ├── dao/           # TransactionDao, BudgetDao, LineItemDao, ...
│   ├── database/      # MisGastosDatabase (migraciones 1→4)
│   └── repository/    # MisGastosRepository
├── ocr/               # OcrRecognizer, ReceiptParser, ParsedReceipt, ReceiptTemplate
├── viewmodel/         # MisGastosViewModel
├── ui/
│   ├── theme/         # Tema y colores
│   ├── nav/           # Navegación (NavHost, Screen)
│   ├── components/    # Componentes reutilizables
│   └── screens/       # Dashboard, Expenses, Income, Budget, Stats, Scan, Detail
└── util/              # DateUtils, Categories, DataExporter
```

## Próximas funciones (según demanda)

- Filtros por rango de fechas.
- Importación de datos (CSV, JSON).
- Sincronización en la nube (Firebase) para multiplataforma.
- Versión para iOS.
