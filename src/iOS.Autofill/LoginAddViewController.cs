using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Utilities.Fido2;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Autofill.Utilities;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginAddViewController : Core.Controllers.LoginAddViewController
    {
        LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();
        LazyResolve<IUserVerificationMediatorService> _userVerificationMediatorService = new LazyResolve<IUserVerificationMediatorService>();

        public LoginAddViewController(IntPtr handle)
            : base(handle)
        {
            DismissModalAction = Cancel;
        }

        public LoginListViewController LoginListController { get; set; }
        public LoginSearchViewController LoginSearchController { get; set; }

        public override UINavigationItem BaseNavItem => NavItem;
        public override UIBarButtonItem BaseCancelButton => CancelBarButton;
        public override UIBarButtonItem BaseSaveButton => SaveBarButton;

        private new Context Context => (Context)base.Context;

        private bool? _isUserVerified;

        public override Action<string> Success => cipherId =>
        {
            if (IsCreatingPasskey)
            {
                Context.PickCredentialForFido2CreationTcs.TrySetResult((cipherId, _isUserVerified));
                return;
            }

            LoginListController?.DismissModal();
            LoginSearchController?.DismissModal();
        };

        public override void ViewDidLoad()
        {
            IsCreatingPasskey = Context.IsCreatingPasskey;
            if (IsCreatingPasskey)
            {
                NameCell.TextField.Text = Context.PasskeyCreationParams?.CredentialName;
                UsernameCell.TextField.Text = Context.PasskeyCreationParams?.UserName;
            }

            base.ViewDidLoad();
        }

        partial void CancelBarButton_Activated(UIBarButtonItem sender)
        {
            Cancel();
        }
        
        private void Cancel()
        {
            DismissViewController(true, null);
        }

        protected override async Task EncryptAndSaveAsync(CipherView cipher)
        {
            if (!IsCreatingPasskey)
            {
                await base.EncryptAndSaveAsync(cipher);
                return;
            }

            if (!UIDevice.CurrentDevice.CheckSystemVersion(17, 0))
            {
                Context?.PickCredentialForFido2CreationTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login on iOS less than 17."));
                return;
            }

            if (Context?.PasskeyCreationParams?.UserVerificationPreference != Fido2UserVerificationPreference.Discouraged)
            {
                var verification = await VerifyUserAsync();
                if (verification.IsCancelled)
                {
                    return;
                }
                _isUserVerified = verification.Result;
            }

            var loadingAlert = Dialogs.CreateLoadingAlert(AppResources.Saving);
            try
            {
                PresentViewController(loadingAlert, true, null);

                var encryptedCipher = await _cipherService.Value.EncryptAsync(cipher);
                await _cipherService.Value.SaveWithServerAsync(encryptedCipher);

                await loadingAlert.DismissViewControllerAsync(true);

                Success(encryptedCipher.Id);
            }
            catch
            {
                await loadingAlert.DismissViewControllerAsync(false);
                throw;
            }
        }

        private async Task<CancellableResult<bool>> VerifyUserAsync()
        {
            try
            {
                if (Context?.PasskeyCreationParams is null)
                {
                    return new CancellableResult<bool>(false);
                }

                return await _userVerificationMediatorService.Value.VerifyUserForFido2Async(
                    new Fido2UserVerificationOptions(
                        false,
                        Context.PasskeyCreationParams.Value.UserVerificationPreference,
                        Context.VaultUnlockedDuringThisSession,
                        Context.PasskeyCredentialIdentity?.RelyingPartyIdentifier
                    )
                );
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new CancellableResult<bool>(false);
            }
        }

        async partial void SaveBarButton_Activated(UIBarButtonItem sender)
        {
            await SaveAsync();
        }

        public override void PrepareForSegue(UIStoryboardSegue segue, NSObject sender)
        {
            if (segue.DestinationViewController is UINavigationController navController)
            {
                if (navController.TopViewController is PasswordGeneratorViewController passwordGeneratorController)
                {
                    passwordGeneratorController.PasswordOptions = Context.PasswordOptions;
                    passwordGeneratorController.Parent = this;
                    segue.DestinationViewController.PresentationController.Delegate =
                        new CustomPresentationControllerDelegate(passwordGeneratorController.DismissModalAction);
                }
            }
        }
    }
}
