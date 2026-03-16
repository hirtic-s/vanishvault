# VanishVault

VanishVault is a secure, ephemeral file-sharing application designed to protect sensitive documents. It allows users to upload files with robust AES-256 encryption, set custom expiration timers, enforce maximum download limits, and utilize a "burn-after-read" feature for ultimate data privacy. 

## Key Features
- **End-to-End Security**: Files are encrypted server-side before being stored in AWS S3.
- **Ephemeral Sharing**: Links automatically expire based on custom time limits or download caps.
- **Burn-After-Read**: Files are permanently deleted from storage immediately after the first successful download.
- **Password Protection**: Mandatory password requirements with dynamic strength validation for all uploads.
- **Real-time Notifications**: WebSocket integration for instant alerts on file access and downloads.
- **Access Logging**: Comprehensive tracking of file interactions, including failed access attempts.

## Tech Stack
- **Backend**: Java, Spring Boot, Spring WebSockets, AWS S3 SDK
- **Frontend**: React.js, Vite, Tailwind CSS, Axios, Lucide React
- **Security**: AES-256 Encryption, Password Hashing
