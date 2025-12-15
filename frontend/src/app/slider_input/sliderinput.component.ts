import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-sliderinput',
  templateUrl: './sliderinput.component.html',
  styleUrls: ['./sliderinput.component.css'],
  standalone: false,
})
export class SliderInputComponent {
  @Input() sliderInputValue: Number;
  @Input() sliderInputMinValue: Number;
  @Input() sliderInputMaxValue: Number;
  @Output() sliderInputValueChanged = new EventEmitter<any>();

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

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
    if (enteredValue < this.sliderInputMinValue.valueOf()) {
      this.sliderInputValue = this.sliderInputMinValue;
      inputElement.value = this.sliderInputValue.toString();
    }
    if (enteredValue > this.sliderInputMaxValue.valueOf()) {
      this.sliderInputValue = this.sliderInputMaxValue;
      inputElement.value = this.sliderInputValue.toString();
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
    this.sliderInputValue = input;
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
      this.sliderInputValue = input;
      this.emitsliderInputValueChanged();
    }
  }

  private emitsliderInputValueChanged() {
    this.sliderInputValueChanged.emit({ value: this.sliderInputValue });
  }
}
