import * as crypto from "crypto";

import {
  randomBytes,
  generateEcKeys,
  deriveKeyAndNonce,
  parsePrivateJwk,
} from "autopush-manager/src/crypto";
import { UncompressedPublicKey } from "autopush-manager/src/crypto-types";
import { Logger } from "autopush-manager/src/index";

import { Storage } from "./storage";
import { fromBufferToUrlB64, fromUrlB64ToBuffer, fromUtf8ToBuffer } from "./string-manipulation";

/**
 * Sends a message to a subscription
 *
 * # Notes
 * All of the code in this command would be server-side in a real application.
 *
 * Also, you should probably use packages to do all of this rather than implement it yourself. It's implemented here just to show how it works in a more linear fashion.
 */
export class SendCommand {
  constructor(
    private readonly logger: Logger,
    private readonly storage: Storage,
  ) {}

  async send(
    message: string,
    subscription: { endpoint: string; p256dh: string; auth: string },
    vapidKeys: { public: string; private: string },
    subject: string,
    ttl: number,
  ): Promise<void> {
    this.logger.info("Encrypting payload...");
    const encryptedMessage = await aes128GcmEncrypt(
      message,
      subscription.p256dh,
      subscription.auth,
    );

    this.logger.info("Signing authentication header...");
    const vapidHeader = await this.generateVapidHeader(subscription.endpoint, vapidKeys, subject);

    const post = {
      method: "POST",
      headers: {
        "Content-Type": "application/octet-stream",
        "Content-Encoding": "aes128gcm",
        Authorization: vapidHeader,
        TTL: ttl.toString(),
      },
      body: encryptedMessage,
    };
    this.logger.info("Sending message...", post);
    const response = await fetch(subscription.endpoint, post);
    this.logger.info("Message sent", response.status);
  }

  async readSubscription(): Promise<{ endpoint: string; p256dh: string; auth: string }> {
    const channelIDs = JSON.parse(await this.storage.read("channelIDs"));
    if (channelIDs.length === 0) {
      throw new Error("No subscriptions found");
    }

    const channelID = channelIDs[0];
    const endpoint = JSON.parse(await this.storage.read<string>(`${channelID}:endpoint`));
    const jwk = JSON.parse(await this.storage.read<string>(`${channelID}:privateEncKey`));
    const auth = JSON.parse(await this.storage.read<string>(`${channelID}:auth`));

    const ecKeys = await parsePrivateJwk(jwk);
    if (!ecKeys || !ecKeys.uncompressedPublicKey) {
      throw new Error("No valid keys found");
    }

    return { endpoint, p256dh: fromBufferToUrlB64(ecKeys.uncompressedPublicKey), auth };
  }

  private async generateVapidHeader(
    audience: string,
    vapidKey: { public: string; private: string },
    subject: string,
  ): Promise<string> {
    const expiration = Math.floor(Date.now() / 1000) + 60; // 1 min

    const vapidPublicBuffer = fromUrlB64ToBuffer(vapidKey.public);
    if (vapidPublicBuffer.length !== 65) {
      throw new Error("Invalid VAPID public key");
    }
    // Building a JWK from the public and private parts of the vapid key so that we can import it using subtle crypto
    const vapidJwk = {
      key_ops: ["sign"],
      ext: true,
      kty: "EC",
      x: fromBufferToUrlB64(vapidPublicBuffer.slice(1, 33)),
      y: fromBufferToUrlB64(vapidPublicBuffer.slice(33)),
      crv: "P-256",
      d: vapidKey.private,
    };

    const ecdhSecretKey = await crypto.subtle.importKey(
      "jwk",
      vapidJwk,
      { name: "ECDSA", namedCurve: "P-256" },
      false,
      ["sign"],
    );

    const audUrl = new URL(audience);

    const tokenHeader = {
      typ: "JWT",
      alg: "ES256",
    };
    const tokenBody = {
      aud: `${audUrl.protocol}//${audUrl.host}`,
      exp: expiration,
      sub: subject,
    };

    const unsignedToken = Buffer.from(
      fromBufferToUrlB64(fromUtf8ToBuffer(JSON.stringify(tokenHeader))) +
        "." +
        fromBufferToUrlB64(fromUtf8ToBuffer(JSON.stringify(tokenBody))),
    );

    const signature = await crypto.subtle.sign(
      { name: "ECDSA", hash: { name: "SHA-256" } },
      ecdhSecretKey,
      unsignedToken,
    );

    return `vapid t=${unsignedToken}.${fromBufferToUrlB64(signature)}, k=${vapidKey.public}`;
  }
}

// This implementation is limited to a single record, and here we set size of that record (the last record can be shorter than the record size).
const recordSize = new Uint8Array([0, 0, 4, 0]);
// This is the length of an uncompressed public key. It's of the form 0x04 + x + y where x and y are 32 bytes each and are the x and y coordinates of the public key.
const keyLength = new Uint8Array([65]);
async function aes128GcmEncrypt(data: string, p256dh: string, auth: string) {
  const publicKey = fromUrlB64ToBuffer(p256dh) as UncompressedPublicKey;
  const authSecret = fromUrlB64ToBuffer(auth);
  // This padding indicates this is the last record (last non-zero byte == 2)
  const paddedData = Buffer.concat([fromUtf8ToBuffer(data), new Uint8Array([2, 0, 0, 0, 0])]);
  const salt = await randomBytes(16);
  const ecKeys = await generateEcKeys();
  const { contentEncryptionKey, nonce } = await deriveKeyAndNonce(
    {
      publicKey,
    },
    {
      publicKey: ecKeys.uncompressedPublicKey,
      privateKey: ecKeys.privateKey,
    },
    authSecret,
    salt,
  );

  const cryptoKey = crypto.createSecretKey(Buffer.from(contentEncryptionKey));
  const cipher = crypto.createCipheriv("aes-128-gcm", cryptoKey, Buffer.from(nonce));
  const encrypted = cipher.update(paddedData);
  cipher.final();
  const authTag = cipher.getAuthTag();
  const result = Buffer.concat([
    salt,
    recordSize,
    keyLength,
    new Uint8Array(ecKeys.uncompressedPublicKey),
    encrypted,
    authTag,
  ]);

  return result;
}
