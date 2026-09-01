import { TestBed } from '@angular/core/testing';
import { OverviewPopoverComponent } from './overview-popover.component';

describe('OverviewPopoverComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OverviewPopoverComponent],
    }).compileComponents();
  });

  it('does not render an info control for an empty overview', () => {
    const fixture = TestBed.createComponent(OverviewPopoverComponent);
    fixture.componentRef.setInput('title', 'Arrival');
    fixture.componentRef.setInput('overview', '   ');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('button')).toBeNull();
  });

  it('opens, toggles, and closes the overview', () => {
    const fixture = TestBed.createComponent(OverviewPopoverComponent);
    fixture.componentRef.setInput('title', 'Arrival');
    fixture.componentRef.setInput('overview', 'A linguist works to communicate with visitors.');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const trigger = host.querySelector<HTMLButtonElement>('.overview-trigger');
    const popover = host.querySelector<HTMLElement>('.overview-popover');
    if (trigger == null || popover == null) {
      throw new Error('Overview controls were not rendered');
    }
    const showPopover = vi.fn<() => void>();
    const hidePopover = vi.fn<() => void>();
    popover.showPopover = showPopover;
    popover.hidePopover = hidePopover;

    trigger.click();
    fixture.detectChanges();
    expect(showPopover).toHaveBeenCalledOnce();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');

    trigger.click();
    fixture.detectChanges();
    expect(hidePopover).toHaveBeenCalledOnce();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');

    trigger.click();
    fixture.detectChanges();
    host.querySelector<HTMLButtonElement>('[aria-label="Close overview"]')?.click();
    fixture.detectChanges();
    expect(hidePopover).toHaveBeenCalledTimes(2);
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
  });
});
