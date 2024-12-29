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
  @Input() spinnerShown = false;
  @Output() close = new EventEmitter<void>();
  @Output() slidersChanged = new EventEmitter<any>();
  @Output() loadCustomPSF = new EventEmitter<any>();

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

  onAiryDiskRadiusUpdated(event: any) {
    this.airyDiskRadius = event.value;
  }
  onSeeingIndexUpdated(event: any) {
    this.seeingIndex = event.value;
  }
  onDiffractionIntensityUpdated(event: any) {
    this.diffractionIntensity = event.value;
  }

  onSlidersChanged() {
    console.log('onSlidersChanged called');
    this.slidersChanged.emit({
      airyDiskRadius: this.airyDiskRadius,
      seeingIndex: this.seeingIndex,
      diffractionIntensity: this.diffractionIntensity,
      wavelength: this.wavelength,
      customPSF: this.customPSF,
    });
  }

  onLoadCustomPSF() {
    this.loadCustomPSF.emit();
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }
}
