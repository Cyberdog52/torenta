import { Directive, Input, HostListener, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Directive({
  selector: '[delayedKeyup]'
})
export class DelayedKeyupDirective implements OnDestroy {
  @Output()
  public delayedKeyup: EventEmitter<any> = new EventEmitter<any>();

  @Input()
  public delay = 300;

  private stream: Subject<any> = new Subject<any>();
  private subscription: Subscription;

  constructor() {
    this.subscription = this.stream
      .pipe(debounceTime(this.delay))
      .subscribe((value: any) => this.delayedKeyup.next(value));
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  @HostListener('keyup', [ '$event' ])
  public onKeyup(value: any): void {
    this.stream.next(value);
  }
}

