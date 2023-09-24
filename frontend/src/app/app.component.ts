import { Component, OnInit } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import version from '../../package.json';
import { AboutComponent } from './about/about.component';
import { LuckyStackWorkerService } from './luckystackworker.service';
import { Profile } from './model/profile';
import { Settings } from './model/settings';
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
  luminanceIncludeRed: boolean = true;
  luminanceIncludeGreen: boolean = true;
  luminanceIncludeBlue: boolean = true;
  luminanceIncludeColor: boolean = true;

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
  settings: Settings;
  dispersionCorrectionEnabled: boolean = false;

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

  realtimeEnabled: boolean = false;
  showHistogram: boolean = true;
  scale: string = '1';

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
      .openReferenceImage(base64EncodedPath, Number(this.scale))
      .subscribe(
        (data) => {
          console.log(data);
          this.refImageSelected = true;
          this.crop = false;
          if (data && data.profile.amount > 0) {
            this.profile = data.profile;
            this.settings = data.settings;
            this.selectedProfile = this.profile.name;
            this.rootFolder = data.settings.rootFolder;
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
    this.luckyStackWorkerService.saveReferenceImage(this.profile).subscribe(
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

  scaleChanged(event: any) {
    console.log('scaleChanged called');
    this.profile.scale = event.value;
    this.scale = event.value;
    this.showSpinner();
    this.luckyStackWorkerService.scale(this.profile).subscribe(
      (data) => {
        console.log(data);
        this.refImageSelected = true;
        this.crop = false;
        if (data && data.profile.amount > 0) {
          this.profile = data.profile;
          this.settings = data.settings;
          this.selectedProfile = this.profile.name;
          this.rootFolder = data.settings.rootFolder;
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
    this.updateProfile();
  }

  radiusChanged(event: any, update: boolean) {
    this.profile.radius = event.value;
    this.radius = event.value;
    this.settings.operation = 'radius';
    console.log('radiusChanged called: ' + this.profile.radius);
    if (update) {
      this.updateProfile();
    }
  }

  amountChanged(event: any, update: boolean) {
    this.profile.amount = event.value;
    this.amount = event.value;
    this.settings.operation = 'amount';
    console.log('amountChanged called: ' + this.profile.amount);
    if (update) {
      this.updateProfile();
    }
  }

  iterationsChanged(event: any, update: boolean) {
    this.profile.iterations = event.value;
    this.iterations = event.value;
    this.settings.operation = 'iterations';
    console.log('iterationsChanged called: ' + this.profile.iterations);
    if (update) {
      this.updateProfile();
    }
  }

  clippingStrengthChanged(event: any, update: boolean) {
    this.profile.clippingStrength = event.value;
    this.clippingStrength = event.value;
    this.settings.operation = 'clippingStrength';
    console.log('clippingStrength called: ' + this.profile.clippingStrength);
    if (update) {
      this.updateProfile();
    }
  }

  clippingRangeChanged(event: any, update: boolean) {
    this.profile.clippingRange = event.value;
    this.clippingRange = event.value;
    this.settings.operation = 'clippingRange';
    console.log('clippingRange called: ' + this.profile.clippingRange);
    if (update) {
      this.updateProfile();
    }
  }

  deringRadiusChanged(event: any, update: boolean) {
    this.profile.deringRadius = event.value;
    this.deringRadius = event.value;
    this.settings.operation = 'deringRadius';
    console.log('deringRadius called: ' + this.profile.deringRadius);
    if (update) {
      this.updateProfile();
    }
  }

  deringStrengthChanged(event: any, update: boolean) {
    this.profile.deringStrength = event.value;
    this.deringStrength = event.value;
    this.settings.operation = 'deringStrength';
    console.log('deringStrength called: ' + this.profile.deringStrength);
    if (update) {
      this.updateProfile();
    }
  }

  sharpenModeChanged(event: any, update: boolean) {
    this.profile.sharpenMode = event ? event.value : this.sharpenMode;
    this.sharpenMode = event ? event.value : this.sharpenMode;
    if (
      !this.luminanceIncludeRed &&
      !this.luminanceIncludeGreen &&
      !this.luminanceIncludeBlue
    ) {
      this.luminanceIncludeRed = true;
      this.luminanceIncludeGreen = true;
      this.luminanceIncludeBlue = true;
    }
    this.profile.luminanceIncludeRed = this.luminanceIncludeRed;
    this.profile.luminanceIncludeGreen = this.luminanceIncludeGreen;
    this.profile.luminanceIncludeBlue = this.luminanceIncludeBlue;
    this.profile.luminanceIncludeColor = this.luminanceIncludeColor;
    this.settings.operation = 'sharpenMode';
    console.log('sharpenMode called: ' + this.profile.sharpenMode);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastModeChanged(event: any, update: boolean) {
    this.profile.localContrastMode = event.value;
    this.localContrastMode = event.value;
    this.settings.operation = 'localContrastMode';
    console.log('localContrastMode called: ' + this.profile.localContrastMode);
    if (update) {
      this.updateProfile();
    }
  }

  edgeArtefactSupressionModeChanged(event: any, update: boolean) {
    if (event.value === 'CLIPPING') {
      this.profile.deringStrength = 0;
      this.settings.operation = 'clippingStrength';
      if (!this.profile.clippingStrength) {
        // If not set, start with the defaults
        this.profile.clippingRange = 50;
        this.profile.clippingStrength = 10;
        this.clippingRange = 50;
        this.clippingStrength = 10;
      }
    } else if (event.value === 'DERING') {
      this.profile.clippingStrength = 0;
      this.settings.operation = 'deringStrength';
      if (!this.profile.deringStrength) {
        this.profile.deringRadius = 3;
        this.profile.deringStrength = 10;
        this.deringRadius = 3;
        this.deringStrength = 10;
      }
    } else {
      // Off
      this.settings.operation = 'deringStrength';
      this.profile.clippingStrength = 0;
      this.profile.deringStrength = 0;
    }
    console.log('edgeArtefactSupressionMode called: ' + event.value);
    this.updateProfile();
  }

  denoiseAmountChanged(event: any, update: boolean) {
    this.profile.denoise = event.value;
    this.denoiseAmount = event.value;
    this.settings.operation = 'denoiseAmount';
    console.log('denoiseAmountChanged called: ' + this.profile.denoise);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseSigmaChanged(event: any, update: boolean) {
    this.profile.denoiseSigma = +event.value;
    this.denoiseSigma = event.value;
    this.settings.operation = 'denoiseSigma';
    console.log('denoiseSigmaChanged called: ' + this.profile.denoiseSigma);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseRadiusChanged(event: any, update: boolean) {
    this.profile.denoiseRadius = event.value;
    this.denoiseRadius = event.value;
    this.settings.operation = 'denoiseRadius';
    console.log('denoiseRadiusChanged called: ' + this.profile.denoiseRadius);
    if (update) {
      this.updateProfile();
    }
  }

  denoiseIterationsChanged(event: any, update: boolean) {
    this.profile.denoiseIterations = event.value;
    this.denoiseIterations = event.value;
    this.settings.operation = 'denoiseIterations';
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
    this.settings.operation = 'gamma';
    console.log('gammaChanged called: ' + this.profile.gamma);
    if (update) {
      this.updateProfile();
    }
  }

  contrastChanged(event: any, update: boolean) {
    this.profile.contrast = event.value;
    this.contrast = event.value;
    this.settings.operation = 'contrast';
    console.log('contrastChanged called: ' + this.profile.contrast);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastFineChanged(event: any, update: boolean) {
    this.profile.localContrastFine = event.value;
    this.localContrastFine = event.value;
    this.settings.operation = 'localContrastFine';
    console.log('localContrastFine called: ' + this.profile.localContrastFine);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastMediumChanged(event: any, update: boolean) {
    this.profile.localContrastMedium = event.value;
    this.localContrastMedium = event.value;
    this.settings.operation = 'localContrastMedium';
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
    this.settings.operation = 'localContrastLarge';
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
    this.settings.operation = 'brightness';
    console.log('brightnessChanged called: ' + this.profile.brightness);
    if (update) {
      this.updateProfile();
    }
  }

  backgroundChanged(event: any, update: boolean) {
    this.profile.background = event.value;
    this.background = event.value;
    this.settings.operation = 'background';
    console.log('backgroundChanged called: ' + this.profile.background);
    if (update) {
      this.updateProfile();
    }
  }

  saturationChanged(event: any, update: boolean) {
    this.profile.saturation = event.value;
    this.saturation = event.value;
    this.settings.operation = 'saturation';
    console.log('saturationChanged called: ' + this.profile.saturation);
    if (update) {
      this.updateProfile();
    }
  }

  greenChanged(event: any, update: boolean) {
    this.profile.green = event.value;
    this.green = event.value;
    this.settings.operation = 'green';
    console.log('greenChanged called: ' + this.profile.green);
    if (update) {
      this.updateProfile();
    }
  }

  redChanged(event: any, update: boolean) {
    this.profile.red = event.value;
    this.red = event.value;
    this.settings.operation = 'red';
    console.log('redChanged called: ' + this.profile.red);
    if (update) {
      this.updateProfile();
    }
  }

  blueChanged(event: any, update: boolean) {
    this.profile.blue = event.value;
    this.settings.operation = 'blue';
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
    this.settings.operation = 'savitzkyGolayIterations';
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
    this.settings.operation = 'savitzkyGolayAmount';
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
    this.settings.operation = 'savitzkyGolaySize';
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
      this.settings.operation = 'savitzkyGolaySize';
      if (!this.profile.savitzkyGolaySize) {
        // If not set, start with the defaults
        this.profile.savitzkyGolaySize = 3;
        this.profile.savitzkyGolayAmount = 100;
        this.profile.savitzkyGolayIterations = 1;
        this.savitzkyGolaySize = '3';
        this.savitzkyGolayAmount = 100;
        this.savitzkyGolayIterations = 1;
      }
    } else if (event.value === 'SIGMA') {
      this.profile.savitzkyGolaySize = 0;
      this.settings.operation = 'denoiseSigma';
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
      this.settings.operation = 'savitzkyGolaySize';
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

  dispersionCorrectionEnabledChanged() {
    console.log(
      'dispersionCorrectionEnabledChanged called: ' +
        this.dispersionCorrectionEnabled
    );
    this.settings.operation = 'dispersionCorrection';
    this.profile.dispersionCorrectionEnabled = this.dispersionCorrectionEnabled;
    this.resetDispersionCorrection();
  }

  resetDispersionCorrection() {
    console.log('resetDispersionCorrection called');
    this.settings.operation = 'dispersionCorrection';
    this.profile.dispersionCorrectionRedX = 0;
    this.profile.dispersionCorrectionRedY = 0;
    this.profile.dispersionCorrectionBlueX = 0;
    this.profile.dispersionCorrectionBlueY = 0;
    this.updateProfile();
  }

  dispersionCorrectionClicked(direction: string, color: string) {
    console.log('dispersionCorrectionClicked called: ' + direction);
    this.settings.operation = 'dispersionCorrection';
    if (color === 'RED') {
      switch (direction) {
        case 'LEFT-UP':
          this.profile.dispersionCorrectionRedX -= 1;
          this.profile.dispersionCorrectionRedY -= 1;
          break;
        case 'UP':
          this.profile.dispersionCorrectionRedY -= 1;
          break;
        case 'RIGHT-UP':
          this.profile.dispersionCorrectionRedX += 1;
          this.profile.dispersionCorrectionRedY -= 1;
          break;
        case 'LEFT':
          this.profile.dispersionCorrectionRedX -= 1;
          break;
        case 'RIGHT':
          this.profile.dispersionCorrectionRedX += 1;
          break;
        case 'LEFT-DOWN':
          this.profile.dispersionCorrectionRedX -= 1;
          this.profile.dispersionCorrectionRedY += 1;
          break;
        case 'DOWN':
          this.profile.dispersionCorrectionRedY += 1;
          break;
        case 'RIGHT-DOWN':
          this.profile.dispersionCorrectionRedX += 1;
          this.profile.dispersionCorrectionRedY += 1;
          break;
      }
      this.profile.dispersionCorrectionRedX;
    } else if (color === 'BLUE') {
      switch (direction) {
        case 'LEFT-UP':
          this.profile.dispersionCorrectionBlueX -= 1;
          this.profile.dispersionCorrectionBlueY -= 1;
          break;
        case 'UP':
          this.profile.dispersionCorrectionBlueY -= 1;
          break;
        case 'RIGHT-UP':
          this.profile.dispersionCorrectionBlueX += 1;
          this.profile.dispersionCorrectionBlueY -= 1;
          break;
        case 'LEFT':
          this.profile.dispersionCorrectionBlueX -= 1;
          break;
        case 'RIGHT':
          this.profile.dispersionCorrectionBlueX += 1;
          break;
        case 'LEFT-DOWN':
          this.profile.dispersionCorrectionBlueX -= 1;
          this.profile.dispersionCorrectionBlueY += 1;
          break;
        case 'DOWN':
          this.profile.dispersionCorrectionBlueY += 1;
          break;
        case 'RIGHT-DOWN':
          this.profile.dispersionCorrectionBlueX += 1;
          this.profile.dispersionCorrectionBlueY += 1;
          break;
      }
    }
    this.updateProfile();
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

  get dispersionCorrectorTitle() {
    return (
      'You should always use a atmospheric dispersion corrector (ADC) with an OSC camera. This dispersion correction function is only meant for minimal corrections,' +
      ' in case you have any left over dispersion in your recordings. The dispersion correction is not persisted with the object profile' +
      ' since it will be different each time.'
    );
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
          this.profile = data.profile;
          this.settings = data.settings;
          this.selectedProfile = this.profile.name;
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
    this.isLargeImage = this.settings.largeImage;
    this.setNonPersistentSettings();
  }

  private setNonPersistentSettings() {
    this.dispersionCorrectionEnabled = false;
    this.luminanceIncludeRed = true;
    this.luminanceIncludeGreen = true;
    this.luminanceIncludeBlue = true;
    this.luminanceIncludeColor = true;
  }

  private updateProfile() {
    if (this.isLargeImage) {
      this.showSpinner();
    }
    this.luckyStackWorkerService
      .updateProfile(this.profile, this.settings.operation)
      .subscribe(
        (data) => {
          console.log(data);
          if (this.isLargeImage) {
            this.hideSpinner();
          }
        },
        (error) => {
          console.log(error);
          if (this.isLargeImage) {
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
          this.profile = data.profile;
          this.settings = data.settings;
          this.selectedProfile = this.profile.name;
          this.rootFolder = data.settings.rootFolder;
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

  nightModeChanged() {
    console.log('nightModeChanged called');
    this.luckyStackWorkerService.nightModeChanged(this.nightMode).subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
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

  histogramSelectionChanged() {
    console.log('cropSelectionChanged called');
    this.luckyStackWorkerService.histogramSelectionChanged().subscribe(
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
