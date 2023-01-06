import { Component, OnInit } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import version from '../../package.json';
import { AboutComponent } from './about/about.component';
import { LuckyStackWorkerService } from './luckystackworker.service';
import { Profile } from './model/profile';
import { NewVersionComponent } from './new_version/newversion.component';

const SERVICE_POLL_DELAY_MS = 250;

interface ProfileSelection {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  profiles: ProfileSelection[] = [
    { value: 'mer', viewValue: 'Mercury' },
    { value: 'ven', viewValue: 'Venus' },
    { value: 'moon', viewValue: 'Moon' },
    { value: 'sun', viewValue: 'Sun' },
    { value: 'mars', viewValue: 'Mars' },
    { value: 'jup', viewValue: 'Jupiter' },
    { value: 'sat', viewValue: 'Saturn' },
    { value: 'uranus', viewValue: 'Uranus' },
    { value: 'neptune', viewValue: 'Neptune' },
  ];

  title = 'LuckyStackWorker';

  // sharpen
  radius: number;
  amount: number;
  iterations: number;
  sharpenMode: string;
  clippingStrength: number;
  clippingRange: number;

  denoiseAmount: number;
  denoiseSigma: string;
  denoiseRadius: number;
  denoiseIterations: number;
  savitzkyGolaySize: string;
  savitzkyGolayAmount: number;
  savitzkyGolayIterations: number;
  gamma: number;
  contrast: number;
  brightness: number;
  background: number;
  saturation: number;
  red: number;
  green: number;
  blue: number;
  profile: Profile;

  selectedProfile: string;
  rootFolder: string = 'C:\\';
  workerStatus: string = 'Idle';
  workerProgress: number;
  refImageSelected: boolean = false;
  nightMode: boolean = false;
  crop: boolean = false;
  _showSpinner = false;
  latestKnownVersion = version.version;
  private isLargeImage: boolean = false;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  constructor(
    private luckyStackWorkerService: LuckyStackWorkerService,
    private aboutSnackbar: MatSnackBar,
    private newVersionSnackbar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.showSpinner();
    this.pollSelectedProfile();
  }

  openReferenceImage() {
    console.log('openReferenceImage called');
    const base64EncodedPath = btoa(this.rootFolder);
    this.showSpinner();
    this.luckyStackWorkerService
      .openReferenceImage(base64EncodedPath)
      .subscribe(
        (data) => {
          console.log(data);
          this.refImageSelected = true;
          this.crop = false;
          if (data && data.amount > 0) {
            this.profile = data;
            this.selectedProfile = data.name;
            this.rootFolder = data.rootFolder;
            this.updateProfileSettings();
          }
          this.hideSpinner();
        },
        (error) => {
          console.log(error);
          this.hideSpinner();
        }
      );
  }

  saveReferenceImage() {
    console.log('saveReferenceImage called');
    this.showSpinner();
    this.luckyStackWorkerService.saveReferenceImage(this.rootFolder).subscribe(
      (data) => {
        this.hideSpinner();
        console.log(data);
      },
      (error) => {
        this.hideSpinner();
        console.log(error);
      }
    );
  }

  applyProfile() {
    console.log('applyProfile called');
    this.showSpinner();
    this.luckyStackWorkerService.applyProfile(this.profile).subscribe(
      (data) => {
        console.log(data);
        this.waitForWorker();
      },
      (error) => {
        console.log(error);
        this.hideSpinner();
      }
    );
    this.workerStatus = 'Working';
    this.workerProgress = 0;
  }

  radiusChanged(event: any, update: boolean) {
    this.profile.radius = event.value;
    this.radius = event.value;
    this.profile.operation = 'radius';
    console.log('radiusChanged called: ' + this.profile.radius);
    if (update) {
      this.updateProfile();
    }
  }

  amountChanged(event: any, update: boolean) {
    this.profile.amount = event.value;
    this.amount = event.value;
    this.profile.operation = 'amount';
    console.log('amountChanged called: ' + this.profile.amount);
    if (update) {
      this.updateProfile();
    }
  }

  iterationsChanged(event: any, update: boolean) {
    this.profile.iterations = event.value;
    this.iterations = event.value;
    this.profile.operation = 'iterations';
    console.log('iterationsChanged called: ' + this.profile.iterations);
    if (update) {
      this.updateProfile();
    }
  }

  clippingStrengthChanged(event: any, update: boolean) {
    this.profile.clippingStrength = event.value;
    this.clippingStrength = event.value;
    this.profile.operation = 'clippingStrength';
    console.log('clippingStrength called: ' + this.profile.clippingStrength);
    if (update) {
      this.updateProfile();
    }
  }

  clippingRangeChanged(event: any, update: boolean) {
    this.profile.clippingRange = event.value;
    this.clippingRange = event.value;
    this.profile.operation = 'clippingRange';
    console.log('clippingRange called: ' + this.profile.clippingRange);
    if (update) {
      this.updateProfile();
    }
  }

  sharpenModeChanged(event: any, update: boolean) {
    this.profile.sharpenMode = event.value;
    this.sharpenMode = event.value;
    this.profile.operation = 'sharpenMode';
    console.log('sharpenMode called: ' + this.profile.sharpenMode);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseAmountChanged(event: any, update: boolean) {
    this.profile.denoise = event.value;
    this.denoiseAmount = event.value;
    this.profile.operation = 'denoiseAmount';
    console.log('denoiseAmountChanged called: ' + this.profile.denoise);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseSigmaChanged(event: any, update: boolean) {
    this.profile.denoiseSigma = +event.value;
    this.denoiseSigma = event.value;
    this.profile.operation = 'denoiseSigma';
    console.log('denoiseSigmaChanged called: ' + this.profile.denoiseSigma);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseRadiusChanged(event: any, update: boolean) {
    this.profile.denoiseRadius = event.value;
    this.denoiseRadius = event.value;
    this.profile.operation = 'denoiseRadius';
    console.log('denoiseRadiusChanged called: ' + this.profile.denoiseRadius);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseIterationsChanged(event: any, update: boolean) {
    this.profile.denoiseIterations = event.value;
    this.denoiseIterations = event.value;
    this.profile.operation = 'denoiseIterations';
    console.log(
      'denoiseIterationsChanged called: ' + this.profile.denoiseRadius
    );
    if (update) {
      this.updateProfile();
    }
  }

  gammaChanged(event: any, update: boolean) {
    this.profile.gamma = event.value;
    this.gamma = event.value;
    this.profile.operation = 'gamma';
    console.log('gammaChanged called: ' + this.profile.gamma);
    if (update) {
      this.updateProfile();
    }
  }

  contrastChanged(event: any, update: boolean) {
    this.profile.contrast = event.value;
    this.contrast = event.value;
    this.profile.operation = 'contrast';
    console.log('contrastChanged called: ' + this.profile.contrast);
    if (update) {
      this.updateProfile();
    }
  }

  brightnessChanged(event: any, update: boolean) {
    this.profile.brightness = event.value;
    this.brightness = event.value;
    this.profile.operation = 'brightness';
    console.log('brightnessChanged called: ' + this.profile.brightness);
    if (update) {
      this.updateProfile();
    }
  }

  backgroundChanged(event: any, update: boolean) {
    this.profile.background = event.value;
    this.background = event.value;
    this.profile.operation = 'background';
    console.log('backgroundChanged called: ' + this.profile.background);
    if (update) {
      this.updateProfile();
    }
  }

  saturationChanged(event: any, update: boolean) {
    this.profile.saturation = event.value;
    this.saturation = event.value;
    this.profile.operation = 'saturation';
    console.log('saturationChanged called: ' + this.profile.saturation);
    if (update) {
      this.updateProfile();
    }
  }

  greenChanged(event: any, update: boolean) {
    this.profile.green = event.value;
    this.green = event.value;
    this.profile.operation = 'green';
    console.log('greenChanged called: ' + this.profile.green);
    if (update) {
      this.updateProfile();
    }
  }

  redChanged(event: any, update: boolean) {
    this.profile.red = event.value;
    this.red = event.value;
    this.profile.operation = 'red';
    console.log('redChanged called: ' + this.profile.red);
    if (update) {
      this.updateProfile();
    }
  }

  blueChanged(event: any, update: boolean) {
    this.profile.blue = event.value;
    this.profile.operation = 'blue';
    this.blue = event.value;
    console.log('blueChanged called: ' + this.profile.blue);
    if (update) {
      this.updateProfile();
    }
  }

  profileSelectionChanged(event: any) {
    console.log('profileChanged called: ' + this.selectedProfile);
    this.luckyStackWorkerService.getProfile(this.selectedProfile).subscribe(
      (data) => {
        console.log(data);
        this.profile = data;
        this.updateProfileSettings();
      },
      (error) => console.log(error)
    );
  }

  savitzkyGolayIterationsChanged(event: any, update: boolean) {
    this.profile.savitzkyGolayIterations = event.value;
    this.savitzkyGolayIterations = event.value;
    this.profile.operation = 'savitzkyGolayIterations';
    console.log(
      'savitzkyGolayIterationsChanged called: ' +
        this.profile.savitzkyGolayIterations
    );
    if (update) {
      this.updateProfile();
    }
  }

  savitzkyGolayAmountChanged(event: any, update: boolean) {
    this.profile.savitzkyGolayAmount = event.value;
    this.savitzkyGolayAmount = event.value;
    this.profile.operation = 'savitzkyGolayAmount';
    console.log(
      'savitzkyGolayAmountChanged called: ' + this.profile.savitzkyGolayAmount
    );
    if (update) {
      this.updateProfile();
    }
  }

  savitzkyGolaySizeChanged(event: any) {
    this.profile.savitzkyGolaySize = +event.value;
    this.savitzkyGolaySize = event.value;
    this.profile.operation = 'savitzkyGolaySize';
    console.log(
      'savitzkyGolaySizeChanged called: ' + this.profile.savitzkyGolaySize
    );
    this.updateProfile();
  }

  get savitzkyGolaySizeDisplayValue(): number {
    if (this.savitzkyGolaySize === '0') {
      return 0;
    } else if (this.savitzkyGolaySize === '2') {
      return 25;
    } else if (this.savitzkyGolaySize === '3') {
      return 49;
    } else if (this.savitzkyGolaySize === '4') {
      return 81;
    }
    return 0;
  }

  selectRootFolder() {
    console.log('selectRootFolder called');
    this.showSpinner();
    this.luckyStackWorkerService.selectRootFolder().subscribe(
      (data) => {
        console.log(data);
        if (data) {
          this.rootFolder = data.rootFolder;
        }
        this.hideSpinner();
      },
      (error) => {
        console.log(error);
        this.hideSpinner();
      }
    );
  }

  exit() {
    console.log('exit called');
    this.showSpinner();
    this.luckyStackWorkerService.exit().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
    // Wait for the background to shutdown gracefully, it takes about 8 seconds
    // but that is too long. Assuming that the user won't immediately re-open.
    setTimeout(() => this.shutdown(), 4000);
  }

  private shutdown() {
    window.close();
  }

  openAbout() {
    this.aboutSnackbar.openFromComponent(AboutComponent, {
      horizontalPosition: 'center',
      verticalPosition: 'top',
    });
  }

  loadProfile() {
    console.log('loadProfile called:');
    this.showSpinner();
    this.luckyStackWorkerService.loadProfile().subscribe(
      (data) => {
        if (data) {
          console.log(data);
          this.profile = data;
          this.selectedProfile = data.name;
          this.updateProfileSettings();
        }
        this.hideSpinner();
      },
      (error) => console.log(error)
    );
  }

  private updateProfileSettings() {
    this.radius = this.profile.radius;
    this.amount = this.profile.amount;
    this.iterations = this.profile.iterations;
    this.denoiseAmount = this.profile.denoise;
    this.denoiseSigma = this.profile.denoiseSigma.toString();
    this.denoiseRadius = this.profile.denoiseRadius;
    this.denoiseIterations = this.profile.denoiseIterations;
    this.savitzkyGolayIterations = this.profile.savitzkyGolayIterations;
    this.savitzkyGolayAmount = this.profile.savitzkyGolayAmount;
    this.savitzkyGolaySize = this.profile.savitzkyGolaySize.toString();
    this.clippingStrength = this.profile.clippingStrength;
    this.clippingRange = this.profile.clippingRange;
    this.sharpenMode = this.profile.sharpenMode;
    this.saturation = this.profile.saturation;
    this.gamma = this.profile.gamma;
    this.contrast = this.profile.contrast;
    this.brightness = this.profile.brightness;
    this.background = this.profile.background;
    this.red = this.profile.red;
    this.green = this.profile.green;
    this.blue = this.profile.blue;
    this.isLargeImage = this.profile.largeImage;
  }

  private updateProfile() {
    if (this.isLargeImage) {
      this.showSpinner();
    }
    this.luckyStackWorkerService.updateProfile(this.profile).subscribe(
      (data) => {
        console.log(data);
        if (this.isLargeImage) {
          this.hideSpinner();
        }
      },
      (error) => {
        console.log(error);
        if (this.profile.largeImage) {
          this.hideSpinner();
        }
      }
    );
  }

  private waitForWorker() {
    this.getStatusUpdate();
    if ('Idle' !== this.workerStatus) {
      console.log(this.workerStatus);
      setTimeout(() => this.waitForWorker(), SERVICE_POLL_DELAY_MS);
    } else {
      console.log('Worker is done!');
      this.hideSpinner();
    }
  }

  private getStatusUpdate() {
    this.luckyStackWorkerService.getStatus().subscribe(
      (data) => {
        console.log(data);
        this.workerStatus = data.message;
        this.workerProgress = Math.round(
          (data.filesProcessedCount / data.totalfilesCount) * 100
        );
      },
      (error) => console.log(error)
    );
  }

  private pollSelectedProfile() {
    this.luckyStackWorkerService.getSelectedProfile().subscribe(
      (data) => {
        console.log(data);
        this.refImageSelected = true;
        if (data) {
          this.profile = data;
          this.selectedProfile = data.name;
          this.rootFolder = data.rootFolder;
          this.updateProfileSettings();
          this.hideSpinner();
          this.checkLatestVersion();
        } else {
          console.log('Profile was not yet selected');
          setTimeout(() => this.pollSelectedProfile(), SERVICE_POLL_DELAY_MS);
        }
      },
      (error) => {
        console.log('Profile was not yet selected');
        setTimeout(() => this.pollSelectedProfile(), SERVICE_POLL_DELAY_MS);
      }
    );
  }

  buttonBarEnabled() {
    return (
      'Idle' === this.workerStatus &&
      this.refImageSelected &&
      !this._showSpinner
    );
  }

  openRefImageEnabled() {
    return 'Idle' === this.workerStatus && !this._showSpinner;
  }

  zoomIn() {
    console.log('zoomIn called');
    this.luckyStackWorkerService.zoomIn().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  zoomOut() {
    console.log('zoomOut called');
    this.luckyStackWorkerService.zoomOut().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  nightModeEnabled(): boolean {
    if (this.nightMode) {
      document.body.style.backgroundColor = '#000000';
    } else {
      document.body.style.backgroundColor = 'rgb(43,43,43)';
    }
    return this.nightMode;
  }

  cropSelectionChanged() {
    console.log('cropSelectionChanged called');
    this.luckyStackWorkerService.cropSelectionChanged().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  colorTheme() {
    return this.nightMode ? this.componentColorNight : this.componentColor;
  }

  private showSpinner() {
    this._showSpinner = true;
  }

  private hideSpinner() {
    this._showSpinner = false;
  }

  shouldShowSpinner(): boolean {
    return this._showSpinner;
  }

  public getCurrentVersion() {
    return version.version;
  }

  public getLatestKnownVersion() {
    return this.latestKnownVersion;
  }

  public showVersionButton(): boolean {
    return version.version !== this.latestKnownVersion;
  }

  private checkLatestVersion() {
    this.luckyStackWorkerService.getLatestVersion().subscribe(
      (data) => {
        this.latestKnownVersion = data.latestVersion;
        if (data.newVersion) {
          this.newVersionSnackbar.openFromComponent(NewVersionComponent, {
            horizontalPosition: 'center',
            verticalPosition: 'top',
          });
        }
      },
      (error) => console.log(error)
    );
  }
}
