import { logger } from "firebase-functions";
import type { NextFunction, Request, Response } from "express";
import { getAppCheck } from "firebase-admin/app-check";

import { ApiError } from "./validation.js";

export type AppCheckGate = "allow" | "reject-missing" | "reject-invalid";

export function decideAppCheck(input: {
  enforce: boolean;
  hasToken: boolean;
  tokenValid: boolean;
}): AppCheckGate {
  if (!input.hasToken) {
    return input.enforce ? "reject-missing" : "allow";
  }
  if (!input.tokenValid) {
    return input.enforce ? "reject-invalid" : "allow";
  }
  return "allow";
}

export function appCheckEnforced(): boolean {
  return (process.env.APP_CHECK_ENFORCE || "0").trim() === "1";
}

export async function verifyAppCheckToken(token: string): Promise<boolean> {
  try {
    await getAppCheck().verifyToken(token);
    return true;
  } catch {
    return false;
  }
}

export function appCheckMiddleware(request: Request, response: Response, next: NextFunction): void {
  const enforce = appCheckEnforced();
  const token = request.header("X-Firebase-AppCheck")?.trim() ?? "";
  const hasToken = token.length > 0;

  const finish = (tokenValid: boolean) => {
    const decision = decideAppCheck({ enforce, hasToken, tokenValid });
    logger.info("App Check gate", {
      path: request.path,
      enforce,
      hasToken,
      tokenValid: hasToken ? tokenValid : false,
      decision,
    });
    if (decision === "reject-missing") {
      next(new ApiError(401, "app-check-required", "A valid App Check token is required."));
      return;
    }
    if (decision === "reject-invalid") {
      next(new ApiError(401, "app-check-invalid", "App Check token verification failed."));
      return;
    }
    next();
  };

  if (!hasToken) {
    finish(false);
    return;
  }

  verifyAppCheckToken(token)
    .then(finish)
    .catch((error: unknown) => {
      logger.warn("App Check verify threw", {
        errorType: error instanceof Error ? error.name : "unknown",
      });
      finish(false);
    });
}
