import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.css'],
  standalone: false,
})
export class NotificationComponent {
  @Input() nightMode: boolean = false;
  @Input() spinnerShown = false;
  @Input() notificationText: string;
  @Output() close = new EventEmitter<any>();

  timeoutProgress: number = 0;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(private confirmationDialog: MatLegacyDialog) {}

  ngOnInit() {
    setTimeout(() => this.increaseTimerProgress(), 500);
  }

  closePopup() {
    this.close.emit();
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  private increaseTimerProgress() {
    if (this.timeoutProgress >= 100) {
      this.closePopup();
    } else {
      this.timeoutProgress += 5;
      setTimeout(() => this.increaseTimerProgress(), 250);
    }
  }
}
