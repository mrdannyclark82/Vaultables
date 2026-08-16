import { defineSecret, defineString } from "firebase-functions/params";

// Sensitive values are bound at deploy time from Firebase Secret Manager.
export const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
export const CARDSIGHT_API_KEY = defineSecret("CARDSIGHT_API_KEY");
export const GOOGLE_CUSTOM_SEARCH_API_KEY = defineSecret("GOOGLE_CUSTOM_SEARCH_API_KEY");
export const GOOGLE_CUSTOM_SEARCH_CX = defineSecret("GOOGLE_CUSTOM_SEARCH_CX");
export const STRIPE_SECRET_KEY = defineSecret("STRIPE_SECRET_KEY");
export const STRIPE_WEBHOOK_SECRET = defineSecret("STRIPE_WEBHOOK_SECRET");

// The CardSight provider URL is not a credential. Firebase prompts for this
// parameter at deploy time and keeps it out of client configuration.
export const CARDSIGHT_API_URL = defineString("CARDSIGHT_API_URL");

export const API_SECRETS = [
  GEMINI_API_KEY,
  CARDSIGHT_API_KEY,
  GOOGLE_CUSTOM_SEARCH_API_KEY,
  GOOGLE_CUSTOM_SEARCH_CX,
  STRIPE_SECRET_KEY,
];
