import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

let nextOverviewId = 0;

@Component({
  selector: 'app-overview-popover',
  imports: [MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './overview-popover.component.scss',
  templateUrl: './overview-popover.component.html',
})
export class OverviewPopoverComponent {
  readonly title = input.required<string>();
  readonly overview = input.required<string>();

  protected readonly popoverId = `overview-popover-${nextOverviewId++}`;
  protected readonly headingId = `${this.popoverId}-heading`;
  protected readonly anchorName = `--${this.popoverId}-anchor`;
  protected readonly isOpen = signal(false);

  protected toggle(event: Event, popover: HTMLElement): void {
    event.stopPropagation();
    if (this.isOpen()) {
      popover.hidePopover();
      this.isOpen.set(false);
    } else {
      popover.showPopover();
      this.isOpen.set(true);
    }
  }

  protected close(event: Event, popover: HTMLElement): void {
    event.stopPropagation();
    popover.hidePopover();
    this.isOpen.set(false);
  }

  protected updateOpenState(event: ToggleEvent): void {
    this.isOpen.set(event.newState === 'open');
  }
}
