# Release security

Android releases are blocked unless the GitHub Actions secret `IMGBB_API_KEY` is configured.

The Android application does not contain the ImgBB API key. Image bytes are sent to the authenticated Firebase Cloud Function `uploadImageToImgbb`, which reads `IMGBB_API_KEY` from Firebase Secret Manager and uploads the image to ImgBB. Firestore stores the resulting ImgBB URL; Firebase Storage is not used for these images.

Before the next release:

1. Rotate the ImgBB API key that was previously embedded in the APK/source.
2. Add the new value as the GitHub Actions repository secret `IMGBB_API_KEY`.
3. The Functions deployment workflow will copy that secret into Firebase Secret Manager and deploy `uploadImageToImgbb`.
4. Only after that deployment succeeds should a new Android release be published.
