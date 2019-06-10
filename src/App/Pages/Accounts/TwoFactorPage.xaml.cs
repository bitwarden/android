using Bit.App.Controls;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class TwoFactorPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;

        private TwoFactorPageViewModel _vm;
        private bool _inited;

        public TwoFactorPage()
        {
            InitializeComponent();
            SetActivityIndicator();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _vm = BindingContext as TwoFactorPageViewModel;
            _vm.Page = this;
            DuoWebView = _duoWebView;
        }

        public HybridWebView DuoWebView { get; set; }

        public void AddContinueButton()
        {
            if(ToolbarItems.Count == 0)
            {
                ToolbarItems.Add(_continueItem);
            }
        }

        public void RemoveContinueButton()
        {
            if(ToolbarItems.Count > 0)
            {
                ToolbarItems.Remove(_continueItem);
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(TwoFactorPage), (message) =>
            {
                if(message.Command == "gotYubiKeyOTP")
                {
                    if(_vm.YubikeyMethod)
                    {
                        Device.BeginInvokeOnMainThread(async () =>
                        {
                            _vm.Token = (string)message.Data;
                            await _vm.SubmitAsync();
                        });
                    }
                }
                else if(message.Command == "resumeYubiKey")
                {
                    if(_vm.YubikeyMethod)
                    {
                        _messagingService.Send("listenYubiKeyOTP", true);
                    }
                }
            });

            if(!_inited)
            {
                _inited = true;
                await LoadOnAppearedAsync(_scrollView, true, () =>
                {
                    _vm.Init();
                    if(_vm.TotpMethod)
                    {
                        RequestFocus(_totpEntry);
                    }
                    return Task.FromResult(0);
                });
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if(!_vm.YubikeyMethod)
            {
                _messagingService.Send("listenYubiKeyOTP", false);
            }
            _broadcasterService.Unsubscribe(nameof(TwoFactorPage));
        }

        private async void Continue_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void Methods_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.AnotherMethodAsync();
            }
        }

        private async void ResendEmail_Clicked(object sender, EventArgs e)
        {
            if(DoOnce())
            {
                await _vm.SendEmailAsync(true, true);
            }
        }
    }
}
