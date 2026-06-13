/**
 * AI Chatbot Widget — Embeddable plugin for any web application.
 * 
 * Usage (single script tag):
 *   <script
 *     src="chatbot-widget.js"
 *     data-api-url="https://your-chatbot-api.run.app/api/chat"
 *     data-title="AI Assistant"
 *     data-theme="#1A73E8"
 *     defer
 *   ></script>
 * 
 * Config attributes:
 *   data-api-url   — required: URL of your Spring Boot Chat API
 *   data-title     — optional: widget title (default: "AI Assistant")
 *   data-theme     — optional: primary color hex (default: "#1A73E8")
 */
(function () {
  'use strict';

  const script  = document.currentScript;
  const API_URL = script.getAttribute('data-api-url');
  const TITLE   = script.getAttribute('data-title')  || 'AI Assistant';
  const THEME   = script.getAttribute('data-theme')  || '#1A73E8';

  if (!API_URL) {
    console.error('[chatbot-widget] data-api-url is required.');
    return;
  }

  let history = [];
  let isOpen  = false;

  // ── Inject CSS ──────────────────────────────────────────────────────────────
  const style = document.createElement('style');
  style.textContent = `
    #ai-cw-btn {
      position: fixed; bottom: 24px; right: 24px; width: 56px; height: 56px;
      background: ${THEME}; border-radius: 50%; border: none; cursor: pointer;
      box-shadow: 0 4px 14px rgba(0,0,0,0.25); z-index: 9998;
      color: #fff; font-size: 22px; display: flex; align-items: center;
      justify-content: center; transition: transform 0.2s; font-family: Arial, sans-serif;
    }
    #ai-cw-btn:hover { transform: scale(1.1); }
    #ai-cw-panel {
      position: fixed; bottom: 90px; right: 24px; width: 380px; height: 530px;
      background: #fff; border-radius: 14px;
      box-shadow: 0 8px 32px rgba(0,0,0,0.18); z-index: 9999;
      display: flex; flex-direction: column; overflow: hidden;
      transition: opacity 0.25s, transform 0.25s;
      font-family: Arial, sans-serif;
    }
    #ai-cw-panel.hidden { opacity: 0; pointer-events: none; transform: translateY(12px); }
    .ai-cw-header {
      background: ${THEME}; color: #fff; padding: 14px 16px;
      font-weight: 700; font-size: 15px; display: flex;
      justify-content: space-between; align-items: center;
    }
    .ai-cw-close { background: none; border: none; color: #fff;
      font-size: 18px; cursor: pointer; padding: 0 4px; }
    .ai-cw-msgs {
      flex: 1; overflow-y: auto; padding: 12px;
      display: flex; flex-direction: column; gap: 8px;
      background: #f8f9fa;
    }
    .ai-cw-msg {
      max-width: 85%; padding: 10px 14px; border-radius: 16px;
      font-size: 13px; line-height: 1.5; word-wrap: break-word;
    }
    .ai-cw-msg.user { background: ${THEME}; color: #fff; align-self: flex-end;
      border-bottom-right-radius: 4px; }
    .ai-cw-msg.bot  { background: #fff; color: #333; align-self: flex-start;
      border-bottom-left-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .ai-cw-msg.typing { color: #999; font-style: italic; }
    .ai-cw-input-row {
      display: flex; padding: 10px 12px; border-top: 1px solid #e8eaed;
      gap: 8px; background: #fff;
    }
    .ai-cw-input {
      flex: 1; padding: 9px 14px; border: 1px solid #dadce0;
      border-radius: 20px; outline: none; font-size: 13px; resize: none;
      font-family: Arial, sans-serif;
    }
    .ai-cw-input:focus { border-color: ${THEME}; }
    .ai-cw-send {
      background: ${THEME}; color: #fff; border: none; border-radius: 50%;
      width: 36px; height: 36px; cursor: pointer; font-size: 14px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .ai-cw-send:disabled { opacity: 0.5; cursor: not-allowed; }
    @media (max-width: 440px) {
      #ai-cw-panel { width: calc(100vw - 24px); right: 12px; }
    }
  `;
  document.head.appendChild(style);

  // ── Build DOM ───────────────────────────────────────────────────────────────
  const container = document.createElement('div');
  container.innerHTML = `
    <button id="ai-cw-btn" aria-label="Open AI Assistant">💬</button>
    <div id="ai-cw-panel" class="hidden" role="dialog" aria-label="AI Chat Assistant">
      <div class="ai-cw-header">
        <span>${TITLE}</span>
        <button class="ai-cw-close" aria-label="Close chat" id="ai-cw-close">✕</button>
      </div>
      <div class="ai-cw-msgs" id="ai-cw-msgs" aria-live="polite">
        <div class="ai-cw-msg bot">
          Hello! I can answer questions about your systems, expand acronyms from our jargon dictionary, and retrieve operational data. How can I help?
        </div>
      </div>
      <div class="ai-cw-input-row">
        <input
          class="ai-cw-input"
          id="ai-cw-input"
          placeholder="Ask a question..."
          maxlength="500"
          autocomplete="off"
        />
        <button class="ai-cw-send" id="ai-cw-send" aria-label="Send message">➤</button>
      </div>
    </div>
  `;
  document.body.appendChild(container);

  const btn   = document.getElementById('ai-cw-btn');
  const panel = document.getElementById('ai-cw-panel');
  const msgs  = document.getElementById('ai-cw-msgs');
  const input = document.getElementById('ai-cw-input');
  const send  = document.getElementById('ai-cw-send');
  const close = document.getElementById('ai-cw-close');

  // ── Toggle panel ────────────────────────────────────────────────────────────
  function togglePanel(open) {
    isOpen = open !== undefined ? open : !isOpen;
    panel.classList.toggle('hidden', !isOpen);
    btn.textContent = isOpen ? '✕' : '💬';
    if (isOpen) input.focus();
  }

  btn.addEventListener('click',   () => togglePanel());
  close.addEventListener('click', () => togglePanel(false));

  // ── Send message ────────────────────────────────────────────────────────────
  async function sendMessage() {
    const text = input.value.trim();
    if (!text) return;

    input.value = '';
    send.disabled = true;

    appendMsg(text, 'user');
    const typingEl = appendMsg('Thinking…', 'bot typing');

    try {
      const res = await fetch(API_URL, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ message: text, history }),
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();

      typingEl.remove();
      const answer = data.answer || data.message || 'No response received.';
      appendMsg(answer, 'bot');

      // Maintain rolling history (last 10 turns = 20 messages)
      history.push({ role: 'user', text }, { role: 'assistant', text: answer });
      if (history.length > 20) history = history.slice(-20);

    } catch (err) {
      typingEl.textContent = `Error: Could not reach the AI service. (${err.message})`;
      typingEl.classList.remove('typing');
    } finally {
      send.disabled = false;
      input.focus();
    }
  }

  function appendMsg(text, className) {
    const el = document.createElement('div');
    el.className = `ai-cw-msg ${className}`;
    el.textContent = text;
    msgs.appendChild(el);
    msgs.scrollTop = msgs.scrollHeight;
    return el;
  }

  send.addEventListener('click', sendMessage);
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

})();
