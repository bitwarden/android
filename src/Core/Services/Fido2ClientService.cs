using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.Core.Utilities.Fido2.Extensions;

namespace Bit.Core.Services
{
    public class Fido2ClientService : IFido2ClientService
    {
        private readonly IStateService _stateService;
        private readonly IEnvironmentService _environmentService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IFido2AuthenticatorService _fido2AuthenticatorService;
        private readonly IFido2GetAssertionUserInterface _getAssertionUserInterface;
        private readonly IFido2MakeCredentialUserInterface _makeCredentialUserInterface;

        public Fido2ClientService(
            IStateService stateService,
            IEnvironmentService environmentService,
            ICryptoFunctionService cryptoFunctionService,
            IFido2AuthenticatorService fido2AuthenticatorService,
            IFido2GetAssertionUserInterface getAssertionUserInterface,
            IFido2MakeCredentialUserInterface makeCredentialUserInterface)
        {
            _stateService = stateService;
            _environmentService = environmentService;
            _cryptoFunctionService = cryptoFunctionService;
            _fido2AuthenticatorService = fido2AuthenticatorService;
            _getAssertionUserInterface = getAssertionUserInterface;
            _makeCredentialUserInterface = makeCredentialUserInterface;
        }

        public async Task<Fido2ClientCreateCredentialResult> CreateCredentialAsync(Fido2ClientCreateCredentialParams createCredentialParams, Fido2ExtraCreateCredentialParams extraParams)
        {
            var blockedUris = await _stateService.GetAutofillBlacklistedUrisAsync();
            var domain = CoreHelpers.GetHostname(createCredentialParams.Origin);
            if (blockedUris != null && blockedUris.Contains(domain))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.UriBlockedError,
                    "Origin is blocked by the user");
            }

            if (!await _stateService.IsAuthenticatedAsync())
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.InvalidStateError,
                    "No user is logged in");
            }

            if (createCredentialParams.Origin == _environmentService.GetWebVaultUrl())
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.NotAllowedError,
                    "Saving Bitwarden credentials in a Bitwarden vault is not allowed");
            }

            if (!createCredentialParams.SameOriginWithAncestors)
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.NotAllowedError,
                    "Credential creation is now allowed from embedded contexts with different origins");
            }

            if (createCredentialParams.User.Id.Length < 1 || createCredentialParams.User.Id.Length > 64)
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.TypeError,
                    "The length of user.id is not between 1 and 64 bytes (inclusive)");
            }

            var isAndroidOrigin = createCredentialParams.Origin.StartsWith("android:apk-key-hash");
            if (!isAndroidOrigin && !createCredentialParams.Origin.StartsWith("https://"))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.SecurityError,
                    "Origin is not a valid https origin");
            }

            if (!isAndroidOrigin && !Fido2DomainUtils.IsValidRpId(createCredentialParams.Rp.Id, createCredentialParams.Origin))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.SecurityError,
                    "RP ID cannot be used with this origin");
            }

            PublicKeyCredentialParameters[] credTypesAndPubKeyAlgs;
            if (createCredentialParams.PubKeyCredParams?.Length > 0)
            {
                // Filter out all unsupported algorithms
                credTypesAndPubKeyAlgs = createCredentialParams.PubKeyCredParams
                    .Where(kp => kp.Alg == (int)Fido2AlgorithmIdentifier.ES256 && kp.Type == Constants.DefaultFido2CredentialType)
                    .ToArray();
            }
            else
            {
                // Assign default algorithms
                credTypesAndPubKeyAlgs = new PublicKeyCredentialParameters[]
                {
                    new PublicKeyCredentialParameters { Alg = (int) Fido2AlgorithmIdentifier.ES256, Type = Constants.DefaultFido2CredentialType },
                    new PublicKeyCredentialParameters { Alg = (int) Fido2AlgorithmIdentifier.RS256, Type = Constants.DefaultFido2CredentialType }
                };
            }

            if (credTypesAndPubKeyAlgs.Length == 0)
            {
                throw new Fido2ClientException(Fido2ClientException.ErrorCode.NotSupportedError, "No supported algorithms found");
            }

            byte[] clientDataJSONBytes = null;
            var clientDataHash = extraParams.ClientDataHash;
            if (clientDataHash == null)
            {
                var clientDataJsonObject = new JsonObject
                {
                    { "type", "webauthn.create" },
                    { "challenge", CoreHelpers.Base64UrlEncode(createCredentialParams.Challenge) },
                    { "origin", createCredentialParams.Origin },
                    { "crossOrigin", !createCredentialParams.SameOriginWithAncestors }
                    // tokenBinding: {} // Not supported
                };
                if (!string.IsNullOrWhiteSpace(extraParams.AndroidPackageName))
                {
                    clientDataJsonObject.Add("androidPackageName", extraParams.AndroidPackageName);
                }
                
                clientDataJSONBytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(clientDataJsonObject));
                clientDataHash = await _cryptoFunctionService.HashAsync(clientDataJSONBytes, CryptoHashAlgorithm.Sha256);
            }
            var makeCredentialParams = MapToMakeCredentialParams(createCredentialParams, credTypesAndPubKeyAlgs, clientDataHash);

            try
            {
                var makeCredentialResult = await _fido2AuthenticatorService.MakeCredentialAsync(makeCredentialParams, _makeCredentialUserInterface);

                Fido2CredPropsResult credProps = null;
                if (createCredentialParams.Extensions?.CredProps == true)
                {
                    credProps = new Fido2CredPropsResult
                    {
                        Rk = makeCredentialParams.RequireResidentKey
                    };
                }

                return new Fido2ClientCreateCredentialResult
                {
                    CredentialId = makeCredentialResult.CredentialId,
                    AttestationObject = makeCredentialResult.AttestationObject,
                    AuthData = makeCredentialResult.AuthData,
                    ClientDataJSON = clientDataJSONBytes,
                    PublicKey = makeCredentialResult.PublicKey,
                    PublicKeyAlgorithm = makeCredentialResult.PublicKeyAlgorithm,
                    Transports = createCredentialParams.Rp.Id == "google.com" ? new string[] { "internal", "usb" } : new string[] { "internal" }, // workaround for a bug on Google's side
                    Extensions = new Fido2CreateCredentialExtensionsResult
                    {
                        CredProps = credProps
                    }
                };
            }
            catch (InvalidStateError)
            {
                throw new Fido2ClientException(Fido2ClientException.ErrorCode.InvalidStateError, "Unknown invalid state encountered");
            }
            catch (Exception)
            {
                throw new Fido2ClientException(Fido2ClientException.ErrorCode.UnknownError, $"An unknown error occurred");
            }
        }

        public async Task<Fido2ClientAssertCredentialResult> AssertCredentialAsync(Fido2ClientAssertCredentialParams assertCredentialParams, Fido2ExtraAssertCredentialParams extraParams)
        {
            var blockedUris = await _stateService.GetAutofillBlacklistedUrisAsync();
            var domain = CoreHelpers.GetHostname(assertCredentialParams.Origin);
            if (blockedUris != null && blockedUris.Contains(domain))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.UriBlockedError,
                    "Origin is blocked by the user");
            }

            if (!await _stateService.IsAuthenticatedAsync())
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.InvalidStateError,
                    "No user is logged in");
            }

            if (assertCredentialParams.Origin == _environmentService.GetWebVaultUrl())
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.NotAllowedError,
                    "Saving Bitwarden credentials in a Bitwarden vault is not allowed");
            }

            var isAndroidOrigin = assertCredentialParams.Origin.StartsWith("android:apk-key-hash");
            if (!isAndroidOrigin && !assertCredentialParams.Origin.StartsWith("https://"))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.SecurityError,
                    "Origin is not a valid https origin");
            }

            if (!isAndroidOrigin && !Fido2DomainUtils.IsValidRpId(assertCredentialParams.RpId, assertCredentialParams.Origin))
            {
                throw new Fido2ClientException(
                    Fido2ClientException.ErrorCode.SecurityError,
                    "RP ID cannot be used with this origin");
            }

            byte[] clientDataJSONBytes = null;
            var clientDataHash = extraParams.ClientDataHash;
            if (clientDataHash == null)
            {
                var clientDataJsonObject = new JsonObject
                {
                    { "type", "webauthn.get" },
                    { "challenge", CoreHelpers.Base64UrlEncode(assertCredentialParams.Challenge) },
                    { "origin", assertCredentialParams.Origin },
                    { "crossOrigin", !assertCredentialParams.SameOriginWithAncestors }
                    // tokenBinding: {} // Not supported
                };
                if (!string.IsNullOrWhiteSpace(extraParams.AndroidPackageName))
                {
                    clientDataJsonObject.Add("androidPackageName", extraParams.AndroidPackageName);
                }

                clientDataJSONBytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(clientDataJsonObject));
                clientDataHash = await _cryptoFunctionService.HashAsync(clientDataJSONBytes, CryptoHashAlgorithm.Sha256);
            }
            var getAssertionParams = MapToGetAssertionParams(assertCredentialParams, clientDataHash);

            try
            {
                var getAssertionResult = await _fido2AuthenticatorService.GetAssertionAsync(getAssertionParams, _getAssertionUserInterface);

                return new Fido2ClientAssertCredentialResult
                {
                    AuthenticatorData = getAssertionResult.AuthenticatorData,
                    ClientDataJSON = clientDataJSONBytes,
                    Id = CoreHelpers.Base64UrlEncode(getAssertionResult.SelectedCredential.Id),
                    RawId = getAssertionResult.SelectedCredential.Id,
                    Signature = getAssertionResult.Signature,
                    SelectedCredential = getAssertionResult.SelectedCredential,
                    ClientDataHash = clientDataHash
                };
            }
            catch (InvalidStateError)
            {
                throw new Fido2ClientException(Fido2ClientException.ErrorCode.InvalidStateError, "Unknown invalid state encountered");
            }
            catch (Exception)
            {
                throw new Fido2ClientException(Fido2ClientException.ErrorCode.UnknownError, $"An unknown error occurred");
            }
        }

        private Fido2AuthenticatorMakeCredentialParams MapToMakeCredentialParams(
            Fido2ClientCreateCredentialParams createCredentialParams,
            PublicKeyCredentialParameters[] credTypesAndPubKeyAlgs,
            byte[] clientDataHash)
        {
            var requireResidentKey = createCredentialParams.AuthenticatorSelection?.ResidentKey == "required" ||
                createCredentialParams.AuthenticatorSelection?.ResidentKey == "preferred" ||
                (createCredentialParams.AuthenticatorSelection?.ResidentKey == null &&
                createCredentialParams.AuthenticatorSelection?.RequireResidentKey == true);

            return new Fido2AuthenticatorMakeCredentialParams
            {
                RequireResidentKey = requireResidentKey,
                UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(createCredentialParams.AuthenticatorSelection?.UserVerification),
                ExcludeCredentialDescriptorList = createCredentialParams.ExcludeCredentials,
                CredTypesAndPubKeyAlgs = credTypesAndPubKeyAlgs,
                Hash = clientDataHash,
                RpEntity = createCredentialParams.Rp,
                UserEntity = createCredentialParams.User,
                Extensions = createCredentialParams.Extensions
            };
        }

        private Fido2AuthenticatorGetAssertionParams MapToGetAssertionParams(
            Fido2ClientAssertCredentialParams assertCredentialParams,
            byte[] cliendDataHash)
        {
            return new Fido2AuthenticatorGetAssertionParams
            {
                RpId = assertCredentialParams.RpId,
                Challenge = assertCredentialParams.Challenge,
                AllowCredentialDescriptorList = assertCredentialParams.AllowCredentials,
                UserVerificationPreference = Fido2UserVerificationPreferenceExtensions.ToFido2UserVerificationPreference(assertCredentialParams?.UserVerification),
                Hash = cliendDataHash
            };
        }
    }
}
