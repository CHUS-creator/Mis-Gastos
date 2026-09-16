# MisGastos

App Android para control de gastos del hogar.

## Características

- **Registro de gastos e ingresos** con importe, categoría, descripción y fecha.
- **Dashboard** con balance total, ingresos y gastos del mes.
- **Presupuestos mensuales por categoría** con barras de progreso y alertas (snackbar) al superar el 80% o el 100% del límite.
- **Estadísticas**: desglose de gastos por categoría del mes en curso.
- **Almacenamiento local** con Room (funciona sin conexión).
- **Navegación inferior** con 5 pantallas: Inicio, Gastos, Ingresos, Presupuestos, Estadísticas.

## Stack tecnológico

- Kotlin + Jetpack Compose
- Material 3
- Room (base de datos local SQLite)
- Navigation-Compose
- ViewModel + StateFlow (arquitectura MVVM)
- KSP para la generación de código de Room

## Cómo abrir el proyecto

1. Clona el repositorio:

   ```bash
   git clone https://github.com/CHUS-creator/Mis-Gastos.git
   ```

2. Ábrelo en **Android Studio** (Hedgehog o superior).
3. Espera a que Gradle sincronice y descargue las dependencias (incluye el Android SDK si no está configurado).
4. Conecta un dispositivo o emulador Android (API 26+) y pulsa **Run**.

## Configuración

- `minSdk`: 26 (Android 8.0)
- `targetSdk`: 34
- `compileSdk`: 34
- Java 17

## Estructura del proyecto

```
app/src/main/java/com/misgastos/app/
├── data/
│   ├── entity/        # Transaction, Budget (entidades Room)
│   ├── dao/            # TransactionDao, BudgetDao
│   ├── database/      # MisGastosDatabase
│   └── repository/    # MisGastosRepository
├── viewmodel/         # MisGastosViewModel
├── ui/
│   ├── theme/         # Tema y colores
│   ├── nav/           # Navegación (NavHost, Screen)
│   ├── components/    # Componentes reutilizables
│   └── screens/      # Dashboard, Expenses, Income, Budget, Stats
└── util/              # DateUtils, Categories
```

## Próximas funciones (según demanda)

- Edición de transacciones existentes.
- Filtros por rango de fechas.
- Exportación/importación de datos (CSV, JSON).
- Sincronización en la nube (Firebase) para multiplataforma.
- Versión para iOS.
