import { Component, OnInit } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacySnackBarRef } from '@angular/material/legacy-snack-bar';
import { timeout } from 'rxjs/operators';
import version from '../../../package.json';
import { LuckyStackWorkerService } from '../luckystackworker.service';

const SERVICE_POLL_DELAY_MS = 250;

@Component({
  selector: 'app-splash',
  templateUrl: './splash.component.html',
  styleUrls: ['./splash.component.css'],
  standalone: false,
})
export class SplashComponent implements OnInit {
  componentColor: ThemePalette = 'primary';

  constructor(
    public snackBarRef: MatLegacySnackBarRef<SplashComponent>,
    private luckyStackWorkerService: LuckyStackWorkerService
  ) {}

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
        error: (error) => {
          console.log('Backend is still starting or is unavailable: ' + error);
          setTimeout(() => this.checkHealth(), SERVICE_POLL_DELAY_MS);
        },
        complete: () => {
          console.log('Backend is up and running');
          this.snackBarRef.dismiss();
        },
      });
  }
}
