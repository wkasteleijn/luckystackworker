export class Profile {
  name: string;
  radius: number;
  amount: number;
  level: number;
  iterations: number;

  // denoise 1
  denoiseAlgorithm1: string;
  denoise1Amount: number;
  denoise1Radius: number;
  denoise1Iterations: number;

  iansAmount: number;
  iansRecovery: number;

  // denoise 2
  denoiseAlgorithm2: string;
  savitzkyGolaySize: number;
  savitzkyGolayAmount: number;
  savitzkyGolayIterations: number;

  denoise2Radius: number;
  denoise2Iterations: number;

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
  clippingStrength: number;
  clippingRange: number;
  deringRadius: number;
  deringStrength: number;
  deringThreshold: number;
  sharpenMode: string;
  dispersionCorrectionEnabled: boolean;
  dispersionCorrectionRedX: number;
  dispersionCorrectionBlueX: number;
  dispersionCorrectionRedY: number;
  dispersionCorrectionBlueY: number;
  scale: number;
}
