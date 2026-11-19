# UYGULAMA TEKNİK ŞARTNAMESİ (SPEC)

## 1. Proje Özeti
Kişisel kullanım için geliştirilen, tamamen yerel veritabanı (Room) üzerinde çalışan, Gemini API destekli akıllı sohbet üzerinden yemek ve spor programı işleyebilen, 3D 360° erkek kas haritasına ve doğa temalı organik bir arayüze sahip Android uygulaması.

---

## 2. Veritabanı Mimarisi (Room / SQLite)
Veritabanı uygulamanın omurgasıdır. İlişkisel bütünlük (`ForeignKey`), sorgu performansı (`Index`) ve temiz veri modellemesi zorunludur.

1. **`UserProfile`**: Yaş, boy (cm), cinsiyet, aktivite/antrenman çarpanı, günlük su (ml) ve makro hedefleri.
2. **`DailyLog`**: Tarih (`localDate` - UNIQUE), toplam içilen su (ml), antrenman/günlük yakılan tahmini kalori (kcal), o günün aktif antrenman referansı.
3. **`MealEntry`**: Tarih ilişkisi (`DailyLog` FK), yemek adı, gramaj (g), kalori (kcal), protein (g), karbonhidrat (g), yağ (g), temel mikro besinler (lif, sodyum, potasyum, demir, C/D vitamini vb.), kaynak (`MANUAL` | `GEMINI_CHAT`) ve animasyon kategorisi (`GRAIN_RICE`, `PROTEIN`, `VEGETABLE`, `FRUIT`, `DAIRY`, `OTHER`).
4. **`WorkoutProgram`**: Program adı (örn. PPL, Upper/Lower, Özel), aktiflik durumu (`isActive`), oluşturulma kaynağı (`MANUAL` | `GEMINI_CHAT`).
5. **`WorkoutDay`**: Programa bağlı günler (`WorkoutProgram` FK), gün sırası/adı (örn. Gün 1: İtiş, Pazartesi vb.).
6. **`Exercise` & `MuscleGroup` & `ExerciseMuscleCrossRef` (Many-to-Many)**:
   - Her egzersizin çalıştırdığı **birincil (`PRIMARY`)** ve **ikincil (`SECONDARY`)** kas grupları tanımlıdır.
   - `MuscleGroup` tablosu, 3D `.glb` modeldeki ilgili anatomik parçanın **mesh/node ID'sini** tutar (örn. `mesh_pectoralis_major`, `mesh_latissimus_dorsi`).
7. **`WorkoutSessionLog`**: Tamamlanan antrenman kaydı, tarih, set/tekrar/ağırlık verileri ve tahmini yakılan kalori.
8. **`BodyMeasurement`**: Kilo, yağ oranı (opsiyonel), göğüs, bel, kol, bacak, omuz, boyun ölçümleri ve `yearMonth` (örn. `"2026-11"`) kolonu üzerinde **UNIQUE kısıtı**.
9. **`ChatMessage`**: Gemini sohbet geçmişi, rol (`USER` | `MODEL` | `TOOL`), zaman damgası ve veritabanında tetiklenen aksiyonun kısa özeti.

---

## 3. Fonksiyonel Gereksinimler

### 3.1 Beslenme, Makro/Mikro ve Metabolizma Takibi
- Kullanıcı günlük protein miktarını ve yediği yemekleri hem manuel olarak hem de Gemini sohbeti üzerinden kaydedebilir.
- **Yatay İlerleme Barları:** Günlük hedefe göre yüzde (%) kaç dolduğunu gösteren net yatay barlar (Protein, Karbonhidrat, Yağ, Lif ve seçili Mikro Besinler).
- **Metabolizma ve Kalori Karşılaştırma Barları:**
  - **BMR (Bazal Metabolizma Hızı):** Mifflin-St Jeor formülüyle dinamik hesaplanır.
  - **TDEE (Günlük Toplam Enerji İhtiyacı):** Kullanıcının spor/aktivite düzeyine göre hesaplanır.
  - Ana ekranda *(1) BMR*, *(2) Sporla Birlikte Günlük İhtiyaç (TDEE)* ve *(3) O Gün Alınan Kalori* görsel barlarla doğrudan karşılaştırılır.
- **Su Takibi:** Günlük içilen su miktarı (ml/L) ve hedef yüzdesi.

### 3.2 Gemini API Sohbet Sekmesi (Function Calling)
- Uygulama içinde özel bir **Sohbet (AI Asistan)** sekmesi bulunur.
- Gemini API aşağıdaki iki temel araç (Tool / Function Declaration) ile doğrudan Room veritabanına bağlı çalışır:
  - **`log_meal`**: Kullanıcı *"Az önce 200g tavuk göğsü, 1 tabak pirinç pilavı ve yoğurt yedim"* yazdığında Gemini yemekleri, gramajları, kalori, makro ve temel mikro değerlerini ayrıştırır; kullanıcıya onay kartı (veya doğrudan kayıt + geri alma) sunarak veritabanına işler. Ana ekrandaki tüm barlar `Flow` üzerinden anında güncellenir.
  - **`create_or_update_workout_program`**: Kullanıcı sohbete yeni bir spor programı veya mevcut programda değişiklik yazdığında Gemini yapılandırılmış günleri, egzersizleri ve kas grubu eşleşmelerini veritabanına kaydeder/günceller.

### 3.3 3D 360° Erkek Kas Haritası
- Özel bir sekmede parmakla **360 derece döndürülebilen ve yakınlaştırılabilen 3D anatomik erkek modeli** yer alır.
- **Dinamik Vurgulama:** O günün aktif programında çalışılacak kas grupları 3D model üzerinde **parlak/canlı** (örn. kil turuncusu veya canlı yaprak yeşili highlight/emissive) görünür; çalışılmayacak diğer kaslar **soluk/mat** tonda kalır.
- Birincil ve ikincil kaslar farklı parlaklık yoğunluğuna sahip olur.
- **Kapsam Kısıtı:** Spor tarafında egzersiz/hareket animasyonu şu aşamada yapılmayacaktır; odak akıcı 360° kamera kontrolü ve veritabanı odaklı doğru mesh aydınlatmasıdır.

### 3.4 Esnek ve İnteraktif Spor Programı Yönetimi
- Birden fazla spor programı oluşturulabilir, aktif program tek dokunuşla değiştirilebilir.
- Günler ve egzersizler hem manuel arayüzden hem de Gemini sohbeti üzerinden güncellenebilir; değişiklikler 3D kas haritasına anında yansır.

### 3.5 Aylık Vücut Ölçümleri (Kural Tabanlı)
- Vücut ölçümleri **her takvim ayında yalnızca 1 kez** eklenebilir (`yearMonth` UNIQUE).
- İlgili ayda kayıt girildiyse yeni kayıt ekleme aksiyonu kilitlenir ve bir sonraki ölçüm tarihine kalan gün sayısı doğa metaforuyla (ay döngüsü / ağaç büyüme halkası) gösterilir.
- Hatalı giriş ihtimaline karşı yalnızca mevcut ayın kaydını *düzenleme* izni sunulabilir; aynı ay içinde ikinci yeni kayıt engellenmelidir.

### 3.6 Hafta Sonu ve Ay Sonu Somut İstatistikler (`StatsMetaphorMapper`)
- Haftalık ve aylık kapanışlarda sayısal veriler gerçek dünya ölçeklerine dönüştürülür:
  - **Yemek Ağırlığı (kg):** *"Bu ay 1.4 el arabası / X çuval / Y kamyon kasası kadar yemek yedin."*
  - **Su Tüketimi (L):** *"Bu hafta 1.5 damacana / bu ay bir küvetin %60'ı kadar su içtin."*
  - **Yakılan Kalori (kcal):** *"Bu enerjiyle yaklaşık X km koşabilirdin."*
- Eşikler ve karşılaştırma birimleri domain katmanında genişletilebilir kurallarla (`StatsMetaphorMapper`) tanımlanır.

---

## 4. Doğa Temalı UI/UX ve Mikro-Animasyonlar
- **Doğal Unsurlar:** Bölücülerde ince ağaç dalı veya kök çizgileri, kartlarda hafif kağıt/toprak dokusu, gökyüzü-ufuk geçişli başlık alanları, yaprak formlu rozetler ve organik ilerleme işaretçileri.
- **Mikro-Animasyonlar (Yalnızca Beslenme ve Su Kısmında):**
  - **Su Ekleme:** Su eklendiğinde bardağın/testinin üzerinde stilize bir bulut belirir, buluttan yağmur damlaları akar ve su seviyesi akışkan bir dalgalanmayla yükselir.
  - **Yemek Ekleme:** Yemeğin `MealEntry` animasyon kategorisine göre kısa, zarif bir doğa kökeni animasyonu tetiklenir — örneğin pilav/tahıl eklendiğinde **çeltik başağından pirinç tanelerinin kaseye dökülmesi**, sebze eklendiğinde **topraktan yaprak filizlenmesi**.

---

## 5. Geliştirme Fazları (Yol Haritası)

- **Faz 0 (Mimari Onay):** Klasör/paket yapısı, Gradle bağımlılık listesi ve tablo/sütun bazlı Veritabanı ER şemasının sunulup onaylanması.
- **Faz 1 (Çekirdek & Veritabanı):** Proje kurulumu, Hilt yapılandırması, Doğa Tasarım Sistemi (`Theme`, `Color`, `Typography`, organik bileşenler) ve eksiksiz Room Entity + DAO + Repository katmanı.
- **Faz 2 (Beslenme & Metabolizma):** BMR/TDEE hesaplama motoru, günlük yemek/protein/su ekleme akışı, yatay makro/mikro ilerleme barları, kalori karşılaştırma barları ve ana dashboard ekranı.
- **Faz 3 (Gemini API & Sohbet):** Güvenli API anahtarı yönetimi, `log_meal` ve `create_or_update_workout_program` Function Calling tanımları, onay/geri alma destekli etkileşimli Sohbet Sekmesi.
- **Faz 4 (Spor Programı & 3D Kas Haritası):** Çoklu spor programı yönetim ekranları ve Android SceneView ile 360° erkek anatomi modeli üzerinde veritabanı bağlantılı parlak/soluk kas vurgulama ekranı.
- **Faz 5 (Aylık Ölçüm & İstatistik Motoru):** Aylık tek kayıt kilitli vücut ölçüm ekranı, `StatsMetaphorMapper` domain motoru ve haftalık/aylık özet ekranları.
- **Faz 6 (Doğa Mikro-Animasyonları & Cilalama):** Buluttan su akma ve çeltikten pirinç dökülme gibi doğa animasyonlarının entegrasyonu, kenar durum (edge-case) testleri ve performans iyileştirmeleri.