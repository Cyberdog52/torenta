# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.6.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

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

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
