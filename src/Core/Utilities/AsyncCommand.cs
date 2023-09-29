using System.Windows.Input;
using CommunityToolkit.Mvvm.Input;

namespace Bit.App.Utilities
{
    // TODO: [MAUI-Migration] DELETE WHEN MIGRATION IS DONE
    /// <summary>

    /// Wrapper of <see cref="AsyncRelayCommand"/> just to ease with the MAUI migration process.
    /// After the process is done, remove this and use AsyncRelayCommand directly
    /// </summary>
    public class AsyncCommand : ICommand
    {
        readonly AsyncRelayCommand _relayCommand;

        public AsyncCommand(Func<Task> execute, Func<bool> canExecute = null, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
        {
            async Task doAsync()
            {
                try
                {
                    await execute?.Invoke();
                }
                catch (Exception ex)
                {
                    onException?.Invoke(ex);
                }
            }

            var safeCanExecute = canExecute;
            if (canExecute is null)
            {
                safeCanExecute = () => true;
            }
            _relayCommand = new AsyncRelayCommand(doAsync, safeCanExecute, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        public event EventHandler CanExecuteChanged;

        public bool CanExecute(object parameter) => _relayCommand.CanExecute(parameter);
        public void Execute(object parameter) => _relayCommand.Execute(parameter);
        public void RaiseCanExecuteChanged() => _relayCommand.NotifyCanExecuteChanged();
    }

    /// Wrapper of <see cref="AsyncRelayCommand"/> just to ease with the MAUI migration process.
    /// After the process is done, remove this and use AsyncRelayCommand directly
    /// </summary>
    public class AsyncCommand<T> : ICommand
    {
        readonly AsyncRelayCommand<T> _relayCommand;

        public AsyncCommand(Func<T, Task> execute, Predicate<T?> canExecute = null, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
        {
            async Task doAsync(T foo)
            {
                try
                {
                    await execute?.Invoke(foo);
                }
                catch (Exception ex)
                {
                    onException?.Invoke(ex);
                }
            }

            var safeCanExecute = canExecute;
            if (canExecute is null)
            {
                safeCanExecute = _ => true;
            }
            _relayCommand = new AsyncRelayCommand<T>(doAsync, safeCanExecute, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        public event EventHandler CanExecuteChanged;

        public bool CanExecute(object parameter) => _relayCommand.CanExecute(parameter);
        public void Execute(object parameter) => _relayCommand.Execute(parameter);
        public void RaiseCanExecuteChanged() => _relayCommand.NotifyCanExecuteChanged();
    }
}
