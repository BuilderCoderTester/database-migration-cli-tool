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
OutputDir=Installer
OutputBaseFilename=MigrationTool_Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin
; SetupIconFile=icon.ico
LicenseFile=LICENSE.txt
UninstallDisplayIcon={app}\MigrationTool.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create Desktop Shortcut"; GroupDescription: "Additional Icons"

[Dirs]
Name: "{commonappdata}\MigrationTool"

[Files]
Source: "output\MigrationTool\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "Prerequisites\vc_redist.x64.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall skipifsourcedoesntexist
Source: "Prerequisites\postgresql.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall skipifsourcedoesntexist

[Icons]
Name: "{group}\MigrationTool"; Filename: "{app}\MigrationTool.exe"
Name: "{autodesktop}\MigrationTool"; Filename: "{app}\MigrationTool.exe"; Tasks: desktopicon

[Run]
Filename: "{tmp}\vc_redist.x64.exe"; Parameters: "/install /quiet /norestart"; \
    StatusMsg: "Installing Microsoft Visual C++ Runtime..."; \
    Flags: waituntilterminated skipifdoesntexist; Check: NeedVCRedist

Filename: "{tmp}\postgresql.exe"; Parameters: "--mode unattended"; \
    StatusMsg: "Installing PostgreSQL..."; \
    Flags: waituntilterminated skipifdoesntexist; Check: InstallPostgresSelected

Filename: "{app}\MigrationTool.exe"; \
    Description: "Launch MigrationTool"; \
    Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{commonappdata}\MigrationTool"

[Code]
var
  PgPage: TInputOptionWizardPage;
  DbPage: TInputQueryWizardPage;

  HostValue: String;
  PortValue: String;
  DatabaseValue: String;
  UsernameValue: String;
  PasswordValue: String;

function IsPostgreSQLInstalled(): Boolean;
var
  S: String;
begin
  Result := RegQueryStringValue(HKLM64, 'SOFTWARE\PostgreSQL\Installations\PostgreSQL', 'Base Directory', S);
  if not Result then
    Result := RegQueryStringValue(HKLM, 'SOFTWARE\PostgreSQL\Installations\PostgreSQL', 'Base Directory', S);
end;

function NeedVCRedist(): Boolean;
begin
  Result := not RegKeyExists(HKLM64, 'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64');
end;

function InstallPostgresSelected(): Boolean;
begin
  // Only install if not present AND user actively selected the install option
  Result := (not IsPostgreSQLInstalled()) and PgPage.Values[0];
end;

procedure InitializeWizard();
begin
  PgPage := CreateInputOptionPage(
      wpSelectDir,
      'PostgreSQL Setup',
      'PostgreSQL was not detected.',
      'Choose how MigrationTool should proceed.',
      True, False);

  PgPage.Add('Install PostgreSQL');
  PgPage.Add('Skip (I will connect to a remote PostgreSQL server)');
  PgPage.Values[0] := True;
  PgPage.Values[1] := False;
  
  DbPage := CreateInputQueryPage(
    PgPage.ID,
    'Database Configuration',
    'Configure PostgreSQL Connection',
    'Enter the PostgreSQL connection details.');

  DbPage.Add('Host:', False);
  DbPage.Values[0] := 'localhost';

  DbPage.Add('Port:', False);
  DbPage.Values[1] := '5432';

  DbPage.Add('Database:', False);
  DbPage.Values[2] := 'postgres';

  DbPage.Add('Username:', False);
  DbPage.Values[3] := 'postgres';

  DbPage.Add('Password:', True);
  DbPage.Values[4] := '';
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  // Skip the "PostgreSQL not detected" options page if it actually IS installed
  if (PageID = PgPage.ID) and IsPostgreSQLInstalled() then
    Result := True;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
    Result := True;

    if CurPageID = DbPage.ID then
    begin
        if Trim(DbPage.Values[0]) = '' then
        begin
            MsgBox('Host is required.', mbError, MB_OK);
            Result := False;
            Exit;
        end;

        if Trim(DbPage.Values[1]) = '' then
        begin
            MsgBox('Port is required.', mbError, MB_OK);
            Result := False;
            Exit;
        end;

        if Trim(DbPage.Values[2]) = '' then
        begin
            MsgBox('Database name is required.', mbError, MB_OK);
            Result := False;
            Exit;
        end;

        if Trim(DbPage.Values[3]) = '' then
        begin
            MsgBox('Username is required.', mbError, MB_OK);
            Result := False;
            Exit;
        end;

        HostValue := DbPage.Values[0];
        PortValue := DbPage.Values[1];
        DatabaseValue := DbPage.Values[2];
        UsernameValue := DbPage.Values[3];
        PasswordValue := DbPage.Values[4];
    end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ConfigPath, Json: String;
begin
  if CurStep = ssPostInstall then
  begin
    ConfigPath := ExpandConstant('{commonappdata}\MigrationTool\config.json');

    // Fallback default values if pages were skipped (e.g., Silent Install)
    if HostValue = '' then HostValue := 'localhost';
    if PortValue = '' then PortValue := '5432';
    if DatabaseValue = '' then DatabaseValue := 'postgres';
    if UsernameValue = '' then UsernameValue := 'postgres';

    if not FileExists(ConfigPath) then
    begin
      Json :=
        '{'#13#10 +
        '  "host": "' + HostValue + '",'#13#10 +
        '  "port": ' + PortValue + ','#13#10 +
        '  "database": "' + DatabaseValue + '",'#13#10 +
        '  "username": "' + UsernameValue + '",'#13#10 +
        '  "password": "' + PasswordValue + '"'#13#10 +
        '}';
      SaveStringToFile(ConfigPath, Json, False);
    end;

    MsgBox(
      'Installation completed successfully.'#13#10#13#10 +
      'Database settings have been saved to config.json.',
      mbInformation, MB_OK);
  end;
end;