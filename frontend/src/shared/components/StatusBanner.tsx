interface StatusBannerProps {
  variant: 'error' | 'success';
  message: string | null;
  onDismiss?: () => void;
}

export function StatusBanner({ variant, message, onDismiss }: StatusBannerProps) {
  if (!message) return null;

  return (
    <div
      className={`status-banner ${
        variant === 'error' ? 'status-banner-error' : 'status-banner-success'
      }`}
      role={variant === 'error' ? 'alert' : 'status'}
    >
      <span>{message}</span>
      {onDismiss && (
        <button type="button" className="btn btn-ghost btn-sm" onClick={onDismiss}>
          Chiudi
        </button>
      )}
    </div>
  );
}
