# Torenta Marp presentation

This folder contains the fullscreen Marp version of the Torenta camp presentation:

```text
presentation.md             Marp content and presenter notes
torenta.css                 Reusable Jambda visual theme
presentation.html           Generated browser presentation
assets/torenta-logo.png     Local copy of the application logo
assets/modernization.svg    Technology-migration diagram
assets/engineering-loop.svg AI-assisted engineering-loop diagram
```

The deck is self-contained and can be moved or shared as a folder.
[`../results/presentation.md`](../results/presentation.md) is the GitHub-native version.

## Source files and generated output

Do **not** edit `presentation.html` directly. Marp generates it from the editable source files:

- Edit `presentation.md` to change slide content, ordering, semantic layout classes, or presenter
  notes.
- Edit `torenta.css` to change colors, typography, spacing, or slide layouts.
- Edit files under `assets/` to change diagrams or images.

After changing any source file:

1. Regenerate `presentation.html` with the command below.
2. Open the generated HTML in a browser.
3. Review the affected slides for layout, overflow, and readability.
4. Commit the updated source files and regenerated `presentation.html` together.

## Prerequisites

- Node.js 18 or newer. This repository already uses Node.js 24 LTS.
- A Chromium-based browser, Firefox, or Safari.

Marp CLI is free and open source. The commands below use `npx`, so no paid tool or global
installation is required.

## Create the HTML presentation

### Windows PowerShell

From the repository root in PowerShell:

```powershell
Set-Location .\jambda-camp\marp
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html presentation.md -o presentation.html
```

Open the generated presentation:

```powershell
Start-Process .\presentation.html
```

### Windows Command Prompt

From the repository root in Command Prompt (`cmd.exe`):

```batch
cd /d jambda-camp\marp
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html presentation.md -o presentation.html
```

Open the generated presentation:

```batch
start presentation.html
```

### Linux and macOS

From the repository root:

```bash
cd ./jambda-camp/marp
npx @marp-team/marp-cli@latest --theme-set torenta.css --html presentation.md -o presentation.html
```

Open the generated presentation on Linux:

```bash
xdg-open ./presentation.html
```

Or on macOS:

```bash
open ./presentation.html
```

The `--html` option is required because the deck uses a small amount of inline HTML for styling.
Use the browser's fullscreen mode (`F11`) when presenting. Navigate with the arrow keys,
Page Up/Page Down, or Space.

`presentation.html` is generated output. Regenerate and commit it whenever `presentation.md`,
`torenta.css`, or a referenced asset changes; never make changes directly in the HTML file.

## Edit with automatic regeneration

Run Marp CLI in watch mode:

```powershell
Set-Location .\jambda-camp\marp
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html --watch presentation.md
```

Marp creates `presentation.html` next to the Markdown source and regenerates it whenever the
source changes. Refresh the browser to see the latest version. Stop watch mode with `Ctrl+C`.

For an on-demand local server instead:

```powershell
Set-Location .\jambda-camp\marp
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html --server .
```

Open the local URL printed by Marp CLI and select `presentation.md`.

## How the Markdown becomes slides

The document starts with Marp front matter:

```yaml
---
marp: true
theme: torenta
size: 16:9
paginate: true
---
```

A horizontal separator starts a new slide:

```markdown
# First slide

---

# Second slide
```

Images use paths relative to `presentation.md`:

```markdown
![width:190px](assets/torenta-logo.png)
```

Slide-specific directives are HTML comments:

```markdown
<!-- _class: lead -->
<!-- _paginate: false -->
```

Presenter notes are also written as comments and do not appear on the slide:

```markdown
<!--
Timing: 45 seconds.

Explain the qualification behind the estimate.
-->
```

Export all presenter notes to a text file:

```powershell
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css presentation.md --notes -o presenter-notes.txt
```

The content and look-and-feel are intentionally separate. `presentation.md` selects the `torenta`
theme and contains only slide content, semantic layout classes, and presenter notes. `torenta.css`
contains the colors, typography, spacing, and component layouts. Marp's `--theme-set` option loads
that external theme and embeds it in generated output.

The theme uses Lato for headings, then falls back to common sans-serif fonts. No font files or
network resources are required.

## IntelliJ IDEA

There is currently no official Marp plugin for IntelliJ IDEA, and no dedicated current plugin was
found in the JetBrains Marketplace. IntelliJ's built-in Markdown support remains useful for editing,
but its preview does not render the file as a Marp slide deck.

The simplest workflow is:

1. Open IntelliJ's integrated terminal.
2. Run the watch command shown above.
3. Open `presentation.html` in a browser.
4. Edit `presentation.md` in IntelliJ and refresh the browser.

To create an IntelliJ External Tool, open **Settings → Tools → External Tools**, add a tool, and use:

| Field | Value |
| --- | --- |
| Program | `npx.cmd` |
| Arguments | `@marp-team/marp-cli@latest --theme-set torenta.css --html --watch presentation.md` |
| Working directory | `$ProjectFileDir$\jambda-camp\marp` |

Run the tool while editing and stop it when the presentation is finished.

## Optional exports

Create a PDF:

```powershell
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html --allow-local-files presentation.md -o presentation.pdf
```

Create a PowerPoint-compatible file:

```powershell
npx.cmd @marp-team/marp-cli@latest --theme-set torenta.css --html --allow-local-files presentation.md -o presentation.pptx
```

HTML is the recommended format for the live presentation because it preserves the intended
fullscreen browser rendering. PDF is a useful offline backup. Standard PPTX export preserves the
appearance but renders slides as non-editable slide images. Marp needs `--allow-local-files` for
these exports to embed the trusted images stored in this presentation folder.
