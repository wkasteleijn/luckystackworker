import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';

@Component({
  selector: 'app-savescaling-panel',
  templateUrl: './savescaling.component.html',
  styleUrls: ['./savescaling.component.css'],
  standalone: false,
})
export class SaveScalingComponent {
  @Input() scale: number = 100;
  @Input() dimensionX: number = 100;
  @Input() dimensionY: number = 100;
  @Input() nightMode: boolean = false;
  @Input() spinnerShown = false;
  @Output() scaleChanged = new EventEmitter<any>();
  @Output() close = new EventEmitter<any>();

  scaleManualInput: number = 100;

  dimensions: string;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor() {}

  closePopup() {
    this.close.emit();
  }

  onDimensionsChanged() {
    this.dimensionX = Number(this.dimensions.split('x')[0]);
    this.dimensionY = Number(this.dimensions.split('x')[1]);
    this.emitScaleChanged();
  }

  onDimensionXChanged() {
    console.log('onDimensionXChanged called');
    this.emitScaleChanged();
  }

  onDimensionYChanged() {
    console.log('onDimensionYChanged called');
    this.emitScaleChanged();
  }

  onScaleUpdated(event: any) {
    console.log('onScaleChanged called');
    this.scale = event.value;
    this.scaleManualInput = this.scale;
  }

  onScaleChanged() {
    console.log('onScaleChanged called');
    this.emitScaleChanged();
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  onInputKeyPress(event: KeyboardEvent): boolean {
    const char = event.key;
    const inputElement = event.target as HTMLInputElement;
    let inputValue = inputElement.value;

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

    const enteredScale = Number(inputValue);
    if (enteredScale < -180) {
      this.scale = -180;
      inputElement.value = this.scale.toString();
    }
    if (enteredScale > 180) {
      this.scale = 180;
      inputElement.value = this.scale.toString();
    }

    if (char === 'Enter') {
      this.onTextInput(event);
      return true;
    }

    event.preventDefault();
    return false;
  }

  private onTextInput(event: any): void {
    console.log('onTextInput called');
    const input = event.target.value;
    event.target.value = input;
    this.scale = input;
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
      this.scale = input;
      this.emitScaleChanged();
    }
  }

  private emitScaleChanged() {
    this.scaleChanged.emit({
      scale: this.scale,
      dimensionX: this.dimensionX,
      dimensionY: this.dimensionY,
    });
  }
}
