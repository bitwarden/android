using System.ComponentModel;
using System.Runtime.CompilerServices;
using Bit.App.Abstractions;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Resources.Localization;
using CommunityToolkit.Mvvm.Input;

namespace Bit.Core.Utilities
{
    public abstract class ExtendedViewModel : INotifyPropertyChanged
    {
        protected LazyResolve<IDeviceActionService> _deviceActionService = new LazyResolve<IDeviceActionService>();
        protected LazyResolve<IPlatformUtilsService> _platformUtilsService = new LazyResolve<IPlatformUtilsService>();
        protected LazyResolve<ILogger> _logger = new LazyResolve<ILogger>();

        public event PropertyChangedEventHandler PropertyChanged;

        protected AsyncRelayCommand CreateDefaultAsyncRelayCommand(Func<Task> execute, Func<bool> canExecute = null, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
        {
            var safeCanExecute = canExecute;
            if (canExecute is null)
            {
                safeCanExecute = () => true;
            }

            async Task doAsync()
            {
                try
                {
                    await execute?.Invoke();
                }
                catch (Exception ex)
                {
                    if (onException != null)
                    {
                        onException(ex);
                    }
                    else
                    {
                        HandleException(ex);
                    }
                }
            }

            return new AsyncRelayCommand(doAsync, safeCanExecute, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        protected AsyncRelayCommand<T> CreateDefaultAsyncRelayCommand<T>(Func<T, Task> execute, Predicate<T?> canExecute = null, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
        {
            var safeCanExecute = canExecute;
            if (canExecute is null)
            {
                safeCanExecute = _ => true;
            }

            async Task doAsync(T foo)
            {
                try
                {
                    await execute?.Invoke(foo);
                }
                catch (Exception ex)
                {
                    if (onException != null)
                    {
                        onException(ex);
                    }
                    else
                    {
                        HandleException(ex);
                    }
                }
            }

            return new AsyncRelayCommand<T>(doAsync, safeCanExecute, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        protected void HandleException(Exception ex, string message = null)
        {
            if (ex is ApiException apiException && apiException.Error != null)
            {
                message = apiException.Error.GetSingleMessage();
            }

            Microsoft.Maui.ApplicationModel.MainThread.InvokeOnMainThreadAsync(async () =>
            {
                await _deviceActionService.Value.HideLoadingAsync();
                await _platformUtilsService.Value.ShowDialogAsync(message ?? AppResources.GenericErrorMessage);
            }).FireAndForget();
            _logger.Value.Exception(ex);
        }

        protected bool SetProperty<T>(ref T backingStore, T value, Action onChanged = null,
            [CallerMemberName] string propertyName = "", string[] additionalPropertyNames = null)
        {
            if (EqualityComparer<T>.Default.Equals(backingStore, value))
            {
                return false;
            }

            backingStore = value;
            TriggerPropertyChanged(propertyName, additionalPropertyNames);
            onChanged?.Invoke();
            return true;
        }

        protected void TriggerPropertyChanged(string propertyName, string[] additionalPropertyNames = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            if (PropertyChanged != null && additionalPropertyNames != null)
            {
                foreach (var prop in additionalPropertyNames)
                {
                    PropertyChanged.Invoke(this, new PropertyChangedEventArgs(prop));
                }
            }
        }
    }
}
