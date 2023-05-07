update profiles set green=0;
update profiles set blue=0;
update profiles set red=0;
update profiles set gamma=1;
update profiles set denoise=0;
update profiles set denoise_radius=1;
update profiles set denoise_sigma=2;
update profiles set saturation=1;
update profiles set contrast=0;
update profiles set brightness=0;
update profiles set background=0;
update profiles set savitzky_golay_size=2;
update profiles set savitzky_golay_amount=50;
update profiles set savitzky_golay_iterations=1;
update profiles set clipping_strength=0;
update profiles set clipping_range=25;
update profiles set dering_strength=0;
update profiles set dering_radius=3;
update profiles set local_contrast_fine=0;
update profiles set local_contrast_medium=0;
update profiles set local_contrast_large=0;
update profiles set local_contrast_mode='LUMINANCE';
update profiles set sharpen_mode='LUMINANCE';
update profiles set dispersion_correction_enabled=false;
update profiles set dispersion_correction_redx=0;
update profiles set dispersion_correction_bluex=0;
update profiles set dispersion_correction_redy=0;
update profiles set dispersion_correction_bluey=0;

select * from profiles;
update settings set latest_known_version='4.0.1';
update settings set latest_known_version_checked=null; --TIMESTAMP '2023-01-24 10.00.00';
update settings set root_folder='C:/';
update settings set extensions='tif,png,tiff';

select * from settings;
