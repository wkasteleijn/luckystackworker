import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-derotation-panel',
  templateUrl: './derotation.component.html',
  styleUrls: ['./derotation.component.css'],
  standalone: false,
})
export class DeRotationComponent {
  @Input() anchorStrength;
  @Input() noiseRobustness;
  @Input() accurateness;
  @Input() nightMode: boolean = false;
  @Input() images: string[];
  @Output() anchorStrengthChanged = new EventEmitter<any>();
  @Output() noiseRobustnessChanged = new EventEmitter<any>();
  @Output() accuratenessChanged = new EventEmitter<any>();
  @Output() derotationRefImageChanged = new EventEmitter<any>();
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

  onAnchorStrengthChanged() {
    console.log('onAnchorStrengthChanged called');
    this.anchorStrengthChanged.emit(this.anchorStrength);
  }

  onNoiseRobustnessChanged() {
    console.log('onNoiseRobustnessChanged called');
    this.noiseRobustnessChanged.emit(this.noiseRobustness);
  }

  onAccuratenessChanged() {
    console.log('onAccuratenessChanged called');
    this.accuratenessChanged.emit(this.accurateness);
  }

  onRefImageChanged() {
    console.log('onRefImageChanged called');
    this.derotationRefImageChanged.emit(this.referenceImage);
  }

  onStart() {
    console.log('onStart called');
    this.startPressed = true;
    if (this.referenceImage !== undefined) {
      this.start.emit();
    }
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  showWarning(): boolean {
    return this.startPressed && this.referenceImage === undefined;
  }
}
