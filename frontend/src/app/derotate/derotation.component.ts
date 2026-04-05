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
  referenceHour: number;
  referenceMinute: number;
  referenceSecond: number;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  closePopup() {
    this.close.emit();
  }

  onStart() {
    console.log('onStart called');
    this.startPressed = true;
    if (
      this.isReferenceImageSelected() ||
      (this.isCompleteTimeEntered() && !this.isInvalidReferenceTime())
    ) {
      this.deRotation.referenceHour = this.referenceHour;
      this.deRotation.referenceMinute = this.referenceMinute;
      this.deRotation.referenceSecond = this.referenceSecond;
      this.start.emit(this.deRotation);
    }
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  showWarning(): boolean {
    return (
      this.startPressed &&
      ((!this.isReferenceImageSelected() && !this.isCompleteTimeEntered()) ||
        (this.isReferenceImageSelected() && this.isCompleteTimeEntered()))
    );
  }

  showInvalidTimeWarning(): boolean {
    return this.startPressed && this.isInvalidReferenceTime();
  }

  onRefImageChanged() {
    this.deRotation.referenceImage = this.referenceImage;
  }

  onNoiseRobustnessChanged(event: any) {
    this.deRotation.noiseRobustness = event;
  }

  onAnchorStrengthChanged(event: any) {
    this.deRotation.anchorStrength = event;
  }

  onAccuratenessChanged(event: any) {
    this.deRotation.accurateness = event;
  }
  onLowSNRDataChanged(event: any) {
    this.deRotation.lowSNRData = event.checked;
  }

  private parseInt(input: string): number {
    if (input === null || input.trim() === '') {
      return -1;
    }
    const num = Number(input);
    if (Number.isInteger(num)) {
      return num;
    }
    return -2;
  }

  private isInvalidReferenceTime(): boolean {
    return (
      this.referenceHour < 0 ||
      this.referenceHour > 23 ||
      this.referenceMinute < 0 ||
      this.referenceMinute > 59 ||
      this.referenceSecond < 0 ||
      this.referenceSecond > 59
    );
  }

  private isCompleteTimeEntered(): boolean {
    return (
      this.referenceHour !== undefined &&
      this.referenceMinute !== undefined &&
      this.referenceSecond !== undefined &&
      this.referenceHour !== null &&
      this.referenceMinute !== null &&
      this.referenceSecond !== null
    );
  }

  private isReferenceImageSelected(): boolean {
    return this.referenceImage !== undefined && this.referenceImage.length > 0;
  }
}
