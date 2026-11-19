# MİMARİ TASLAK (Faz 0)

> Kullanıcı uyurken hazırlandı; onay beklemeden Faz 1'e geçildi (bkz. `NOTES.md` → Sorular). Değişiklik istenirse şema `version = 1` iken migration gerektirmeden güncellenebilir.

## 1. Paket Yapısı (tek `:app` modülü, katmanlar paketle ayrılır)

```
dev.kok.app
├── KokApplication.kt / MainActivity.kt
├── core/                 # Ortak araçlar: zaman sağlayıcı (Clock), dispatcher qualifier'ları
├── data/
│   ├── local/
│   │   ├── KokDatabase.kt, Converters.kt
│   │   ├── entity/       # Room @Entity sınıfları
│   │   ├── dao/          # Room @Dao arayüzleri
│   │   ├── relation/     # @Relation / @Embedded sorgu sonuç modelleri
│   │   └── seed/         # İlk kurulumda yüklenen kas grubu + egzersiz kataloğu
│   ├── mapper/           # Entity <-> Domain dönüşümleri
│   └── repository/       # Domain repository arayüzlerinin implementasyonları
├── domain/
│   ├── model/            # Saf Kotlin modelleri (Android bağımlılığı yok)
│   ├── repository/       # Repository arayüzleri
│   ├── engine/           # MetabolismCalculator (BMR/TDEE), StatsMetaphorMapper
│   └── usecase/          # İş kuralları (ör. AddBodyMeasurementUseCase)
├── di/                   # Hilt modülleri
└── presentation/
    ├── theme/            # Color, Type, Shape, Theme (Doğa Tasarım Sistemi)
    ├── components/       # Organik bileşenler (dal ayırıcı, yaprak rozet, yatay bar…)
    ├── navigation/       # Tip güvenli rotalar + alt gezinme
    └── feature/
        ├── dashboard/    # Ana ekran (barlar, BMR/TDEE/alınan)
        ├── nutrition/    # Yemek & su ekleme
        ├── profile/      # Profil / hedefler
        ├── chat/         # Gemini sohbet (Faz 3)
        ├── workout/      # Programlar (Faz 4)
        ├── musclemap/    # 3D kas haritası (Faz 4)
        ├── body/         # Aylık ölçüm (Faz 5)
        └── stats/        # Haftalık/aylık özet (Faz 5)
```

Neden tek modül: kişisel uygulama, CI derleme süresi ve telefon-odaklı geliştirme akışı. Katman sınırları paketle korunur; `domain` paketi Android/Room importu içermez.

## 2. Bağımlılıklar (`gradle/libs.versions.toml`)

| Alan | Kütüphane |
|---|---|
| Derleme | AGP 8.7.3, Gradle 8.11.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28 |
| UI | Compose BOM 2024.12.01 (Material 3), Navigation Compose 2.8.5 (tip güvenli rotalar), Lifecycle 2.8.7 |
| DI | Hilt 2.52 + hilt-navigation-compose 1.2.0 |
| Veri | Room 2.6.1 (KSP, şema dışa aktarımı `app/schemas`) |
| Async | Coroutines 1.9.0, Flow/StateFlow |
| Ağ / AI (Faz 3) | OkHttp 4.12 + kotlinx.serialization 1.7.3 → Gemini REST (Function Calling) |
| Güvenlik (Faz 3) | androidx.security-crypto (EncryptedSharedPreferences, API anahtarı) |
| 3D (Faz 4) | SceneView (eklenecek) |
| Fontlar | Fraunces + DM Sans (değişken TTF, OFL lisanslı, `res/font`) |

SDK: `minSdk 26` (değişken font + java.time), `targetSdk/compileSdk 35`.

## 3. Veritabanı ER Şeması (Room, `version = 1`)

```
user_profile 1 ─────────────────────────── (tekil satır, id = 1)

daily_log 1 ──< meal_entry
daily_log 1 ──< workout_session_log 1 ──< workout_set_log >── 1 exercise
daily_log >── 0..1 workout_day (o günün aktif antrenmanı)

workout_program 1 ──< workout_day 1 ──< workout_day_exercise >── 1 exercise
exercise >──< muscle_group   (exercise_muscle_cross_ref, role = PRIMARY|SECONDARY)

body_measurement (year_month UNIQUE)
chat_message
```

| Tablo | Sütunlar | Kısıtlar / Index |
|---|---|---|
| `user_profile` | id, age, height_cm, weight_kg, sex, activity_level, water_goal_ml, protein_goal_g, carb_goal_g, fat_goal_g, fiber_goal_g, sodium_goal_mg, potassium_goal_mg, iron_goal_mg, vitamin_c_goal_mg, vitamin_d_goal_mcg, updated_at | PK id (=1) |
| `daily_log` | id, local_date, water_ml, burned_kcal, active_workout_day_id | UNIQUE(local_date); FK active_workout_day_id → workout_day SET NULL; index |
| `meal_entry` | id, daily_log_id, name, grams, kcal, protein_g, carb_g, fat_g, fiber_g, sodium_mg, potassium_mg, iron_mg, vitamin_c_mg, vitamin_d_mcg, source, animation_category, created_at | FK → daily_log CASCADE; index(daily_log_id) |
| `workout_program` | id, name, is_active, source, created_at | index(is_active); tek aktif program kuralı DAO @Transaction ile |
| `workout_day` | id, program_id, day_order, name | FK → workout_program CASCADE; UNIQUE(program_id, day_order) |
| `exercise` | id, name (NOCASE) | UNIQUE(name) |
| `muscle_group` | id, code, display_name, mesh_node_id, body_region | UNIQUE(code), UNIQUE(mesh_node_id) |
| `exercise_muscle_cross_ref` | exercise_id, muscle_group_id, role | PK(exercise_id, muscle_group_id); FK'ler CASCADE; index(muscle_group_id) |
| `workout_day_exercise` | id, workout_day_id, exercise_id, order_index, target_sets, target_reps_min, target_reps_max, target_weight_kg, notes | FK → workout_day CASCADE, → exercise RESTRICT; UNIQUE(workout_day_id, order_index); index(exercise_id) |
| `workout_session_log` | id, daily_log_id, workout_day_id, started_at, duration_min, burned_kcal, notes | FK → daily_log CASCADE, → workout_day SET NULL; index'ler |
| `workout_set_log` | id, session_id, exercise_id, set_index, reps, weight_kg | FK → session CASCADE, → exercise RESTRICT; UNIQUE(session_id, exercise_id, set_index) |
| `body_measurement` | id, year_month, measured_on, weight_kg, body_fat_percent?, chest_cm?, waist_cm?, arm_cm?, leg_cm?, shoulder_cm?, neck_cm? | UNIQUE(year_month) |
| `chat_message` | id, role, text, created_at, tool_name?, tool_payload_json?, action_summary?, undo_ref? | index(created_at) |

SPEC'ten bilinçli sapmalar:
- `user_profile.weight_kg` eklendi: BMR (Mifflin-St Jeor) kilo ister; aylık ölçüm girilince profil kilosu da güncellenir.
- `workout_day_exercise` eklendi: bir günün hangi egzersizleri hangi set/tekrar hedefiyle içerdiğini tutmak için zorunlu bağlantı tablosu.
- `workout_set_log` eklendi: "set/tekrar/ağırlık verileri" tek kolona JSON gömmek yerine normalize edildi.
- Tarihler `LocalDate` → ISO `TEXT` (yyyy-MM-dd) olarak saklanır; sözlük sırası = kronolojik sıra.

Migration stratejisi: `exportSchema = true`, her şema değişikliğinde `version++` ve açık `Migration` nesnesi; `fallbackToDestructiveMigration` kullanılmaz (kişisel veri kaybı riski).
