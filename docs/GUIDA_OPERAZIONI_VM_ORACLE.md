# Registro Tecnico Completo: Operazioni e Comandi Eseguiti sulla VM Oracle Cloud

Questo documento raccoglie in ordine cronologico **tutte le operazioni effettuate** sulla Virtual Machine Oracle Cloud (`84.8.253.31`, architettura `aarch64` / ARM64 Ubuntu) e sul repository del progetto, spiegando nel dettaglio **ogni singolo comando inviato**, i parametri utilizzati e il motivo architetturale di ogni scelta.

---

## Indice
1. [Preparazione della Chiave SSH e Primo Accesso](#1-preparazione-della-chiave-ssh-e-primo-accesso)
2. [Aggiornamento Sistema Operativo e Pacchetti Base](#2-aggiornamento-sistema-operativo-e-pacchetti-base)
3. [Installazione del Cluster Kubernetes (k3s)](#3-installazione-del-cluster-kubernetes-k3s)
4. [Installazione di ArgoCD (Motore GitOps)](#4-installazione-di-argocd-motore-gitops)
5. [Installazione di Cloudflare Tunnel (cloudflared)](#5-installazione-di-cloudflare-tunnel-cloudflared)
6. [Creazione della Memoria di Swap (2 GB)](#6-creazione-della-memoria-di-swap-2-gb)
7. [Gestione Sicura dei Segreti Kubernetes (Zero Secrets in Git)](#7-gestione-sicura-dei-segreti-kubernetes-zero-secrets-in-git)
8. [Pipeline CI/CD Multi-Architettura e GitHub Container Registry](#8-pipeline-cicd-multi-architettura-e-github-container-registry)
9. [Attivazione dell'Applicazione ArgoCD e Ottimizzazione per 1 OCPU](#9-attivazione-dellapplicazione-argocd-e-ottimizzazione-per-1-ocpu)
10. [Comandi Utili di Ispezione e Manutenzione Quotidiani](#10-comandi-utili-di-ispezione-e-manutenzione-quotidiani)

---

## 1. Preparazione della Chiave SSH e Primo Accesso

### 1.1 Messa in sicurezza dei permessi della chiave privata (sul tuo Mac)
```bash
chmod 600 /Users/daniele/Downloads/ssh-key-2026-10-26.key
mkdir -p ~/.ssh
cp /Users/daniele/Downloads/ssh-key-2026-10-26.key ~/.ssh/oracle_expense_vm.key
chmod 600 ~/.ssh/oracle_expense_vm.key
```
* **Cosa fa**:
  * `chmod 600`: imposta i permessi del file in modo che **solo il tuo utente proprietario** possa leggerlo e scriverlo (`-rw-------`). Il client OpenSSH rifiuta per sicurezza qualsiasi chiave privata che sia leggibile da altri utenti del sistema (errore `UNPROTECTED PRIVATE KEY FILE`).
  * `cp ... ~/.ssh/oracle_expense_vm.key`: copia la chiave nella cartella standard `~/.ssh/` così che non vada persa se in futuro svuoti la cartella `Downloads`.

### 1.2 Test di connessione e verifica hardware della VM
```bash
ssh -i ~/.ssh/oracle_expense_vm.key -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 ubuntu@84.8.253.31 "uname -a && free -h && df -h /"
```
* **Cosa fa**:
  * `-i ~/.ssh/oracle_expense_vm.key`: specifica quale chiave privata usare per autenticarsi.
  * `-o StrictHostKeyChecking=accept-new`: accetta automaticamente l'impronta digitale (fingerprint ED25519) del server al primo collegamento salvandola in `~/.ssh/known_hosts`, senza bloccare il terminale con la richiesta interattiva `Are you sure you want to continue connecting (yes/no)?`.
  * `uname -a`: mostra il kernel Linux (`6.17.0-1020-oracle`) e l'architettura del processore (`aarch64` = ARM 64-bit).
  * `free -h`: mostra la RAM totale e libera in formato leggibile (`human-readable`).
  * `df -h /`: mostra lo spazio disco totale e disponibile sulla partizione principale `/` (`45 GB` totali, `42 GB` liberi).

---

## 2. Aggiornamento Sistema Operativo e Pacchetti Base

Sulla VM sono stati eseguiti i seguenti comandi tramite lo script [`scripts/setup-oracle-vm.sh`](file:///Users/daniele/Dev/expense-tracker-project/scripts/setup-oracle-vm.sh):

```bash
apt-get update -qq
apt-get install -y -qq curl wget git jq ufw
```
* **Cosa fa**:
  * `apt-get update -qq`: scarica la lista aggiornata dei pacchetti dai repository ufficiali Ubuntu in modalità silenziosa (`-qq` = *quiet*).
  * `apt-get install -y -qq ...`: installa senza chiedere conferma (`-y`) gli strumenti di utilità essenziali:
    * `curl` e `wget`: per scaricare gli installer di k3s, ArgoCD e cloudflared.
    * `git`: per operazioni sui repository.
    * `jq`: per formattare e interrogare risposte JSON da terminale.
    * `ufw`: gestore semplificato del firewall Linux.

---

## 3. Installazione del Cluster Kubernetes (k3s)

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644" sh -
```
* **Cosa fa**:
  * Scarica ed esegue lo script ufficiale di installazione di **k3s** (la distribuzione Kubernetes leggera certificata CNCF creata da Rancher).
  * `INSTALL_K3S_EXEC="--write-kubeconfig-mode 644"`: istruisce k3s a creare il file di configurazione del cluster (`/etc/rancher/k3s/k3s.yaml`) con permessi di lettura `644`, permettendo a `kubectl` di leggere la configurazione senza problemi di permessi.
  * Registra e avvia automaticamente il servizio di sistema `k3s.service` tramite `systemd` (così se la VM viene riavviata, Kubernetes riparte da solo in automatico).
  * Crea automaticamente i link simbolici `/usr/local/bin/kubectl`, `crictl` e `ctr`.

---

## 4. Installazione di ArgoCD (Motore GitOps)

### 4.1 Creazione del Namespace `argocd`
```bash
sudo k3s kubectl create namespace argocd --dry-run=client -o yaml | sudo k3s kubectl apply -f -
```
* **Cosa fa**:
  * Crea il namespace isolato `argocd` in modo **idempotente**: se il namespace non esiste lo crea, se esiste già non restituisce errore (grazie alla combinazione `--dry-run=client -o yaml | kubectl apply -f -`).

### 4.2 Applicazione dei Manifest di ArgoCD (Server-Side Apply)
```bash
sudo k3s kubectl apply --server-side --force-conflicts -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
* **Cosa fa**:
  * Scarica e installa tutti i componenti ufficiali di **ArgoCD** (Application Controller, Repo Server, API Server, Redis, Dex, ApplicationSet Controller).
* **Perché `--server-side --force-conflicts`**:
  * Con un normale `kubectl apply` (client-side), Kubernetes salva l'intero manifesto precedente in un'annotazione chiamata `kubectl.kubernetes.io/last-applied-configuration`, che ha un limite massimo di **262.144 byte (256 KB)**.
  * La CustomResourceDefinition `applicationsets.argoproj.io` supera i 256 KB, causando l'errore `Too long: may not be more than 262144 bytes`.
  * Il flag `--server-side` delega la gestione dei campi direttamente all'API Server di Kubernetes, superando il limite dei 256 KB senza creare annotazioni giganti.

---

## 5. Installazione di Cloudflare Tunnel (`cloudflared`)

```bash
curl -L --output /tmp/cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb
sudo dpkg -i /tmp/cloudflared.deb
rm -f /tmp/cloudflared.deb
cloudflared --version
```
* **Cosa fa**:
  * `curl -L --output ...`: scarica il pacchetto Debian ufficiale compilato per architettura **ARM64** (`cloudflared-linux-arm64.deb`) dall'ultima release GitHub di Cloudflare.
  * `sudo dpkg -i /tmp/cloudflared.deb`: installa il binario `cloudflared` nel sistema (`/usr/bin/cloudflared`).
  * `rm -f /tmp/cloudflared.deb`: pulisce il file temporaneo di installazione.
  * `cloudflared --version`: verifica la corretta installazione (`version 2026.10.4`).

---

## 6. Creazione della Memoria di Swap (2 GB)

Le VM Cloud Ubuntu nascono con `0 B` di Swap. Per proteggere il sistema da eventuali picchi di memoria durante il boot simultaneo di 4 JVM + k3s + ArgoCD, è stato creato un file di Swap da **2 GB**:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```
* **Cosa fa**:
  * `fallocate -l 2G /swapfile`: pre-alloca istantaneamente un file contiguo da 2 Gigabyte su disco.
  * `chmod 600 /swapfile`: impedisce la lettura della memoria di swap a utenti non-root.
  * `mkswap /swapfile`: formatta il file come area di swap Linux.
  * `swapon /swapfile`: attiva immediatamente i 2 GB di swap nel kernel senza dover riavviare.
  * `echo '/swapfile ...' | sudo tee -a /etc/fstab`: aggiunge la riga in `/etc/fstab` affinché lo swap rimanga attivo anche dopo ogni futuro riavvio della VM.

---

## 7. Gestione Sicura dei Segreti Kubernetes (Zero Secrets in Git)

Per evitare di caricare password e chiavi API (Neon, CloudAMQP, Gemini, Telegram) sul repository pubblico GitHub, il `Secret` Kubernetes è stato iniettato **direttamente nel cluster k3s tramite tunnel SSH** e poi rimosso dai file tracciati da Git:

```bash
cat k8s/base/config-and-secrets.yaml | ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl apply -f -"
rm -f k8s/base/config-and-secrets.yaml
```
* **Cosa fa**:
  * Legge il file locale contenente il `Secret` `expense-tracker-secrets` e lo invia attraverso lo standard input (`stdin`) crittografato di SSH direttamente al comando `k3s kubectl apply -f -` sulla VM.
  * In questo modo le password risiedono **unicamente dentro l'etcd del tuo cluster k3s** sulla VM Oracle, mentre su GitHub è stato committato solo [`k8s/base/configmap.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/base/configmap.yaml) (che contiene solo gli URL non sensibili).

---

## 8. Pipeline CI/CD Multi-Architettura e GitHub Container Registry

Nei 4 `Dockerfile` dei microservizi è stata applicata un'ottimizzazione fondamentale per il build multi-architettura:

```dockerfile
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-noble AS builder
```
* **Perché è fondamentale**:
  * Il codice Java compilato (`.jar`) è bytecode indipendente dall'architettura hardware (gira identico su x86 e su ARM64).
  * Senza `--platform=$BUILDPLATFORM`, GitHub Actions dovrebbe emulare un intero processore ARM64 con QEMU durante la compilazione Maven, impiegando ~15 minuti.
  * Con `--platform=$BUILDPLATFORM`, Maven compila alla massima velocità nativa del runner e solo lo stage finale (`FROM eclipse-temurin:25-jre-noble`) viene impacchettato per `linux/arm64` e `linux/amd64`, riducendo il tempo di build a ~1 minuto!

---

## 9. Attivazione dell'Applicazione ArgoCD e Ottimizzazione per 1 OCPU

### 9.1 Registrazione dell'Application su ArgoCD
```bash
cat k8s/argocd-application.yaml | ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl apply -f -"
```
* **Cosa fa**:
  * Crea la risorsa `Application` di ArgoCD ([`k8s/argocd-application.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/argocd-application.yaml)).
  * Da questo istante, ArgoCD monitora costantemente il repository `https://github.com/Dany313/expense-tracker-project.git` (percorso `k8s/base`) con `selfHeal: true` e `prune: true`: qualsiasi modifica committata su GitHub viene applicata automaticamente sul cluster.

### 9.2 Ottimizzazione Strategia di Deploy (`Recreate`) e Probe di Avvio
Durante il primo avvio simultaneo di tutti i 4 microservizi su 1 sola OCPU:
1. L'avvio simultaneo di 4 JVM Spring Boot 4 / Java 25 richiede circa **60-90 secondi** di CPU.
2. Con la strategia predefinita `RollingUpdate`, durante un aggiornamento Kubernetes avvia 4 nuovi pod mentre tiene ancora accesi i 4 vecchi pod (8 JVM contemporanee su 1 OCPU), rallentando la CPU e facendo scattare la `livenessProbe` prima della fine del boot.

Per risolvere definitivamente questo comportamento su macchine a 1 OCPU, nei 4 file Deployment ([`api-gateway.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/base/api-gateway.yaml), [`expense-service.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/base/expense-service.yaml), [`receipt-ocr-service.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/base/receipt-ocr-service.yaml), [`expense-agent-service.yaml`](file:///Users/daniele/Dev/expense-tracker-project/k8s/base/expense-agent-service.yaml)) sono stati configurati:
* **`strategy: type: Recreate`**: termina sempre il vecchio pod prima di avviare il nuovo, garantendo che non ci siano mai più di 4 JVM attive contemporaneamente.
* **`livenessProbe.initialDelaySeconds: 120`**: concede fino a 2 minuti di tempo al primo avvio a freddo prima di iniziare i controlli di riavvio.
* **`limits.cpu: "1000m"`**: permette a ciascun microservizio di utilizzare il 100% della OCPU durante la fase di boot iniziale.

E per forzare ArgoCD a sincronizzare immediatamente senza attendere i 3 minuti di polling standard:
```bash
sudo k3s kubectl annotate application expense-tracker -n argocd argocd.argoproj.io/refresh=hard --overwrite
```

---

## 10. Comandi Utili di Ispezione e Manutenzione Quotidiani

Ecco i comandi che puoi eseguire in qualsiasi momento dal tuo Mac per controllare la tua infrastruttura:

### Collegarsi in SSH alla VM Oracle
```bash
ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31
```

### Controllare lo stato dei Pod e di ArgoCD
```bash
ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl get pods -n expense-tracker && sudo k3s kubectl get applications -n argocd"
```

### Leggere i log in tempo reale di un microservizio
```bash
# Log di expense-service
ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl logs -f -n expense-tracker deployment/expense-service"

# Log di api-gateway
ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl logs -f -n expense-tracker deployment/api-gateway"
```

### Recuperare la password di admin della Web UI di ArgoCD
```bash
ssh -i ~/.ssh/oracle_expense_vm.key ubuntu@84.8.253.31 "sudo k3s kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d && echo"
```

### Aprire la Web UI di ArgoCD sul tuo Mac (Port-Forward sicuro via SSH)
Se vuoi vedere la dashboard grafica di ArgoCD sul tuo Mac senza esporla su internet:
```bash
ssh -i ~/.ssh/oracle_expense_vm.key -L 8443:localhost:8443 ubuntu@84.8.253.31 "sudo k3s kubectl port-forward svc/argocd-server -n argocd 8443:443"
```
Poi apri nel browser **[https://localhost:8443](https://localhost:8443)** (utente: `admin`, password: quella ottenuta col comando sopra).
