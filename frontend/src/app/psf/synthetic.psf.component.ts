import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-synthetic-psf-panel',
  templateUrl: './synthetic.psf.component.html',
  styleUrls: ['./synthetic.psf.component.css'],
})
export class SyntheticPsfComponent {
  @Input() imagePath: string = '';
  @Output() close = new EventEmitter<void>();

  slider1Value: number = 50;
  slider2Value: number = 50;

  ngOnInit() {}
  closePopup() {
    this.close.emit();
  }
}
