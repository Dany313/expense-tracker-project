interface EmptyStateProps {
  icon?: string;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function EmptyState({
  icon = '📂',
  title,
  description,
  action,
}: EmptyStateProps) {
  return (
    <div className="empty-state-box" role="status">
      <span className="empty-state-icon" aria-hidden="true">
        {icon}
      </span>
      <h3 className="empty-state-title">{title}</h3>
      {description && <p className="empty-state-desc">{description}</p>}
      {action && (
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={action.onClick}
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
