import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-newversion',
  templateUrl: './newversion.component.html',
  styleUrls: ['./newversion.component.css'],
  standalone: false,
})
export class NewVersionComponent implements OnInit {
  @Input() version: string;
  @Input() releaseNotes: string[];

  @Output() close = new EventEmitter<void>();

  ngOnInit(): void {}

  onClose() {
    this.close.emit();
  }
}
