import { Component, OnInit } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacySnackBar } from '@angular/material/legacy-snack-bar';
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

  openImageModes: ProfileSelection[] = [
    { value: 'RGB', viewValue: 'RGB' },
    { value: 'RGB', viewValue: 'Gray' },
    { value: 'RED', viewValue: 'Red' },
    { value: 'GREEN', viewValue: 'Green' },
    { value: 'BLUE', viewValue: 'Blue' },
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
  deringThreshold: number;
  luminanceIncludeRed: boolean = true;
  luminanceIncludeGreen: boolean = true;
  luminanceIncludeBlue: boolean = true;
  luminanceIncludeColor: boolean = true;

  // denoise 1
  denoiseAlgorithm1: string;

  denoise1Amount: number;
  denoise1Radius: number;
  denoise1Iterations: number;

  iansAmount: number;
  iansRecovery: number;

  // denoise 2
  denoiseAlgorithm2: string;

  denoise2Radius: number;
  denoise2Iterations: number;

  savitzkyGolaySize: string;
  savitzkyGolayAmount: number;
  savitzkyGolayIterations: number;

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
  equalizeLocalHistograms: number;

  // color
  red: number;
  green: number;
  blue: number;
  purple: number;
  dispersionCorrectionEnabled: boolean = false;

  profile: Profile;
  settings: Settings;
  selectedProfile: string;
  rootFolder: string = 'C:\\';
  workerStatus: string = 'Idle';
  workerProgress: number;
  refImageSelected: boolean = true; // TODO: set to false by default!
  nightMode: boolean = false;
  crop: boolean = false;
  _showSpinner = false;
  latestKnownVersion = version.version;
  private slowProcessing: boolean = false;
  zoomFactor: number = 0;
  isMaximized: boolean = false;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  realtimeEnabled: boolean = false;
  showHistogram: boolean = true;
  scale: string = '1';
  openImageMode: string = 'RGB';

  constructor(
    private luckyStackWorkerService: LuckyStackWorkerService,
    private aboutSnackbar: MatLegacySnackBar,
    private newVersionSnackbar: MatLegacySnackBar
  ) {}

  ngOnInit(): void {
    document.addEventListener('keyup', (event) => {
      if (event.altKey) {
        event.preventDefault();
      }
    });
  }

  openReferenceImage() {
    console.log('openReferenceImage called');
    this.showSpinner();
    this.callopenReferenceImage();
  }

  private callopenReferenceImage() {
    this.luckyStackWorkerService
      .openReferenceImage(Number(this.scale), this.openImageMode)
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
            this.zoomFactor = data.settings.zoomFactor;
            this.checkLatestVersion();
            this.updateProfileSettings();
          }
          this.hideSpinner();
          this.workerProgress = 0;
        },
        (error) => {
          console.log('Backend is still starting or is unavailable');
          setTimeout(
            () => this.callopenReferenceImage(),
            SERVICE_POLL_DELAY_MS
          );
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

  scaleChanged() {
    console.log('scaleChanged called');
    if (this.profile) {
      this.profile.scale = Number(this.scale);
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
    }
  }

  openImageModeChanged() {
    console.log('openImageModeChanged called');
    if (this.profile) {
      this.profile.openImageMode = this.openImageMode;
    }
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

  deringThresholdChanged(event: any, update: boolean) {
    this.profile.deringThreshold = event.value;
    this.deringThreshold = event.value;
    this.settings.operation = 'deringThreshold';
    console.log('deringThreshold called: ' + this.profile.deringThreshold);
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

  denoise1AmountChanged(event: any, update: boolean) {
    this.denoise1Amount = event.value;
    this.profile.denoise1Amount = event.value;
    this.settings.operation = 'denoise1Amount';
    console.log('denoise1AmountChanged called: ' + this.profile.denoise1Amount);
    if (update) {
      this.updateProfile();
    }
  }

  denoise1RadiusChanged(event: any, update: boolean) {
    this.profile.denoise1Radius = event.value;
    this.denoise1Radius = event.value;
    this.settings.operation = 'denoise1Radius';
    console.log('denoise1RadiusChanged called: ' + this.profile.denoise1Radius);
    if (update) {
      this.updateProfile();
    }
  }

  denoise1IterationsChanged(event: any, update: boolean) {
    this.profile.denoise1Iterations = event.value;
    this.denoise1Iterations = event.value;
    this.settings.operation = 'denoise1Iterations';
    console.log(
      'denoise1IterationsChanged called: ' + this.profile.denoise1Iterations
    );
    if (update) {
      this.updateProfile();
    }
  }

  iansAmountChanged(event: any, update: boolean) {
    this.profile.iansAmount = event.value;
    this.iansAmount = event.value;
    this.settings.operation = 'iansAmount';
    console.log('iansAmount called: ' + this.profile.iansAmount);
    if (update) {
      this.updateProfile();
    }
  }

  iansRecoveryChanged(event: any, update: boolean) {
    this.profile.iansRecovery = event.value;
    this.iansRecovery = event.value;
    this.settings.operation = 'iansRecovery';
    console.log('iansRecovery called: ' + this.profile.iansRecovery);
    if (update) {
      this.updateProfile();
    }
  }

  denoise2RadiusChanged(event: any, update: boolean) {
    this.profile.denoise2Radius = event.value;
    this.denoise2Radius = event.value;
    this.settings.operation = 'denoise2Radius';
    console.log('denoise2RadiusChanged called: ' + this.profile.denoise2Radius);
    if (update) {
      this.updateProfile();
    }
  }

  denoise2IterationsChanged(event: any, update: boolean) {
    this.profile.denoise2Iterations = event.value;
    this.denoise2Iterations = event.value;
    this.settings.operation = 'denoise2Iterations';
    console.log(
      'denoise2IterationsChanged called: ' + this.profile.denoise2Iterations
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

  equalizeLocalHistogramsChanged(event: any, update: boolean) {
    this.profile.equalizeLocalHistogramsStrength = event.value;
    this.equalizeLocalHistograms = event.value;
    this.settings.operation = 'equalizeLocalHistogramsStrength';
    console.log(
      'equalizeLocalHistograms called: ' +
        this.profile.equalizeLocalHistogramsStrength
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
    this.profile.green = -event.value;
    this.green = event.value;
    this.settings.operation = 'green';
    console.log('greenChanged called: ' + this.profile.green);
    if (update) {
      this.updateProfile();
    }
  }

  redChanged(event: any, update: boolean) {
    this.profile.red = -event.value;
    this.red = event.value;
    this.settings.operation = 'red';
    console.log('redChanged called: ' + this.profile.red);
    if (update) {
      this.updateProfile();
    }
  }

  blueChanged(event: any, update: boolean) {
    this.profile.blue = -event.value;
    this.settings.operation = 'blue';
    this.blue = event.value;
    console.log('blueChanged called: ' + this.profile.blue);
    if (update) {
      this.updateProfile();
    }
  }

  purpleChanged(event: any, update: boolean) {
    this.profile.purple = event.value;
    this.settings.operation = 'purple';
    this.purple = event.value;
    console.log('purpleChanged called: ' + this.profile.purple);
    if (update) {
      this.updateProfile();
    }
  }

  profileSelectionChanged() {
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

  denoiseAlgorithm1Changed(event: any) {
    if (event.value === 'SIGMA1') {
      this.settings.operation = 'denoise1Amount';
      this.profile.denoiseAlgorithm1 = 'SIGMA1';
    } else if (event.value === 'IAN') {
      this.settings.operation = 'iansAmount';
      this.profile.denoiseAlgorithm1 = 'IAN';
    } else {
      this.settings.operation = 'DENOISEALGORITHM1';
      this.profile.denoiseAlgorithm1 = 'OFF';
    }
    console.log('denoiseAlgorithm1Changed called: ' + event.value);
    this.updateProfile();
  }

  denoiseAlgorithm2Changed(event: any) {
    if (event.value === 'SAVGOLAY') {
      this.settings.operation = 'savitzkyGolayAmount';
      this.profile.denoiseAlgorithm2 = 'SAVGOLAY';
    } else if (event.value === 'SIGMA2') {
      this.settings.operation = 'denoise2Iterations';
      this.profile.denoiseAlgorithm2 = 'SIGMA2';
    } else {
      this.settings.operation = 'DENOISEALGORITHM2';
      this.profile.denoiseAlgorithm2 = 'OFF';
    }
    console.log('denoiseAlgorithm1Changed called: ' + event.value);
    this.updateProfile();
  }

  realtimeEnabledChanged() {
    console.log('realtimeEnabledChanged called: ' + this.realtimeEnabled);
    this.realtimeEnabled = !this.realtimeEnabled;
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
    this.dispersionCorrectionEnabled = !this.dispersionCorrectionEnabled;
    this.settings.operation = 'dispersionCorrection';
    this.profile.dispersionCorrectionEnabled = this.dispersionCorrectionEnabled;
    this.resetDispersionCorrection();
  }

  dispersionCorrectionIsEnabled() {
    return this.dispersionCorrectionEnabled;
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

  stopWorker() {
    console.log('Stop worker called');
    this.luckyStackWorkerService.stop().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  private updateProfileSettings() {
    this.radius = this.profile.radius;
    this.amount = this.profile.amount;
    this.iterations = this.profile.iterations;

    this.denoiseAlgorithm1 = this.profile.denoiseAlgorithm1;
    this.denoise1Amount = this.profile.denoise1Amount;
    this.denoise1Radius = this.profile.denoise1Radius;
    this.denoise1Iterations = this.profile.denoise1Iterations;
    this.iansAmount = this.profile.iansAmount;
    this.iansRecovery = this.profile.iansRecovery;

    this.denoiseAlgorithm2 = this.profile.denoiseAlgorithm2;
    this.denoise2Radius = this.profile.denoise2Radius;
    this.denoise2Iterations = this.profile.denoise2Iterations;
    this.savitzkyGolayIterations = this.profile.savitzkyGolayIterations;
    this.savitzkyGolayAmount = this.profile.savitzkyGolayAmount;
    this.savitzkyGolaySize = this.profile.savitzkyGolaySize
      ? this.profile.savitzkyGolaySize.toString()
      : '0';

    this.clippingStrength = this.profile.clippingStrength;
    this.clippingRange = this.profile.clippingRange;

    this.deringRadius = this.profile.deringRadius;
    this.deringStrength = this.profile.deringStrength;
    this.deringThreshold = this.profile.deringThreshold;
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
    this.red = -this.profile.red;
    this.green = -this.profile.green;
    this.blue = -this.profile.blue;
    this.purple = this.profile.purple;
    this.slowProcessing = this.determineIfSlowProcessing(
      this.profile,
      this.settings.largeImage
    );
    this.equalizeLocalHistograms = this.profile.equalizeLocalHistogramsStrength;
    this.setNonPersistentSettings();
  }

  private determineIfSlowProcessing(
    profile: Profile,
    largeImage: boolean
  ): boolean {
    return (
      largeImage ||
      profile.denoiseAlgorithm1 === 'IAN' ||
      profile.equalizeLocalHistogramsStrength > 0
    );
  }

  private setNonPersistentSettings() {
    this.dispersionCorrectionEnabled = false;
    this.luminanceIncludeRed = true;
    this.luminanceIncludeGreen = true;
    this.luminanceIncludeBlue = true;
    this.luminanceIncludeColor = true;
  }

  private updateProfile() {
    this.slowProcessing = this.determineIfSlowProcessing(
      this.profile,
      this.settings.largeImage
    );
    if (this.slowProcessing) {
      this.showSpinner();
    }
    this.luckyStackWorkerService
      .updateProfile(this.profile, this.settings.operation)
      .subscribe(
        (data) => {
          console.log(data);
          if (this.slowProcessing) {
            this.hideSpinner();
          }
        },
        (error) => {
          console.log(error);
          if (this.slowProcessing) {
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

  buttonBarEnabled() {
    return 'Idle' === this.workerStatus && !this._showSpinner;
  }

  openRefImageEnabled() {
    return 'Idle' === this.workerStatus && !this._showSpinner;
  }

  isWorkerBusy() {
    return 'Idle' !== this.workerStatus;
  }

  zoomIn() {
    console.log('zoomIn called');
    if (this.zoomFactor <= 4) {
      this.zoomFactor++;
      this.luckyStackWorkerService.zoomIn().subscribe(
        (data) => {
          console.log('Response');
        },
        (error) => console.log(error)
      );
    }
  }

  zoomOut() {
    if (this.zoomFactor >= -2) {
      console.log('zoomOut called');
      this.zoomFactor--;
      this.luckyStackWorkerService.zoomOut().subscribe(
        (data) => {
          console.log('Response');
        },
        (error) => console.log(error)
      );
    }
  }

  maximize() {
    console.log('maximize called');
    this.isMaximized = !this.isMaximized;
    if (this.isMaximized) {
      this.zoomFactor = 0;
    }
    this.luckyStackWorkerService.maximize().subscribe(
      (data) => {
        this.zoomFactor = Number(data);
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  isZoomedIn(): boolean {
    return this.zoomFactor > 0;
  }

  isZoomedOut(): boolean {
    return this.zoomFactor < 0;
  }

  nightModeEnabled(): boolean {
    return this.nightMode;
  }

  nightModeChanged() {
    console.log('nightModeChanged called');
    this.nightMode = !this.nightMode;
    if (this.nightMode) {
      document.body.style.backgroundColor = '#000000';
      document.documentElement.style.setProperty('--lsw-tab-color', 'orange');
    } else {
      document.body.style.backgroundColor = 'rgb(43,43,43)';
      document.documentElement.style.setProperty(
        '--lsw-tab-color',
        'lightgreen'
      );
    }
    this.luckyStackWorkerService.nightModeChanged(this.nightMode).subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  cropSelectionChanged() {
    console.log('cropSelectionChanged called');
    this.crop = !this.crop;
    this.luckyStackWorkerService.cropSelectionChanged().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  cropEnabled() {
    return this.crop;
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

  getWorkerStatusDisplayText() {
    return this.workerStatus.length > 40
      ? this.workerStatus.substring(0, 40) + '...'
      : this.workerStatus;
  }

  dispersionIndicatorRedColor(): string {
    if (this.nightModeEnabled()) {
      if (
        this.profile.dispersionCorrectionRedX !== 0 ||
        this.profile.dispersionCorrectionRedY !== 0
      ) {
        return 'sliderRowCategoryTitleColorNight';
      } else {
        return '';
      }
    } else {
      if (
        this.profile.dispersionCorrectionRedX !== 0 ||
        this.profile.dispersionCorrectionRedY !== 0
      ) {
        return 'sliderRowCategoryTitleColor';
      } else {
        return '';
      }
    }
  }

  dispersionIndicatorBlueColor(): string {
    if (this.nightModeEnabled()) {
      if (
        this.profile.dispersionCorrectionBlueX !== 0 ||
        this.profile.dispersionCorrectionBlueY !== 0
      ) {
        return 'sliderRowCategoryTitleColorNight';
      } else {
        return '';
      }
    } else {
      if (
        this.profile.dispersionCorrectionBlueX !== 0 ||
        this.profile.dispersionCorrectionBlueY !== 0
      ) {
        return 'sliderRowCategoryTitleColor';
      } else {
        return '';
      }
    }
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

  gmicUnavailableMessage() {
    return "This control is unavailable because G'MIC isn't installed on your machine. Open a terminal window and type: brew install gmic. If brew isn't installed either then go to https://docs.brew.sh/Installation and follow the instructions.";
  }

  openDownloadPage() {
    window.open(
      'https://github.com/wkasteleijn/luckystackworker/releases/latest',
      '_blank'
    );
  }

  exitButtonAvailable(): boolean {
    const userAgent = navigator.userAgent.toLowerCase();
    return userAgent.indexOf('mac') === -1;
  }

  private showSpinner() {
    this._showSpinner = true;
  }

  private hideSpinner() {
    this._showSpinner = false;
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
