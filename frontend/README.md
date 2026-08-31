# Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 6.2.2.

## Development server

Run `npm start` and navigate to <http://localhost:4200/>. The development server proxies `/api`
requests to the backend at <http://localhost:8080/> and automatically reloads when source files
change.

## Build

Run `npm run build` to build the project. Build artifacts are written to `dist/`. Add the production
flag with `npm run build -- --prod`. CI runs the plain `npm run build`.

## Running unit tests

Run `npm test` to execute the unit tests via [Karma](https://karma-runner.github.io/).

## Linting

Run `npm run lint` to check the TypeScript code with TSLint and codelyzer.

## Running end-to-end tests

Install Playwright's bundled Chromium browser once:

```bash
npx playwright install chromium
```

The Playwright configuration starts the Spring Boot backend and Angular dev server automatically.
Locally, it reuses either server if it is already running.

Run the smoke test headlessly:

```bash
npm run e2e
```

Open Playwright's interactive UI to run, watch, and debug the test:

```bash
npm run e2e:ui
```

Both commands verify that the application renders and that the Preferences page loads its data from
the backend through the frontend proxy.

Tests are organized by feature under `e2e/`, for example `e2e/app/` and `e2e/preferences/`.

## Code scaffolding

Run `npm run ng -- generate component component-name` to generate a component. Angular CLI can
also generate directives, pipes, services, classes, guards, interfaces, enums, and modules.

## Further help

Run `npm run ng -- help` or consult the
[Angular CLI 6 documentation](https://v6.angular.io/cli).
