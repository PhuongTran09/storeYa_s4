// Production build replacement for src/environment/api.config.ts, wired through
// angular.json -> configurations.production.fileReplacements.
//
// Same-origin '/api' assumes the frontend and backend are served behind one reverse
// proxy that forwards /api -> the Spring Boot app. If they live on different hosts,
// put the absolute backend origin here and add that public origin to
// CORS_ALLOWED_ORIGINS on the backend.
//
// cloudName and uploadPreset are not credentials: they are sent in the browser
// request by design. The real API secret stays server-side.
export const environment = {
  production: true,
  cloudinary: {
    cloudName: 'dz0dqy62d',
    uploadPreset: 'store_ya',
    folder: 'imageProduct_storeYa'
  },
};

export const apiUrl = {
  BASE_URL: '/api'
};
