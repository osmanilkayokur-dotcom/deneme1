# Kök

Kişisel beslenme, su, antrenman ve vücut takibi için Android uygulaması. Veriler tamamen telefonda (Room) tutulur, Gemini destekli sohbetle yemek ve program kaydı yapılabilir.

## APK'yı telefona kurmak
1. GitHub'da bu repo → **Actions** → en üstteki yeşil ✅ **Android Build**.
2. Sayfanın altındaki **Artifacts** → `kok-debug-apk` indir, zip'i aç.
3. `app-debug.apk`'yı aç → "bilinmeyen kaynaklara izin ver" → **Yükle**.

Her yeni sürüm aynı imzayla derlenir; eskisinin üstüne kurulur, verilerin silinmez.

## Ekranlar
| Sekme | İçerik |
|---|---|
| **Bugün** | BMR / TDEE / alınan kalori barları, makro + mikro barları, su testisi (buluttan yağmur), öğün listesi, geçmiş günler |
| **Asistan** | Gemini sohbeti: "200 g tavuk ve 1 tabak pilav yedim" → onay kartı → kayıt (geri alınabilir). Program oluşturma/güncelleme |
| **Spor** | Aktif program, bugünün günü, 2D/3D kas haritası, antrenman kaydı (MET ile kalori), elle program editörü |
| **Vücut** | Ayda bir kilitli ölçüm (ay döngüsü + ağaç halkası), geçmiş, Profil ve hedefler |
| **Özet** | Haftalık/aylık toplamlar ve "1,4 el arabası kadar yemek yedin" tarzı somut karşılıklar |

## Belgeler
- `RULES.md` — mimari ve kodlama kuralları
- `SPEC.md` — teknik şartname
- `ARCHITECTURE.md` — paket yapısı, bağımlılıklar, veritabanı şeması
- `PROGRESS.md` — faz ilerlemesi (oturumlar arası hafıza)
- `NOTES.md` — açık sorular ve senin yapman gerekenler

## Geliştirme
- Kotlin 2.0, Jetpack Compose (Material 3), Hilt, Room, Coroutines/Flow, OkHttp + kotlinx.serialization, SceneView
- `./gradlew testDebugUnitTest assembleDebug`
- Fontlar: Fraunces ve DM Sans (SIL OFL, `docs/licenses/`)
