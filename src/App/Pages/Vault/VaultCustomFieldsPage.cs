using System;
using System.Linq;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Utilities;
using Plugin.Connectivity.Abstractions;
using Bit.App.Models;
using Bit.App.Enums;
using System.Collections.Generic;

namespace Bit.App.Pages
{
    public class VaultCustomFieldsPage : ExtendedContentPage
    {
        private readonly ICipherService _cipherService;
        private readonly IUserDialogs _userDialogs;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IConnectivity _connectivity;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly string _cipherId;
        private Cipher _cipher;
        private DateTime? _lastAction;

        public VaultCustomFieldsPage(string cipherId)
            : base(true)
        {
            _cipherId = cipherId;
            _cipherService = Resolver.Resolve<ICipherService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ToolbarItem SaveToolbarItem { get; set; }
        public Label NoDataLabel { get; set; }
        public TableSection FieldsSection { get; set; }
        public ExtendedTableView Table { get; set; }

        private void Init()
        {
            FieldsSection = new TableSection(Helpers.GetEmptyTableSectionTitle());

            Table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    FieldsSection
                }
            };

            NoDataLabel = new Label
            {
                Text = AppResources.NoCustomFields,
                HorizontalTextAlignment = TextAlignment.Center,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                Margin = new Thickness(10, 40, 10, 0)
            };

            SaveToolbarItem = new ToolbarItem(AppResources.Save, Helpers.ToolbarImage("envelope.png"), async () =>
            {
                if(_lastAction.LastActionWasRecent() || _cipher == null)
                {
                    return;
                }
                _lastAction = DateTime.UtcNow;

                if(!_connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(FieldsSection.Count > 0)
                {
                    var fields = new List<Field>();
                    foreach(var cell in FieldsSection)
                    {
                        if(cell is FormEntryCell entryCell)
                        {
                            fields.Add(new Field
                            {
                                Name = string.IsNullOrWhiteSpace(entryCell.Label.Text) ? null :
                                    entryCell.Label.Text.Encrypt(_cipher.OrganizationId),
                                Value = string.IsNullOrWhiteSpace(entryCell.Entry.Text) ? null :
                                    entryCell.Entry.Text.Encrypt(_cipher.OrganizationId),
                                Type = entryCell.Entry.IsPassword ? FieldType.Hidden : FieldType.Text
                            });
                        }
                        else if(cell is ExtendedSwitchCell switchCell)
                        {
                            var value = switchCell.On ? "true" : "false";
                            fields.Add(new Field
                            {
                                Name = string.IsNullOrWhiteSpace(switchCell.Text) ? null :
                                    switchCell.Text.Encrypt(_cipher.OrganizationId),
                                Value = value.Encrypt(_cipher.OrganizationId),
                                Type = FieldType.Boolean
                            });
                        }
                    }
                    _cipher.Fields = fields;
                }
                else
                {
                    _cipher.Fields = null;
                }

                _userDialogs.ShowLoading(AppResources.Saving, MaskType.Black);
                var saveTask = await _cipherService.SaveAsync(_cipher);
                _userDialogs.HideLoading();

                if(saveTask.Succeeded)
                {
                    _deviceActionService.Toast(AppResources.CustomFieldsUpdated);
                    _googleAnalyticsService.TrackAppEvent("UpdatedCustomFields");
                    await Navigation.PopForDeviceAsync();
                }
                else if(saveTask.Errors.Count() > 0)
                {
                    await DisplayAlert(AppResources.AnErrorHasOccurred, saveTask.Errors.First().Message, AppResources.Ok);
                }
                else
                {
                    await DisplayAlert(null, AppResources.AnErrorHasOccurred, AppResources.Ok);
                }
            }, ToolbarItemOrder.Default, 0);

            Title = AppResources.CustomFields;
            Content = Table;

            if(Device.RuntimePlatform == Device.iOS)
            {
                Table.RowHeight = -1;
                Table.EstimatedRowHeight = 44;
            }
        }

        protected async override void OnAppearing()
        {
            base.OnAppearing();

            _cipher = await _cipherService.GetByIdAsync(_cipherId);
            if(_cipher == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            if(_cipher.Fields != null && _cipher.Fields.Any())
            {
                Content = Table;
                ToolbarItems.Add(SaveToolbarItem);
                if(Device.RuntimePlatform == Device.iOS)
                {
                    ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
                }

                foreach(var field in _cipher.Fields)
                {
                    var label = field.Name?.Decrypt(_cipher.OrganizationId) ?? string.Empty;
                    var value = field.Value?.Decrypt(_cipher.OrganizationId);
                    switch(field.Type)
                    {
                        case FieldType.Text:
                        case FieldType.Hidden:
                            var hidden = field.Type == FieldType.Hidden;

                            var textFieldCell = new FormEntryCell(label, isPassword: hidden, useButton: hidden);
                            textFieldCell.Entry.Text = value;
                            textFieldCell.Entry.DisableAutocapitalize = true;
                            textFieldCell.Entry.Autocorrect = false;

                            if(hidden)
                            {
                                textFieldCell.Entry.FontFamily = Helpers.OnPlatform(
                                    iOS: "Menlo-Regular", Android: "monospace", Windows: "Courier");
                                textFieldCell.Button.Image = "eye.png";
                                textFieldCell.Button.Command = new Command(() =>
                                {
                                    textFieldCell.Entry.InvokeToggleIsPassword();
                                    textFieldCell.Button.Image = 
                                        "eye" + (!textFieldCell.Entry.IsPasswordFromToggled ? "_slash" : string.Empty) + ".png";
                                });
                            }

                            textFieldCell.InitEvents();
                            FieldsSection.Add(textFieldCell);
                            break;
                        case FieldType.Boolean:
                            var switchFieldCell = new ExtendedSwitchCell
                            {
                                Text = label,
                                On = value == "true"
                            };
                            FieldsSection.Add(switchFieldCell);
                            break;
                        default:
                            continue;
                    }
                }
            }
            else
            {
                Content = NoDataLabel;
                if(Device.RuntimePlatform == Device.iOS)
                {
                    ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
                }
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            if(FieldsSection != null && FieldsSection.Count > 0)
            {
                foreach(var cell in FieldsSection)
                {
                    if(cell is FormEntryCell entrycell)
                    {
                        entrycell.Dispose();
                    }
                }
            }
        }

        private void AlertNoConnection()
        {
            DisplayAlert(AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage,
                AppResources.Ok);
        }
    }
}
