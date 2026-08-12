#!/data/data/com.termux/files/usr/bin/bash
# ZCORE v1.0.0 - Déploiement Rclone Chiffré
# Montage du coffre chiffré crypt_zcore dans ~/ZCORE-Vault

REMOTE_NAME="crypt_zcore"
MOUNT_POINT="$HOME/ZCORE-Vault"
LOG_FILE="$HOME/.rclone_zcore.log"

echo "[ZCORE] Démarrage du montage sécurisé..."

# 1. Créer le dossier si inexistant
mkdir -p "$MOUNT_POINT"

# 2. Démonter si déjà monté
fusermount -u -z "$MOUNT_POINT" 2>/dev/null

# 3. Lancer le mount en arrière plan
rclone mount "$REMOTE_NAME:" "$MOUNT_POINT" \
  --vfs-cache-mode writes \
  --dir-cache-time 5m \
  --poll-interval 1m \
  --log-file "$LOG_FILE" \
  --log-level INFO \
  --daemon

sleep 3

# 4. Vérification du contenu
echo "[OK] Vérification du montage..."
echo "[OK] Point de montage: $MOUNT_POINT"
echo "[OK] Contenu du coffre:"
ls -la "$MOUNT_POINT"

echo ""
echo "[INFO] Logs disponibles: cat $LOG_FILE"
echo "[INFO] Pour démonter: fusermount -u $MOUNT_POINT"
