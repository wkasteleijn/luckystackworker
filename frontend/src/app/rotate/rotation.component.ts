import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-rotation-panel',
  templateUrl: './rotation.component.html',
  styleUrls: ['./rotation.component.css'],
  standalone: false,
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

  onInputKeyPress(event: KeyboardEvent): boolean {
    const char = event.key;
    const inputValue = (event.target as HTMLInputElement).value;

    if (/\d/.test(char)) {
      const decimalIndex = inputValue.indexOf('.');
      if (decimalIndex !== -1 && inputValue.length - decimalIndex > 1) {
        event.preventDefault();
        return false;
      }
      return true;
    }

    if (char === '.' && !inputValue.includes('.')) {
      return true;
    }

    if (char === '-' && inputValue === '') {
      return true;
    }

    // TODO: fix this, can still enter numbers larger than 180 or smaller than -180.
    const enteredAngle = Number(inputValue);
    if (enteredAngle < -180) {
      this.angle = -180;
    }
    if (enteredAngle > 180) {
      this.angle = 180;
    }

    if (char === 'Enter') {
      this.onTextInput(event);
      return true;
    }

    event.preventDefault();
    return false;
  }

  onTextInput(event: any): void {
    console.log('onTextInput called');
    const input = event.target.value;
    event.target.value = input;
    this.angle = input;
    const decimalMatch = input.match(/\./g);

    let isValidNumber = true;
    if (decimalMatch && decimalMatch.length > 1) {
      isValidNumber = false;
    }
    if (input.includes('-') && input.indexOf('-') !== 0) {
      isValidNumber = false;
    }

    if (isValidNumber) {
      event.target.value = input;
      this.angle = input;
      this.emitAngleChanged();
    }
  }

  private emitAngleChanged() {
    this.angleChanged.emit(this.angle);
  }
}
