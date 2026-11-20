# NOTLAR — Sorular, Kararlar ve Senin Görevlerin

Gece boyunca otomatik ilerlerken çıkan konular. Uyanınca bunlara bakıp beraber karar veririz.

## 👤 Senin Görevlerin (manuel / hesap / API)
- [ ] **APK'yı telefona kur:** GitHub → repo → *Actions* → en son yeşil "Android Build" → altta *Artifacts* → `kok-debug-apk` indir → zip'i aç → `app-debug.apk` kur. (İlk sefer "bilinmeyen kaynak" izni ister.)
- [ ] **Gemini API anahtarı (Faz 3):** https://aistudio.google.com/apikey adresinden ücretsiz anahtar al. Anahtar koda/GitHub'a girmeyecek; uygulama içindeki Ayarlar ekranına yapıştırılacak (şifreli saklanır).
- [ ] **3D erkek anatomi modeli (Faz 4):** Kas grupları ayrı mesh/node olan bir `.glb` lazım (lisansı kişisel kullanıma uygun). Seçince mesh isimlerini `muscle_group.mesh_node_id` ile eşleyeceğiz.
- [ ] **İlk açılışta profil:** Yaş, boy, kilo, cinsiyet, aktivite düzeyini uygulamada gir; hedefler otomatik hesaplanır, sonra elle değiştirebilirsin.
- [ ] (Opsiyonel) Claude ortamına `dl.google.com` erişimi verilirse APK'yı burada da derleyebilirim; şu an derleme sadece GitHub Actions'ta.

## ❓ Sorular (cevap bekleyen)
1. **Faz 0 onayı:** `ARCHITECTURE.md`'deki paket yapısı ve tablo şemasına onay vermen gerekiyordu; sen uyurken varsayılanla devam ettim. Değiştirmek istediğin bir şey var mı?
2. **Uygulama adı / paket adı:** Geçici olarak **"Kök"** ve `dev.kok.app` seçtim. Başka isim ister misin? (Paket adı ilk kurulumdan sonra değişirse uygulama yeniden kurulmalı.)
3. **Cinsiyet:** 3D model erkek, ama BMR formülü cinsiyete göre değişir. Profilde seçim var; sadece erkek mi kalsın?
4. **Gemini yemek kaydı:** SPEC "onay kartı veya doğrudan kayıt + geri al" diyor. Varsayılan olarak **onay kartı** yapacağım. Uygun mu?
5. **Koyu tema:** RULES'ta opsiyonel. Sistem ayarını takip edecek şekilde hazırlıyorum.
6. **TDEE ve antrenman kalorisi:** "Sporla günlük ihtiyaç" = BMR × aktivite çarpanı (çarpan antrenmanı zaten içeriyor). Kaydettiğin antrenmanın kalorisi ayrıca gösteriliyor ama ihtiyaca eklenmiyor (çift sayım olmasın diye). Eklenmesini ister misin?
7. **Program güncellemesini geri alma:** Asistanla oluşturulan *yeni* program geri alınabiliyor; mevcut programı *güncelleme* geri alınamıyor (eski hali saklanmıyor). Gerekirse sürüm geçmişi ekleriz.
8. **Antrenman kaydı detayı:** Şu an süre + yoğunluk (MET) ile kalori tahmini kaydediliyor. Set/tekrar/ağırlık girişini (tablo hazır: `workout_set_log`) ekranda da isteyecek misin?
9. **Alt menü:** 5 sekme sınırı yüzünden Profil, Vücut sekmesinin içine taşındı (Bugün · Asistan · Spor · Vücut · Özet).
10. **Tek modül:** Gradle'da çok modüllü yapı yerine tek modül + katman paketleri seçtim (derleme hızı). Uygun mu?

## 🧭 Alınan Kararlar (otomatik)
- Tarihler Türkiye saatine göre cihaz yerel saatiyle (`LocalDate.now()`) hesaplanır.
- Sabit debug imza anahtarı repoda (`app/keystore/debug.keystore`): her yeni APK eskisinin **üstüne kurulur, veriler silinmez**.
- Mikro besin hedefleri varsayılan olarak yetişkin erkek RDA değerleri (lif 38 g, sodyum ≤2300 mg, potasyum 3400 mg, demir 8 mg, C vit. 90 mg, D vit. 15 mcg).
