import { Component, Inject } from '@angular/core';
import {
  MAT_LEGACY_DIALOG_DATA,
  MatLegacyDialogRef,
} from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-confirmation',
  templateUrl: './confirmation.component.html',
  styleUrl: './confirmation.component.css',
})
export class ConfirmationComponent {
  message: string;

  constructor(
    public dialogRef: MatLegacyDialogRef<ConfirmationComponent>,
    @Inject(MAT_LEGACY_DIALOG_DATA) public data: ConfirmationModel
  ) {
    this.message = data.message;
  }

  ngOnInit() {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onDismiss(): void {
    this.dialogRef.close(false);
  }
}

export class ConfirmationModel {
  constructor(public message: string) {}
}
