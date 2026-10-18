export const AUTH_STYLES = `
  .auth-page {
    min-height: calc(100vh - 200px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    background: radial-gradient(circle at 50% 20%, #eff6ff 0%, #f8fafc 100%);
  }
  .auth-card {
    width: 100%;
    max-width: 480px;
    padding: 36px;
    box-shadow: 0 20px 45px -10px rgba(29, 78, 216, 0.15);
  }
  .auth-card.wide { max-width: 640px; }
  .auth-logo-center { text-align: center; margin-bottom: 24px; }
  .auth-icon {
    width: 50px; height: 50px;
    background: linear-gradient(135deg, #1d4ed8 0%, #0284c7 100%);
    color: white; border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    margin: 0 auto 12px;
    box-shadow: 0 6px 16px rgba(29, 78, 216, 0.3);
  }
  .auth-logo-center h2 { font-size: 1.5rem; color: #0f172a; }
  .auth-sub { font-size: 0.85rem; color: #64748b; margin-top: 4px; }
  .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .btn-block { width: 100%; margin-top: 10px; }
  .auth-links { display: flex; justify-content: space-between; margin-top: 18px; font-size: 0.85rem; }
  .auth-links a { color: #1d4ed8; font-weight: 600; text-decoration: none; }
  .auth-links a:hover { text-decoration: underline; }
  .alert-danger, .alert-success {
    padding: 12px; border-radius: 8px; font-size: 0.85rem; margin-top: 16px;
  }
  .alert-danger { background: #fee2e2; color: #b91c1c; }
  .alert-success { background: #dcfce7; color: #166534; }
  .field-error { color: #b91c1c; font-size: 0.78rem; margin-top: 4px; }
  .pwd-rules { list-style: none; padding: 0; margin: 8px 0 0; font-size: 0.78rem; }
  .pwd-rules li { color: #94a3b8; }
  .pwd-rules li.ok { color: #16a34a; }
  .pwd-rules li::before { content: '○ '; }
  .pwd-rules li.ok::before { content: '✔ '; }
`;

export interface PasswordCheck {
  label: string;
  ok: boolean;
}

export function passwordChecks(pwd: string): PasswordCheck[] {
  return [
    { label: '8 à 64 caractères', ok: pwd.length >= 8 && pwd.length <= 64 },
    { label: 'Une majuscule', ok: /[A-Z]/.test(pwd) },
    { label: 'Une minuscule', ok: /[a-z]/.test(pwd) },
    { label: 'Un chiffre', ok: /\d/.test(pwd) },
    { label: 'Un caractère spécial', ok: /[^A-Za-z0-9\s]/.test(pwd) },
    { label: 'Sans espace', ok: pwd.length > 0 && !/\s/.test(pwd) }
  ];
}

export function isPasswordValid(pwd: string): boolean {
  return passwordChecks(pwd).every(c => c.ok);
}

export function apiErrorMessage(err: any, fallback = 'Une erreur est survenue. Veuillez réessayer.'): string {
  if (err?.status === 0) {
    return 'Serveur injoignable. Vérifiez que le backend est démarré.';
  }
  return err?.error?.message || fallback;
}
