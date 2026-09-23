#!/data/data/com.termux/files/usr/bin/bash
# ReDHawK Code — AndroidIDE ↔ Workspace senkronizasyonu
# Kullanım:  ./sync_redhawk.sh [workspace_root]
set -e

PHONE_DIR="/storage/emulated/0/AndroidIDEProjects/ReDHawKCode"
WS_DIR="${1:-$(pwd)}"

if [ ! -d "$PHONE_DIR" ]; then
  echo "HATA: $PHONE_DIR bulunamadi."
  echo "AndroidIDE projenizi bu yolda olusturdugunuzdan emin olun."
  exit 1
fi

# .gitignore dosyalarını hariç tutarak sadecek farklılığı kopyala
rsync -av --delete \
  --exclude='.git/' \
  --exclude='build/' \
  --exclude='.gradle/' \
  --exclude='*.apk' \
  --exclude='*.aab' \
  --exclude='.cxx/' \
  "$WS_DIR/" "$PHONE_DIR/"

echo "SINKRON TAMAMLANDI: $WS_DIR -> $PHONE_DIR"
echo "Dosya sayisi: $(find "$PHONE_DIR" -type f ! -path '*.git/*' ! -path '*/build/*' | wc -l)"
