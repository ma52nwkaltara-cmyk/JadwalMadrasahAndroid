# Absensi RDMKU — Notifikasi dan TTS Latar Belakang

URL WebView: `https://absensi.rdmku.cloud/index.html`

## Yang disiapkan
- WebView dengan dukungan geolokasi Android.
- Notifikasi lokal Android.
- Firebase Cloud Messaging (FCM) untuk notifikasi dari server.
- TTS Android saat notifikasi FCM diterima, jika sistem mengizinkan.
- Pengumuman TTS terjadwal menggunakan AlarmManager untuk alarm yang disimpan oleh pengguna.
- GitHub Actions membangun satu APK debug dan satu APK release sebagai artifact terpisah.

## Penting: FCM memerlukan konfigurasi Firebase
1. Buat proyek di Firebase Console.
2. Tambahkan aplikasi Android dengan package ID `id.nwkalutara.absensi`.
3. Unduh `google-services.json` dan taruh di folder `app/`.
4. Jangan unggah `google-services.json` ke repositori publik; tambahkan ke repositori privat atau gunakan secret/penyimpanan aman.
5. Aktifkan Cloud Messaging.
6. Untuk mengirim pesan, backend harus mengirim FCM melalui Firebase Admin SDK atau HTTP v1 menggunakan kredensial server yang dirahasiakan. Jangan taruh server key di APK.
7. Endpoint/server perlu mengelola token FCM perangkat. Contoh aplikasi ini menampilkan token di Logcat, bukan mengirimnya otomatis ke server.

## Pengumuman terjadwal
- Contoh ini menjadwalkan satu alarm yang dipilih pengguna melalui pemilih waktu.
- Alarm dibuat melalui `AlarmManager.setAndAllowWhileIdle()`, sehingga bukan jaminan menit-presisi dan dapat tertunda karena Doze/penghemat baterai.
- Android 13+ memerlukan izin notifikasi. Android versi tertentu juga dapat membatasi alarm atau eksekusi latar belakang.
- Pengguna harus membuka aplikasi sekali, memberi izin notifikasi/lokasi, dan tidak force-stop aplikasi. Force-stop biasanya mencegah alarm/push diproses sampai aplikasi dibuka lagi.
- Untuk jadwal berulang yang andal, perlu antarmuka pengelolaan jadwal dan pengujian di perangkat sasaran.

## Mengubah ID paket untuk APK kedua
Ubah `applicationId` di `app/build.gradle.kts` dan package namespace/source path secara konsisten bila diperlukan. Dua aplikasi hanya bisa dipasang bersamaan jika ID paket berbeda. Firebase juga perlu didaftarkan untuk setiap ID aplikasi yang dipakai.

## Build
GitHub Actions berjalan saat push ke `main` atau secara manual dari tab Actions. Artifact: `Absensi-RDMKU-APK-Debug` dan `Absensi-RDMKU-APK-Release`.
