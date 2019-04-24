using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Bit.App.Pages
{
    public partial class ViewPage : ContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private ViewPageViewModel _vm;

        public ViewPage(string cipherId)
        {
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _vm = BindingContext as ViewPageViewModel;
            _vm.Page = this;
            _vm.CipherId = cipherId;
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(ViewPage), async (message) =>
            {
                if(message.Command == "syncCompleted")
                {
                    var data = message.Data as Dictionary<string, object>;
                    if(data.ContainsKey("successfully"))
                    {
                        var success = data["successfully"] as bool?;
                        if(success.HasValue && success.Value)
                        {
                            await _vm.LoadAsync();
                        }
                    }
                }
            });
            await _vm.LoadAsync();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(ViewPage));
        }
    }
}
