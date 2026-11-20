# PROJE İLERLEME TAKİBİ (PROGRESS)

> **Yapay Zeka İçin Not:** Bu dosya oturumlar arası hafıza köprüsüdür. Her faz tamamlandığında sadece değişen/eklenen kritik bilgileri kısa maddeler halinde güncelle; gereksiz uzun açıklamalar ekleme.

## Mevcut Durum
- **Aktif Faz:** Tüm fazlar kodlandı → kullanıcı testi ve geri bildirim
- **Durum:** Faz 0–4 CI'da yeşil; Faz 5–6 + testler push edildi
- **Son Güncelleme:** 2026-11-20 (gece otomatik oturum)

## Faz Kontrol Listesi
- [x] **Faz 0:** Klasör yapısı, bağımlılıklar ve Room ER şeması onayı
- [x] **Faz 1:** Proje kurulumu, Doğa Tasarım Sistemi, Room Entity/DAO/Repository
- [x] **Faz 2:** BMR/TDEE motoru, manuel beslenme/su takibi, yatay barlar, Dashboard
- [x] **Faz 3:** Gemini API Function Calling (`log_meal`, `create_or_update_workout_program`) ve Sohbet Sekmesi
- [x] **Faz 4:** Dinamik spor programı yönetimi + SceneView 3D 360° Kas Haritası
- [x] **Faz 5:** Aylık kilitli vücut ölçümü + `StatsMetaphorMapper` ve Haftalık/Aylık özetler
- [x] **Faz 6:** Doğa mikro-animasyonları (buluttan su, çeltikten pirinç vb.) + test ve optimizasyon

## Sabitlenen Teknik Kararlar
- **Paket Adı:** `dev.kok.app` (uygulama adı "Kök") — geçici, NOTES.md Soru 2
- **Min SDK / Target SDK:** 26 / 35 (compileSdk 35), Kotlin 2.0.21, AGP 8.7.3, Gradle 8.11.1
- **Gemini Model:** `gemini-flash-latest` (Ayarlar'dan değiştirilebilir), REST v1beta + OkHttp, Function Calling
- **3D:** SceneView 2.2.1; model yolu `assets/models/male_anatomy.glb`; node adı `muscle_group.mesh_node_id` ile **başlamalı** (ör. `mesh_pectoralis_major_l`). Model yoksa 2D harita.
- **Derleme:** Sadece GitHub Actions (`.github/workflows/android.yml`); Claude ortamında `dl.google.com` kapalı. Sabit debug keystore repoda.
- **Room şeması:** v1, `exportSchema=true` → `app/schemas` CI'da üretilir; repoya henüz eklenmedi (migration öncesi eklenmeli).

## Oluşturulan Kritik Bileşenler
- **Entities:** 13 tablo (bkz. ARCHITECTURE.md) + `DatabaseSeeder` (18 kas, 40 egzersiz)
- **DAOs / Repositories:** Profile, DailyLog, Meal, Workout, BodyMeasurement, Chat + `SecureSettingsStore` (EncryptedSharedPreferences)
- **Domain:** `MetabolismCalculator`, `StatsMetaphorMapper`, `TurkishSuffix`, `ChatAssistantUseCase`, `ObserveTrainingTodayUseCase`, `MuscleHighlight`, `LogWorkoutSessionUseCase`
- **Ekranlar:** Dashboard (Bugün), AddMeal, Profile, Chat (Asistan), Settings, Workout (Spor), ProgramEditor, Body (Vücut), Stats (Özet)
- **Animasyonlar:** `RainCloud` (su), `FoodOriginAnimation` (6 kategori), su testisi dalga sıçraması, bar yay animasyonları
- **Testler:** domain motorları + use case'ler (JVM), `GeminiGatewayTest` (ayrıştırma, geçmiş sentezi)

## Sonraki Oturum İçin Notlar & Bilinen Eksikler
- Domain testleri JVM'de hızlı koşturulabilir (domain+core paketleri saf Kotlin).
- Gemini onay akışı: MODEL mesajı (functionCall ham JSON) → kart → onay → TOOL mesajı (functionResponse) → model kapanış mesajı. Yanıtsız çağrılara CANCELLED sentezlenir.
- Bekleyen (kullanıcıya bağlı): Gemini API anahtarı girişi, 3D `.glb` modeli, NOTES.md soruları.
- Room şeması CI artifact'ı (`room-schemas`) olarak yükleniyor; şema değişmeden önce `app/schemas/.../1.json` repoya eklenmeli.