# CloakRun privacy policy (DRAFT)

> **Draft for review.** Arnav Games must review this before publishing it or linking it from the app or a store
> listing. It is not legal advice. Items in [square brackets] need a decision. It describes CloakRun as of v0.6.0.

Last updated: [date of publication]

CloakRun is a real-world hide and seek game made by Arnav Games ("we"). This policy explains what the app collects,
why, who can see it, and how to delete it.

## The short version

- Your exact location never leaves your phone.
- During a match, other players in your lobby see a fuzzy zone around you, never your exact position. The zone is at
  least 25 metres across and shrinks over time.
- Match data is deleted when the match ends. We keep no location history.
- No ads, no selling of data, no advertising ID.
- You can delete your account and data in the app at any time (Profile, then Delete my account and data).

## What we collect and why

| Data | Where it is kept | Why | How long |
| --- | --- | --- | --- |
| Age range (under 13, 13 to 17, 18 or older) | Only on your phone | To keep the game suitable for you | Until you uninstall |
| Player name and avatar seed (a random code that draws your avatar) | Your phone, and Google Firebase (Cloud Firestore) once you are online | So players in your lobby can recognise you, and so a linked Google account can restore your profile | Until you delete your account |
| Account identifier (a random ID from Firebase Authentication; your Google account email if you choose to link one) | Google Firebase Authentication | To sign you in and protect your data | Until you delete your account |
| Lobby data (lobby code, play area chosen by the host, settings, players, roles, readiness) | Google Firebase (Cloud Firestore and Realtime Database) | To run the lobby | Deleted when the host ends the lobby, or after 6 hours at most |
| Online status in a lobby | Google Firebase Realtime Database | To show who is connected | Deleted when the lobby ends |
| Fuzzy zone during a match (a rounded area, never your exact position) | Google Firebase Realtime Database | So seekers can play | Deleted when the match ends |
| Exact location | Only on your phone | Gameplay, the play-area designer, safety warnings and cheat checks | Never uploaded |
| Crash reports, usage statistics and performance traces (optional) | Google Firebase Crashlytics, Analytics and Performance Monitoring | To fix bugs and improve the game | Off unless you turn it on. Retention follows Google's defaults: [confirm in the Firebase console] |
| App integrity checks | Google Firebase App Check and Play Integrity | To protect the game's servers from abuse | Handled by Google |

Location is used only while you are designing a play area or playing a match you joined. A notification shows the
whole time a match is using location.

## Who can see your information

- **Players in your lobby** see your name, your avatar, your role, whether you are ready and online, and, during a
  match, your fuzzy zone.
- **Nobody else** can list or browse lobbies. A lobby can only be found with its code.
- **Google** processes the data above as our service provider (Firebase). See https://firebase.google.com/support/privacy.
- We do not sell or share your information with advertisers or data brokers.

## Children

CloakRun is for players aged 13 and over. Players under 13 cannot use the app. Players aged 13 to 17 should have a
parent or guardian's permission and play with an adult nearby. If you believe a child under 13 has used CloakRun,
contact us and we will delete the data.

## Your choices and rights

- **Diagnostics:** turn crash reports and usage statistics on or off in Profile.
- **Location:** you can deny or revoke location permission in Android settings. Matches need it to play.
- **Delete everything:** Profile, then Delete my account and data. This leaves any lobby you are in, deletes your
  cloud profile and your account, and removes your profile from the phone.
- [Add any region-specific rights statement, for example GDPR or India's DPDP Act, once the target countries are
  decided.]

## Security

Data in transit is encrypted (HTTPS). Server-side Security Rules limit every read and write to the players who need
it. No system is perfectly secure, so please play with people you know.

## Changes

If this policy changes, the app will show the updated version before you continue.

## Contact

Arnav Games, [contact email]
