using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Lists.ItemViewModels.CustomFields
{
    public class HiddenCustomFieldItemViewModel : BaseCustomFieldItemViewModel
    {
        private readonly CipherView _cipher;
        private readonly IPasswordPromptable _passwordPromptable;
        private readonly IEventService _eventService;
        private bool _showHiddenValue;

        public HiddenCustomFieldItemViewModel(FieldView field,
                                              bool isEditing,
                                              ICommand fieldOptionsCommand,
                                              CipherView cipher,
                                              IPasswordPromptable passwordPromptable,
                                              IEventService eventService,
                                              ICommand copyFieldCommand)
            : base(field, isEditing, fieldOptionsCommand)
        {
            _cipher = cipher;
            _passwordPromptable = passwordPromptable;
            _eventService = eventService;

            CopyFieldCommand = new Command(() => copyFieldCommand?.Execute(Field));
            ToggleHiddenValueCommand = new AsyncCommand(ToggleHiddenValueAsync, null, ex =>
            {
//#if !FDROID
//                Microsoft.AppCenter.Crashes.Crashes.TrackError(ex);
//#endif
            });
        }

        public ICommand CopyFieldCommand { get; }

        public ICommand ToggleHiddenValueCommand { get; set; }

        public bool ShowHiddenValue
        {
            get => _showHiddenValue;
            set => SetProperty(ref _showHiddenValue, value);
        }

        public bool ShowViewHidden => _cipher.ViewPassword || (_isEditing && _field.NewField);

        public override bool ShowCopyButton => !_isEditing && _cipher.ViewPassword && !string.IsNullOrWhiteSpace(Field.Value);

        public async Task ToggleHiddenValueAsync()
        {
            if (!_isEditing && !await _passwordPromptable.PromptPasswordAsync())
            {
                return;
            }

            ShowHiddenValue = !ShowHiddenValue;
            if (ShowHiddenValue && (!_isEditing || _cipher?.Id != null))
            {
                await _eventService.CollectAsync(
                    Core.Enums.EventType.Cipher_ClientToggledHiddenFieldVisible, _cipher.Id);
            }
        }
    }
}
