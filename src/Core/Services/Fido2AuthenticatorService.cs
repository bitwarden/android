using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Core.Utilities.Fido2;

namespace Bit.Core.Services
{
    public class Fido2AuthenticatorService : IFido2AuthenticatorService
    {
        private INativeLogService _logService;
        private ICipherService _cipherService;
        private ISyncService _syncService;
        private IFido2UserInterface _userInterface;
        
        public Fido2AuthenticatorService(INativeLogService logService, ICipherService cipherService, ISyncService syncService, IFido2UserInterface userInterface)
        {
            _logService = logService;
            _cipherService = cipherService;
            _syncService = syncService;
            _userInterface = userInterface;
        }
        
        public async Task<Fido2AuthenticatorGetAssertionResult> GetAssertionAsync(Fido2AuthenticatorGetAssertionParams assertionParams)
        {
            // throw new NotAllowedError();
            List<CipherView> cipherOptions;

            // await userInterfaceSession.ensureUnlockedVault();
            await _syncService.FullSyncAsync(false);

            if (assertionParams.AllowCredentialDescriptorList?.Length > 0) {
                cipherOptions = await FindCredentialsById(
                    assertionParams.AllowCredentialDescriptorList,
                    assertionParams.RpId
                );
            } else {
                cipherOptions = new List<CipherView>();
                // cipherOptions = await this.findCredentialsByRp(params.rpId);
            }

            if (cipherOptions.Count == 0) {
                _logService.Info(
                    "[Fido2Authenticator] Aborting because no matching credentials were found in the vault."
                );

                throw new NotAllowedError();
            }

            var response = await _userInterface.PickCredentialAsync(new Fido2PickCredentialParams {
                CipherIds = cipherOptions.Select((cipher) => cipher.Id).ToArray(),
                UserVerification = assertionParams.RequireUserVerification
            });
            
            // TODO: IMPLEMENT this
            return new Fido2AuthenticatorGetAssertionResult
            {
                AuthenticatorData = new byte[32],
                Signature = new byte[8]
            };
        }

    private async Task<List<CipherView>> FindCredentialsById(PublicKeyCredentialDescriptor[] credentials, string rpId)
    {
        var ids = new List<string>();

        foreach (var credential in credentials)
        {
            try
            {
                ids.Add(GuidToStandardFormat(credential.Id));
            }
            catch {}
        }

        if (ids.Count == 0)
        {
            return new List<CipherView>();
        }

        var ciphers = await _cipherService.GetAllDecryptedAsync();
        return ciphers.FindAll((cipher) =>
            !cipher.IsDeleted &&
            cipher.Type == CipherType.Login &&
            cipher.Login.HasFido2Credentials &&
            cipher.Login.Fido2Credentials[0].RpId == rpId &&
            ids.Contains(cipher.Login.Fido2Credentials[0].CredentialId)
        );
    }

    private string GuidToStandardFormat(byte[] bytes)
    {
        return new Guid(bytes).ToString();
    }
    }
}
