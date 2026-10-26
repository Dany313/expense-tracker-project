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
          : 'Errore nella comunicazione con expense-agent-service.'
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
        `⏳ Scontrino accodato su RabbitMQ (Job ID: ${job.jobId}). Analisi Gemini Vision in corso...`
      );

      // Polling dello stato del job OCR fino al completamento o fallimento
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
                `✅ Scontrino salvato! Negozio: ${ext.storeName} — Importo: ${Number(ext.amount).toFixed(2)} € — Data scontrino: ${ext.expenseDate}`
              );
            } else {
              setSuccessMsg(`✅ Scontrino elaborato e salvato con successo!`);
            }
            return;
          }
          if (statusRes.status === 'FAILED_DLQ') {
            setSuccessMsg(null);
            setErrorMsg(`Errore OCR Scontrino: ${statusRes.message}`);
            return;
          }
        } catch {
          // Continua il polling in caso di errore di rete transitorio
        }
      }

      await onRefreshExpenses();
      setSuccessMsg(
        `Scontrino in coda su RabbitMQ (Job ID: ${job.jobId}). L'elaborazione sta proseguendo in background.`
      );
    } catch (err) {
      setErrorMsg(
        err instanceof ApiError
          ? err.message
          : 'Errore durante l’invio dello scontrino a receipt-ocr-service.'
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="page-header-block">
        <div>
          <h1 className="page-title">Assistente AI & Scansione Scontrini</h1>
          <p className="page-description">
            Interagisci con Gemini + MCP oppure carica la foto di uno scontrino sulla coda RabbitMQ
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
        <section className="surface-card" aria-labelledby="ai-chat-heading">
          <h2 id="ai-chat-heading" className="surface-card-title">
            Comandi in Linguaggio Naturale (Gemini MCP)
          </h2>
          <p className="page-description">
            Scrivi una frase come faresti su Telegram (es.{' '}
            <em>"Ho speso 24,90 euro oggi da Conad per la spesa alimentare"</em>).
          </p>

          <form ref={chatFormRef} className="form-stack" onSubmit={handleAgentSubmit}>
            <div className="field-group">
              <label htmlFor="agent-prompt" className="field-label">
                Messaggio per l’Agente AI *
              </label>
              <textarea
                id="agent-prompt"
                rows={3}
                className="input-control"
                placeholder="Es. Registra 18,50 euro spesi ieri da Tigotà per igiene casa..."
                required
                value={agentPrompt}
                onChange={(e) => setAgentPrompt(e.target.value)}
              />
            </div>

            <div className="row-actions">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? 'Elaborazione Gemini...' : 'Invia a Gemini'}
              </button>
            </div>
          </form>

          {agentReply && (
            <div className="status-banner status-banner-success" role="status">
              <span>
                <strong>Risposta Gemini:</strong> {agentReply}
              </span>
            </div>
          )}
        </section>

        <section className="surface-card" aria-labelledby="ocr-upload-heading">
          <h2 id="ocr-upload-heading" className="surface-card-title">
            Scansione Foto Scontrino (OCR + Coda RabbitMQ)
          </h2>
          <p className="page-description">
            Carica la foto dello scontrino: verrà accodata su{' '}
            <code>receipt.ocr.job.queue</code> e salvata automaticamente su PostgreSQL.
          </p>

          <form className="form-stack" onSubmit={handleReceiptUpload}>
            <label htmlFor="receipt-file" className="upload-dropzone">
              <strong>
                {selectedFile
                  ? `File selezionato: ${selectedFile.name}`
                  : 'Seleziona o scatta la foto dello scontrino'}
              </strong>
              <span className="field-hint">Formati supportati: JPEG, PNG, WEBP, HEIC</span>
              <input
                id="receipt-file"
                type="file"
                accept="image/*"
                className="input-control"
                onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
              />
            </label>

            <div className="row-actions">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={!selectedFile || busy}
              >
                {busy ? 'Accodamento...' : 'Estrai e Salva Scontrino'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </>
  );
}
