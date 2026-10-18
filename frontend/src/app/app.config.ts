import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { TRAINING_API_BASE_URL } from './features/training/config/training-api.config';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: TRAINING_API_BASE_URL, useValue: '/api/training' },
    provideHttpClient(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' })
    )
  ]
};
