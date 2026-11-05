# FernVPN

Минималистичный VPN-клиент для Android на Cloudflare WARP (WireGuard).

## Что внутри

- **Kotlin + Jetpack Compose**, строго Material 3 (Material You) с динамическими цветами.
- Подключение через `com.wireguard.android:tunnel` (`GoBackend`), ключи X25519 генерируются
  на устройстве, конфигурация берётся у Cloudflare WARP.
- Главный экран: круглая кнопка подключения, график скорости на `Canvas`, карточка статистики.
- Питомец «Трава»: росток на главном экране растёт по мере накопленного времени VPN.
- Настройки: питомец, язык (системный / русский / английский), ачивки.

## Сборка

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

CI: GitHub Actions собирает APK на каждый пуш в `main` и выкладывает артефакт.
