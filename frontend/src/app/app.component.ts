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
  edgeArtefactSupressionMode: string;
  clippingStrength: number;
  clippingRange: number;
  deringRadius: number;
  deringStrength: number;

  // denoise
  denoiseAmount: number;
  denoiseSigma: string;
  denoiseRadius: number;
  denoiseIterations: number;
  savitzkyGolaySize: string;
  savitzkyGolayAmount: number;
  savitzkyGolayIterations: number;
  denoiseAlgorithm: string;

  //  light
  gamma: number;
  contrast: number;
  brightness: number;
  background: number;
  saturation: number;
  localContrastMode: string;
  localContrastFine: number;
  localContrastMedium: number;
  localContrastLarge: number;

  // color
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

  realtimeEnabled: boolean = true;

  constructor(
    private luckyStackWorkerService: LuckyStackWorkerService,
    private aboutSnackbar: MatSnackBar,
    private newVersionSnackbar: MatSnackBar
  ) {}

  ngOnInit(): void {
    document.addEventListener('keyup', (event) => {
      if (event.altKey) {
        event.preventDefault();
      }
    });
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
          this.workerProgress = 0;
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
    this.workerStatus = 'Working';
    this.workerProgress = 0;
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

  deringRadiusChanged(event: any, update: boolean) {
    this.profile.deringRadius = event.value;
    this.deringRadius = event.value;
    this.profile.operation = 'deringRadius';
    console.log('deringRadius called: ' + this.profile.deringRadius);
    if (update) {
      this.updateProfile();
    }
  }

  deringStrengthChanged(event: any, update: boolean) {
    this.profile.deringStrength = event.value;
    this.deringStrength = event.value;
    this.profile.operation = 'deringStrength';
    console.log('deringStrength called: ' + this.profile.deringStrength);
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

  localContrastModeChanged(event: any, update: boolean) {
    this.profile.localContrastMode = event.value;
    this.localContrastMode = event.value;
    this.profile.operation = 'localContrastMode';
    console.log('localContrastMode called: ' + this.profile.localContrastMode);
    if (update) {
      this.updateProfile();
    }
  }

  edgeArtefactSupressionModeChanged(event: any, update: boolean) {
    if (event.value === 'CLIPPING') {
      this.profile.deringStrength = 0;
      this.profile.operation = 'clippingStrength';
      if (!this.profile.clippingStrength) {
        // If not set, start with the defaults
        this.profile.clippingRange = 50;
        this.profile.clippingStrength = 10;
        this.clippingRange = 50;
        this.clippingStrength = 10;
      }
    } else if (event.value === 'DERING') {
      this.profile.clippingStrength = 0;
      this.profile.operation = 'deringStrength';
      if (!this.profile.deringStrength) {
        this.profile.deringRadius = 3;
        this.profile.deringStrength = 10;
        this.deringRadius = 3;
        this.deringStrength = 10;
      }
    } else {
      // Off
      this.profile.clippingStrength = 0;
      this.profile.deringStrength = 0;
    }
    console.log('edgeArtefactSupressionMode called: ' + event.value);
    this.updateProfile();
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

  localContrastFineChanged(event: any, update: boolean) {
    this.profile.localContrastFine = event.value;
    this.localContrastFine = event.value;
    this.profile.operation = 'localContrastFine';
    console.log('localContrastFine called: ' + this.profile.localContrastFine);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastMediumChanged(event: any, update: boolean) {
    this.profile.localContrastMedium = event.value;
    this.localContrastMedium = event.value;
    this.profile.operation = 'localContrastMedium';
    console.log(
      'localContrastMedium called: ' + this.profile.localContrastMedium
    );
    if (update) {
      this.updateProfile();
    }
  }

  localContrastLargeChanged(event: any, update: boolean) {
    this.profile.localContrastLarge = event.value;
    this.localContrastLarge = event.value;
    this.profile.operation = 'localContrastLarge';
    console.log(
      'localContrastLarge called: ' + this.profile.localContrastLarge
    );
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

  savitzkyGolaySizeChanged(event: any, update: boolean) {
    this.profile.savitzkyGolaySize = +event.value;
    this.savitzkyGolaySize = event.value;
    this.profile.operation = 'savitzkyGolaySize';
    console.log(
      'savitzkyGolaySizeChanged called: ' + this.profile.savitzkyGolaySize
    );
    if (update) {
      this.updateProfile();
    }
  }

  denoiseAlgorithmChanged(event: any) {
    if (event.value === 'SAVGOLAY') {
      this.profile.denoiseSigma = 0;
      this.profile.operation = 'savitzkyGolaySize';
      if (!this.profile.savitzkyGolaySize) {
        // If not set, start with the defaults
        this.profile.savitzkyGolaySize = 3;
        this.profile.savitzkyGolayAmount = 75;
        this.profile.savitzkyGolayIterations = 1;
        this.savitzkyGolaySize = '3';
        this.savitzkyGolayAmount = 75;
        this.savitzkyGolayIterations = 1;
      }
    } else if (event.value === 'SIGMA') {
      this.profile.savitzkyGolaySize = 0;
      this.profile.operation = 'denoiseSigma';
      if (!this.profile.denoiseSigma) {
        this.profile.denoiseSigma = 2;
        this.profile.denoise = 50;
        this.profile.denoiseRadius = 1;
        this.profile.denoiseIterations = 1;
        this.denoiseSigma = '2';
        this.denoiseAmount = 50;
        this.denoiseRadius = 1;
        this.denoiseIterations = 1;
      }
    } else {
      // Off
      this.profile.denoiseSigma = 0;
      this.profile.savitzkyGolaySize = 0;
    }
    console.log('denoiseAlgorithmChanged called: ' + event.value);
    this.updateProfile();
  }

  realtimeEnabledChanged() {
    console.log('realtimeEnabledChanged called: ' + this.realtimeEnabled);
    this.luckyStackWorkerService
      .realtimeChanged(this.realtimeEnabled)
      .subscribe(
        (data) => {
          console.log(data);
        },
        (error) => {
          console.log(error);
        }
      );
  }

  get savitzkyGolaySizeDisplayValue(): number {
    if (this.savitzkyGolaySize == '0') {
      return 0;
    } else if (this.savitzkyGolaySize == '2') {
      return 25;
    } else if (this.savitzkyGolaySize == '3') {
      return 49;
    } else if (this.savitzkyGolaySize == '4') {
      return 81;
    } else if (this.savitzkyGolaySize == '5') {
      return 121;
    } else if (this.savitzkyGolaySize == '6') {
      return 169;
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
    this.denoiseSigma = this.profile.denoiseSigma
      ? this.profile.denoiseSigma.toString()
      : '0';
    this.denoiseRadius = this.profile.denoiseRadius;
    this.denoiseIterations = this.profile.denoiseIterations;
    this.savitzkyGolayIterations = this.profile.savitzkyGolayIterations;
    this.savitzkyGolayAmount = this.profile.savitzkyGolayAmount;
    this.savitzkyGolaySize = this.profile.savitzkyGolaySize
      ? this.profile.savitzkyGolaySize.toString()
      : '0';
    if (!this.profile.denoiseSigma && !this.profile.savitzkyGolaySize) {
      this.denoiseAlgorithm = 'OFF';
    } else if (this.profile.savitzkyGolaySize) {
      this.denoiseAlgorithm = 'SAVGOLAY';
      // prevent that both are selected under the hood, and prefer savitzky-golay in that case.
      this.denoiseSigma = '0';
      this.profile.denoiseSigma = 0;
    } else {
      this.denoiseAlgorithm = 'SIGMA';
    }
    this.clippingStrength = this.profile.clippingStrength;
    this.clippingRange = this.profile.clippingRange;
    this.deringRadius = this.profile.deringRadius;
    this.deringStrength = this.profile.deringStrength;
    if (!this.profile.clippingStrength && !this.profile.deringStrength) {
      this.edgeArtefactSupressionMode = 'OFF';
    } else if (this.profile.deringStrength) {
      this.edgeArtefactSupressionMode = 'DERING';
      // prevent that both are selected under the hood, and prefer dering in that case.
      this.clippingStrength = 0;
      this.profile.clippingStrength = 0;
    } else {
      this.edgeArtefactSupressionMode = 'CLIPPING';
    }
    this.sharpenMode = this.profile.sharpenMode;
    this.saturation = this.profile.saturation;
    this.gamma = this.profile.gamma;
    this.contrast = this.profile.contrast;
    this.brightness = this.profile.brightness;
    this.background = this.profile.background;
    this.localContrastMode = this.profile.localContrastMode;
    this.localContrastFine = this.profile.localContrastFine;
    this.localContrastMedium = this.profile.localContrastMedium;
    this.localContrastLarge = this.profile.localContrastLarge;
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
      this.workerProgress = 100;
      this.hideSpinner();
    }
  }

  private getStatusUpdate() {
    this.luckyStackWorkerService.getStatus().subscribe(
      (data) => {
        console.log(data);
        this.workerStatus = data.message;
        if (data.filesProcessedCount && data.totalfilesCount) {
          this.workerProgress = Math.round(
            (data.filesProcessedCount / data.totalfilesCount) * 100
          );
        }
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
