# Demo Guide — AI Chatbot Live Presentation

> **Audience:** Mixed — technical developers + non-technical business stakeholders  
> **Format:** Live demo + Q&A  
> **Estimated time:** 20–30 minutes

---

## Before You Start — Preparation Checklist

### 30 Minutes Before

- [ ] All 3 services running (chatbot-api, business-data-api, mcp-server)
- [ ] Browser open at `web-widget/index.html`
- [ ] Terminal open with curl commands ready to paste
- [ ] PostgreSQL connected and seed data verified
- [ ] Internet connection tested (Vertex AI requires GCP access)
- [ ] Zoom/screen share tested — widget visible on shared screen

### Pre-flight Verification (run these before demo)

```bash
# Confirm all services healthy
curl -s http://localhost:8081/api/business/health | python3 -m json.tool
curl -s http://localhost:8080/api/chat/health    | python3 -m json.tool

# Warm up Vertex AI (first call is slow — do this 2 min before demo)
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello"}' | python3 -m json.tool
```

---

## Demo Script

### Opening (2 minutes) — For Everyone

> **Say this:**
> *"Every team has its own language — SLAs, KPIs, RBAC, MTTR. New members spend weeks learning it. Existing members get interrupted to explain the same things repeatedly. We also have operational data spread across dashboards that require SQL knowledge to query. I wanted to solve both problems with a single, embeddable AI assistant."*

**Show:** The chat widget floating in the bottom-right corner of the demo page.

> *"This is the result. One script tag. Drops into any existing web application. Let me show you what it can do."*

---

### Demo 1: Jargon Translation (3 minutes) — For Everyone

**This resonates with ALL audiences — no technical knowledge needed.**

**Type in widget:**
```
What does SLA mean in our context?
```

**Expected response:** Expands SLA to Service Level Agreement with domain-specific description and example usage.

> **Say this after response:**
> *"It didn't give a generic Wikipedia answer. It pulled this definition directly from our jargon dictionary table in PostgreSQL. If our definition of SLA changes, we update the database — the AI answer updates automatically overnight."*

**Type next:**
```
What is the difference between RTO and RPO?
```

> **Say this:**
> *"Notice it handles related terms in context. The jargon dictionary has 20 entries today — we can add as many as we want without touching any code."*

---

### Demo 2: Operational Data Query (4 minutes) — For Business Stakeholders

**Type in widget:**
```
Show me all entities that have SLA compliance issues
```

**Expected response:** Lists entities with breach/at-risk status, current uptime vs threshold, recommendations.

> **Say this:**
> *"This answer came from live data. The AI called a tool on our backend that computed SLA compliance in real time — not from a static report, not from a cached snapshot. The business logic — what counts as 'at risk' vs 'breached' — lives in Java code that the team can review and modify."*

**Type next:**
```
What is the overall health of the Alpha Service?
```

> **Say this:**
> *"The health score you see — 0 to 100 — is computed from uptime percentage, response time, and number of open events. That formula is configurable by the engineering team. This is not the AI guessing — it's the AI presenting computed business intelligence in conversational form."*

---

### Demo 3: Technical Architecture (5 minutes) — For Technical Audience

**Switch to terminal. Show the curl command transparently.**

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"How many critical events happened in the last 24 hours?"}' \
  | python3 -m json.tool
```

> **Say this while it runs:**
> *"What's happening right now: the Chat API received this request and called two MCP tools — get_jargon_dictionary and get_active_events. The MCP Server ran a query against our Business Data API, which queried PostgreSQL and applied our business logic. That context was injected into the Gemini prompt. Gemini generated this answer grounded in that data."*

**Show architecture diagram** (from slide deck):

> *"MCP Server — Model Context Protocol — is the key piece. It's an open standard that lets any AI model call your tools in a standardized way. Today we're using Gemini. Tomorrow we could swap in Claude or GPT-4. The tools stay the same. The data stays in our GCP project. Nothing leaves our security perimeter."*

---

### Demo 4: Multi-Turn Conversation (3 minutes) — For Everyone

**Type in widget:**
```
Which service has the highest risk right now?
```

*(After response)*

```
What should the team do about it?
```

*(After response)*

```
Explain what MTTR means in that context
```

> **Say this:**
> *"Notice it remembered the earlier context — it knew 'it' referred to the service we were discussing. The conversation history is maintained in the widget session. Jargon terms get expanded inline. This is the kind of experience we can offer to every team member, embedded directly in our existing applications."*

---

### Demo 5: The Embed Story (2 minutes) — For Everyone / CTO

**Open browser developer tools. Show the script tag.**

> **Say this:**
> *"Here is the entire integration code for any existing web application."*

```html
<script
  src="chatbot-widget.js"
  data-api-url="https://chatbot-api.run.app/api/chat"
  data-title="AI Assistant"
  data-theme="#1A73E8"
  defer
></script>
```

> *"One line. No npm install. No framework dependency. No rebuild of the host application. Angular, React, Vue, plain HTML — it works in all of them. The theme color is configurable. The title is configurable. Each team can point it at the same backend but customize their experience."*

---

## Q&A Preparation — Anticipated Questions

### Business / Leadership Questions

**Q: How does it stay accurate as our data changes?**
> *"Two mechanisms. For operational data — SLAs, events, metrics — it queries live. For the jargon dictionary and knowledge base, we sync nightly via an automated job. If you add a new term today, it's live in the AI by tomorrow morning."*

**Q: What happens if the AI gets something wrong?**
> *"The system prompt instructs Gemini: 'Only answer from the provided context. If you cannot find an answer, say so.' Temperature is set to 0.2 — the lowest practical setting — which makes answers factual and conservative. We're not using it for creative writing; we're using it as a structured data retrieval layer."*

**Q: Could we use this for customer-facing applications?**
> *"Today it's designed for internal use. For customer-facing, we'd add authentication via Google Identity-Aware Proxy and potentially a review layer. The architecture supports this — it's an incremental step, not a rebuild."*

**Q: What is the cost?**
> *"Vertex AI charges per 1,000 tokens. Gemini 1.5 Flash — the model we're using — is priced at approximately $0.075 per million input tokens and $0.30 per million output tokens. For internal usage at our scale, we're looking at low double-digit dollars per month for the AI calls. Cloud Run scales to zero — we pay for compute only when requests are made."*

### Technical Questions

**Q: Why MCP Server? Why not call Vertex AI directly?**
> *"MCP — Model Context Protocol — is an open standard that decouples the AI from your data sources. The MCP Server is the universal adapter. Tomorrow if we want to add Confluence pages, Jira tickets, or a new database table as a data source, we add one MCP tool. The AI client, the widget, the chat API — none of them change."*

**Q: How is the database secured?**
> *"Three layers. First, a read-only PostgreSQL user — even if the MCP Server were compromised, it cannot modify data. Second, the Business Data API and MCP Server run with `--ingress internal` on Cloud Run — they have no public internet access. Third, all passwords and API keys are in GCP Secret Manager — never in environment variables or code."*

**Q: Why Spring Boot for the Business Data API instead of just extending the MCP Server?**
> *"Business logic — computing a health score from uptime, response time, and event count; determining SLA breach risk; aggregating KPIs — is best expressed in Java. It's type-safe, unit-testable, reviewable via PR, and can be deployed independently. The Node.js MCP Server handles the AI protocol layer. Separation of concerns."*

**Q: What's the difference between Option A and Option B?**
> *"Option A is what's deployed today — the MCP Server calls the Spring Boot API via HTTP. Option B makes Spring Boot itself speak the MCP protocol directly, removing the Node.js layer for business data. Both produce identical AI outputs. Option B is on a feature branch for teams that want a purely Java path."*

**Q: Can we add write capabilities — like creating Jira tickets from chat?**
> *"Yes — that's on the roadmap. Write tools need additional safeguards: a confirmation step, an approval gate, audit logging. The MCP architecture supports this natively. We'd add a `create_ticket` tool with explicit confirmation required before execution."*

---

## Demo Flow Reference Card

*(Print this and keep it next to your keyboard)*

```
1. [2 min] Opening — "Every team has its own language"
2. [3 min] Demo 1: "What does SLA mean?" + "RTO vs RPO?"
3. [4 min] Demo 2: "SLA compliance issues" + "Alpha Service health"
4. [5 min] Demo 3: Terminal curl + architecture explanation
5. [3 min] Demo 4: Multi-turn conversation
6. [2 min] Demo 5: The <script> tag embed story
7. [Q&A]  Use prepared answers above
```

---

## Fallback Plan (if live demo fails)

If any service is down during the presentation:

1. **Show the curl output** — pre-run the demo questions and save output to a text file. Paste and explain.
2. **Show the code** — walk through `BusinessDataService.java` health score logic instead of running it.
3. **Show the architecture slide** — explain the flow without running it. "Let me show you what's happening under the hood."

**Never apologize for a demo failure with "sorry, the demo is broken."**  
Say: *"Let me show you the underlying logic while the service reconnects — this is actually more interesting."*

---

## Post-Demo — Next Steps to Offer

At the end, offer concrete next steps to close the conversation:

1. **"We can embed this in [specific existing app] this week"** — pick one concrete application
2. **"We can expand the jargon dictionary with your team's terms"** — invite participation
3. **"We can add [specific business data] as a new data source"** — make it relevant to their work
4. **"I can share the GitHub repository for any team that wants to review the code"**

---

*Demo Guide — MCP-Vertex-RAG-Java-Springboot-Chatbot*  
*Designed by Curiosity. Built by Discipline. Delivered by Nirav Mehta.*
