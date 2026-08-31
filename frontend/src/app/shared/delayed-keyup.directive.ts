import { Directive, input } from '@angular/core';
import { outputFromObservable } from '@angular/core/rxjs-interop';
import { debounce, Subject, timer } from 'rxjs';

/**
 * Emits the input's value after the user stopped typing for `delay` ms.
 *
 * `debounce(() => timer(this.delay()))` re-reads the delay on every emission.
 * The previous implementation called `debounceTime(this.delay)` in the
 * constructor, before inputs were bound, so a custom `[delay]` was silently
 * ignored and it always used the default.
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
