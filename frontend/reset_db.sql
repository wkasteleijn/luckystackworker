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
update profiles set savitzky_golay_size=0;
update profiles set savitzky_golay_amount=100;
update profiles set savitzky_golay_iterations=1;
update profiles set clipping_strength=25;
update profiles set clipping_range=25;
update profiles set sharpen_mode='LUMINANCE';
select * from profiles;
update settings set latest_known_version='2.4.0';
update settings set latest_known_version_checked=null;
update settings set root_folder='C:/';
select * from settings;
