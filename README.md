# Atelier Mobilă — proiect Android

Proiect Android WebView care include versiunea HTML v14 în `app/src/main/assets/index.html`.

## Construire APK

Proiectul este pregătit pentru GitHub Actions. După încărcarea conținutului acestui ZIP într-un repository GitHub, deschide **Actions → Build Android APK → Run workflow** (sau împinge commit-ul pe `main`/`master`). La final, descarcă artifact-ul **Atelier-Mobila-APK**; în arhivă se află `app-debug.apk`.

- Aplicația funcționează offline, folosind HTML-ul inclus.
- Exportul JSON și CSV este direcționat în `Downloads/AtelierMobila`.
- Tipărirea folosește fereastra nativă Android.
- `applicationId`: `ro.ateliermobila.app`.

Notă: APK-ul nu a fost compilat local în mediul de pregătire, deoarece Android SDK/Gradle nu sunt instalate și accesul la internet pentru descărcarea lor nu este disponibil. Construirea efectivă trebuie confirmată de workflow-ul GitHub Actions.
