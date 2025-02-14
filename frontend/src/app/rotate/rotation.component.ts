import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
    selector: 'app-rotation-panel',
    templateUrl: './rotation.component.html',
    styleUrls: ['./rotation.component.css'],
    standalone: false
})
export class RotationComponent {
  @Input() angle: number = 0;
  @Input() nightMode: boolean = false;
  @Input() spinnerShown = false;
  @Output() angleChanged = new EventEmitter<any>();
  @Output() close = new EventEmitter<any>();

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  closePopup() {
    this.close.emit();
  }

  onAngleUpdated(event: any) {
    console.log('onAngleUpdated called');
    this.angle = event.value;
  }

  onAngleChanged() {
    console.log('onAngleChanged called');
    this.emitAngleChanged();
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  private emitAngleChanged() {
    this.angleChanged.emit(this.angle);
  }
}
