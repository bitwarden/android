using System;
using System.Collections.Generic;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;
using Xamarin.Forms.PlatformConfiguration;
using Xamarin.Forms.PlatformConfiguration.iOSSpecific;
using Entry = Xamarin.Forms.Entry;

namespace Bit.App.Pages
{
    public partial class SendAddEditPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;

        private SendAddEditPageViewModel _vm;

        public SendAddEditPage(
            string sendId = null,
            SendType? type = null)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            InitializeComponent();
            _vm = BindingContext as SendAddEditPageViewModel;
            _vm.Page = this;
            _vm.SendId = sendId;
            _vm.Type = type;
            _vm.Init();
            SetActivityIndicator();
            if (Device.RuntimePlatform == Device.Android)
            {
                if (_vm.EditMode)
                {
                    ToolbarItems.Add(_removePassword);
                    ToolbarItems.Add(_copyLink);
                    ToolbarItems.Add(_shareLink);
                    ToolbarItems.Add(_deleteItem);
                }
                _vm.SegmentedButtonHeight = 36;
                _vm.SegmentedButtonFontSize = 13;
                _vm.SegmentedButtonMargins = new Thickness(0, 10, 0, 0);
                _vm.EditorMargins = new Thickness(0, 5, 0, 0);
            }
            else if (Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(_closeItem);
                if (_vm.EditMode)
                {
                    ToolbarItems.Add(_moreItem);
                }
                _vm.SegmentedButtonHeight = 30;
                _vm.SegmentedButtonFontSize = 15;
                _vm.SegmentedButtonMargins = new Thickness(0, 5, 0, 0);
                _vm.ShowEditorSeparators = true;
                _vm.EditorMargins = new Thickness(0, 10, 0, 5);
                _deletionDateTypePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
                _expirationDateTypePicker.On<iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            }

            _deletionDateTypePicker.ItemDisplayBinding = new Binding("Key");
            _expirationDateTypePicker.ItemDisplayBinding = new Binding("Key");

            if (_vm.IsText)
            {
                _nameEntry.ReturnType = ReturnType.Next;
                _nameEntry.ReturnCommand = new Command(() => _textEditor.Focus());
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            _broadcasterService.Subscribe(nameof(SendAddEditPage), message =>
            {
                if (message.Command == "selectFileResult")
                {
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        var data = message.Data as Tuple<byte[], string>;
                        _vm.FileData = data.Item1;
                        _vm.FileName = data.Item2;
                    });
                }
            });
            await LoadOnAppearedAsync(_scrollView, true, async () =>
            {
                var success = await _vm.LoadAsync();
                if (!success)
                {
                    await Navigation.PopModalAsync();
                    return;
                }
                if (!_vm.EditMode && string.IsNullOrWhiteSpace(_vm.Send?.Name))
                {
                    RequestFocus(_nameEntry);
                }
            });
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            if (Device.RuntimePlatform != Device.iOS)
            {
                _broadcasterService.Unsubscribe(nameof(SendAddEditPage));
            }
        }

        private void TextType_Clicked(object sender, EventArgs eventArgs)
        {
            _vm.TypeChanged(SendType.Text);
            _nameEntry.ReturnType = ReturnType.Next;
            _nameEntry.ReturnCommand = new Command(() => _textEditor.Focus());
            RequestFocus(_nameEntry);
        }
        
        private void FileType_Clicked(object sender, EventArgs eventArgs)
        {
            _vm.TypeChanged(SendType.File);
            _nameEntry.ReturnType = ReturnType.Done;
            _nameEntry.ReturnCommand = null;
            RequestFocus(_nameEntry);
        }

        private void OnMaxAccessCountTextChanged(object sender, TextChangedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(e.NewTextValue))
            {
                _vm.MaxAccessCount = null;
                _maxAccessCountStepper.Value = 0;
                return;
            }
            // accept only digits
            if (!int.TryParse(e.NewTextValue, out int _))
            {
                ((Entry)sender).Text = e.OldTextValue;
            }
        }

        private async void ChooseFile_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ChooseFileAsync();
            }
        }

        private void ClearExpirationDate_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                _vm.ClearExpirationDate();
            }
        }

        private async void Save_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.SubmitAsync();
            }
        }

        private async void RemovePassword_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.RemovePasswordAsync();
            }
        }

        private async void CopyLink_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.CopyLinkAsync();
            }
        }

        private async void ShareLink_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await _vm.ShareLinkAsync();
            }
        }

        private async void Delete_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                if (await _vm.DeleteAsync())
                {
                    await Navigation.PopModalAsync();
                }
            }
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            if (!DoOnce())
            {
                return;
            }
            var options = new List<string>();
            if (_vm.Send.HasPassword)
            {
                options.Add(AppResources.RemovePassword);
            }
            if (_vm.EditMode)
            {
                options.Add(AppResources.CopyLink);
                options.Add(AppResources.ShareLink);
            }

            var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                _vm.EditMode ? AppResources.Delete : null, options.ToArray());
            if (selection == AppResources.RemovePassword)
            {
                await _vm.RemovePasswordAsync();
            }
            else if (selection == AppResources.CopyLink)
            {
                await _vm.CopyLinkAsync();
            }
            else if (selection == AppResources.ShareLink)
            {
                await _vm.ShareLinkAsync();
            }
            else if (selection == AppResources.Delete)
            {
                if (await _vm.DeleteAsync())
                {
                    await Navigation.PopModalAsync();
                }
            }
        }

        private async void Close_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                await Navigation.PopModalAsync();
            }
        }
    }
}
