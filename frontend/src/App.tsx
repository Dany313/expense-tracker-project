import { useState } from 'react';
import { useAuth } from './core/auth/useAuth';
import { AppLayout, type AppSection } from './core/layout/AppLayout';
import { MonthlyAnalyticsView } from './features/analytics/components/MonthlyAnalyticsView';
import { AiAssistantView } from './features/assistant/components/AiAssistantView';
import { LoginPage } from './features/auth/components/LoginPage';
import { DashboardView } from './features/dashboard/components/DashboardView';
import { CreateExpenseView } from './features/expenses/components/CreateExpenseView';
import { ExpensesArchiveView } from './features/expenses/components/ExpensesArchiveView';
import { useExpenses } from './features/expenses/hooks/useExpenses';
import { StatusBanner } from './shared/components/StatusBanner';

export function App() {
  const { isCheckingSession, isAuthenticated } = useAuth();
  const [activeSection, setActiveSection] = useState<AppSection>('dashboard');

  const {
    categories,
    filters,
    setFilters,
    resetFilters,
    expenses,
    totalElements,
    totalPages,
    summary,
    loading,
    submitting,
    errorMessage,
    successMessage,
    clearMessages,
    refreshData,
    createExpense,
    replaceExpense,
    deleteExpense,
  } = useExpenses(isAuthenticated);

  if (isCheckingSession) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <p className="page-description">Verifica sessione sul Gateway in corso...</p>
        </div>
      </div>
    );
  }

  // Hard Authentication Gate: blocca chiunque non sia autenticato mostrando esclusivamente la LoginPage
  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <AppLayout
      activeSection={activeSection}
      onSelectSection={(sec) => {
        clearMessages();
        setActiveSection(sec);
      }}
      onRefresh={() => void refreshData(filters)}
      loading={loading}
    >
      <StatusBanner
        variant="error"
        message={errorMessage}
        onDismiss={clearMessages}
      />
      <StatusBanner
        variant="success"
        message={successMessage}
        onDismiss={clearMessages}
      />

      {activeSection === 'dashboard' && (
        <DashboardView
          summary={summary}
          recentExpenses={expenses}
          categories={categories}
          loading={loading}
          onNavigateCreate={() => setActiveSection('create')}
          onNavigateArchive={() => setActiveSection('archive')}
          onNavigateMonthly={() => setActiveSection('monthly')}
        />
      )}

      {activeSection === 'monthly' && (
        <MonthlyAnalyticsView categories={categories} />
      )}

      {activeSection === 'archive' && (
        <ExpensesArchiveView
          expenses={expenses}
          categories={categories}
          filters={filters}
          totalElements={totalElements}
          totalPages={totalPages}
          loading={loading}
          submitting={submitting}
          onChangeFilters={setFilters}
          onResetFilters={resetFilters}
          onReplaceExpense={replaceExpense}
          onDeleteExpense={deleteExpense}
          onNavigateCreate={() => setActiveSection('create')}
        />
      )}

      {activeSection === 'create' && (
        <CreateExpenseView
          categories={categories}
          submitting={submitting}
          onCreateExpense={createExpense}
          onSuccessNavigateArchive={() => setActiveSection('archive')}
        />
      )}

      {activeSection === 'assistant' && (
        <AiAssistantView onRefreshExpenses={() => refreshData(filters)} />
      )}
    </AppLayout>
  );
}
