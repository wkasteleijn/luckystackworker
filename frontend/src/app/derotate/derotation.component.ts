import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';
import { DeRotation } from '../model/derotation';

@Component({
  selector: 'app-derotation-panel',
  templateUrl: './derotation.component.html',
  styleUrls: ['./derotation.component.css'],
  standalone: false,
})
export class DeRotationComponent {
  @Input() deRotation: DeRotation;
  @Input() nightMode: boolean = false;
  @Output() close = new EventEmitter<any>();
  @Output() start = new EventEmitter<any>();

  referenceImage: string;
  startPressed: boolean = false;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  closePopup() {
    this.close.emit();
  }

  onStart() {
    console.log('onStart called');
    this.startPressed = true;
    if (this.referenceImage !== undefined) {
      this.start.emit(this.deRotation);
    }
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  showWarning(): boolean {
    return this.startPressed && this.referenceImage === undefined;
  }

  onRefImageChanged() {
    this.deRotation.referenceImage = this.referenceImage;
  }

  onAnchorStrengthChanged(event: any) {
    this.deRotation.anchorStrength = event;
  }
}
