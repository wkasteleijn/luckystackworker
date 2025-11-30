import { Component, OnInit } from '@angular/core';
import { ThemePalette } from '@angular/material/core';
import { MatLegacyDialog } from '@angular/material/legacy-dialog';
import { MatLegacySnackBar } from '@angular/material/legacy-snack-bar';
import { timeout } from 'rxjs/operators';
import { AboutComponent } from './about/about.component';
import {
  ConfirmationComponent,
  ConfirmationModel,
} from './confirmation/confirmation.component';
import { LuckyStackWorkerService } from './luckystackworker.service';
import { DeRotation } from './model/derotation';
import { Profile } from './model/profile';
import { PSF } from './model/psf';
import { Settings } from './model/settings';
import { Version } from './model/version';

const SERVICE_POLL_DELAY_MS = 250;
const ROOT_FOLDER_MAX_LENGTH = 28;

interface ProfileSelection {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: false,
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
  clippingSuppression: number;
  radius: number;
  amount: number;
  iterations: number;
  blendRaw: number;
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
  applyUnsharpMask: boolean = true;
  applyWienerDeconvolution: boolean;
  wienerDeconvolutionQuadratic: boolean = false;
  wienerIterations: number;

  // denoise 1
  denoiseAlgorithm1: string;

  denoise1Amount: number;
  denoise1Radius: number;
  denoise1Iterations: number;

  rofTheta: number = 1;
  rofIterations: number = 2;

  bilateralSigmaColor: number;
  bilateralSigmaSpace: number;
  bilateralRadius: number = 1;
  bilateralIterations: number = 1;

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
  lightness: number;
  background: number;
  saturation: number;
  localContrastMode: string;
  localContrastFine: number;
  localContrastMedium: number;
  localContrastLarge: number;
  preserveDarkBackground: boolean = true;

  // color
  red: number;
  green: number;
  blue: number;
  purple: number;
  dispersionCorrectionEnabled: boolean = false;
  normalizeColorBalance: boolean = false;

  profile: Profile;
  settings: Settings;
  selectedProfile: string;
  rootFolder: string = 'C:\\';
  workerStatus: string = 'Idle';
  workerProgress: number;
  refImageSelected: boolean = true; // TODO: set to false by default!
  nightMode: boolean = false;
  blinkClippedAreas: boolean = false;
  roi: boolean = false;
  didChangesWhileRoiEnabled: boolean = false;
  _showSpinner = false;
  private slowProcessing: boolean = false;
  zoomFactor: number = 0;
  isMaximized: boolean = false;

  componentColor: ThemePalette = 'primary';
  componentColorNight: ThemePalette = 'warn';

  realtimeEnabled: boolean = false;
  showHistogram: boolean = true;
  scale: string = '1';
  rotationAngle: number = 0;
  openImageMode: string = 'RGB';
  visibleChannel: string = 'RGB';
  applySharpenToChannel: string = 'RGB';
  applyDenoiseToChannel: string = 'RGB';
  isPsfPanelVisible: boolean = false;
  isRotationPanelVisible: boolean = false;
  isProgressPanelVisible: boolean = false;
  isDerotationPanelVisible: boolean = false;
  isNotificationVisible: boolean = false;
  notificationText: string = 'Done!';
  psfImage: string = '';
  sliderTextDisplayed: boolean = false;
  deRotation: DeRotation;

  versionInfo: Version;
  showNewVersionPopup: boolean = false;

  resetToRawText = 'Are you sure you want to reset everything?';

  constructor(
    private luckyStackWorkerService: LuckyStackWorkerService,
    private aboutSnackbar: MatLegacySnackBar,
    private confirmationDialog: MatLegacyDialog
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
    this.checkHealth();
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
          console.log('Backend is up and running, opening the reference image');
          this.callopenReferenceImage();
        },
      });
  }

  private callopenReferenceImage() {
    this.luckyStackWorkerService
      .openReferenceImage(Number(this.scale), this.openImageMode)
      .subscribe({
        next: (data) => {
          console.log('Successfully opened the image: ', data);
          this.refImageSelected = true;
          this.roi = false;
          if (data && data.profile) {
            this.profile = data.profile;
            this.settings = data.settings;
            this.selectedProfile = this.profile.name;
            this.rootFolder = this.getRootFolderCapped(
              data.settings.rootFolder
            );
            this.zoomFactor = data.settings.zoomFactor;
            this.psfImage = data.settings.psfImage;
            this.checkLatestVersion();
            this.updateProfileSettings();
          }
          this.hideSpinner();
          this.workerProgress = 0;
        },
        error: (error) => {
          console.log('Opening the image failed: ' + error);
          this.hideSpinner();
          this.workerProgress = 0;
        },
        complete: () => {
          console.log('Request completed successfully');
        },
      });
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

  applyProfileBatch() {
    console.log('applyProfileBatch called');
    this.isProgressPanelVisible = true;
    this.workerStatus = 'Working';
    this.workerProgress = 0;
    this.luckyStackWorkerService.applyProfileBatch(this.profile).subscribe(
      (data) => {
        console.log(data);
        this.waitForWorker();
      },
      (error) => {
        console.log(error);
        this.isProgressPanelVisible = false;
        this.notificationText = 'The following error occured: ' + error;
        this.isNotificationVisible = true;
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
          this.roi = false;
          if (data && data.profile.amount > 0) {
            this.profile = data.profile;
            this.settings = data.settings;
            this.selectedProfile = this.profile.name;
            this.rootFolder = this.getRootFolderCapped(
              data.settings.rootFolder
            );
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

  rotationAngleChanged(event: any) {
    console.log('rotationAngleChanged called');
    this.settings.operations = ['ROTATE'];
    this.profile.rotationAngle = event;
    this.rotationAngle = Number(this.profile.rotationAngle);
    this.updateProfile();
  }

  openImageModeChanged() {
    console.log('openImageModeChanged called');
    if (this.profile) {
      this.profile.openImageMode = this.openImageMode;
    }
  }

  wienerIterationsChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.wienerIterationsGreen = event.value;
        break;
      case 'B':
        this.profile.wienerIterationsBlue = event.value;
        break;
      case 'R':
        this.profile.wienerIterations = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.wienerIterations = event.value;
        this.profile.wienerIterationsGreen = event.value;
        this.profile.wienerIterationsBlue = event.value;
    }
    this.wienerIterations = event.value;
    this.settings.operations = ['WIENER_DECONV'];
    console.log(
      'wienerIterationsChanged called: ' + this.profile.wienerIterations
    );
    if (update) {
      this.updateProfile();
    }
  }

  clippingSuppressionChanged(event: any, update: boolean) {
    this.profile.clippingSuppression = event.value;
    this.clippingSuppression = event.value;
    this.settings.operations = ['CLIPPING_SUPPRESSION'];
    console.log(
      'clippingSuppressionChanged called: ' + this.profile.clippingSuppression
    );
    if (update) {
      this.updateProfile();
    }
  }

  loadPSF() {
    console.log('loadPSF called');
  }

  createPSF() {
    console.log('createPSF called');
    this.isPsfPanelVisible = true;
  }

  openRotationPanel() {
    console.log('openRotationPanel called');
    this.isRotationPanelVisible = true;
  }

  hidePSF() {
    console.log('hidePSF called');
    this.isPsfPanelVisible = false;
  }

  hideRotationPanel() {
    console.log('hideRotationPanel called');
    this.isRotationPanelVisible = false;
  }

  radiusChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.radiusGreen = event.value;
        break;
      case 'B':
        this.profile.radiusBlue = event.value;
        break;
      case 'R':
        this.profile.radius = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.radius = event.value;
        this.profile.radiusGreen = event.value;
        this.profile.radiusBlue = event.value;
    }
    this.radius = event.value;
    this.settings.operations = ['SHARPEN'];
    console.log('radiusChanged called: ' + this.profile.radius);
    if (update) {
      this.updateProfile();
    }
  }

  amountChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.amountGreen = event.value;
        break;
      case 'B':
        this.profile.amountBlue = event.value;
        break;
      case 'R':
        this.profile.amount = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.amount = event.value;
        this.profile.amountGreen = event.value;
        this.profile.amountBlue = event.value;
    }
    this.amount = event.value;
    this.settings.operations = ['SHARPEN'];
    console.log('amountChanged called: ' + this.profile.amount);
    if (update) {
      this.updateProfile();
    }
  }

  iterationsChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.iterationsGreen = event.value;
        break;
      case 'B':
        this.profile.iterationsBlue = event.value;
        break;
      case 'R':
        this.profile.iterations = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.iterations = event.value;
        this.profile.iterationsGreen = event.value;
        this.profile.iterationsBlue = event.value;
    }
    this.iterations = event.value;
    this.settings.operations = ['SHARPEN'];
    console.log('iterationsChanged called: ' + this.profile.iterations);
    if (update) {
      this.updateProfile();
    }
  }

  blendRawChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.blendRawGreen = event.value;
        break;
      case 'B':
        this.profile.blendRawBlue = event.value;
        break;
      case 'R':
        this.profile.blendRaw = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.blendRaw = event.value;
        this.profile.blendRawGreen = event.value;
        this.profile.blendRawBlue = event.value;
    }
    this.blendRaw = event.value;
    this.settings.operations = ['SHARPEN', 'WIENER_DECONV'];
    console.log('blendRawChanged called: ' + this.profile.blendRaw);
    if (update) {
      this.updateProfile();
    }
  }

  clippingStrengthChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.clippingStrengthGreen = event.value;
        break;
      case 'B':
        this.profile.clippingStrengthBlue = event.value;
        break;
      case 'R':
        this.profile.clippingStrength = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.clippingStrength = event.value;
        this.profile.clippingStrengthGreen = event.value;
        this.profile.clippingStrengthBlue = event.value;
    }
    this.clippingStrength = event.value;
    this.settings.operations = ['SHARPEN'];
    console.log('clippingStrength called: ' + this.profile.clippingStrength);
    if (update) {
      this.updateProfile();
    }
  }

  clippingRangeChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.clippingRangeGreen = event.value;
        break;
      case 'B':
        this.profile.clippingRangeBlue = event.value;
        break;
      case 'R':
        this.profile.clippingRange = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.clippingRange = event.value;
        this.profile.clippingRangeGreen = event.value;
        this.profile.clippingRangeBlue = event.value;
    }
    this.clippingRange = event.value;
    this.settings.operations = ['SHARPEN'];
    console.log('clippingRange called: ' + this.profile.clippingRange);
    if (update) {
      this.updateProfile();
    }
  }

  deringRadiusChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.deringRadiusGreen = event.value;
        break;
      case 'B':
        this.profile.deringRadiusBlue = event.value;
        break;
      case 'R':
        this.profile.deringRadius = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.deringRadius = event.value;
        this.profile.deringRadiusGreen = event.value;
        this.profile.deringRadiusBlue = event.value;
    }
    this.deringRadius = event.value;
    this.setSharpenOperationForDeringing();
    console.log('deringRadius called: ' + this.profile.deringRadius);
    if (update) {
      this.updateProfile();
    }
  }

  deringStrengthChanged(event: any, update: boolean) {
    switch (this.applySharpenToChannel) {
      case 'G':
        this.profile.deringStrengthGreen = event.value;
        break;
      case 'B':
        this.profile.deringStrengthBlue = event.value;
        break;
      case 'R':
        this.profile.deringStrength = event.value;
        break;
      default:
        this.equalizeChannelsForSharpening();
        this.profile.deringStrength = event.value;
        this.profile.deringStrengthGreen = event.value;
        this.profile.deringStrengthBlue = event.value;
    }
    this.deringStrength = event.value;
    this.setSharpenOperationForDeringing();
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
    this.settings.operations = ['SHARPEN', 'WIENER_DECONV'];
    console.log('sharpenMode called: ' + this.profile.sharpenMode);
    if (update) {
      this.updateProfile();
    }
  }

  applyWienerDeconvolutionChanged() {
    this.profile.applyWienerDeconvolution = this.applyWienerDeconvolution;
    this.settings.operations = ['WIENER_DECONV'];
    this.updateProfile();
  }

  applyUnsharpMaskChanged() {
    this.profile.applyUnsharpMask = this.applyUnsharpMask;
    this.settings.operations = ['SHARPEN'];
    this.updateProfile();
  }

  applyWienerDeconvolutionQuadraticChanged() {
    this.profile.wienerRepetitions = this.wienerDeconvolutionQuadratic ? 2 : 1;
    this.settings.operations = ['WIENER_DECONV'];
    this.updateProfile();
  }

  localContrastModeChanged(event: any, update: boolean) {
    this.profile.localContrastMode = event.value;
    this.localContrastMode = event.value;
    this.settings.operations = ['LOCAL_CONTRAST'];
    console.log('localContrastMode called: ' + this.profile.localContrastMode);
    if (update) {
      this.updateProfile();
    }
  }

  edgeArtefactSupressionModeChanged(event: any, update: boolean) {
    if (event.value === 'CLIPPING') {
      this.profile.deringStrength = 0;
      if (!this.profile.clippingStrength) {
        // If not set, start with the defaults
        this.profile.clippingRange = 50;
        this.profile.clippingStrength = 10;
        this.clippingRange = 50;
        this.clippingStrength = 10;
      }
    } else if (event.value === 'DERING') {
      this.profile.clippingStrength = 0;
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
    this.settings.operations = ['SHARPEN', 'WIENER_DECONV'];
    console.log('edgeArtefactSupressionMode called: ' + event.value);
    this.updateProfile();
  }

  applySharpenToChannelChanged(event: any) {
    console.log('applySharpenToChannelChanged called: ' + event.value);
    this.settings.operations = ['SHARPEN'];
    this.profile.applySharpenToChannel = this.applySharpenToChannel;
    switch (this.applySharpenToChannel) {
      case 'G':
        this.radius = this.profile.radiusGreen;
        this.amount = this.profile.amountGreen;
        this.iterations = this.profile.iterationsGreen;
        this.blendRaw = this.profile.blendRawGreen;
        this.clippingStrength = this.profile.clippingStrengthGreen;
        this.clippingRange = this.profile.clippingRangeGreen;
        this.deringRadius = this.profile.deringRadiusGreen;
        this.deringStrength = this.profile.deringStrengthGreen;
        break;
      case 'B':
        this.radius = this.profile.radiusBlue;
        this.amount = this.profile.amountBlue;
        this.iterations = this.profile.iterationsBlue;
        this.blendRaw = this.profile.blendRawBlue;
        this.clippingStrength = this.profile.clippingStrengthBlue;
        this.clippingRange = this.profile.clippingRangeBlue;
        this.deringRadius = this.profile.deringRadiusBlue;
        this.deringStrength = this.profile.deringStrengthBlue;
        break;
      default:
        this.radius = this.profile.radius;
        this.amount = this.profile.amount;
        this.iterations = this.profile.iterations;
        this.blendRaw = this.profile.blendRaw;
        this.clippingStrength = this.profile.clippingStrength;
        this.clippingRange = this.profile.clippingRange;
        this.deringRadius = this.profile.deringRadius;
        this.deringStrength = this.profile.deringStrength;
    }
    this.visibleChannel = this.applySharpenToChannel;
    this.luckyStackWorkerService.channelChanged(this.visibleChannel).subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  applyDenoiseToChannelChanged(event: any) {
    console.log('applyDenoiseToChannelChanged called: ' + event.value);
    this.settings.operations = ['SIGMA_DENOISE_1'];
    this.profile.applyDenoiseToChannel = this.applyDenoiseToChannel;
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.denoise1Amount = this.profile.denoise1AmountGreen;
        this.denoise1Radius = this.profile.denoise1RadiusGreen;
        this.denoise1Iterations = this.profile.denoise1IterationsGreen;
        this.savitzkyGolaySize = this.profile.savitzkyGolaySizeGreen
          ? this.profile.savitzkyGolaySizeGreen.toString()
          : '0';
        this.savitzkyGolayAmount = this.profile.savitzkyGolayAmountGreen;
        this.savitzkyGolayIterations =
          this.profile.savitzkyGolayIterationsGreen;
        this.denoise2Radius = this.profile.denoise2RadiusGreen;
        this.denoise2Iterations = this.profile.denoise2IterationsGreen;
        this.rofTheta = this.profile.rofThetaGreen;
        this.rofIterations = this.profile.rofIterationsGreen;
        this.bilateralSigmaColor = this.profile.bilateralSigmaColorGreen;
        this.bilateralSigmaSpace = this.profile.bilateralSigmaSpaceGreen;
        this.bilateralRadius = this.profile.bilateralRadiusGreen;
        this.bilateralIterations = this.profile.bilateralIterationsGreen;

        break;
      case 'B':
        this.denoise1Amount = this.profile.denoise1AmountBlue;
        this.denoise1Radius = this.profile.denoise1RadiusBlue;
        this.denoise1Iterations = this.profile.denoise1IterationsBlue;
        this.savitzkyGolaySize = this.profile.savitzkyGolaySizeBlue
          ? this.profile.savitzkyGolaySizeBlue.toString()
          : '0';
        this.savitzkyGolayAmount = this.profile.savitzkyGolayAmountBlue;
        this.savitzkyGolayIterations = this.profile.savitzkyGolayIterationsBlue;
        this.denoise2Radius = this.profile.denoise2RadiusBlue;
        this.denoise2Iterations = this.profile.denoise2IterationsBlue;
        this.rofTheta = this.profile.rofThetaBlue;
        this.rofIterations = this.profile.rofIterationsBlue;
        this.bilateralSigmaColor = this.profile.bilateralSigmaColorBlue;
        this.bilateralSigmaSpace = this.profile.bilateralSigmaSpaceBlue;
        this.bilateralRadius = this.profile.bilateralRadiusBlue;
        this.bilateralIterations = this.profile.bilateralIterationsBlue;
        break;
      default:
        this.denoiseAlgorithm1 = this.profile.denoiseAlgorithm1;
        this.denoise1Amount = this.profile.denoise1Amount;
        this.denoise1Radius = this.profile.denoise1Radius;
        this.denoise1Iterations = this.profile.denoise1Iterations;
        this.denoiseAlgorithm2 = this.profile.denoiseAlgorithm2;
        this.savitzkyGolaySize = this.profile.savitzkyGolaySize
          ? this.profile.savitzkyGolaySize.toString()
          : '0';
        this.savitzkyGolayAmount = this.profile.savitzkyGolayAmount;
        this.savitzkyGolayIterations = this.profile.savitzkyGolayIterations;
        this.denoise2Radius = this.profile.denoise2Radius;
        this.denoise2Iterations = this.profile.denoise2Iterations;
        this.rofTheta = this.profile.rofTheta;
        this.rofIterations = this.profile.rofIterations;
        this.rofIterations = this.profile.rofIterationsBlue;
        this.bilateralSigmaColor = this.profile.bilateralSigmaColor;
        this.bilateralSigmaSpace = this.profile.bilateralSigmaSpace;
        this.bilateralRadius = this.profile.bilateralRadius;
        this.bilateralIterations = this.profile.bilateralIterations;
    }
    this.visibleChannel = this.applyDenoiseToChannel;
    this.luckyStackWorkerService.channelChanged(this.visibleChannel).subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  denoise1AmountChanged(event: any, update: boolean) {
    this.denoise1Amount = event.value;
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.denoise1AmountGreen = event.value;
        break;
      case 'B':
        this.profile.denoise1AmountBlue = event.value;
        break;
      case 'R':
        this.profile.denoise1Amount = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.denoise1Amount = event.value;
        this.profile.denoise1AmountGreen = event.value;
        this.profile.denoise1AmountBlue = event.value;
    }
    this.settings.operations = ['SIGMA_DENOISE_1'];
    console.log('denoise1AmountChanged called: ' + this.denoise1Amount);
    if (update) {
      this.updateProfile();
    }
  }

  denoise1RadiusChanged(event: any, update: boolean) {
    this.denoise1Radius = event.value;
    this.settings.operations = ['SIGMA_DENOISE_1'];
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.denoise1RadiusGreen = event.value;
        break;
      case 'B':
        this.profile.denoise1RadiusBlue = event.value;
        break;
      case 'R':
        this.profile.denoise1Radius = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.denoise1Radius = event.value;
        this.profile.denoise1RadiusGreen = event.value;
        this.profile.denoise1RadiusBlue = event.value;
    }
    console.log('denoise1RadiusChanged called: ' + this.profile.denoise1Radius);
    if (update) {
      this.updateProfile();
    }
  }

  denoise1IterationsChanged(event: any, update: boolean) {
    this.denoise1Iterations = event.value;
    this.settings.operations = ['SIGMA_DENOISE_1'];
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.denoise1IterationsGreen = event.value;
        break;
      case 'B':
        this.profile.denoise1IterationsBlue = event.value;
        break;
      case 'R':
        this.profile.denoise1Iterations = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.denoise1Iterations = event.value;
        this.profile.denoise1IterationsGreen = event.value;
        this.profile.denoise1IterationsBlue = event.value;
    }
    console.log(
      'denoise1IterationsChanged called: ' + this.profile.denoise1Iterations
    );
    if (update) {
      this.updateProfile();
    }
  }

  bilateralIterationsChanged(event: any, update: boolean) {
    this.bilateralIterations = event.value;
    this.settings.operations = ['BILATERAL_DENOISE'];
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.bilateralIterationsGreen = event.value;
        break;
      case 'B':
        this.profile.bilateralIterationsBlue = event.value;
        break;
      case 'R':
        this.profile.bilateralIterations = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.bilateralIterations = event.value;
        this.profile.bilateralIterationsGreen = event.value;
        this.profile.bilateralIterationsBlue = event.value;
    }
    console.log(
      'bilateralIterationsChanged called: ' + this.profile.bilateralIterations
    );
    if (update) {
      this.updateProfile();
    }
  }

  bilateralSigmaColorChanged(event: any, update: boolean) {
    this.bilateralSigmaColor = event.value;
    this.settings.operations = ['BILATERAL_DENOISE'];
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.bilateralSigmaColorGreen = event.value;
        break;
      case 'B':
        this.profile.bilateralSigmaColorBlue = event.value;
        break;
      case 'R':
        this.profile.bilateralSigmaColor = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.bilateralSigmaColor = event.value;
        this.profile.bilateralSigmaColorGreen = event.value;
        this.profile.bilateralSigmaColorBlue = event.value;
    }
    console.log(
      'bilateralSigmaColorChanged called: ' + this.profile.bilateralSigmaColor
    );
    if (update) {
      this.updateProfile();
    }
  }

  bilateralRadiusChanged(event: any, update: boolean) {
    this.bilateralRadius = event.value;
    this.settings.operations = ['BILATERAL_DENOISE'];
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.bilateralRadiusGreen = event.value;
        break;
      case 'B':
        this.profile.bilateralRadiusBlue = event.value;
        break;
      case 'R':
        this.profile.bilateralRadius = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.bilateralRadius = event.value;
        this.profile.bilateralRadiusGreen = event.value;
        this.profile.bilateralRadiusBlue = event.value;
    }
    console.log(
      'bilateralRadiusChanged called: ' + this.profile.bilateralRadius
    );
    if (update) {
      this.updateProfile();
    }
  }

  denoise2RadiusChanged(event: any, update: boolean) {
    this.denoise2Radius = event.value;
    this.settings.operations = ['SIGMA_DENOISE_2'];
    console.log('denoise2RadiusChanged called: ' + this.profile.denoise2Radius);
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.denoise2RadiusGreen = event.value;
        break;
      case 'B':
        this.profile.denoise2RadiusBlue = event.value;
        break;
      case 'R':
        this.profile.denoise2Radius = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.denoise2Radius = event.value;
        this.profile.denoise2RadiusGreen = event.value;
        this.profile.denoise2RadiusBlue = event.value;
    }
    if (update) {
      this.updateProfile();
    }
  }

  denoise2IterationsChanged(event: any, update: boolean) {
    this.denoise2Iterations = event.value;
    this.settings.operations = ['SIGMA_DENOISE_2'];
    console.log(
      'denoise2IterationsChanged called: ' + this.profile.denoise2Iterations
    );
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.denoise2IterationsGreen = event.value;
        break;
      case 'B':
        this.profile.denoise2IterationsBlue = event.value;
        break;
      case 'R':
        this.profile.denoise2Iterations = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.denoise2Iterations = event.value;
        this.profile.denoise2IterationsGreen = event.value;
        this.profile.denoise2IterationsBlue = event.value;
    }
    if (update) {
      this.updateProfile();
    }
  }

  gammaChanged(event: any, update: boolean) {
    this.profile.gamma = event.value;
    this.gamma = event.value;
    this.settings.operations = ['GAMMA'];
    console.log('gammaChanged called: ' + this.profile.gamma);
    if (update) {
      this.updateProfile();
    }
  }

  contrastChanged(event: any, update: boolean) {
    this.profile.contrast = event.value;
    this.contrast = event.value;
    this.settings.operations = ['HISTOGRAM_STRETCH'];
    console.log('contrastChanged called: ' + this.profile.contrast);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastFineChanged(event: any, update: boolean) {
    this.profile.localContrastFine = event.value;
    this.localContrastFine = event.value;
    this.settings.operations = ['LOCAL_CONTRAST'];
    console.log('localContrastFine called: ' + this.profile.localContrastFine);
    if (update) {
      this.updateProfile();
    }
  }

  localContrastMediumChanged(event: any, update: boolean) {
    this.profile.localContrastMedium = event.value;
    this.localContrastMedium = event.value;
    this.settings.operations = ['LOCAL_CONTRAST'];
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
    this.settings.operations = ['LOCAL_CONTRAST'];
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
    this.settings.operations = ['HISTOGRAM_STRETCH'];
    console.log('brightnessChanged called: ' + this.profile.brightness);
    if (update) {
      this.updateProfile();
    }
  }

  lightnessChanged(event: any, update: boolean) {
    this.profile.lightness = event.value;
    this.lightness = event.value;
    this.settings.operations = ['HISTOGRAM_STRETCH'];
    console.log('lightnessChanged called: ' + this.profile.lightness);
    if (update) {
      this.updateProfile();
    }
  }

  backgroundChanged(event: any, update: boolean) {
    this.profile.background = event.value;
    this.background = event.value;
    this.settings.operations = ['HISTOGRAM_STRETCH'];
    console.log('backgroundChanged called: ' + this.profile.background);
    if (update) {
      this.updateProfile();
    }
  }

  saturationChanged(event: any, update: boolean) {
    this.profile.saturation = event.value;
    this.saturation = event.value;
    this.settings.operations = ['SATURATION'];
    console.log('saturationChanged called: ' + this.profile.saturation);
    if (update) {
      this.updateProfile();
    }
  }

  greenChanged(event: any, update: boolean) {
    this.profile.green = -event.value;
    this.green = event.value;
    this.settings.operations = ['RGB_BALANCE'];
    console.log('greenChanged called: ' + this.profile.green);
    if (update) {
      this.updateProfile();
    }
  }

  redChanged(event: any, update: boolean) {
    this.profile.red = -event.value;
    this.red = event.value;
    this.settings.operations = ['RGB_BALANCE'];
    console.log('redChanged called: ' + this.profile.red);
    if (update) {
      this.updateProfile();
    }
  }

  blueChanged(event: any, update: boolean) {
    this.profile.blue = -event.value;
    this.settings.operations = ['RGB_BALANCE'];
    this.blue = event.value;
    console.log('blueChanged called: ' + this.profile.blue);
    if (update) {
      this.updateProfile();
    }
  }

  purpleChanged(event: any, update: boolean) {
    this.profile.purple = event.value;
    this.settings.operations = ['RGB_BALANCE'];
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
    this.savitzkyGolayIterations = event.value;
    this.settings.operations = ['SAVITSKY_GOLAY'];
    console.log(
      'savitzkyGolayIterationsChanged called: ' +
        this.profile.savitzkyGolayIterations
    );
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.savitzkyGolayIterationsGreen = event.value;
        break;
      case 'B':
        this.profile.savitzkyGolayIterationsBlue = event.value;
        break;
      case 'R':
        this.profile.savitzkyGolayIterations = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.savitzkyGolayIterations = event.value;
        this.profile.savitzkyGolayIterationsGreen = event.value;
        this.profile.savitzkyGolayIterationsBlue = event.value;
    }
    if (update) {
      this.updateProfile();
    }
  }

  savitzkyGolayAmountChanged(event: any, update: boolean) {
    this.savitzkyGolayAmount = event.value;
    this.settings.operations = ['SAVITSKY_GOLAY'];
    console.log(
      'savitzkyGolayAmountChanged called: ' + this.profile.savitzkyGolayAmount
    );
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.savitzkyGolayAmountGreen = event.value;
        break;
      case 'B':
        this.profile.savitzkyGolayAmountBlue = event.value;
        break;
      case 'R':
        this.profile.savitzkyGolayAmount = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.savitzkyGolayAmount = event.value;
        this.profile.savitzkyGolayAmountGreen = event.value;
        this.profile.savitzkyGolayAmountBlue = event.value;
    }
    if (update) {
      this.updateProfile();
    }
  }

  savitzkyGolaySizeChanged(event: any, update: boolean) {
    this.savitzkyGolaySize = event.value;
    this.settings.operations = ['SAVITSKY_GOLAY'];
    console.log(
      'savitzkyGolaySizeChanged called: ' + this.profile.savitzkyGolaySize
    );
    switch (this.applyDenoiseToChannel) {
      case 'G':
        this.profile.savitzkyGolaySizeGreen = event.value;
        break;
      case 'B':
        this.profile.savitzkyGolaySizeBlue = event.value;
        break;
      case 'R':
        this.profile.savitzkyGolaySize = event.value;
        break;
      default:
        this.equalizeChannelsForDenoising();
        this.profile.savitzkyGolaySize = event.value;
        this.profile.savitzkyGolaySizeGreen = event.value;
        this.profile.savitzkyGolaySizeBlue = event.value;
    }
    if (update) {
      this.updateProfile();
    }
  }

  denoiseAlgorithm1Changed(event: any) {
    if (event.value === 'SIGMA1') {
      this.settings.operations = ['SIGMA_DENOISE_1'];
      this.profile.denoiseAlgorithm1 = 'SIGMA1';
    } else if (event.value === 'BILATERAL') {
      this.settings.operations = ['BILATERAL_DENOISE'];
      this.profile.denoiseAlgorithm1 = 'BILATERAL';
    } else {
      this.settings.operations = ['SIGMA_DENOISE_1'];
      this.profile.denoiseAlgorithm1 = 'OFF';
    }
    console.log('denoiseAlgorithm1Changed called: ' + event.value);
    this.updateProfile();
  }

  denoiseAlgorithm2Changed(event: any) {
    if (event.value === 'SAVGOLAY') {
      this.settings.operations = ['SAVITSKY_GOLAY'];
      this.profile.denoiseAlgorithm2 = 'SAVGOLAY';
    } else if (event.value === 'SIGMA2') {
      this.settings.operations = ['SIGMA_DENOISE_2'];
      this.profile.denoiseAlgorithm2 = 'SIGMA2';
    } else {
      this.settings.operations = ['SAVITSKY_GOLAY'];
      this.profile.denoiseAlgorithm2 = 'OFF';
    }
    console.log('denoiseAlgorithm1Changed called: ' + event.value);
    this.updateProfile();
  }

  colorNormalizationChanged() {
    this.settings.operations = ['COLOR_NORMALIZE'];
    this.profile.normalizeColorBalance = this.normalizeColorBalance;
    console.log(
      'colorNormalizationChanged called: ' + this.normalizeColorBalance
    );
    this.updateProfile();
  }

  preserveDarkBackgroundChanged() {
    this.settings.operations = ['HISTOGRAM_STRETCH'];
    this.profile.preserveDarkBackground = this.preserveDarkBackground;
    console.log(
      'preserveDarkBackgroundChanged called: ' + this.preserveDarkBackground
    );
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
    this.settings.operations = ['DISPERSION'];
    this.profile.dispersionCorrectionEnabled = this.dispersionCorrectionEnabled;
    this.resetDispersionCorrection();
  }

  dispersionCorrectionIsEnabled() {
    return this.dispersionCorrectionEnabled;
  }

  resetDispersionCorrection() {
    console.log('resetDispersionCorrection called');
    this.settings.operations = ['DISPERSION'];
    this.profile.dispersionCorrectionRedX = 0;
    this.profile.dispersionCorrectionRedY = 0;
    this.profile.dispersionCorrectionBlueX = 0;
    this.profile.dispersionCorrectionBlueY = 0;
    this.updateProfile();
  }

  dispersionCorrectionClicked(direction: string, color: string) {
    console.log('dispersionCorrectionClicked called: ' + direction);
    this.settings.operations = ['DISPERSION'];
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

  derotationAnchorStrengthChanged(event: any) {
    console.log('derotationAnchorStrengthChanged called');
    this.deRotation.anchorStrength = event.value;
  }
  derotationNoiseRobustnessChanged(event: any) {
    console.log('derotationNoiseRobustnessChanged called');
    this.deRotation.noiseRobustness = event.value;
  }
  derotationAccuratenessChanged(event: any) {
    console.log('derotationAccuratenessChanged called');
    this.deRotation.acurateness = event.value;
  }

  derotationRefImageChanged(event: any) {
    console.log('derotationAccuratenessChanged called');
    this.deRotation.referenceImage = event.value;
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
          this.rootFolder = this.getRootFolderCapped(data.rootFolder);
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
        this.notificationText = 'Batch process was stopped.';
        this.isProgressPanelVisible = false;
        this.isNotificationVisible = true;
      },
      (error) => console.log(error)
    );
  }

  psfSlidersChanged(event: any) {
    console.log('psfSlidersChanged called');
    this.profile.psf = Object.assign(new PSF(), event);
    this.settings.operations = ['PSF'];
    this.updateProfile();
  }

  loadCustomPSFClicked() {
    console.log('loadCustomPSFClicked called:');
    this.showSpinner();
    this.luckyStackWorkerService.loadCustomPSF().subscribe(
      (data) => {
        if (data) {
          this.psfImage = data.psfImage;
          this.profile.psf.type = 'CUSTOM';
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
    this.blendRaw = this.profile.blendRaw;

    this.denoiseAlgorithm1 = this.profile.denoiseAlgorithm1;
    this.denoise1Amount = this.profile.denoise1Amount;
    this.denoise1Radius = this.profile.denoise1Radius;
    this.denoise1Iterations = this.profile.denoise1Iterations;
    this.rofTheta = this.profile.rofTheta;
    this.rofIterations = this.profile.rofIterations;
    this.bilateralIterations = this.profile.bilateralIterations;
    this.bilateralRadius = this.profile.bilateralRadius;
    this.bilateralSigmaColor = this.profile.bilateralSigmaColor;
    this.bilateralSigmaSpace = this.profile.bilateralSigmaSpace;

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
    this.lightness = this.profile.lightness;
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
    this.normalizeColorBalance = this.profile.normalizeColorBalance;
    this.preserveDarkBackground = this.profile.preserveDarkBackground;
    this.applyUnsharpMask = this.profile.applyUnsharpMask;
    this.applyWienerDeconvolution = this.profile.applyWienerDeconvolution;
    this.wienerIterations = this.profile.wienerIterations;
    this.clippingSuppression = this.profile.clippingSuppression;
    this.setNonPersistentSettings();
  }

  private determineIfSlowProcessing(
    profile: Profile,
    largeImage: boolean
  ): boolean {
    return largeImage;
  }

  private setNonPersistentSettings() {
    this.dispersionCorrectionEnabled = false;
    this.luminanceIncludeRed = true;
    this.luminanceIncludeGreen = true;
    this.luminanceIncludeBlue = true;
    this.luminanceIncludeColor = true;
  }

  private updateProfile() {
    if (this.roiEnabled && !this.didChangesWhileRoiEnabled) {
      this.didChangesWhileRoiEnabled = true;
    }
    this.slowProcessing = this.determineIfSlowProcessing(
      this.profile,
      this.settings.largeImage
    );
    if (this.slowProcessing) {
      this.showSpinner();
    }
    this.luckyStackWorkerService
      .updateProfile(this.profile, this.settings.operations)
      .subscribe(
        (data) => {
          if (data) {
            this.psfImage = data.imageData;
          }
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
      this.isProgressPanelVisible = false;
      if (!this.isNotificationVisible) {
        this.notificationText = 'Done!';
        this.isNotificationVisible = true;
      }
    }
  }

  private getStatusUpdate() {
    this.luckyStackWorkerService.getStatus().subscribe(
      (data) => {
        console.log(data);
        this.workerStatus = data.message;
        if (data.filesProcessedCount && data.totalFilesCount) {
          this.workerProgress = Math.round(
            (data.filesProcessedCount / data.totalFilesCount) * 100
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
    if (this.zoomFactor >= -4) {
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

  blinkClippedAreasChanged() {
    console.log('blinkClippedAreasChanged called');
    this.blinkClippedAreas = !this.blinkClippedAreas;
    this.luckyStackWorkerService.blinkClippedAreasChanged().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  roiSelectionChanged() {
    console.log('roiSelectionChanged called');
    this.luckyStackWorkerService.roiSelectionChanged().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
    const wasRoiSelectionEnabled = this.roi;
    this.roi = !this.roi;
    if (wasRoiSelectionEnabled && this.didChangesWhileRoiEnabled) {
      this.updateProfile();
      this.didChangesWhileRoiEnabled = false;
    }
  }

  channelChanged() {
    console.log('channel called');
    switch (this.visibleChannel) {
      case 'RGB':
        this.visibleChannel = 'R';
        break;
      case 'R':
        this.visibleChannel = 'G';
        break;
      case 'G':
        this.visibleChannel = 'B';
        break;
      case 'B':
        this.visibleChannel = 'RGB';
        break;
      default:
        this.visibleChannel = 'RGB';
    }
    this.luckyStackWorkerService.channelChanged(this.visibleChannel).subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
  }

  roiEnabled() {
    return this.roi;
  }

  histogramSelectionChanged() {
    console.log('roiSelectionChanged called');
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

  public showVersionButton(): boolean {
    return (
      this.versionInfo &&
      this.versionInfo.localVersionConverted <
        this.versionInfo.latestVersionConverted
    );
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

  openConfirmation(confirmationText: string) {
    const dialogData = new ConfirmationModel(confirmationText);

    const dialogRef = this.confirmationDialog.open(ConfirmationComponent, {
      maxWidth: '400px',
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe((dialogResult) => {
      if (dialogResult) {
        this.resetToRaw();
      }
    });
  }

  resetBalanceClicked() {
    console.log('Reset color balance called');
    this.red = 0;
    this.green = 0;
    this.blue = 0;
    this.profile.red = 0;
    this.profile.green = 0;
    this.profile.blue = 0;
    this.updateProfile();
  }

  isRunningOnWindows() {
    return /Win/i.test(navigator.userAgent);
  }

  hideNewVersionPopup() {
    this.showNewVersionPopup = false;
  }

  loadFilesForDerotation() {
    this.showSpinner();
    this.luckyStackWorkerService.getFilesForDerotation().subscribe(
      (data) => {
        if (data && data.images && data.images.length > 0) {
          console.log(data);
          this.deRotation = data;
          this.isDerotationPanelVisible = true;
        }
        this.hideSpinner();
      },
      (error) => console.log(error)
    );
  }

  startDeRotation() {
    this.isDerotationPanelVisible = false;
    this.isProgressPanelVisible = true;
    this.workerStatus = 'Working';
    this.workerProgress = 0;
    this.luckyStackWorkerService.startDeRotation().subscribe(
      (data) => {
        console.log(data);
        this.waitForWorker();
      },
      (error) => {
        console.log(error);
        this.isProgressPanelVisible = false;
        this.notificationText = 'The following error occured: ' + error;
        this.isNotificationVisible = true;
      }
    );
  }

  hideDeRotationPanel() {
    this.isDerotationPanelVisible = false;
  }

  hideNotification() {
    this.isNotificationVisible = false;
  }

  private setSharpenOperationForDeringing() {
    if (this.applyWienerDeconvolution) {
      this.settings.operations = ['WIENER_DECONV'];
      return;
    }
    this.settings.operations = ['SHARPEN'];
  }

  private resetToRaw() {
    // Disable sharpening, set values to defaults.
    this.profile.applyWienerDeconvolution = false;
    this.profile.psf.airyDiskRadius = 20;
    this.profile.psf.diffractionIntensity = 60;
    this.profile.psf.seeingIndex = 4;
    this.profile.psf.type = 'SYNTHETIC';

    this.profile.applyUnsharpMask = false;
    this.profile.amount = 5000;
    this.profile.amountBlue = 5000;
    this.profile.amountGreen = 5000;
    this.profile.radius = 1.5;
    this.profile.radiusGreen = 1.5;
    this.profile.radiusBlue = 1.5;
    this.profile.iterations = 1;
    this.profile.iterationsBlue = 1;
    this.profile.iterationsGreen = 1;
    this.profile.wienerIterations = 5;
    this.profile.clippingSuppression = 0;
    this.profile.blendRaw = 0;
    this.profile.blendRawBlue = 0;
    this.profile.blendRawGreen = 0;

    // Disable clipping, set values to defaults.
    this.profile.clippingRange = 50;
    this.profile.clippingStrength = 0;

    // Disable deringing, set values to defaults.
    this.profile.deringStrength = 0;
    this.profile.deringRadius = 3;
    this.profile.deringRadiusBlue = 3;
    this.profile.deringRadiusBlue = 3;

    // Disable denoising, set values to defaults.
    this.profile.denoiseAlgorithm1 = 'OFF';

    // Bilateral
    this.profile.bilateralSigmaColor = 150;
    this.profile.bilateralSigmaColorGreen = 150;
    this.profile.bilateralSigmaColorBlue = 150;
    this.profile.bilateralRadius = 1;
    this.profile.bilateralRadiusBlue = 1;
    this.profile.bilateralRadiusGreen = 1;
    this.profile.bilateralIterations = 2;
    this.profile.bilateralIterationsBlue = 2;
    this.profile.bilateralIterationsGreen = 2;

    // Sigma 1
    this.profile.denoise1Amount = 50;
    this.profile.denoise1AmountBlue = 50;
    this.profile.denoise1AmountGreen = 50;
    this.profile.denoise1Radius = 1.5;
    this.profile.denoise1RadiusGreen = 1.5;
    this.profile.denoise1RadiusBlue = 1.5;
    this.profile.denoise1Iterations = 1;
    this.profile.denoise1IterationsBlue = 1;
    this.profile.denoise1IterationsGreen = 1;

    this.profile.denoiseAlgorithm2 = 'OFF';

    // SG
    this.profile.savitzkyGolaySize = 3;
    this.profile.savitzkyGolaySizeGreen = 3;
    this.profile.savitzkyGolaySizeBlue = 3;
    this.profile.savitzkyGolayAmount = 100;
    this.profile.savitzkyGolayAmountBlue = 100;
    this.profile.savitzkyGolayAmountGreen = 100;
    this.profile.savitzkyGolayIterations = 1;
    this.profile.savitzkyGolayIterationsGreen = 1;
    this.profile.savitzkyGolayIterationsBlue = 1;

    // Sigma 2
    this.profile.denoise2Iterations = 1;
    this.profile.denoise2IterationsGreen = 1;
    this.profile.denoise2IterationsBlue = 1;
    this.profile.denoise2Radius = 1;
    this.profile.denoise2RadiusBlue = 1;
    this.profile.denoise2RadiusGreen = 1;

    // Reset contrast & light
    this.profile.localContrastFine = 0;
    this.profile.localContrastMedium = 0;
    this.profile.localContrastLarge = 0;
    this.profile.gamma = 1;
    this.profile.contrast = 0;
    this.profile.brightness = 0;
    this.profile.lightness = 0;
    this.profile.background = 0;
    this.profile.preserveDarkBackground = true;

    // Reset color & dispersion
    this.profile.saturation = 1;
    this.profile.purple = 0;
    this.profile.normalizeColorBalance = false;
    this.profile.red = 0;
    this.profile.green = 0;
    this.profile.blue = 0;
    this.profile.dispersionCorrectionEnabled = false;

    this.selectedProfile = this.profile.name;
    this.updateProfileSettings();
    this.updateProfile();
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
        this.versionInfo = data;
        if (data.newVersion) {
          this.showNewVersionPopup = true;
        }
      },
      (error) => console.log(error)
    );
  }

  private equalizeChannelsForSharpening() {
    this.profile.radiusGreen = this.radius;
    this.profile.amountGreen = this.amount;
    this.profile.iterationsGreen = this.iterations;
    this.profile.blendRawGreen = this.blendRaw;
    this.profile.clippingStrengthGreen = this.clippingStrength;
    this.profile.clippingRangeGreen = this.clippingRange;
    this.profile.deringRadiusGreen = this.deringRadius;
    this.profile.deringStrengthGreen = this.deringStrength;
    this.profile.wienerIterationsGreen = this.wienerIterations;
    this.profile.radiusBlue = this.radius;
    this.profile.amountBlue = this.amount;
    this.profile.iterationsBlue = this.iterations;
    this.profile.blendRawBlue = this.blendRaw;
    this.profile.clippingStrengthBlue = this.clippingStrength;
    this.profile.clippingRangeBlue = this.clippingRange;
    this.profile.deringRadiusBlue = this.deringRadius;
    this.profile.deringStrengthBlue = this.deringStrength;
    this.profile.wienerIterationsBlue = this.wienerIterations;
  }

  private equalizeChannelsForDenoising() {
    this.profile.denoise1AmountGreen = this.denoise1Amount;
    this.profile.denoise1RadiusGreen = this.denoise1Radius;
    this.profile.denoise1IterationsGreen = this.denoise1Iterations;
    this.profile.savitzkyGolaySizeGreen = Number(this.savitzkyGolaySize);
    this.profile.savitzkyGolayAmountGreen = this.savitzkyGolayAmount;
    this.profile.savitzkyGolayIterationsGreen = this.savitzkyGolayIterations;
    this.profile.rofThetaGreen = this.rofTheta;
    this.profile.rofIterationsGreen = this.rofIterations;
    this.profile.bilateralIterationsGreen = this.bilateralIterations;
    this.profile.bilateralRadiusGreen = this.bilateralRadius;
    this.profile.bilateralSigmaColorGreen = this.bilateralSigmaColor;
    this.profile.bilateralSigmaSpaceGreen = this.bilateralSigmaSpace;

    this.profile.denoise2RadiusGreen = this.denoise2Radius;
    this.profile.denoise2IterationsGreen = this.denoise2Iterations;
    this.profile.denoise1AmountBlue = this.denoise1Amount;
    this.profile.denoise1RadiusBlue = this.denoise1Radius;
    this.profile.denoise1IterationsBlue = this.denoise1Iterations;
    this.profile.savitzkyGolaySizeBlue = Number(this.savitzkyGolaySize);
    this.profile.savitzkyGolayAmountBlue = this.savitzkyGolayAmount;
    this.profile.savitzkyGolayIterationsBlue = this.savitzkyGolayIterations;
    this.profile.denoise2RadiusBlue = this.denoise2Radius;
    this.profile.denoise2IterationsBlue = this.denoise2Iterations;
    this.profile.rofThetaBlue = this.rofTheta;
    this.profile.rofIterationsBlue = this.rofIterations;
    this.profile.bilateralRadiusBlue = this.bilateralRadius;
    this.profile.bilateralSigmaColorBlue = this.bilateralSigmaColor;
    this.profile.bilateralSigmaSpaceBlue = this.bilateralSigmaSpace;
  }

  private getRootFolderCapped(rootFolder: string): string {
    const rootFolderLength = rootFolder?.length;
    return rootFolderLength > ROOT_FOLDER_MAX_LENGTH
      ? '-' +
          rootFolder?.substring(
            rootFolderLength - ROOT_FOLDER_MAX_LENGTH,
            rootFolderLength
          )
      : rootFolder;
  }
}
