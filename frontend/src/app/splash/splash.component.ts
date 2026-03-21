import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { timeout } from 'rxjs/operators';
import version from '../../../package.json';
import { LuckyStackWorkerService } from '../luckystackworker.service';
import { Settings } from '../model/settings';

const SERVICE_POLL_DELAY_MS = 250;

@Component({
  selector: 'app-splash',
  templateUrl: './splash.component.html',
  styleUrls: ['./splash.component.css'],
  standalone: false,
})
export class SplashComponent implements OnInit {
  @Output() close = new EventEmitter<Settings>();

  componentColor: ThemePalette = 'primary';

  constructor(private luckyStackWorkerService: LuckyStackWorkerService) {}

  ngOnInit(): void {
    this.checkHealth();
  }

  colorTheme() {
    return this.componentColor;
  }

  getVersion() {
    return version.version;
  }

  private checkHealth() {
    this.luckyStackWorkerService
      .checkHealth()
      .pipe(timeout(2000))
      .subscribe({
        next: (data) => {
          console.log('Backend is up and running');
          this.close.emit(data);
        },
        error: (error) => {
          console.log('Backend is still starting or is unavailable: ' + error);
          setTimeout(() => this.checkHealth(), SERVICE_POLL_DELAY_MS);
        },
      });
  }
}
