import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatLegacySnackBarRef } from '@angular/material/legacy-snack-bar';

@Component({
  selector: 'app-newversion',
  templateUrl: './newversion.component.html',
  styleUrls: ['./newversion.component.css'],
  standalone: false,
})
export class NewVersionComponent implements OnInit {
  @Input() version: string;
  @Input() releaseNotes: string[];

  @Output() close = new EventEmitter<void>();

  constructor(public snackBarRef: MatLegacySnackBarRef<NewVersionComponent>) {}

  ngOnInit(): void {}

  onClose() {
    this.close.emit();
  }
}
