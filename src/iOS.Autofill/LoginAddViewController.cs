using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.iOS.Autofill.Models;
using Bit.iOS.Core.Utilities;
using Bit.iOS.Core.Views;
using Foundation;
using UIKit;

namespace Bit.iOS.Autofill
{
    public partial class LoginAddViewController : Core.Controllers.LoginAddViewController
    {
        LazyResolve<ICipherService> _cipherService = new LazyResolve<ICipherService>();

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

        public override Action<string> Success => cipherId =>
        {
            if (IsCreatingPasskey)
            {
                Context.ConfirmNewCredentialTcs.TrySetResult((cipherId, true));
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
                Context?.ConfirmNewCredentialTcs?.TrySetException(new InvalidOperationException("Trying to save passkey as new login on iOS less than 17."));
                return;
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
