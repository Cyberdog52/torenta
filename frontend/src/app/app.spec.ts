import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { routes } from './app.routes';
import { PreferenceService } from './preference/preference.service';
import { NotificationService } from './shared/notification/notification.service';
import { BehaviorSubject } from 'rxjs';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('renders the shell with the toolbar', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('mat-toolbar')?.textContent).toContain('Torenta');
    expect(
      compiled.querySelectorAll('a[href="/search"], [routerLink="/search"]').length,
    ).toBeGreaterThan(0);
  });

  it('redirects to preferences when the TMDB key is missing', async () => {
    const routerEvents = new BehaviorSubject(new NavigationEnd(1, '/search', '/search'));
    const navigateSpy = vi.fn().mockResolvedValue(true);
    const notifications: unknown[] = [];
    TestBed.overrideProvider(PreferenceService, {
      useValue: {
        preferenceResource: {
          hasValue: () => true,
          value: () => ({ downloadDirectoryPath: '/media', tmdbServiceKey: null }),
        },
      },
    });
    TestBed.overrideProvider(NotificationService, {
      useValue: {
        notification: () => null,
        notify: (notification: unknown) => notifications.push(notification),
      },
    });
    TestBed.overrideProvider(Router, {
      useValue: {
        events: routerEvents.asObservable(),
        navigate: navigateSpy,
        routerState: { root: { firstChild: null, snapshot: { data: {} } } },
      },
    });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    expect(navigateSpy).toHaveBeenCalledWith(['/preferences']);
    expect(notifications).toHaveLength(1);
    expect((notifications[0] as { content?: string }).content).toBe(
      'Set your TMDB service key in Preferences to start using Torenta.',
    );
  });
});
