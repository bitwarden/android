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
        private readonly ILoginService _loginService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;
        private readonly IGoogleAnalyticsService _googleAnalyticsService;
        private readonly string _loginId;
        private Login _login;
        private DateTime? _lastAction;

        public VaultCustomFieldsPage(string loginId)
            : base(true)
        {
            _loginId = loginId;
            _loginService = Resolver.Resolve<ILoginService>();
            _connectivity = Resolver.Resolve<IConnectivity>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();

            Init();
        }

        public ToolbarItem SaveToolbarItem { get; set; }
        public Label NoDataLabel { get; set; }
        public TableSection FieldsSection { get; set; }
        public ExtendedTableView Table { get; set; }

        private void Init()
        {
            FieldsSection = new TableSection(" ");

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

            SaveToolbarItem = new ToolbarItem(AppResources.Save, null, async () =>
            {
                if(_lastAction.LastActionWasRecent() || _login == null)
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
                                    entryCell.Label.Text.Encrypt(_login.OrganizationId),
                                Value = string.IsNullOrWhiteSpace(entryCell.Entry.Text) ? null :
                                    entryCell.Entry.Text.Encrypt(_login.OrganizationId),
                                Type = entryCell.Entry.IsPassword ? FieldType.Hidden : FieldType.Text
                            });
                        }
                        else if(cell is ExtendedSwitchCell switchCell)
                        {
                            var value = switchCell.On ? "true" : "false";
                            fields.Add(new Field
                            {
                                Name = string.IsNullOrWhiteSpace(switchCell.Text) ? null :
                                    switchCell.Text.Encrypt(_login.OrganizationId),
                                Value = value.Encrypt(_login.OrganizationId),
                                Type = FieldType.Boolean
                            });
                        }
                    }
                    _login.Fields = fields;
                }
                else
                {
                    _login.Fields = null;
                }

                _userDialogs.ShowLoading(AppResources.Saving, MaskType.Black);
                var saveTask = await _loginService.SaveAsync(_login);

                _userDialogs.HideLoading();

                if(saveTask.Succeeded)
                {
                    _userDialogs.Toast(AppResources.CustomFieldsUpdated);
                    _googleAnalyticsService.TrackAppEvent("UpdatedCustomFields");
                    await Navigation.PopForDeviceAsync();
                }
                else if(saveTask.Errors.Count() > 0)
                {
                    await _userDialogs.AlertAsync(saveTask.Errors.First().Message, AppResources.AnErrorHasOccurred);
                }
                else
                {
                    await _userDialogs.AlertAsync(AppResources.AnErrorHasOccurred);
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

            _login = await _loginService.GetByIdAsync(_loginId);
            if(_login == null)
            {
                await Navigation.PopForDeviceAsync();
                return;
            }

            if(_login.Fields != null && _login.Fields.Any())
            {
                Content = Table;
                ToolbarItems.Add(SaveToolbarItem);
                if(Device.RuntimePlatform == Device.iOS)
                {
                    ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Cancel));
                }

                foreach(var field in _login.Fields)
                {
                    var label = field.Name?.Decrypt(_login.OrganizationId) ?? string.Empty;
                    var value = field.Value?.Decrypt(_login.OrganizationId);
                    switch(field.Type)
                    {
                        case FieldType.Text:
                        case FieldType.Hidden:
                            var hidden = field.Type == FieldType.Hidden;

                            var textFieldCell = new FormEntryCell(label, isPassword: hidden);
                            textFieldCell.Entry.Text = value;
                            textFieldCell.Entry.DisableAutocapitalize = true;
                            textFieldCell.Entry.Autocorrect = false;

                            if(hidden)
                            {
                                textFieldCell.Entry.FontFamily = Helpers.OnPlatform(
                                    iOS: "Menlo-Regular", Android: "monospace", WinPhone: "Courier");
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
