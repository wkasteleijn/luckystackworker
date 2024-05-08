; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "LuckyStackWorker"
;#define MyAppVersion "5.2.0" ;NOTE: version not relevant
#define MyAppPublisher "Wilco Kasteleijn"
#define MyAppURL "https://www.wilcokas.com/luckystackworker"
#define MyAppExeName "LuckyStackWorker.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{F811B04C-3095-4F0E-87B3-1BB831C562A1}
AppName={#MyAppName}
;AppVersion={#MyAppVersion} ;NOTE: version not relevant
AppVerName={#MyAppName}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
ArchitecturesInstallIn64BitMode=x64
DisableProgramGroupPage=yes
LicenseFile=C:\Users\wkast\git\luckystackworker\frontend\LuckyStackWorker-win32-x64\LICENSE_LSW.txt
; Uncomment the following line to run in non administrative install mode (install for current user only.)
;PrivilegesRequired=lowest
OutputBaseFilename=LuckyStackWorker-setup
SetupIconFile=C:\Users\wkast\git\luckystackworker\frontend\LuckyStackWorker-win32-x64\resources\app\lsw_icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkablealone

[Code]
procedure InitializeWizard;
begin
  WizardForm.LicenseAcceptedRadio.Checked := True;
end;

[Files]
Source: "C:\Users\wkast\git\luckystackworker\frontend\LuckyStackWorker-win32-x64\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\wkast\git\luckystackworker\frontend\LuckyStackWorker-win32-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

