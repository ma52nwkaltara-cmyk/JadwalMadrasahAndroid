# Absensi RDMKU Android — Satu APK

Aplikasi Android WebView untuk membuka `https://absensi.rdmku.cloud/index.html`.

## Fitur
- Satu APK release.
- Izin lokasi Android dan dukungan geolokasi WebView untuk domain absensi resmi.
- Build otomatis dengan GitHub Actions.
- Nama aplikasi: Absensi RDMKU.

## Build otomatis
1. Ekstrak ZIP ini.
2. Unggah seluruh isi folder ke root repositori GitHub, termasuk folder `.github`.
3. Buka Actions dan jalankan workflow **Build APK Absensi RDMKU**.
4. Unduh artifact `Absensi-RDMKU-APK` setelah workflow berhasil.

## Membuat APK kedua dengan ID berbeda
Untuk menghasilkan aplikasi kedua, ubah nilai `applicationId` di `app/build.gradle.kts`, misalnya dari `id.nwkalutara.absensi` menjadi `id.nwkalutara.absensi.kedua`, lalu commit dan jalankan build kembali. APK dengan ID berbeda dapat dipasang berdampingan. Nama APK saja tidak cukup; yang harus berbeda adalah applicationId.

## Catatan
Izin Android tidak menjamin lokasi selalu tersedia. Pengguna harus mengaktifkan Lokasi/GPS dan memberi izin kepada aplikasi. Halaman situs juga harus meminta geolokasi melalui HTTPS dan mengizinkan fitur itu di sisi server.
