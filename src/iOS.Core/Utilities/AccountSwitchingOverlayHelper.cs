using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using CoreGraphics;
using Microsoft.Maui.Platform;
using SkiaSharp.Views.iOS;
using UIKit;

namespace Bit.iOS.Core.Utilities
{
    public class AccountSwitchingOverlayHelper
    {
        const string DEFAULT_SYSTEM_AVATAR_IMAGE = "person.2";
        
        readonly IStateService _stateService;
        readonly IMessagingService _messagingService;
        readonly ILogger _logger;

        public AccountSwitchingOverlayHelper()
        {
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _logger = ServiceContainer.Resolve<ILogger>("logger");
        }

        public async Task<UIImage?> CreateAvatarImageAsync()
        {
            try
            {
                if (_stateService is null)
                {
                    throw new NullReferenceException(nameof(_stateService));
                }

                var avatarInfo = await _stateService.GetActiveUserCustomDataAsync<AvatarInfo?>(a => a?.Profile is null
                    ? null
                    : new AvatarInfo(a.Profile.UserId, a.Profile.Name, a.Profile.Email, a.Profile.AvatarColor));

                if (!avatarInfo.HasValue)
                {
                    return UIImage.GetSystemImage(DEFAULT_SYSTEM_AVATAR_IMAGE);
                }

                using (var avatarUIImage = SKAvatarImageHelper.Draw(avatarInfo.Value))
                {
                    return avatarUIImage?.ToUIImage()?.ImageWithRenderingMode(UIImageRenderingMode.AlwaysOriginal)
                        ?? UIImage.GetSystemImage(DEFAULT_SYSTEM_AVATAR_IMAGE);
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                return UIImage.GetSystemImage(DEFAULT_SYSTEM_AVATAR_IMAGE);
            }
        }

        public AccountSwitchingOverlayView CreateAccountSwitchingOverlayView(UIView containerView)
        {
            var overlay = new AccountSwitchingOverlayView()
            {
                LongPressAccountEnabled = false,
                AfterHide = () =>
                {
                    if (containerView != null)
                    {
                        containerView.Hidden = true;
                    }
                }
            };

            var vm = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                FromIOSExtension = true
            };
            overlay.BindingContext = vm;
            overlay.IsVisible = false;

            if (MauiContextSingleton.Instance.MauiContext is null)
            {
                throw new ArgumentNullException("Maui context should be set to create the account switching overlay view");
            }

            var view = overlay.ToPlatform(MauiContextSingleton.Instance.MauiContext);
            view.TranslatesAutoresizingMaskIntoConstraints = false;

            containerView.AddSubview(view);
            containerView.AddConstraints(new NSLayoutConstraint[]
            {
                    NSLayoutConstraint.Create(containerView, NSLayoutAttribute.Trailing, NSLayoutRelation.Equal, view, NSLayoutAttribute.Trailing, 1f, 0f),
                    NSLayoutConstraint.Create(containerView, NSLayoutAttribute.Leading, NSLayoutRelation.Equal, view, NSLayoutAttribute.Leading, 1f, 0f),
                    NSLayoutConstraint.Create(containerView, NSLayoutAttribute.Top, NSLayoutRelation.Equal, view, NSLayoutAttribute.Top, 1f, 0f),
                    NSLayoutConstraint.Create(containerView, NSLayoutAttribute.Bottom, NSLayoutRelation.Equal, view, NSLayoutAttribute.Bottom, 1f, 0f)
            });
            containerView.Hidden = true;

            return overlay;
        }

        public void OnToolbarItemActivated(AccountSwitchingOverlayView accountSwitchingOverlayView, UIView containerView)
        {
            var overlayVisible = accountSwitchingOverlayView.IsVisible;
            if (!overlayVisible)
            {
                // So that the animation doesn't break we only care about showing it
                // and the hiding if done through AccountSwitchingOverlayView -> AfterHide
                containerView.Hidden = false;
            }
            accountSwitchingOverlayView.ToggleVisibililtyCommand.Execute(null);
            containerView.UserInteractionEnabled = !overlayVisible;
            containerView.Subviews[0].UserInteractionEnabled = !overlayVisible;
        }

        public async Task<UIControl> CreateAccountSwitchToolbarButtonItemCustomViewAsync()
        {
            const float size = 40f;
            var image = await CreateAvatarImageAsync();
            var accountSwitchButton = new UIControl(new CGRect(0, 0, size, size));
            if (image != null)
            {
                var accountSwitchAvatarImageView = new UIImageView(new CGRect(0, 0, size, size))
                {
                    Image = image
                };
                accountSwitchButton.AddSubview(accountSwitchAvatarImageView);
            }

            return accountSwitchButton;
        }

        public void DisposeAccountSwitchToolbarButtonItemImage(UIControl accountSwitchButton)
        {
            if (accountSwitchButton?.Subviews?.FirstOrDefault() is UIImageView accountSwitchImageView && accountSwitchImageView.Image != null)
            {
                var img = accountSwitchImageView.Image;
                accountSwitchImageView.Image = null;
                img.Dispose();
            }
        }
    }
}
