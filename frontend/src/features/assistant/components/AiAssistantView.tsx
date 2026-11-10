import React, { useState } from 'react';
import { ApiError } from '../../../core/api/httpClient';
import { StatusBanner } from '../../../shared/components/StatusBanner';
import { useFormValidity } from '../../../shared/hooks/useFormValidity';
import { assistantApi } from '../api/assistantApi';

interface AiAssistantViewProps {
  onRefreshExpenses: () => Promise<void>;
}

export function AiAssistantView({ onRefreshExpenses }: AiAssistantViewProps) {
  const [agentPrompt, setAgentPrompt] = useState<string>('');
  const [agentReply, setAgentReply] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [busy, setBusy] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const chatFormRef = useFormValidity<HTMLFormElement>();

  const handleAgentSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!agentPrompt.trim()) return;
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const response = await assistantApi.sendAgentTextCommand(agentPrompt.trim());
      setAgentReply(response.agentResponse);
      setAgentPrompt('');
      await onRefreshExpenses();
    } catch (err) {
      setErrorMsg(
        err instanceof ApiError
          ? err.message
          : 'Errore nella comunicazione con il servizio AI.'
      );
    } finally {
      setBusy(false);
    }
  };

  const handleReceiptUpload = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedFile) return;
    setBusy(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const job = await assistantApi.enqueueReceiptFile(selectedFile, true);
      setSelectedFile(null);
      setSuccessMsg(
        `⏳ Scontrino accodato su RabbitMQ (Job ID: ${job.jobId.slice(0, 8)}...). Analisi Gemini Vision in corso...`
      );

      // Polling dello stato del job OCR
      for (let attempt = 0; attempt < 25; attempt++) {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        try {
          const statusRes = await assistantApi.getOcrJobStatus(job.jobId);
          if (statusRes.status === 'COMPLETED') {
            await new Promise((resolve) => setTimeout(resolve, 700));
            await onRefreshExpenses();
            const ext = statusRes.extractedExpense;
            if (ext) {
              setSuccessMsg(
                `✅ Scontrino salvato! Negozio: ${ext.storeName} — Importo: ${Number(ext.amount).toFixed(2)} € — Data: ${ext.expenseDate}`
              );
            } else {
              setSuccessMsg(`✅ Scontrino elaborato e salvato con successo!`);
            }
            return;
          }
          if (statusRes.status === 'FAILED_DLQ') {
            setSuccessMsg(null);
            setErrorMsg(`Errore elaborazione scontrino: ${statusRes.message}`);
            return;
          }
        } catch {
          // Ignora errori di rete transitori durante il polling
        }
      }

      await onRefreshExpenses();
      setSuccessMsg(
        `Scontrino in coda su RabbitMQ. L'elaborazione sta proseguendo in background.`
      );
    } catch (err) {
      setErrorMsg(
        err instanceof ApiError
          ? err.message
          : 'Errore durante l’invio dello scontrino.'
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      {/* Header */}
      <div className="page-header-block">
        <div>
          <h1 className="page-title">Assistente AI & Scansione Scontrini</h1>
          <p className="page-description">
            Registra spese in linguaggio naturale con Gemini oppure carica la foto di uno scontrino
          </p>
        </div>
      </div>

      <StatusBanner
        variant="error"
        message={errorMsg}
        onDismiss={() => setErrorMsg(null)}
      />
      <StatusBanner
        variant="success"
        message={successMsg}
        onDismiss={() => setSuccessMsg(null)}
      />

      <div className="dashboard-split-grid">
        {/* Comandi in Linguaggio Naturale */}
        <section className="surface-card" aria-labelledby="ai-chat-heading">
          <h2 id="ai-chat-heading" className="surface-card-title">
            Comandi Vocali / Testuali
          </h2>
          <p className="page-description">
            Scrivi liberamente come faresti con un messaggio (es.{' '}
            <em>"Ho speso 24,90 euro oggi da Conad per la spesa alimentare"</em>).
          </p>

          <form ref={chatFormRef} className="form-stack" onSubmit={handleAgentSubmit}>
            <div className="field-group">
              <label htmlFor="agent-prompt" className="field-label">
                Testo del messaggio
              </label>
              <textarea
                id="agent-prompt"
                rows={3}
                className="input-control"
                placeholder="es. Pagato 18,50 euro da Tigotà per detersivi e igiene casa..."
                required
                value={agentPrompt}
                onChange={(e) => setAgentPrompt(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? 'Elaborazione in corso...' : 'Invia a Gemini'}
              </button>
            </div>
          </form>

          {agentReply && (
            <div
              style={{
                marginTop: '1rem',
                padding: '1rem',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-accent-soft)',
                border: '1px solid var(--color-accent-border)',
                color: 'var(--text-primary)',
                fontSize: '0.9rem',
                lineHeight: 1.5,
              }}
            >
              <strong style={{ color: 'var(--color-accent)', display: 'block', marginBottom: '0.25rem' }}>
                🤖 Risposta dell'Assistente:
              </strong>
              {agentReply}
            </div>
          )}
        </section>

        {/* Scansione Foto Scontrino */}
        <section className="surface-card" aria-labelledby="ocr-upload-heading">
          <h2 id="ocr-upload-heading" className="surface-card-title">
            Scansione Foto Scontrino
          </h2>
          <p className="page-description">
            Carica o scatta una foto: i dati verranno estratti automaticamente e salvati su PostgreSQL.
          </p>

          <form className="form-stack" onSubmit={handleReceiptUpload}>
            <label htmlFor="receipt-file" className="upload-dropzone">
              <span style={{ fontSize: '2rem' }} aria-hidden="true">
                📷
              </span>
              <strong style={{ fontSize: '0.95rem' }}>
                {selectedFile
                  ? `Selezionato: ${selectedFile.name}`
                  : 'Tocca per scattare o scegliere la foto dello scontrino'}
              </strong>
              <span className="field-hint">Supporta JPEG, PNG, WEBP o HEIC</span>
              <input
                id="receipt-file"
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
              />
            </label>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={!selectedFile || busy}
              >
                {busy ? 'Analisi in coda...' : 'Estrai e Salva'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </>
  );
}
