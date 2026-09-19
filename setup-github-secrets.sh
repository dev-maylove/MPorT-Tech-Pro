#!/data/data/com.termux/files/usr/bin/bash

cd ~/MPorT-Tech-Pro || exit 1

set -e

echo "=============================================="
echo " MPorT Tech Pro - GitHub Secrets Setup"
echo "=============================================="
echo
echo "⚠️ MODE AMAN:"
echo "✓ Tidak mengubah Workflow"
echo "✓ Tidak mengubah app/build.gradle.kts"
echo "✓ Tidak mengubah file proyek"
echo

# ==============================================
# KONFIGURASI
# ==============================================

KEYSTORE_FILE="app/release-keystore.jks"
KEY_ALIAS="mport-release"
STORE_TYPE="JKS"

# ==============================================
# CEK DEPENDENSI
# ==============================================

command -v gh >/dev/null 2>&1 || {
    echo "❌ GitHub CLI belum terinstall."
    echo
    echo "Install dengan:"
    echo "pkg install gh -y"
    exit 1
}

command -v keytool >/dev/null 2>&1 || {
    echo "❌ keytool tidak ditemukan."
    echo
    echo "Install dengan:"
    echo "pkg install openjdk-21 -y"
    exit 1
}

# ==============================================
# CEK PROJECT & KEYSTORE
# ==============================================

if [ ! -d ".git" ]; then
    echo "❌ Folder ini bukan Git repository!"
    exit 1
fi

if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ Keystore tidak ditemukan:"
    echo "$(pwd)/$KEYSTORE_FILE"
    exit 1
fi

echo "✅ Git repository ditemukan"
echo "✅ Keystore ditemukan:"
echo "   $(pwd)/$KEYSTORE_FILE"
echo

# ==============================================
# INPUT PASSWORD
# ==============================================

read -s -p "Masukkan KEYSTORE PASSWORD: " STORE_PASSWORD
echo

read -s -p "Masukkan KEY PASSWORD (ENTER jika sama): " KEY_PASSWORD
echo

if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$STORE_PASSWORD"
fi

echo

# ==============================================
# VALIDASI KEYSTORE
# ==============================================

echo "🔍 Memvalidasi keystore..."

if ! keytool -list \
    -keystore "$KEYSTORE_FILE" \
    -storetype "$STORE_TYPE" \
    -storepass "$STORE_PASSWORD" \
    -alias "$KEY_ALIAS" >/dev/null 2>&1
then
    echo
    echo "❌ VALIDASI GAGAL!"
    echo
    echo "Periksa:"
    echo "- Password keystore"
    echo "- Alias: $KEY_ALIAS"
    echo "- File: $KEYSTORE_FILE"
    echo "- Tipe: $STORE_TYPE"
    echo
    unset STORE_PASSWORD KEY_PASSWORD
    exit 1
fi

echo "✅ Keystore valid"
echo "✅ Alias valid: $KEY_ALIAS"

# ==============================================
# LOGIN GITHUB
# ==============================================

echo
echo "🔗 Mengecek GitHub login..."

if ! gh auth status >/dev/null 2>&1; then
    echo
    echo "GitHub CLI belum login."
    echo "Silakan login..."
    gh auth login
fi

echo "✅ GitHub CLI siap"

# ==============================================
# DETEKSI REPOSITORY
# ==============================================

REPO=$(gh repo view \
    --json nameWithOwner \
    -q '.nameWithOwner')

if [ -z "$REPO" ]; then
    echo "❌ Gagal mendeteksi repository GitHub!"
    unset STORE_PASSWORD KEY_PASSWORD
    exit 1
fi

echo
echo "📦 Repository:"
echo "$REPO"

# ==============================================
# KONVERSI KEYSTORE KE BASE64
# ==============================================

echo
echo "🔐 Menyiapkan KEYSTORE_BASE64..."

KEYSTORE_BASE64=$(
    base64 "$KEYSTORE_FILE" | tr -d '\n'
)

if [ -z "$KEYSTORE_BASE64" ]; then
    echo "❌ Gagal mengubah keystore ke Base64!"
    unset STORE_PASSWORD KEY_PASSWORD
    exit 1
fi

echo "✅ Base64 berhasil dibuat"

# ==============================================
# KONFIRMASI
# ==============================================

echo
echo "=============================================="
echo "GITHUB SECRETS YANG AKAN DISET"
echo "=============================================="
echo
echo "Repository: $REPO"
echo
echo "Keystore:"
echo "✓ $KEYSTORE_FILE"
echo "✓ Type: $STORE_TYPE"
echo "✓ Alias: $KEY_ALIAS"
echo
echo "Secrets:"
echo "✓ KEYSTORE_BASE64"
echo "✓ KEYSTORE_PASSWORD"
echo "✓ KEY_ALIAS"
echo "✓ KEY_PASSWORD"
echo

read -p "Lanjut mengatur GitHub Secrets? (y/N): " CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo
    echo "⚠️ Dibatalkan. Tidak ada perubahan."
    unset STORE_PASSWORD KEY_PASSWORD KEYSTORE_BASE64
    exit 0
fi

# ==============================================
# SET GITHUB SECRETS
# ==============================================

echo
echo "🔒 Mengatur GitHub Secrets..."

printf "%s" "$KEYSTORE_BASE64" | \
gh secret set KEYSTORE_BASE64 \
    --repo "$REPO"

printf "%s" "$STORE_PASSWORD" | \
gh secret set KEYSTORE_PASSWORD \
    --repo "$REPO"

printf "%s" "$KEY_ALIAS" | \
gh secret set KEY_ALIAS \
    --repo "$REPO"

printf "%s" "$KEY_PASSWORD" | \
gh secret set KEY_PASSWORD \
    --repo "$REPO"

# ==============================================
# HAPUS DATA SENSITIF DARI VARIABEL
# ==============================================

unset KEYSTORE_BASE64
unset STORE_PASSWORD
unset KEY_PASSWORD

# ==============================================
# VERIFIKASI
# ==============================================

echo
echo "🔍 Memverifikasi GitHub Secrets..."

gh secret list --repo "$REPO"

echo
echo "=============================================="
echo "🎉 GITHUB SECRETS BERHASIL DIATUR!"
echo "=============================================="
echo
echo "Repository: $REPO"
echo
echo "Keystore:"
echo "✓ app/release-keystore.jks"
echo "✓ Type: JKS"
echo "✓ Alias: mport-release"
echo
echo "Secrets:"
echo "✓ KEYSTORE_BASE64"
echo "✓ KEYSTORE_PASSWORD"
echo "✓ KEY_ALIAS"
echo "✓ KEY_PASSWORD"
echo
echo "🛡️ Tidak ada Workflow yang diubah."
echo "🛡️ app/build.gradle.kts tidak diubah."
echo "🛡️ Tidak ada file proyek yang diubah."
echo
echo "=============================================="
echo "SELESAI"
echo "=============================================="
