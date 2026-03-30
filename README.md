# QR Scanner — Android Приложение

Приложение для сканирования и генерации QR-кодов.

## 📁 Структура

```
qr-scanner/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/qrscanner/app/
│   │   │   ├── MainActivity.kt       # Сканирование QR
│   │   │   └── GenerateActivity.kt   # Генерация QR
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🚀 Сборка APK

### Требования:
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK 34

### Команды сборки:

**Debug APK:**
```bash
cd qr-scanner
gradlew assembleDebug
```

**Release APK:**
```bash
gradlew assembleRelease
```

APK файлы будут в:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## 📱 Функционал

### Сканирование:
- ✅ Сканирование через камеру (CameraX + ML Kit)
- ✅ Сканирование из галереи
- ✅ Фонарик для тёмных мест
- ✅ Копирование результата
- ✅ Поделиться результатом

### Генерация:
- ✅ Генерация QR из текста/URL
- ✅ Выбор цвета QR и фона
- ✅ Регулировка размера
- ✅ Сохранение в галерею
- ✅ Поделиться QR-кодом

## 🛠 Технологии

- **Язык:** Kotlin
- **CameraX:** Камера
- **ML Kit:** Распознавание QR
- **ZXing:** Генерация QR
- **Material 3:** UI компоненты
- **ViewBinding:** Работа с View

## 📸 Скриншоты

Приложение имеет современный Material Design 3 интерфейс с поддержкой тёмной темы.
