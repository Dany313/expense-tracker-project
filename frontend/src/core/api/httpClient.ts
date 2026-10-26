import type { ProblemDetailError } from '../../features/expenses/types/expense.types';

const JWT_STORAGE_KEY = 'expense_tracker_jwt';
const UNAUTHORIZED_EVENT = 'expense-tracker:unauthorized';

export class ApiError extends Error {
  public readonly status: number;
  public readonly fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function getStoredJwt(): string | null {
  try {
    return localStorage.getItem(JWT_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredJwt(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(JWT_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(JWT_STORAGE_KEY);
    }
  } catch {
    // Ignore storage errors
  }
}

export function onUnauthorizedEvent(listener: () => void): () => void {
  window.addEventListener(UNAUTHORIZED_EVENT, listener);
  return () => window.removeEventListener(UNAUTHORIZED_EVENT, listener);
}

export async function httpRequest<T>(
  url: string,
  options: RequestInit = {},
  skipUnauthorizedRedirect = false
): Promise<T> {
  const headers = new Headers(options.headers);
  const jwt = getStoredJwt();
  if (jwt && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${jwt}`);
  }
  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has('Content-Type')
  ) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorBody: ProblemDetailError | null = null;
    try {
      errorBody = (await response.json()) as ProblemDetailError;
    } catch {
      // Response is not JSON
    }

    if (response.status === 401 && !skipUnauthorizedRedirect) {
      setStoredJwt(null);
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
    }

    const detail =
      errorBody?.detail ||
      errorBody?.title ||
      `Errore HTTP ${response.status}: ${response.statusText}`;
    throw new ApiError(detail, response.status, errorBody?.errors);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
