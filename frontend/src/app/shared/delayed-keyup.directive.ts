import { Directive, input } from '@angular/core';
import { outputFromObservable } from '@angular/core/rxjs-interop';
import { debounce, Subject, timer } from 'rxjs';

/**
 * Emits the input's value after the user stopped typing for `delay` ms.
 *
 * `debounce(() => timer(this.delay()))` re-reads the delay on every emission,
 * so a custom `[delay]` input is respected even though it isn't available yet
 * when this class is constructed.
 */
@Directive({
  selector: '[appDelayedKeyup]',
  host: {
    '(keyup)': 'onKeyup($event)',
  },
})
export class DelayedKeyupDirective {
  readonly delay = input(300);

  private readonly stream = new Subject<string>();

  readonly appDelayedKeyup = outputFromObservable(
    this.stream.pipe(debounce(() => timer(this.delay()))),
  );

  protected onKeyup(event: KeyboardEvent): void {
    this.stream.next((event.target as HTMLInputElement).value);
  }
}
