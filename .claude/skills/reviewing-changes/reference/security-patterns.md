# Security Patterns Quick Reference

Quick reference for Bitwarden Android security patterns during code reviews. For comprehensive details, read `docs/ARCHITECTURE.md#security`.

## Encryption and Key Storage

**✅ GOOD - Android Keystore**:
```kotlin
// Sensitive data encrypted with Keystore
class SecureStorage @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    suspend fun storePin(pin: String): Result<Unit> = runCatching {
        val encrypted = keystoreManager.encrypt(pin.toByteArray())
        securePreferences.putBytes(KEY_PIN, encrypted)
    }
}

// Or use EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**❌ BAD - Plaintext or weak encryption**:
```kotlin
// ❌ CRITICAL - Plaintext storage
sharedPreferences.edit {
    putString("pin", userPin)  // Never store sensitive data in plaintext
}

// ❌ CRITICAL - Weak encryption
val cipher = Cipher.getInstance("DES")  // Use AES-256-GCM

// ❌ CRITICAL - Hardcoded keys
val key = "my_secret_key_123"  // Use Android Keystore
```

**Key Rules**:
- Use Android Keystore for encryption keys
- Use EncryptedSharedPreferences for simple key-value storage
- Use AES-256-GCM for encryption
- Never store sensitive data in plaintext
- Never hardcode encryption keys

Reference: `docs/ARCHITECTURE.md#security`

---

## Logging Sensitive Data

**✅ GOOD - No sensitive data**:
```kotlin
Log.d(TAG, "Authentication attempt for user")
Log.d(TAG, "Vault sync completed with ${items.size} items")
```

**❌ BAD - Logs sensitive data**:
```kotlin
// ❌ CRITICAL
Log.d(TAG, "Password: $password")
Log.d(TAG, "Auth token: $token")
Log.d(TAG, "PIN: $pin")
Log.d(TAG, "Encryption key: ${key.encoded}")
```

**Key Rules**:
- Never log passwords, PINs, tokens, keys
- Never log encryption keys or sensitive data
- Be careful with error messages (don't include sensitive context)

---

## Quick Checklist

### Security
- [ ] Sensitive data encrypted with Keystore?
- [ ] No plaintext passwords/keys?
- [ ] No sensitive data in logs?
- [ ] Using AES-256-GCM for encryption?
- [ ] No hardcoded encryption keys?

---

For comprehensive security details, always refer to:
- `docs/ARCHITECTURE.md#security` - Complete security architecture and zero-knowledge principles
