export class Profile {
  name: string;

  // sharpen red or all channels
  radius: number;
  amount: number;
  level: number;
  iterations: number;
  clippingStrength: number;
  clippingRange: number;
  deringRadius: number;
  deringStrength: number;
  deringThreshold: number;

  // sharpen green
  radiusGreen: number;
  amountGreen: number;
  levelGreen: number;
  iterationsGreen: number;
  clippingStrengthGreen: number;
  clippingRangeGreen: number;
  deringRadiusGreen: number;
  deringStrengthGreen: number;
  deringThresholdGreen: number;

  // sharpen blue
  radiusBlue: number;
  amountBlue: number;
  levelBlue: number;
  iterationsBlue: number;
  clippingStrengthBlue: number;
  clippingRangeBlue: number;
  deringRadiusBlue: number;
  deringStrengthBlue: number;
  deringThresholdBlue: number;

  sharpenMode: string;
  applySharpenToChannel: string;

  // denoise 1 red or all channels
  denoiseAlgorithm1: string;
  denoise1Amount: number;
  denoise1Radius: number;
  denoise1Iterations: number;
  iansAmount: number;
  iansRecovery: number;

  // denoise 2 red or all channels
  denoiseAlgorithm2: string;
  savitzkyGolaySize: number;
  savitzkyGolayAmount: number;
  savitzkyGolayIterations: number;
  denoise2Radius: number;
  denoise2Iterations: number;

  // denoise 1 green
  denoiseAlgorithm1Green: string;
  denoise1AmountGreen: number;
  denoise1RadiusGreen: number;
  denoise1IterationsGreen: number;
  iansAmountGreen: number;
  iansRecoveryGreen: number;

  // denoise 2 green
  denoiseAlgorithm2Green: string;
  savitzkyGolaySizeGreen: number;
  savitzkyGolayAmountGreen: number;
  savitzkyGolayIterationsGreen: number;
  denoise2RadiusGreen: number;
  denoise2IterationsGreen: number;

  // denoise 1 blue
  denoiseAlgorithm1Blue: string;
  denoise1AmountBlue: number;
  denoise1RadiusBlue: number;
  denoise1IterationsBlue: number;
  iansAmountBlue: number;
  iansRecoveryBlue: number;

  // denoise 2 blue
  denoiseAlgorithm2Blue: string;
  savitzkyGolaySizeBlue: number;
  savitzkyGolayAmountBlue: number;
  savitzkyGolayIterationsBlue: number;
  denoise2RadiusBlue: number;
  denoise2IterationsBlue: number;

  applyDenoiseToChannel: string;

  luminanceIncludeRed: boolean;
  luminanceIncludeGreen: boolean;
  luminanceIncludeBlue: boolean;
  luminanceIncludeColor: boolean;

  gamma: number;
  contrast: number;
  brightness: number;
  background: number;
  localContrastMode: string;
  localContrastFine: number;
  localContrastMedium: number;
  localContrastLarge: number;
  equalizeLocalHistogramsStrength: number;
  red: number;
  green: number;
  blue: number;
  purple: number;
  saturation: number;
  dispersionCorrectionEnabled: boolean;
  dispersionCorrectionRedX: number;
  dispersionCorrectionBlueX: number;
  dispersionCorrectionRedY: number;
  dispersionCorrectionBlueY: number;
  scale: number;
  openImageMode: string;
}
