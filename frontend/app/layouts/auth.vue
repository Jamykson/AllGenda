<template>

<div class="auth">

    <slot />

</div>

</template>


<style>
.auth {
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background:
        radial-gradient(circle at 15% 0%, rgba(147, 197, 253, 0.38), transparent 45%),
        radial-gradient(circle at 85% 0%, rgba(96, 165, 250, 0.28), transparent 45%),
        linear-gradient(180deg, #eff6ff 0%, #ffffff 48%);
}

.auth .auth-page.auth-page {
    display: flex;
    flex: 1 0 auto;
    flex-direction: column;
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
    box-sizing: border-box;
    overflow: visible;
    background: transparent;
    color: #0f172a;
}

.auth .auth-page .auth-shell {
    display: flex;
    flex: 1 0 auto;
    align-items: center;
    justify-content: center;
    width: 100%;
    min-height: 0;
    box-sizing: border-box;
    padding: 28px 24px 44px;
}

.auth .auth-page .auth-content {
    width: min(100%, 400px);
    margin: 0 auto;
}

.auth .auth-page .brand {
    margin: 0 0 16px;
    text-align: center;
}

.auth .auth-page .brand-name {
    margin: 0;
    color: #0f172a;
    font-size: 23px;
    font-weight: 800;
    line-height: 1.2;
    letter-spacing: 0;
}

.auth .auth-page .headline {
    margin: 0 0 20px;
    text-align: center;
}

.auth .auth-page .headline h2 {
    margin: 0;
    color: #1f2937;
    font-size: 21px;
    font-weight: 700;
    line-height: 1.3;
    letter-spacing: 0;
}

.auth .auth-page .headline p {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 13px;
    line-height: 1.45;
}

.auth .auth-page .form {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.auth .auth-page .field-group {
    display: grid;
    gap: 4px;
    margin: 0;
}

.auth .auth-page .field {
    box-sizing: border-box;
    width: 100%;
    height: 42px;
    padding: 0 12px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    outline: none;
    background: rgba(255, 255, 255, 0.97);
    color: #0f172a;
    font: inherit;
    font-size: 14px;
    transition: border-color 150ms ease, box-shadow 150ms ease;
}

.auth .auth-page .field::placeholder {
    color: #94a3b8;
    opacity: 1;
}

.auth .auth-page .field:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.auth .auth-page .field.field-error {
    border-color: #dc2626;
}

.auth .auth-page .password-wrapper {
    position: relative;
    width: 100%;
}

.auth .auth-page .field-password {
    padding-right: 44px;
}

.auth .auth-page .password-toggle {
    position: absolute;
    top: 50%;
    right: 6px;
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    padding: 0;
    transform: translateY(-50%);
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: #64748b;
    cursor: pointer;
    transition: background-color 150ms ease, color 150ms ease;
}

.auth .auth-page .password-toggle svg {
    width: 18px;
    height: 18px;
}

.auth .auth-page .password-toggle:hover {
    background: #eff6ff;
    color: #2563eb;
}

.auth .auth-page .submit-button {
    width: 100%;
    height: 42px;
    border: 1px solid #2563eb;
    border-radius: 8px;
    background: #2563eb;
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 150ms ease, border-color 150ms ease;
}

.auth .auth-page .submit-button:hover:not(:disabled) {
    border-color: #1d4ed8;
    background: #1d4ed8;
}

.auth .auth-page .submit-button:disabled {
    border-color: #e2e8f0;
    background: #e2e8f0;
    color: #475569;
    cursor: not-allowed;
}

.auth .auth-page .forgot-link {
    display: block;
    align-self: center;
    margin-top: 4px;
    color: #2563eb;
    font-size: 13px;
    line-height: 1.4;
    text-align: center;
    text-decoration: none;
}

.auth .auth-page .forgot-link:hover,
.auth .auth-page .headline a:hover,
.auth .auth-page .auth-footer a:hover {
    text-decoration: underline;
}

.auth .auth-page .auth-footer {
    position: static;
    z-index: auto;
    margin-top: auto;
    padding: 12px 20px 16px;
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
    text-align: center;
}

.auth .auth-page .auth-footer a,
.auth .auth-page .headline a {
    color: #2563eb;
    text-decoration: none;
}

.auth .auth-page .field-message,
.auth .auth-page .login-message,
.auth .auth-page .form-error {
    margin: 0;
    color: #b91c1c;
    font-size: 12px;
    line-height: 1.4;
}

.auth .auth-page .password-toggle:focus-visible,
.auth .auth-page .submit-button:focus-visible,
.auth .auth-page .forgot-link:focus-visible,
.auth .auth-page .headline a:focus-visible,
.auth .auth-page .auth-footer a:focus-visible {
    outline: 2px solid #2563eb;
    outline-offset: 3px;
}

@media (max-width: 640px) {
    .auth .auth-page .auth-shell {
        padding: 24px 18px 36px;
    }

    .auth .auth-page .auth-footer {
        padding: 10px 18px 14px;
    }
}

@media (max-height: 680px) {
    .auth .auth-page .auth-shell {
        padding-top: 16px;
        padding-bottom: 24px;
    }

    .auth .auth-page .auth-footer {
        padding-top: 8px;
        padding-bottom: 10px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .auth .auth-page .field,
    .auth .auth-page .password-toggle,
    .auth .auth-page .submit-button {
        transition: none;
    }
}
</style>