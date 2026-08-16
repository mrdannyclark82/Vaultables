import { createHash } from "node:crypto";

import express, { type NextFunction, type Request, type Response } from "express";
import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth, type DecodedIdToken } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/v2/https";
import Stripe from "stripe";

import {
  API_SECRETS,
  STRIPE_SECRET_KEY,
  STRIPE_WEBHOOK_SECRET,
} from "./config.js";
import { analyzeCard } from "./providers.js";
import {
  ApiError,
  validateDispute,
  validateEscrowId,
  validateIdempotencyKey,
  validateListingId,
  validateScanRequest,
  validateShipping,
} from "./validation.js";

if (!getApps().length) {
  initializeApp();
}

const db = getFirestore();
const auth = getAuth();
const region = "us-central1";

type RecordValue = Record<string, unknown>;
type EscrowState = "paid" | "shipped" | "inspection" | "release_processing" | "released" | "disputed";
type ReleaseStatus = "held" | "blocked" | "release_pending" | "release_failed" | "released";

type Listing = {
  sellerId: string;
  priceMinor: number;
  currency: string;
};

type PaymentAttempt = {
  buyerId: string;
  sellerId: string;
  listingId: string;
  amountMinor: number;
  currency: string;
  paymentIntentId: string;
  clientSecret: string;
  stripeStatus: string;
};

type PaymentReservation = {
  listing: Listing;
  existing?: PaymentAttempt;
};

type Escrow = {
  buyerId: string;
  sellerId: string;
  listingId: string;
  amountMinor: number;
  currency: string;
  paymentIntentId: string;
  paymentStatus: "paid";
  state: EscrowState;
  releaseStatus: ReleaseStatus;
  sellerConfirmedShipped: boolean;
  buyerConfirmedReceived: boolean;
  stripeTransferId?: string;
};

function asRecord(value: unknown): RecordValue | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? value as RecordValue
    : undefined;
}

function isSafeMinorAmount(value: unknown): value is number {
  return typeof value === "number" && Number.isSafeInteger(value) && value > 0 && value <= 99_999_999;
}

function stringField(record: RecordValue | undefined, field: string): string | undefined {
  const value = record?.[field];
  return typeof value === "string" ? value : undefined;
}

function valueIsEscrowState(value: unknown): value is EscrowState {
  return value === "paid"
    || value === "shipped"
    || value === "inspection"
    || value === "release_processing"
    || value === "released"
    || value === "disputed";
}

function valueIsReleaseStatus(value: unknown): value is ReleaseStatus {
  return value === "held"
    || value === "blocked"
    || value === "release_pending"
    || value === "release_failed"
    || value === "released";
}

function parseListing(data: unknown): Listing {
  const record = asRecord(data);
  const sellerId = stringField(record, "sellerId");
  const priceMinor = record?.priceMinor;
  const currency = stringField(record, "currency")?.toLowerCase();
  if (
    !sellerId ||
    !isSafeMinorAmount(priceMinor) ||
    !currency ||
    !/^[a-z]{3}$/.test(currency)
  ) {
    throw new ApiError(409, "failed-precondition", "The listing has invalid server-side payment data.");
  }
  return { sellerId, priceMinor, currency };
}

function parsePaymentAttempt(data: unknown): PaymentAttempt {
  const record = asRecord(data);
  const buyerId = stringField(record, "buyerId");
  const sellerId = stringField(record, "sellerId");
  const listingId = stringField(record, "listingId");
  const paymentIntentId = stringField(record, "paymentIntentId");
  const clientSecret = stringField(record, "clientSecret");
  const stripeStatus = stringField(record, "stripeStatus");
  const amountMinor = record?.amountMinor;
  const currency = stringField(record, "currency")?.toLowerCase();
  if (
    !buyerId || !sellerId || !listingId || !paymentIntentId || !clientSecret || !stripeStatus ||
    !isSafeMinorAmount(amountMinor) || !currency || !/^[a-z]{3}$/.test(currency)
  ) {
    throw new ApiError(500, "internal", "A server payment record is invalid.");
  }
  return {
    buyerId,
    sellerId,
    listingId,
    amountMinor,
    currency,
    paymentIntentId,
    clientSecret,
    stripeStatus,
  };
}

function parseEscrow(data: unknown): Escrow {
  const record = asRecord(data);
  const buyerId = stringField(record, "buyerId");
  const sellerId = stringField(record, "sellerId");
  const listingId = stringField(record, "listingId");
  const paymentIntentId = stringField(record, "paymentIntentId");
  const currency = stringField(record, "currency")?.toLowerCase();
  const amountMinor = record?.amountMinor;
  const state = record?.state;
  const releaseStatus = record?.releaseStatus;
  const paymentStatus = record?.paymentStatus;
  if (
    !buyerId || !sellerId || !listingId || !paymentIntentId || !currency ||
    !isSafeMinorAmount(amountMinor) || !valueIsEscrowState(state) ||
    !valueIsReleaseStatus(releaseStatus) || paymentStatus !== "paid" ||
    typeof record?.sellerConfirmedShipped !== "boolean" ||
    typeof record?.buyerConfirmedReceived !== "boolean"
  ) {
    throw new ApiError(500, "internal", "A server escrow record is invalid.");
  }
  const stripeTransferId = stringField(record, "stripeTransferId");
  return {
    buyerId,
    sellerId,
    listingId,
    amountMinor,
    currency,
    paymentIntentId,
    paymentStatus,
    state,
    releaseStatus,
    sellerConfirmedShipped: record.sellerConfirmedShipped,
    buyerConfirmedReceived: record.buyerConfirmedReceived,
    ...(stripeTransferId ? { stripeTransferId } : {}),
  };
}

function summarizeEscrow(id: string, escrow: Escrow): RecordValue {
  return {
    escrowId: id,
    state: escrow.state,
    releaseStatus: escrow.releaseStatus,
    buyerConfirmedReceived: escrow.buyerConfirmedReceived,
    sellerConfirmedShipped: escrow.sellerConfirmedShipped,
  };
}

async function requireUser(request: Request): Promise<DecodedIdToken> {
  const header = request.header("Authorization");
  if (!header?.startsWith("Bearer ")) {
    throw new ApiError(401, "unauthenticated", "A Firebase ID token is required.");
  }
  const token = header.slice("Bearer ".length).trim();
  if (!token) {
    throw new ApiError(401, "unauthenticated", "A Firebase ID token is required.");
  }
  try {
    return await auth.verifyIdToken(token, true);
  } catch {
    throw new ApiError(401, "unauthenticated", "The Firebase ID token is invalid or expired.");
  }
}

function requestRecord(value: unknown): RecordValue {
  const record = asRecord(value);
  if (!record) {
    throw new ApiError(400, "invalid-argument", "A JSON object is required.");
  }
  return record;
}

function hasOnlyKeys(value: RecordValue, keys: readonly string[]): boolean {
  return Object.keys(value).every((key) => keys.includes(key));
}

function stripeClient(): Stripe {
  const secret = STRIPE_SECRET_KEY.value();
  if (!secret) {
    throw new ApiError(500, "failed-precondition", "The payment provider is not configured.");
  }
  return new Stripe(secret);
}

function paymentAttemptId(uid: string, idempotencyKey: string): string {
  return createHash("sha256").update(`${uid}\u0000${idempotencyKey}`).digest("hex");
}

function stripeError(error: unknown): ApiError {
  if (error instanceof Stripe.errors.StripeError) {
    const status = error.type === "StripeCardError" ? 402 : 502;
    return new ApiError(status, "payment-provider-error", "The payment provider could not create the payment intent.");
  }
  return new ApiError(502, "payment-provider-error", "The payment provider could not create the payment intent.");
}

async function createPaymentIntent(
  uid: string,
  body: unknown,
  idempotencyKey: string,
): Promise<RecordValue> {
  const request = requestRecord(body);
  if (!hasOnlyKeys(request, ["listingId"])) {
    throw new ApiError(400, "invalid-argument", "The payment request contains unsupported fields.");
  }
  const listingId = validateListingId(request.listingId);
  const attemptId = paymentAttemptId(uid, idempotencyKey);
  const attemptRef = db.collection("paymentAttempts").doc(attemptId);
  const listingRef = db.collection("marketplaceListings").doc(listingId);
  const lockRef = db.collection("listingPaymentLocks").doc(listingId);
  const reservation = await db.runTransaction<PaymentReservation>(async (transaction) => {
    const [listingSnapshot, existingSnapshot, lockSnapshot] = await Promise.all([
      transaction.get(listingRef),
      transaction.get(attemptRef),
      transaction.get(lockRef),
    ]);
    if (!listingSnapshot.exists) {
      throw new ApiError(404, "not-found", "The listing does not exist.");
    }
    const listing = parseListing(listingSnapshot.data());
    if (listing.sellerId === uid) {
      throw new ApiError(403, "permission-denied", "A seller cannot purchase their own listing.");
    }
    if (existingSnapshot.exists) {
      const existing = parsePaymentAttempt(existingSnapshot.data());
      if (existing.buyerId !== uid || existing.listingId !== listingId) {
        throw new ApiError(409, "idempotency-conflict", "The idempotency key was already used for another payment.");
      }
      return { listing, existing };
    }
    if (listingSnapshot.get("status") !== "active" || listingSnapshot.get("visibility") !== "public") {
      throw new ApiError(409, "failed-precondition", "The listing is not available for purchase.");
    }
    if (lockSnapshot.exists && lockSnapshot.get("attemptId") !== attemptId) {
      throw new ApiError(409, "listing-payment-pending", "Another payment is already in progress for this listing.");
    }
    if (!lockSnapshot.exists) {
      transaction.create(lockRef, {
        attemptId,
        buyerId: uid,
        sellerId: listing.sellerId,
        createdAt: FieldValue.serverTimestamp(),
      });
    }
    return { listing };
  });
  if (reservation.existing) {
    const attempt = reservation.existing;
    return {
      paymentIntentId: attempt.paymentIntentId,
      clientSecret: attempt.clientSecret,
      amountMinor: attempt.amountMinor,
      currency: attempt.currency,
    };
  }
  const listing = reservation.listing;

  let intent: Stripe.PaymentIntent;
  try {
    intent = await stripeClient().paymentIntents.create(
      {
        amount: listing.priceMinor,
        currency: listing.currency,
        automatic_payment_methods: { enabled: true },
        metadata: {
          attemptId,
          listingId,
          buyerId: uid,
          sellerId: listing.sellerId,
        },
      },
      { idempotencyKey: `vaultables-payment-${attemptId}` },
    );
  } catch (error) {
    logger.warn("Stripe PaymentIntent creation failed", {
      errorType: error instanceof Error ? error.name : "unknown",
    });
    throw stripeError(error);
  }

  if (!intent.client_secret) {
    throw new ApiError(502, "payment-provider-error", "The payment provider did not return a client secret.");
  }
  const persisted = await db.runTransaction(async (transaction) => {
    const [current, lockSnapshot] = await Promise.all([
      transaction.get(attemptRef),
      transaction.get(lockRef),
    ]);
    if (current.exists) {
      const attempt = parsePaymentAttempt(current.data());
      if (
        attempt.buyerId !== uid ||
        attempt.listingId !== listingId ||
        attempt.paymentIntentId !== intent.id
      ) {
        throw new ApiError(409, "idempotency-conflict", "The idempotency key was already used for another payment.");
      }
      return attempt;
    }
    if (!lockSnapshot.exists || lockSnapshot.get("attemptId") !== attemptId) {
      throw new ApiError(409, "invalid-state", "The listing payment reservation was lost.");
    }
    const attempt: PaymentAttempt = {
      buyerId: uid,
      sellerId: listing.sellerId,
      listingId,
      amountMinor: listing.priceMinor,
      currency: listing.currency,
      paymentIntentId: intent.id,
      clientSecret: intent.client_secret as string,
      stripeStatus: intent.status,
    };
    transaction.create(attemptRef, {
      ...attempt,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return attempt;
  });

  return {
    paymentIntentId: persisted.paymentIntentId,
    clientSecret: persisted.clientSecret,
    amountMinor: persisted.amountMinor,
    currency: persisted.currency,
  };
}

const openPaymentStates = new Set([
  "requires_payment_method",
  "requires_confirmation",
  "requires_action",
  "processing",
  "succeeded",
]);

async function recordPaymentSucceeded(event: Stripe.Event, intent: Stripe.PaymentIntent): Promise<void> {
  const attemptId = intent.metadata.attemptId;
  if (!attemptId || !/^[a-f0-9]{64}$/.test(attemptId)) {
    throw new ApiError(400, "invalid-webhook", "The payment intent does not belong to this service.");
  }
  const eventRef = db.collection("stripeEvents").doc(event.id);
  const attemptRef = db.collection("paymentAttempts").doc(attemptId);
  const escrowRef = db.collection("escrows").doc(intent.id);

  await db.runTransaction(async (transaction) => {
    const eventSnapshot = await transaction.get(eventRef);
    if (eventSnapshot.exists) {
      return;
    }
    const attemptSnapshot = await transaction.get(attemptRef);
    if (!attemptSnapshot.exists) {
      throw new ApiError(503, "payment-attempt-not-ready", "The payment attempt is not yet durable.");
    }
    const attempt = parsePaymentAttempt(attemptSnapshot.data());
    if (
      intent.status !== "succeeded" ||
      intent.id !== attempt.paymentIntentId ||
      intent.amount !== attempt.amountMinor ||
      intent.currency.toLowerCase() !== attempt.currency ||
      intent.metadata.listingId !== attempt.listingId ||
      intent.metadata.buyerId !== attempt.buyerId ||
      intent.metadata.sellerId !== attempt.sellerId ||
      !openPaymentStates.has(attempt.stripeStatus)
    ) {
      throw new ApiError(409, "invalid-webhook", "Payment intent data does not match the server payment attempt.");
    }

    const existingEscrow = await transaction.get(escrowRef);
    const listingRef = db.collection("marketplaceListings").doc(attempt.listingId);
    const listingSnapshot = await transaction.get(listingRef);
    if (existingEscrow.exists) {
      const escrow = parseEscrow(existingEscrow.data());
      if (
        escrow.paymentIntentId !== intent.id ||
        escrow.buyerId !== attempt.buyerId ||
        escrow.sellerId !== attempt.sellerId ||
        escrow.amountMinor !== attempt.amountMinor ||
        escrow.currency !== attempt.currency
      ) {
        throw new ApiError(409, "invalid-webhook", "An escrow identifier conflicts with payment data.");
      }
      transaction.set(eventRef, {
        type: event.type,
        status: "duplicate",
        receivedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    transaction.create(escrowRef, {
      buyerId: attempt.buyerId,
      sellerId: attempt.sellerId,
      participantIds: [attempt.buyerId, attempt.sellerId],
      listingId: attempt.listingId,
      amountMinor: attempt.amountMinor,
      currency: attempt.currency,
      paymentIntentId: intent.id,
      paymentStatus: "paid",
      state: "paid",
      releaseStatus: "held",
      sellerConfirmedShipped: false,
      buyerConfirmedReceived: false,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(attemptRef, {
      stripeStatus: "succeeded",
      escrowId: escrowRef.id,
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (listingSnapshot.exists && listingSnapshot.get("status") === "active") {
      transaction.update(listingRef, {
        status: "reserved",
        reservedBy: attempt.buyerId,
        reservedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.create(eventRef, {
      type: event.type,
      status: "processed",
      paymentIntentId: intent.id,
      escrowId: escrowRef.id,
      receivedAt: FieldValue.serverTimestamp(),
    });
  });
}

async function recordPaymentFailure(event: Stripe.Event, intent: Stripe.PaymentIntent): Promise<void> {
  const attemptId = intent.metadata.attemptId;
  if (!attemptId || !/^[a-f0-9]{64}$/.test(attemptId)) {
    return;
  }
  const eventRef = db.collection("stripeEvents").doc(event.id);
  const attemptRef = db.collection("paymentAttempts").doc(attemptId);
  await db.runTransaction(async (transaction) => {
    const eventSnapshot = await transaction.get(eventRef);
    if (eventSnapshot.exists) {
      return;
    }
    const attemptSnapshot = await transaction.get(attemptRef);
    const attempt = attemptSnapshot.exists ? parsePaymentAttempt(attemptSnapshot.data()) : undefined;
    const lockSnapshot = attempt
      ? await transaction.get(db.collection("listingPaymentLocks").doc(attempt.listingId))
      : undefined;
    if (attempt) {
      if (
        attempt.paymentIntentId === intent.id &&
        attempt.stripeStatus !== "succeeded" &&
        ["payment_intent.payment_failed", "payment_intent.canceled"].includes(event.type)
      ) {
        transaction.update(attemptRef, {
          stripeStatus: intent.status,
          updatedAt: FieldValue.serverTimestamp(),
        });
      }
      if (
        event.type === "payment_intent.canceled" &&
        attempt.paymentIntentId === intent.id &&
        lockSnapshot?.exists &&
        lockSnapshot.get("attemptId") === attemptId
      ) {
        transaction.delete(lockSnapshot.ref);
      }
    }
    transaction.create(eventRef, {
      type: event.type,
      status: "processed",
      paymentIntentId: intent.id,
      receivedAt: FieldValue.serverTimestamp(),
    });
  });
}

async function recordIgnoredEvent(event: Stripe.Event): Promise<void> {
  const eventRef = db.collection("stripeEvents").doc(event.id);
  await db.runTransaction(async (transaction) => {
    const existing = await transaction.get(eventRef);
    if (!existing.exists) {
      transaction.create(eventRef, {
        type: event.type,
        status: "ignored",
        receivedAt: FieldValue.serverTimestamp(),
      });
    }
  });
}

async function shipEscrow(uid: string, escrowId: string, body: unknown): Promise<RecordValue> {
  const shipping = validateShipping(body);
  const escrowRef = db.collection("escrows").doc(escrowId);
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(escrowRef);
    if (!snapshot.exists) {
      throw new ApiError(404, "not-found", "The escrow does not exist.");
    }
    const escrow = parseEscrow(snapshot.data());
    if (escrow.sellerId !== uid) {
      throw new ApiError(403, "permission-denied", "Only the seller can mark an escrow as shipped.");
    }
    if (escrow.state === "shipped" && escrow.sellerConfirmedShipped) {
      return summarizeEscrow(escrowId, escrow);
    }
    if (escrow.state !== "paid" || escrow.releaseStatus !== "held") {
      throw new ApiError(409, "invalid-state", "An escrow can only be shipped after payment succeeds.");
    }
    const updated: Escrow = {
      ...escrow,
      state: "shipped",
      sellerConfirmedShipped: true,
    };
    transaction.update(escrowRef, {
      state: updated.state,
      sellerConfirmedShipped: true,
      shipping: {
        carrier: shipping.carrier,
        trackingNumber: shipping.trackingNumber,
        shippedAt: FieldValue.serverTimestamp(),
      },
      updatedAt: FieldValue.serverTimestamp(),
    });
    return summarizeEscrow(escrowId, updated);
  });
}

type ReleasePreparation =
  | { kind: "not-ready"; escrow: Escrow }
  | { kind: "blocked"; escrow: Escrow }
  | { kind: "ready"; escrow: Escrow; stripeConnectedAccountId: string };

async function prepareRelease(escrowId: string): Promise<ReleasePreparation> {
  const escrowRef = db.collection("escrows").doc(escrowId);
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(escrowRef);
    if (!snapshot.exists) {
      throw new ApiError(404, "not-found", "The escrow does not exist.");
    }
    const escrow = parseEscrow(snapshot.data());
    if (escrow.state === "released") {
      return { kind: "not-ready", escrow };
    }
    if (
      !escrow.sellerConfirmedShipped ||
      !escrow.buyerConfirmedReceived ||
      !["inspection", "release_processing"].includes(escrow.state) ||
      escrow.paymentStatus !== "paid"
    ) {
      return { kind: "not-ready", escrow };
    }
    const payoutRef = db.collection("users").doc(escrow.sellerId).collection("private").doc("payout");
    const payout = await transaction.get(payoutRef);
    const stripeConnectedAccountId = payout.exists ? stringField(asRecord(payout.data()), "stripeConnectedAccountId") : undefined;
    if (!stripeConnectedAccountId || !/^acct_[A-Za-z0-9]+$/.test(stripeConnectedAccountId)) {
      if (escrow.state === "inspection" && escrow.releaseStatus !== "blocked") {
        transaction.update(escrowRef, {
          releaseStatus: "blocked",
          updatedAt: FieldValue.serverTimestamp(),
        });
      }
      return {
        kind: "blocked",
        escrow: {
          ...escrow,
          releaseStatus: escrow.state === "inspection" ? "blocked" : escrow.releaseStatus,
        },
      };
    }

    if (escrow.state === "inspection") {
      transaction.update(escrowRef, {
        state: "release_processing",
        releaseStatus: "release_pending",
        updatedAt: FieldValue.serverTimestamp(),
      });
      return {
        kind: "ready",
        escrow: { ...escrow, state: "release_processing", releaseStatus: "release_pending" },
        stripeConnectedAccountId,
      };
    }
    return { kind: "ready", escrow, stripeConnectedAccountId };
  });
}

async function finalizeRelease(
  escrowId: string,
  release: Extract<ReleasePreparation, { kind: "ready" }>,
): Promise<RecordValue> {
  let transfer: Stripe.Transfer;
  try {
    transfer = await stripeClient().transfers.create(
      {
        amount: release.escrow.amountMinor,
        currency: release.escrow.currency,
        destination: release.stripeConnectedAccountId,
        metadata: {
          escrowId,
          paymentIntentId: release.escrow.paymentIntentId,
        },
      },
      { idempotencyKey: `vaultables-release-${escrowId}` },
    );
  } catch (error) {
    logger.error("Stripe transfer release failed", {
      escrowId,
      errorType: error instanceof Error ? error.name : "unknown",
    });
    await db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(db.collection("escrows").doc(escrowId));
      if (snapshot.exists) {
        const current = parseEscrow(snapshot.data());
        if (current.state === "release_processing") {
          transaction.update(snapshot.ref, {
            state: "inspection",
            releaseStatus: "release_failed",
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      }
    });
    throw new ApiError(502, "payout-provider-error", "The seller payout could not be released. Funds remain held.");
  }

  return db.runTransaction(async (transaction) => {
    const escrowRef = db.collection("escrows").doc(escrowId);
    const snapshot = await transaction.get(escrowRef);
    if (!snapshot.exists) {
      throw new ApiError(500, "internal", "The escrow disappeared while processing its release.");
    }
    const current = parseEscrow(snapshot.data());
    if (current.state === "released") {
      if (current.stripeTransferId !== transfer.id) {
        throw new ApiError(500, "internal", "The immutable payout identifier conflicts with Stripe.");
      }
      return summarizeEscrow(escrowId, current);
    }
    if (
      current.state !== "release_processing" ||
      !current.sellerConfirmedShipped ||
      !current.buyerConfirmedReceived ||
      current.paymentStatus !== "paid"
    ) {
      throw new ApiError(409, "invalid-state", "The escrow is no longer eligible for release.");
    }
    const released: Escrow = {
      ...current,
      state: "released",
      releaseStatus: "released",
      stripeTransferId: transfer.id,
    };
    transaction.update(escrowRef, {
      state: released.state,
      releaseStatus: released.releaseStatus,
      stripeTransferId: transfer.id,
      releasedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return summarizeEscrow(escrowId, released);
  });
}

async function confirmReceipt(uid: string, escrowId: string): Promise<RecordValue> {
  const escrowRef = db.collection("escrows").doc(escrowId);
  const confirmation = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(escrowRef);
    if (!snapshot.exists) {
      throw new ApiError(404, "not-found", "The escrow does not exist.");
    }
    const escrow = parseEscrow(snapshot.data());
    if (escrow.buyerId !== uid) {
      throw new ApiError(403, "permission-denied", "Only the buyer can confirm receipt.");
    }
    if (escrow.state === "released") {
      return escrow;
    }
    if (escrow.state === "release_processing" && escrow.buyerConfirmedReceived) {
      return escrow;
    }
    if (!["shipped", "inspection"].includes(escrow.state)) {
      throw new ApiError(409, "invalid-state", "Receipt can only be confirmed after shipment.");
    }
    if (escrow.buyerConfirmedReceived) {
      return escrow;
    }
    const updated: Escrow = {
      ...escrow,
      state: "inspection",
      buyerConfirmedReceived: true,
    };
    transaction.update(escrowRef, {
      state: updated.state,
      buyerConfirmedReceived: true,
      buyerConfirmedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return updated;
  });

  if (confirmation.state === "released") {
    return summarizeEscrow(escrowId, confirmation);
  }
  const release = await prepareRelease(escrowId);
  if (release.kind === "ready") {
    return finalizeRelease(escrowId, release);
  }
  if (release.kind === "blocked") {
    return {
      ...summarizeEscrow(escrowId, release.escrow),
      payoutNotice: "Seller payout is not configured; funds remain held.",
    };
  }
  return summarizeEscrow(escrowId, release.escrow);
}

async function openDispute(uid: string, escrowId: string, body: unknown): Promise<RecordValue> {
  const dispute = validateDispute(body);
  const escrowRef = db.collection("escrows").doc(escrowId);
  const disputeRef = escrowRef.collection("disputes").doc();
  return db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(escrowRef);
    if (!snapshot.exists) {
      throw new ApiError(404, "not-found", "The escrow does not exist.");
    }
    const escrow = parseEscrow(snapshot.data());
    if (uid !== escrow.buyerId && uid !== escrow.sellerId) {
      throw new ApiError(403, "permission-denied", "Only an escrow participant can open a dispute.");
    }
    if (!["paid", "shipped", "inspection"].includes(escrow.state)) {
      throw new ApiError(409, "invalid-state", "This escrow cannot be disputed in its current state.");
    }
    transaction.create(disputeRef, {
      openedBy: uid,
      reason: dispute.reason,
      ...(dispute.details ? { details: dispute.details } : {}),
      status: "open",
      createdAt: FieldValue.serverTimestamp(),
    });
    transaction.update(escrowRef, {
      state: "disputed",
      releaseStatus: "held",
      disputedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return {
      ...summarizeEscrow(escrowId, { ...escrow, state: "disputed", releaseStatus: "held" }),
      disputeId: disputeRef.id,
    };
  });
}

type ProtectedHandler = (request: Request, response: Response, user: DecodedIdToken) => Promise<void>;

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "6mb", type: "application/json" }));

function protectedPost(path: string, handler: ProtectedHandler): void {
  app.post(path, async (request, response, next) => {
    try {
      const user = await requireUser(request);
      await handler(request, response, user);
    } catch (error) {
      next(error);
    }
  });
}

protectedPost("/api/v1/scanner/analyze", async (request, response) => {
  const scan = validateScanRequest(request.body);
  const analysis = await analyzeCard(scan);
  response.status(200).json(analysis);
});

protectedPost("/api/v1/escrow/create-intent", async (request, response, user) => {
  const result = await createPaymentIntent(
    user.uid,
    request.body,
    validateIdempotencyKey(request.header("Idempotency-Key")),
  );
  response.status(200).json(result);
});

protectedPost("/api/v1/escrow/:escrowId/ship", async (request, response, user) => {
  const result = await shipEscrow(user.uid, validateEscrowId(request.params.escrowId), request.body);
  response.status(200).json(result);
});

protectedPost("/api/v1/escrow/:escrowId/confirm-receipt", async (request, response, user) => {
  const result = await confirmReceipt(user.uid, validateEscrowId(request.params.escrowId));
  response.status(200).json(result);
});

protectedPost("/api/v1/escrow/:escrowId/disputes", async (request, response, user) => {
  const result = await openDispute(user.uid, validateEscrowId(request.params.escrowId), request.body);
  response.status(201).json(result);
});

app.use((_request, _response, next) => {
  next(new ApiError(404, "not-found", "Endpoint not found."));
});

app.use((error: unknown, request: Request, response: Response, _next: NextFunction) => {
  if (error instanceof ApiError) {
    response.status(error.status).json({ error: { code: error.code, message: error.message } });
    return;
  }
  if (error instanceof SyntaxError && "body" in error) {
    response.status(400).json({ error: { code: "invalid-json", message: "The request body is not valid JSON." } });
    return;
  }
  if (asRecord(error)?.type === "entity.too.large") {
    response.status(413).json({ error: { code: "payload-too-large", message: "The request body is too large." } });
    return;
  }
  logger.error("Unhandled authenticated API error", {
    path: request.path,
    method: request.method,
    errorType: error instanceof Error ? error.name : "unknown",
  });
  response.status(500).json({ error: { code: "internal", message: "An internal error occurred." } });
});

export const api = onRequest(
  {
    region,
    timeoutSeconds: 60,
    memory: "512MiB",
    invoker: "public",
    secrets: API_SECRETS,
  },
  app,
);

export const stripeWebhook = onRequest(
  {
    region,
    timeoutSeconds: 30,
    invoker: "public",
    secrets: [STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET],
  },
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).json({ error: { code: "method-not-allowed", message: "POST is required." } });
      return;
    }
    const signature = request.header("stripe-signature");
    const rawBody = (request as unknown as { rawBody?: Buffer }).rawBody;
    if (!signature || !rawBody) {
      response.status(400).json({ error: { code: "invalid-webhook", message: "Stripe signature is required." } });
      return;
    }

    let event: Stripe.Event;
    try {
      const webhookSecret = STRIPE_WEBHOOK_SECRET.value();
      if (!webhookSecret) {
        throw new ApiError(500, "failed-precondition", "The webhook verifier is not configured.");
      }
      event = stripeClient().webhooks.constructEvent(rawBody, signature, webhookSecret);
    } catch (error) {
      if (error instanceof ApiError) {
        logger.error("Stripe webhook configuration error", { code: error.code });
        response.status(error.status).json({ error: { code: error.code, message: error.message } });
      } else {
        logger.warn("Rejected Stripe webhook with invalid signature");
        response.status(400).json({ error: { code: "invalid-webhook", message: "Stripe signature verification failed." } });
      }
      return;
    }

    try {
      const intent = event.data.object as Stripe.PaymentIntent;
      if (event.type === "payment_intent.succeeded") {
        await recordPaymentSucceeded(event, intent);
      } else if (event.type === "payment_intent.payment_failed" || event.type === "payment_intent.canceled") {
        await recordPaymentFailure(event, intent);
      } else {
        await recordIgnoredEvent(event);
      }
      response.status(200).json({ received: true });
    } catch (error) {
      logger.error("Stripe webhook processing failed", {
        eventType: event.type,
        errorType: error instanceof Error ? error.name : "unknown",
      });
      response.status(500).json({ error: { code: "webhook-processing-failed", message: "Webhook processing failed." } });
    }
  },
);
