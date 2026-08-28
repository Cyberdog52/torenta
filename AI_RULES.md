# AI Rules and Security Policy

This repository is intended to be used with AI-assisted development tools such as:

- GitHub Copilot
- Claude Code
- Cursor
- Aider
- Continue.dev
- OpenCode
- Other AI coding assistants

## Protected Files

The following files may contain secrets, credentials, tokens, or other confidential information and must never be used as AI context:

```text
src/main/resources/application.properties
.env
.env.*
```

AI tools must not:

- Read these files.
- Index these files.
- Summarize these files.
- Generate code based on these files.
- Include information from these files in responses.

If configuration information is required, request sanitized or placeholder values instead.

---

## Repository AI Configuration

This repository may contain one or more of the following files:

```text
.claude/settings.json
.cursorignore
.aiderignore
.continueignore
.aiignore

AGENTS.md
CLAUDE.md
.github/copilot-instructions.md
```

These files provide tool-specific configuration, exclusions, and instructions.

---

## Tool-Specific Notes

### Claude Code

Claude Code supports repository-local permission settings.

Configuration:

```text
.claude/settings.json
```

Example:

```json
{
  "permissions": {
    "deny": [
      "Read(src/main/resources/application.properties)",
      "Read(.env)",
      "Read(.env.*)"
    ]
  }
}
```

This is the strongest repository-level protection currently available.

### GitHub Copilot

GitHub Copilot does not currently provide a universally supported repository-local deny mechanism equivalent to Claude Code permissions.

Repository files such as:

```text
.github/copilot-instructions.md
AGENTS.md
AI_RULES.md
```

should be considered guidance rather than enforcement.

Protection depends on:

- GitHub Enterprise policies
- Organization-level settings
- Repository-level Copilot configuration (if enabled by the organization)

Developers must not assume that files inside the repository are inaccessible to Copilot.

### Cursor, Aider, Continue.dev and Similar Tools

These tools typically support ignore files such as:

```text
.cursorignore
.aiderignore
.continueignore
.aiignore
```

These files help prevent indexing and inclusion in context but should not be considered a security boundary.

---

## Security Principles

### Do Not Commit Secrets

Secrets must never be stored in source control.

Examples:

- Access tokens
- API keys
- Passwords
- Certificates
- Private keys
- Production connection strings

### Use Placeholder Files

Safe, committed template files are allowed, including:

```text
src/main/resources/application-template.properties
.env.template
.env.example
```

These files must contain placeholder values only, never real credentials. For example:

```properties
# src/main/resources/application-template.properties
ch.andreskonrad.torenta.tmdb.service.key=YOUR_TMDB_API_KEY
```

```env
DATABASE_URL=
API_KEY=
```

### Use Secret Management

Preferred solutions:

- Azure Key Vault
- HashiCorp Vault
- AWS Secrets Manager
- Kubernetes Secrets
- Environment variables
- Other approved secret management systems