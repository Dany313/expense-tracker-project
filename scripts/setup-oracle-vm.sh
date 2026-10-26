#!/usr/bin/env bash
# ==============================================================================
# Script di Setup Automatico per VM Oracle Cloud (Ubuntu / Debian)
# Installa:
#   1. k3s (Lightweight Kubernetes)
#   2. ArgoCD (GitOps Engine)
#   3. cloudflared (Cloudflare Tunnel per accesso sicuro a zero porte aperte)
#
# Esegui sulla VM Oracle come utente root o con sudo:
#   curl -sSL https://raw.githubusercontent.com/Dany313/expense-tracker-project/main/scripts/setup-oracle-vm.sh | sudo bash
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo "🚀 Inizio Configurazione VM Oracle Cloud per Expense Tracker"
echo "=========================================================="

# 1. Aggiornamento pacchetti di base
echo "📦 Aggiornamento pacchetti di sistema..."
apt-get update -qq
apt-get install -y -qq curl wget git jq ufw

# 2. Installazione k3s
echo "☸️ Installazione k3s (Kubernetes)..."
if ! command -v k3s &> /dev/null; then
  curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644" sh -
  echo "Attesa avvio k3s..."
  sleep 15
else
  echo "k3s è già installato."
fi

# Imposta alias comodi se non presenti
if ! grep -q "alias kubectl='k3s kubectl'" /root/.bashrc; then
  echo "alias kubectl='k3s kubectl'" >> /root/.bashrc
  echo "alias k='k3s kubectl'" >> /root/.bashrc
fi

# 3. Installazione ArgoCD
echo "🐙 Installazione ArgoCD nel cluster k3s..."
k3s kubectl create namespace argocd --dry-run=client -o yaml | k3s kubectl apply -f -
k3s kubectl apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 4. Installazione cloudflared (Cloudflare Tunnel)
echo "🛡️ Installazione Cloudflare Tunnel (cloudflared)..."
ARCH=$(dpkg --print-architecture)
if ! command -v cloudflared &> /dev/null; then
  curl -L --output /tmp/cloudflared.deb "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${ARCH}.deb"
  dpkg -i /tmp/cloudflared.deb
  rm -f /tmp/cloudflared.deb
else
  echo "cloudflared è già installato."
fi

echo "=========================================================="
echo "✅ Installazione k3s, ArgoCD e cloudflared completata!"
echo "=========================================================="
echo ""
echo "📋 Prossimi passi da eseguire sulla VM:"
echo "1. Recupera la password iniziale di admin per ArgoCD:"
echo "   k3s kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d && echo"
echo ""
echo "2. Collega il tunnel Cloudflare con il token fornito dalla dashboard Cloudflare Zero Trust:"
echo "   sudo cloudflared service install <IL_TUO_TUNNEL_TOKEN>"
echo ""
