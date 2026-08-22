# SkyWatch

![SkyWatch Logo](assets/logo.png)

SkyWatch is an Android TV application for browsing and playing media from SMB network shares.

The goal is to provide a simple, TV-friendly media browser that can connect to network storage, discover SMB servers on the local network, browse their shares and folders, and play video files directly on the device, focusing on performance and simplicity.

## Subtitle Server

SkyWatch can provide subtitles from the internet through an independently running Subtitle Server.

The Subtitle Server requires a SubDL API key to retrieve subtitles. The API key is kept outside of the Android application, so the Subtitle Server runs separately from SkyWatch rather than being bundled into the APK.

SkyWatch can discover Subtitle Servers running on the local network using mDNS. Alternatively, a Subtitle Server can be configured manually by specifying its URL in the SkyWatch settings.