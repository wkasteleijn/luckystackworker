import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { PSF } from '../model/psf';

@Component({
  selector: 'app-synthetic-psf-panel',
  templateUrl: './synthetic.psf.component.html',
  styleUrls: ['./synthetic.psf.component.css'],
})
export class SyntheticPsfComponent {
  @Input() imageData: string = '';
  @Input() nightMode: boolean = false;
  @Input() psf: PSF;

  @Output() close = new EventEmitter<void>();
  @Output() slidersChanged = new EventEmitter<any>();

  airyDiskRadius: number;
  seeingIndex: number;
  diffractionIntensity: number;
  wavelength: number;
  customPSF: string;
  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  ngOnInit() {
    this.airyDiskRadius = this.psf.airyDiskRadius;
    this.seeingIndex = this.psf.seeingIndex;
    this.diffractionIntensity = this.psf.diffractionIntensity;
    this.wavelength = this.psf.wavelength;
    this.customPSF = this.psf.customPSF;
  }

  closePopup() {
    this.close.emit();
  }

  onSlidersChanged() {
    this.slidersChanged.emit({
      airyDiskRadius: this.airyDiskRadius,
      seeingIndex: this.seeingIndex,
      diffractionIntensity: this.diffractionIntensity,
      wavelength: this.wavelength,
      customPSF: this.customPSF,
    });
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }
}