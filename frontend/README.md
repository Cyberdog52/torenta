# Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 6.2.2.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

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

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
