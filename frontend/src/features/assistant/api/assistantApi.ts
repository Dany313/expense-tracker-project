import { httpRequest } from '../../../core/api/httpClient';

export interface OcrExtractedExpense {
  expenseDate: string;
  storeName: string;
  amount: number;
  category: string;
  subcategory?: string | null;
  description?: string | null;
  recurring: boolean;
}

export interface OcrJobResponse {
  jobId: string;
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED_DLQ';
  autoSaveExpense: boolean;
  extractedExpense?: OcrExtractedExpense | null;
  message: string;
}

export interface AgentChatResponse {
  inputType: 'TEXT' | 'VOICE';
  processedText: string;
  agentResponse: string;
  timestamp: string;
}

export const assistantApi = {
  async enqueueReceiptFile(file: File, autoSaveExpense = true): Promise<OcrJobResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return httpRequest<OcrJobResponse>(
      `/api/v1/ocr/receipts/enqueue?autoSaveExpense=${String(autoSaveExpense)}`,
      {
        method: 'POST',
        body: formData,
      }
    );
  },

  async getOcrJobStatus(jobId: string): Promise<OcrJobResponse> {
    return httpRequest<OcrJobResponse>(`/api/v1/ocr/receipts/jobs/${encodeURIComponent(jobId)}`);
  },

  async sendAgentTextCommand(text: string): Promise<AgentChatResponse> {
    return httpRequest<AgentChatResponse>('/api/v1/agent/text', {
      method: 'POST',
      body: JSON.stringify({ text }),
    });
  },
};
