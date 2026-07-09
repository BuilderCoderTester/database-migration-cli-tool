; ------------------------------------------------------------
; MigrationTool Installer
; ------------------------------------------------------------

#define MyAppName "MigrationTool"
#define MyAppVersion "0.0.2"
#define MyAppPublisher "BuilderCoderTester"
#define MyAppExeName "MigrationTool.exe"

[Setup]
AppId={{3813E479-BE12-4BDE-B58F-BCC8461223BD}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}

DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}

UninstallDisplayIcon={app}\{#MyAppExeName}

ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

PrivilegesRequired=admin

Compression=lzma2
SolidCompression=yes
WizardStyle=modern

DisableProgramGroupPage=yes

OutputDir=C:\Users\Sigilotech-User\Desktop
OutputBaseFilename=MigrationTool_Setup_v0.0.2

VersionInfoVersion=0.0.2
VersionInfoCompany=BuilderCoderTester
VersionInfoDescription=Migration Tool
VersionInfoProductName=MigrationTool

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create Desktop Shortcut"; GroupDescription: "Additional Icons:"; Flags: unchecked

[Dirs]
Name: "{commonappdata}\MigrationTool"
Name: "{commonappdata}\MigrationTool\config"
Name: "{commonappdata}\MigrationTool\drivers"
Name: "{commonappdata}\MigrationTool\migrations"
Name: "{commonappdata}\MigrationTool\logs"

[Files]

; =====================================================
; CHANGE THIS PATH IF YOUR PROJECT LOCATION CHANGES
; =====================================================

Source: "C:\Users\Sigilotech-User\IdeaProjects\Database_Migration\Backend\demo\output\MigrationTool\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]

Name: "{group}\MigrationTool"; Filename: "{app}\MigrationTool.exe"

Name: "{autodesktop}\MigrationTool"; Filename: "{app}\MigrationTool.exe"; Tasks: desktopicon

[Run]

Filename: "{app}\MigrationTool.exe"; Description: "Launch MigrationTool"; Flags: nowait postinstall skipifsilent

[UninstallDelete]

Type: filesandordirs; Name: "{app}\logs"
Type: filesandordirs; Name: "{app}\migrations"
Type: filesandordirs; Name: "{app}\config"
Type: filesandordirs; Name: "{app}\drivers"