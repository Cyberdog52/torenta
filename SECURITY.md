# Security Policy

## Reporting a vulnerability

Please report suspected security vulnerabilities **privately**. Do **not** open a public GitHub
issue, pull request, or discussion for a security problem.

- Preferred: open a private report via GitHub Security Advisories
  ("Security" tab → "Report a vulnerability"), if enabled for this repository.
- Otherwise: contact the repository maintainer directly.

Please include enough detail to reproduce the issue (affected endpoint or component, steps,
and impact). We will acknowledge your report and keep you informed of progress toward a fix.

## Handling secrets

This project reads configuration and credentials from files that must never be committed or
shared. The authoritative list of protected files is in [`AI_RULES.md`](./AI_RULES.md) §1 and
includes (non-exhaustive):

- `src/main/resources/application.properties` and `application-*.properties`
  (except `application-template.properties`)
- `src/main/resources/bitthief.properties`
- `.env` / `.env.*` (except `.env.template` / `.env.example`)
- Anything under `secrets/`, `certificates/`, `credentials/`
- Private keys and keystores (`*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.keystore`, `*.crt`)

If you discover a secret committed to the repository history, report it privately as a
vulnerability — do **not** paste the secret into an issue, PR, or comment. The exposed credential
should be rotated.

## Security notes for this project

`torenta` builds URLs and parses external HTML (jsoup) and scrapes third-party sites. When
contributing, validate external input, avoid path traversal in file/directory handling, never log
secrets, and do not weaken TLS/crypto. See `AI_RULES.md §1.4` for the full set of rules.
