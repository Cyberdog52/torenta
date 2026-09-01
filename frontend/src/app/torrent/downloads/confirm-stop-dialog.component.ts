import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from '@angular/material/dialog';
import { inject } from '@angular/core';

@Component({
  selector: 'app-confirm-stop-dialog',
  imports: [MatButtonModule, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Stop download?</h2>
    <mat-dialog-content>
      Stop “{{ title }}” and permanently delete its downloaded files?
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" color="warn" [mat-dialog-close]="true">
        Stop and delete
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmStopDialogComponent {
  protected readonly title = inject<string>(MAT_DIALOG_DATA);
}
