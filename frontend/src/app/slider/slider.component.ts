import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-slider',
  templateUrl: './slider.component.html',
  styleUrls: ['./slider.component.css'],
  standalone: false,
})
export class SliderComponent {
  @Input() sliderLabel: number;
  @Input() sliderValue: number;
  @Input() sliderStep: number;
  @Input() sliderMinValue: number;
  @Input() sliderMaxValue: number;
  @Input() nightMode: boolean = false;
  @Output() sliderValueChanged = new EventEmitter<any>();

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  onSliderValueUpdated(event: any) {
    console.log('onSliderUpdated called');
    this.sliderValue = event.value;
  }

  onSliderValueChanged() {
    console.log('onSliderChanged called');
    this.emitSliderValueChanged();
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

    const enteredValue = Number(inputValue);
    if (enteredValue < this.sliderMinValue) {
      this.sliderValue = this.sliderMinValue;
      inputElement.value = this.sliderValue.toString();
    }
    if (enteredValue > this.sliderMaxValue) {
      this.sliderValue = this.sliderMaxValue;
      inputElement.value = this.sliderValue.toString();
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
    this.sliderValue = input;
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
      this.sliderValue = input;
      this.emitSliderValueChanged();
    }
  }

  private emitSliderValueChanged() {
    this.sliderValueChanged.emit(this.sliderValue);
  }
}
