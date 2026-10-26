import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import { AuthProvider } from './core/auth/AuthContext';
import { SplitSharedProvider } from './core/context/SplitSharedContext';
import { ThemeProvider } from './theme/ThemeContext';
import './theme/index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <SplitSharedProvider>
          <App />
        </SplitSharedProvider>
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>
);
