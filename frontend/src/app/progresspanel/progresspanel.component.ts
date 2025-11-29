import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-progress-panel',
  templateUrl: './progresspanel.component.html',
  styleUrls: ['./progresspanel.component.css'],
  standalone: false,
})
export class ProgressPanelComponent {
  @Input() nightMode: boolean = false;
  @Input() spinnerShown = false;
  @Input() workerProgress: number;
  @Input() workerStatus: string;
  @Output() stop = new EventEmitter<any>();

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  getWorkerStatusDisplayText() {
    return this.workerStatus.length > 40
      ? this.workerStatus.substring(0, 40) + '...'
      : this.workerStatus;
  }

  stopWorker() {
    this.stop.emit();
  }
}
