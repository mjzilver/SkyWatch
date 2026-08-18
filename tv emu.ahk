#Requires AutoHotkey v2

#HotIf WinActive("Android Emulator")

Up::Run("adb shell input keyevent DPAD_UP", , "Hide")
Down::Run("adb shell input keyevent DPAD_DOWN", , "Hide")
Left::Run("adb shell input keyevent DPAD_LEFT", , "Hide")
Right::Run("adb shell input keyevent DPAD_RIGHT", , "Hide")
Enter::Run("adb shell input keyevent DPAD_CENTER", , "Hide")
Escape::Run("adb shell input keyevent BACK", , "Hide")

#HotIf