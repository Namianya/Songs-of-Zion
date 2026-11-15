# Privacy Policy for Songs of Zion

**Effective Date:** November 15, 2026  
**Package Name:** `com.wekesa.songsofzion.wzpkqt`  
**Contact:** Wekesa350@gmail.com

Songs of Zion is an offline-first Seventh-day Adventist English and Tamil hymnal application. Your privacy is important to us. This Privacy Policy explains what information the app accesses, how it is used, and how your data is handled.

---

## 1. Summary

- **No Account Required:** Songs of Zion does not require you to sign in or create an account.
- **No Ads or Analytics Tracking:** The app does not include advertising SDKs, behavioral tracking, or third-party analytics.
- **Offline-First Storage:** Your hymnal database, bookmarks, Sabbath worship setlists, display settings, and imported hymn files are stored locally on your device.

---

## 2. Information Stored Locally on Your Device

Songs of Zion stores the following data strictly on your device using local Android storage (Room database and Preferences DataStore):

- Pre-bundled English, Tamil (`தமிழ்`), and Romanized Tamil hymn lyrics and metadata.
- Your bookmarked/favorite hymns and custom Sabbath worship setlists.
- Reading and appearance preferences (font size, Sanctuary Night Mode, theme mode, and preferred language).
- Any custom hymn catalog JSON files you choose to import, as well as Tamil translations cached after an on-demand translation.

We do not have access to this local data, and it is never uploaded to our own servers. You can delete all locally stored data at any time by clearing the app's storage in Android Settings or by uninstalling the app.

---

## 3. Permissions and File Access

Songs of Zion requests minimal Android permissions:

- **Internet Access (`INTERNET`):** Used only when you actively request an on-demand Tamil translation for an English hymn that is not yet saved offline, and for Firebase App Check verification (described below). All pre-bundled hymns and saved translations work completely offline.
- **User-Selected JSON Files (Storage Access Framework):** When you choose to import a hymn database or export a sample JSON template, the app uses Android's built-in document picker. The app only accesses the specific file you select and does **not** request broad storage, media, contacts, camera, microphone, or location permissions.

---

## 4. Optional On-Demand Tamil Translation (Third-Party Services)

If you choose to translate an English-only hymn into Tamil inside the reader, the app uses **Firebase AI Logic (Gemini API)** and **Firebase App Check (reCAPTCHA)** provided by Google:

- **What is sent:** Only the hymn's title, number, and public English lyric text needed to produce the Tamil translation, along with standard network/integrity metadata required by Firebase App Check to prevent abuse of the translation service.
- **Personal data:** Songs of Zion does not collect or attach your name, email address, contacts, or personal files to translation requests.
- **Third-Party Policies:** Requests processed through Firebase AI Logic and reCAPTCHA are subject to [Google's Privacy Policy](https://policies.google.com/privacy) and [Firebase Privacy and Security Documentation](https://firebase.google.com/support/privacy).

If you do not trigger an on-demand translation, no hymn content is transmitted over the network.

---

## 5. Data Sharing and Sale

We do **not** sell, rent, trade, or share personal information with third parties for marketing or advertising purposes.

---

## 6. Children's Privacy

Songs of Zion does not knowingly collect personal information from children under 13 (or the applicable age of digital consent in your jurisdiction). Because the app works without user accounts or personal data collection, users of all ages can browse and read hymns offline.

---

## 7. Open-Source Transparency

This app's source code is intended to be published in a public GitHub repository so anyone can inspect how data, permissions, and translations are handled.

---

## 8. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect app updates or legal requirements. Any changes will be posted in this document with an updated effective date.

---

## 9. Contact Us

If you have any questions or suggestions regarding this Privacy Policy, please contact:

- **Email:** Wekesa350@gmail.com
