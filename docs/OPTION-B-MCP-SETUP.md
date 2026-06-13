# Option B: Spring Boot as MCP Server

> Default implementation uses **Option A** (MCP Node.js calls REST API).  
> Switch to this branch: `git checkout feature/option-b-mcp`

## What Option B Does

Spring Boot exposes a `/mcp` SSE endpoint that speaks the MCP protocol (JSON-RPC 2.0).  
The AI connects to it directly — no Node.js MCP Server needed for business data.

## When to Use Option B

- You want to eliminate the Node.js layer entirely
- Your team is Java-only and prefers one fewer runtime
- You want AI-native tool registration in Spring Boot

## Setup Steps

### 1. Add MCP Java SDK to pom.xml

```xml
<dependency>
  <groupId>io.modelcontextprotocol.sdk</groupId>
  <artifactId>mcp-spring-webmvc</artifactId>
  <version>0.9.0</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 2. Activate McpServerConfig

Uncomment the full implementation in `McpServerConfig.java`  
and set the env var: `ENABLE_MCP_ENDPOINT=true`

### 3. Auth Options (same as Option A)

| Mode | Env var | Result |
|------|---------|--------|
| API Key | `API_KEY_ENABLED=true` | `ApiKeyFilter` guards `/mcp` endpoint |
| No Auth | `API_KEY_ENABLED=false` | Open (safe with `--ingress internal`) |

### 4. Connect Chat API to Spring Boot MCP endpoint

```yaml
# chatbot-api/application.yml
mcp:
  biz-mcp-server-url: ${BIZ_MCP_SERVER_URL}
  biz-api-key: ${BIZ_API_KEY}   # leave blank for no-auth
```

The MCP client performs a handshake at `/mcp` and auto-discovers tools.

### 5. Deploy

```bash
gcloud run deploy biz-data-mcp \
  --set-env-vars ENABLE_MCP_ENDPOINT=true,API_KEY_ENABLED=true \
  --ingress internal
```

MCP SSE endpoint: `https://biz-data-mcp-xxx.run.app/mcp`
