# LuckyStackWorker - RELEASE NOTES

## 02/01/2026 major release 7.0.0

- Added Derotation function to automatically derotate a set of images to a selected reference image
- Added Stacking function to stack a set of images
- Added "Set save scale & size" function to apply scaling and background resizing during saving and batch apply
- Dispersion correction now automatically corrects to the estimated optimal values. Works on sub-pixel level
- Enlarged histogram graphic for better visual reading
- Filter values can now also be adjusted manually by entering the numerical values directly
- Load profile now also applies the earlier used dispersion correction
- Added "Quadratic" deconvolution option to apply deconvolution 2x
- Revised look-and-feel: replaced batch panel with dedicated batch button bar, more "balanced" contrast colors
- Permanently removed deprecated GMIC filters Ian's denoise and Enhance locally.
- Upgraded all dependencies to the latest versions.

## 14/09/2025 revision release 6.21.1

- Fixed bug when opening a non 16-bit image the app did not show a warning and the spinner stayed active
- Renamed Crop to ROI in the metadata panel
- Fixed issue with new version popup not showing the release notes
- Fixed issue button tooltips no longer showing after version update is postponed.

## 24/08/2025 minor release 6.21.0

- Added new Suppress clipping filter that reduces the clipped areas after applying deconvolution
- Added new Highlight clipped areas that will make the clipped parts of the image blink
- Transformed the clipping function into a more general purpose ROI selection function that can be used to crop or apply the filters only to the selected ROI, thereby improving the performance
- Histogram now showing the individual red, green and blue graphic
- Brightness and Lightness filters can now also be moved below 0 to reduce the value
- ROI selection will be re-used now also when disabling the ROI and later re-enable it again, as long as the same image is being processed
- When saving the image the app will now ask for confirmation when the specified filename already exists in the folder
- Save button has been moved to the bottom right of the button panel, to prevent confusion in case the user accidentally pressed to Open reference image button instead of Save
- Improved the new version check
- Fixed issue with the histogram indicators sometimes showing wrong values
- Fixed issue with root folder not showing the end part
- Using java 24 non pinning virtualthreads to optimize multithreading
- Migrated backend build to Gradle
- Upgraded all dependencies to the latest versions.

## 03/03/2025 revision release 6.12.1

- Fixed bug when setting sharpen amount to 0 the next time you start the app it would not work anymore
- Fixed bug in the windows startup that only happened for users with a space character in their username
- Fixed bug in Bilateral Denoise not able to change values for green and blue channel individually.

## 23/02/2025 minor release 6.12.0

- Added new Deconvolution using a Point Spread Function (PSF)
- Added Rotation function
- Added Bilater denoising filter
- Added Caching mechanism to improve performance
- Revert buttons now reverts to an unprocessed image stack
- Improved Crop function
- Improved look and feel for the control panel
- Fixed bug freezing control panel when setting sharpen amount to 0.
- Added unlock checkbox to Ians' NR and Equalize locally to discourage further usage

## 22/11/2024 minor release 6.6.0

- Added reset button to color balance, to easily set the sliders back to 0
- Improved color balance, adjusting to higher Red, Green or Blue values will not increase the background brightness
- Added 2 new sliders to Ian's noise reduction for medium sized details and iterations
- Added new blend raw slider to the sharpening control.
- Added new lightness slider to the light control
- Added switch to preserve/not preserve the dark background to the light control
- Fixed bug when opening png files, image no longer initially shown over-exposed
- Fixed bug when loading an earlier saved profile yaml file with the pass 2 denoise switched off would always open with pass 2 denoise on
- Fixed bug when opening mono image stack show over-exposed
- Upgraded all dependencies to the latest versions.

## 05/10/2024 minor release 6.1.0

- Added "Revert changes" button to go back to the setting when the image stack was opened
- Fixed broken Edge artefact suppression - clipping option
- Fixed issue with saving as JPG with RGB channels out of balance and being over-exposed.
- Fixed small UI layout issue with button misalignment.

## 08/09/2024 major release 6.0.0

- Replaced histogram window with a new metadata panel integrated in the image window containing the histogram graphic plus seperate indicators per channel
- New object metadata indicators added to the image window
- New image metatada indictors also added to the image window
- Added ability to sharpen & denoise the red, green & blue channels seperately
- Added new button to switch the visible channel between color, red, green and blue
- Added new color balance normalization switch to automatically equalize the color balance
- New and more compact look and feel, replaced textual buttons with icons
- New revised image window with better control panel integration
- New buttton to force showing the image windows at 100% scale
- Many other noticeable and less noticeable changes to the user interface
- Upgraded frontend & backend dependencies to the latest versions.

## 30/04/2024 minor release 5.2.0:

- Added ability to open stacks cumulatively for specific channels (RGB, gray, red, green, blue)
- Profiles will persist with the installation of a new version from now on
- Removed usage of internal database which was no longer needed
- Improved startup time of the backend
- Upgraded GUI to the latest version of Electron.
- Fixed problem on MacOS with GUI processes not being terminated when app is closed.

## 21/01/2024: major release 5.0.0:

- Created seperate tab for denoise controls
- Added possibility to apply denoising in 2 passes with different algorithms of choice
- Added new denoise algorithm: Ian's NR (from G'MIC)
- Added 'Equalize locally' slider to apply very powerful HDR alike contrast enhancement (from G'MIC Equalize local histograms)
- Released the first linux distribution
- Added dispersion deviation indicators to the dispersion corrector
- Fixed bug in histogram not opening for monochrome image stacks
- More compact layout
- Revised processing core, resulting in a better (more quiet) user experience
- Change TCP port number used for GUI-backend interaction to 36469.
- Upgraded frontend & backend dependencies to the latest versions.

## 03/10/2023 minor release 4.8.0:

- Added new slider for controlling the dering threshold
- Added radio controls to open stack at different scales
- Added new slider to reduce purple fringing
- Added stop button to the batch worker
- Improved the startup experience
- Several UI layout improvements
- Shortcut automatically added to the desktop during installation
- Several bugfixes

## 13/08/2023 minor release 4.3.0-beta:

- Realtime function is now off by default
- Apply profile now saves results in a subfolder called 'lsw\_<profile>'
- Added histogram window
- Added checkboxes to luminance sharpen to include/exclude specific channels (red, green, blue, color)
- Fixed issue with local contrast causing artefacts under certain conditions in combination with RGB sharpen
- Fixed issue with RGB align causing line artefacts under certain conditions
- Refactored backend java code
- Upgraded java backend to the latest spring boot version, also upgraded other dependencies.

## 07/05/2023 revision release 4.0.1:

- Fixed ".tiff" not being accepted as valid extension.
- Fixed dispersion correction issue.
- Fixed switching denoise or edge artefact switch off not always switching off.
- Added ".jpeg" as an allowed extension (common on MacOS).

## 21/04/2023 major release 4.0.0-beta:

- Added new method for removal of the edge ring artefact happening on planet surfaces.
- Added new sliders to control local contrast on fine, medium and large details
- Splitted up the tabs in 3: Sharpen & Denoise, Contrast & Light, Color & Dispersion.
- Added a Dispersion Correction tool.
- Added ability to save individual images in 100% quality JPG format.

## 11/03/2023 revision release 3.1.1:

- Fixed broken 16-bit PNG file opening
- Fixed issues on MacOS with window focus and minimizing
- Fixed problem on MacOS when closing GUI the image window would not immediately close
- Reduced size of MacOS binary
- Fixed issue with crop size indicator showing when window is de-iconified

## 04/03/2023 minor release 3.1.0:

- First release of the mac version (both x64 and arm)
- Fixed bugs in file opening (folders with dots were causing problems)
- Fixed db file not overwritten when installing new version
- Fixed UI windown close icon not closing the backend of the app

## 31/01/2023 major release 3.0.0-beta:

- New options for sharpening (luminance only). Replaced sharpening algorithm with a faster custom build version.
- New sliders for supressing the edge clipping effect on Mars & Venus
- Revised denoise options, choice between Savitzky-Golay or Sigma filter. Replaced Sigma filter sigma slider with a choice between mode 1 or 2
- Added additional 129 & 169 kernel size options to the Savitzky-Golay filter
- Added realtime processing, useful to pick up files automatically as they are being outputted by stacking apps (e.g. Autostakkert)
- Full 16-bit support for Saturation slider through a custom build algorithm
- Replaced RGB balance filter with a faster custom build algorithm
- Better input validation, prevent non supported formats to be opened (non 16-bit image files)
- Close currently opened image stack if a new one is opened
- Added keyboard support by adding in a rate limit on the sliders, thereby fixing the overloading issue when arrow keys where held down
- Various layout improvements

## 29/12/2022 revision release 2.4.0:

- Moved database and log files to ~/AppData/Local folder
- Added an setup installer for easy installation.

## 28/12/2022 revision release 2.3.0:

- Added new Savitzky-Golay denoising filter controls

## 30/10/2022 revision release 2.2.3:

- Fixed bug in load profile not switching to the profile in the yaml file being loaded.

## 04/09/2022 revision release 2.2.2:

- Fixed spinner no longer showing for large images when load profile was selected.
- Upgraded to the latest jdk 17 and angular 14.

## 30/7/2022 revision release 2.2.1:

- Some layout fixes

## 29/7/2022 minor release 2.2.0:

- Added ROI dimension indicator
- Improved startup time
- Replaced card layout with tabs to reduce GUI size
- Fixed issues with crop selection

## 17/7/2022 minor release 2.1.0:

- Added new sliders to control contrast, brightness & darken background
- Fixed spinner not always showing when processing large images
- Replaced initial exposure correction problem of ImageJ with correct handling of exposure (exact match with original)
- Fixed Firecapture target not being recognized if filename does not start with it

## 1/7/2022: revision release 2.0.1

- Fixed issue with cropping selection using the wrong mouse picker after deselecting the ROI.
- Using 2 decimals for saturation
- Fixed minor layout alignment issue.

## 30/6/2022: major release 2.0.0

- Added a new panel for denoising. Added 3 new sliders for controlling the sigma, radius and number of iterations of the sigma filter denoising.
- Added saturation control slider
- Added cropping function
- Added Mouse hand control to scroll through images larger than the window size
- Added an automatic new version notification
- More compact UI layout design
- Added hover on buttons
- Replaced slider thumbs with immediate indicator value on the right
- Added spinner when processing large images

## 25/5/2022 revision release 1.4.1:

- Fixed some minor layout issues.

## 21/5/2022 minor release 1.4.0:

- Added profile saving in a yaml file along with the TIF whenever profile is applied.
- Profile is saved to a yaml file also for every reference image that is saved.
- Profile is now loaded first from yaml file if found. If not it will load it from the database.
- Added a Load Profile button to allow selecting the profile from earlier saved yaml file.

## 26/3/2022 revision release 1.3.3:

- Fixed worker not processing file stacks orgininating from recordings using older versions of Firecapture that used a different profile prefix.

## 13/3/2022 revision release 1.3.2:

- Fixed non working grayscale PNG file opening.

## 12/3/2022 revision release 1.3.1:

- Decreased the sharpen slider lowest possible value to 5000
- Some textual changes in the about panel
- Minor improvements in opening file
- Fixed bug when cancelling open reference file it would reset all the sliders.

## 1/3/2022 minor release 1.3.0:

- Replaced ROF denoise with Sigma Filter Plus denoise
- Removed internal conversion to 32-bit depth that sometimes caused issues with color shifting
- Improved gamma & color controls
- Improved initial exposure correction
- Fixed broken PNG file reading
- Save files in 24-bit depth TIFF format

## 18/2/2022 minor release 1.2.0:

- Added night mode
- Changed website link
- Minor UI improvements

## 12/2/2022 revision release 1.1.4:

- Fixed layout rendering issue happening on some machines.

## 11/2/2022 revision release 1.1.3:

- Added icon to image window and file picker.
- File picker with native look and feel.
- Several layout improvements.

## 10/2/2022 minor release 1.1.2:

- Added zoom in/out function.

## 7/2/2022 revision 1.0.2:

- Fixed root folder not showing after selecting the reference image.
- Decreased shutdown wait delay to 4 seconds.

## 6/2/2022 revision 1.0.1:

- Fixed issues with starting up and shutting down the app.

## 5/2/2022 release 1.0.0:

First official release.
