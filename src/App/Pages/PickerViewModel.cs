using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;

namespace Bit.App.Pages
{
    public class PickerViewModel<TKey> : ExtendedViewModel
    {
        const string SELECTED_CHARACTER = "✓";

        private readonly IDeviceActionService _deviceActionService;
        private readonly ILogger _logger;
        private readonly Func<TKey, Task<bool>> _onSelectionChangingAsync;
        private readonly string _title;

        public Dictionary<TKey, string> _items;
        private TKey _selectedKey;
        private TKey _defaultSelectedKeyIfFailsToFind;
        private Func<TKey, Task> _afterSelectionChangedAsync;

        public PickerViewModel(IDeviceActionService deviceActionService,
            ILogger logger,
            Func<TKey, Task<bool>> onSelectionChangingAsync,
            string title,
            Func<object, bool> canExecuteSelectOptionCommand = null,
            Action<Exception> onSelectOptionCommandException = null)
        {
            _deviceActionService = deviceActionService;
            _logger = logger;
            _onSelectionChangingAsync = onSelectionChangingAsync;
            _title = title;

            SelectOptionCommand = new AsyncCommand(SelectOptionAsync, canExecuteSelectOptionCommand, onSelectOptionCommandException, allowsMultipleExecutions: false);
        }

        public AsyncCommand SelectOptionCommand { get; }

        public TKey SelectedKey => _selectedKey;

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

        public void Init(Dictionary<TKey, string> items, TKey currentSelectedKey, TKey defaultSelectedKeyIfFailsToFind, bool logIfKeyNotFound = true)
        {
            _items = items;
            _defaultSelectedKeyIfFailsToFind = defaultSelectedKeyIfFailsToFind;

            Select(currentSelectedKey, logIfKeyNotFound);
        }

        public void Select(TKey key, bool logIfKeyNotFound = true)
        {
            if (!_items.ContainsKey(key))
            {
                if (logIfKeyNotFound)
                {
                    _logger.Error($"There is no {_title} options for key: {key}");
                }
                key = _defaultSelectedKeyIfFailsToFind;
            }

            _selectedKey = key;

            MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(SelectedValue)));
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

            if (EqualityComparer<TKey>.Default.Equals(optionKey, _selectedKey)
                ||
                !await _onSelectionChangingAsync(optionKey))
            {
                return;
            }

            _selectedKey = optionKey;
            TriggerPropertyChanged(nameof(SelectedValue));

            if (_afterSelectionChangedAsync != null)
            {
                await _afterSelectionChangedAsync(_selectedKey);
            }
        }

        public void SetAfterSelectionChanged(Func<TKey, Task> afterSelectionChangedAsync) => _afterSelectionChangedAsync = afterSelectionChangedAsync;

        private string CreateSelectableOption(string option, bool selected) => selected ? ToSelectedOption(option) : option;

        private string ToSelectedOption(string option) => $"{SELECTED_CHARACTER} {option}";
    }
}
