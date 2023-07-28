using System;
using System.Threading.Tasks;
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

        public AsyncCommand(Func<Task> execute, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
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

            _relayCommand = new AsyncRelayCommand(doAsync, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        public event EventHandler CanExecuteChanged;

        public bool CanExecute(object parameter) => _relayCommand.CanExecute(parameter);
        public void Execute(object parameter) => _relayCommand.Execute(parameter);
    }

    /// Wrapper of <see cref="AsyncRelayCommand"/> just to ease with the MAUI migration process.
    /// After the process is done, remove this and use AsyncRelayCommand directly
    /// </summary>
    public class AsyncCommand<T> : ICommand
    {
        readonly AsyncRelayCommand<T> _relayCommand;

        public AsyncCommand(Func<T, Task> execute, Action<Exception> onException = null, bool allowsMultipleExecutions = true)
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

            _relayCommand = new AsyncRelayCommand<T>(doAsync, allowsMultipleExecutions ? AsyncRelayCommandOptions.AllowConcurrentExecutions : AsyncRelayCommandOptions.None);
        }

        public event EventHandler CanExecuteChanged;

        public bool CanExecute(object parameter) => _relayCommand.CanExecute(parameter);
        public void Execute(object parameter) => _relayCommand.Execute(parameter);
    }
}
