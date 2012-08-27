!define APPNAME "Proof Pad"
!define DESCRIPTION "An IDE for ACL2"

!define VERSIONMAJOR 0
!define VERSIONMINOR 1
 
RequestExecutionLevel admin
 
InstallDir "$PROGRAMFILES\${APPNAME}"
 
Name "${APPNAME}"
Icon "media\IDEIcon64.ico"
outFile "Proof Pad installer.exe"
 
!include LogicLib.nsh
 
# Just three pages - license agreement, install location, and installation
page directory
Page instfiles
 
!macro VerifyUserIsAdmin
UserInfo::GetAccountType
pop $0
${If} $0 != "admin"
        messageBox mb_iconstop "Administrator rights required."
        setErrorLevel 740
        quit
${EndIf}
!macroend
 
function .onInit
	setShellVarContext all
	!insertmacro VerifyUserIsAdmin
functionEnd
 
section "install"
	setOutPath $INSTDIR
	File "proofpad.jar"
	File "media\IDEIcon64.ico"
	File /r "acl2"
 
	writeUninstaller "$INSTDIR\uninstall.exe"
 
	# Start Menu
	createDirectory "$SMPROGRAMS"
	createShortCut "$SMPROGRAMS\${APPNAME}.lnk" "$INSTDIR\proofpad.jar" "" "$INSTDIR\IDEIcon64.ico"
 
	# Registry information for add/remove programs
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME} - ${DESCRIPTION}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$\"$INSTDIR$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$\"$INSTDIR\IDEIcon64.ico$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "$\"Caleb Eggensperger$\""
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "$\"${VERSIONMAJOR}.${VERSIONMINOR}$\""
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMajor" ${VERSIONMAJOR}
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "VersionMinor" ${VERSIONMINOR}
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
	WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
	# Fix certs
	ExecWait "$INSTDIR\acl2\run_acl2.exe"
sectionEnd
 
# Uninstaller
 
function un.onInit
	SetShellVarContext all
	!insertmacro VerifyUserIsAdmin
functionEnd
 
section "uninstall"
	delete "$SMPROGRAMS\${APPNAME}.lnk"
 
#	Delete $INSTDIR\launcher.exe
#	Delete $INSTDIR\proofpad.jar
#	RMDir /r $INSTDIR\acl2
#	delete $INSTDIR\uninstall.exe
	RMDir /r $INSTDIR
 
	# Remove uninstaller information from the registry
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
sectionEnd
