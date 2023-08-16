using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Bit.App.Pages
{
    public class PickerViewModel<TKey> : ExtendedViewModel
    {
        const string SELECTED_CHARACTER = "✓";

        private readonly IDeviceActionService _deviceActionService;
        private readonly Func<TKey, Task<bool>> _onSelectionChangedAsync;
        private readonly string _title;

        public Dictionary<TKey, string> _items;
        private TKey _selectedKey;
        private TKey _defaultSelectedKeyIfFailsToFind;

        public PickerViewModel(IDeviceActionService deviceActionService,
            Func<TKey, Task<bool>> onSelectionChangedAsync,
            string title,
            Func<object, bool> canExecuteSelectOptionCommand = null,
            Action<Exception> onSelectOptionCommandException = null)
        {
            _deviceActionService = deviceActionService;
            _onSelectionChangedAsync = onSelectionChangedAsync;
            _title = title;

            SelectOptionCommand = new AsyncCommand(SelectOptionAsync, canExecuteSelectOptionCommand, onSelectOptionCommandException, allowsMultipleExecutions: false);
        }

        public void Init(Dictionary<TKey, string> items, TKey currentSelectedKey, TKey defaultSelectedKeyIfFailsToFind)
        {
            _items = items;
            _selectedKey = currentSelectedKey;
            _defaultSelectedKeyIfFailsToFind = defaultSelectedKeyIfFailsToFind;

            TriggerPropertyChanged(nameof(SelectedValue));
        }

        public AsyncCommand SelectOptionCommand { get; }

        public string SelectedValue
        {
            get
            {
                if (_items.TryGetValue(_selectedKey, out var option))
                {
                    return option;
                }

                _selectedKey = _defaultSelectedKeyIfFailsToFind;
                return _items[_selectedKey];
            }
        }

        private async Task SelectOptionAsync()
        {
            var selection = await _deviceActionService.DisplayActionSheetAsync(_title,
                AppResources.Cancel,
                null,
                _items.Select(o => CreateSelectableOption(o.Value, EqualityComparer<TKey>.Default.Equals(o.Key, _selectedKey)))
                      .ToArray()
            );

            if (selection == null || selection == AppResources.Cancel)
            {
                return;
            }

            var sanitizedSelection = selection.Replace($"{SELECTED_CHARACTER} ", string.Empty);
            var optionKey = _items.First(o => o.Value == sanitizedSelection).Key;

            if (!await _onSelectionChangedAsync(optionKey))
            {
                return;
            }

            _selectedKey = optionKey;
            TriggerPropertyChanged(nameof(SelectedValue));
        }

        private string CreateSelectableOption(string option, bool selected) => selected ? ToSelectedOption(option) : option;

        private string ToSelectedOption(string option) => $"{SELECTED_CHARACTER} {option}";
    }
}
