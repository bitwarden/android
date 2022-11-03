using System;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class TimerTask
    {
        private readonly ILogger _logger;
        private readonly Action _action;
        private readonly Func<Task> _actionTask;
        private readonly CancellationTokenSource _cancellationTokenSource;

        public TimerTask(ILogger logger, Action action, CancellationTokenSource cancellationTokenSource)
        {
            _logger = logger;
            _action = action ?? throw new ArgumentNullException(nameof(action));
            _cancellationTokenSource = cancellationTokenSource;
        }

        public TimerTask(ILogger logger, Func<Task> actionTask, CancellationTokenSource cancellationTokenSource)
        {
            _logger = logger;
            _actionTask = actionTask ?? throw new ArgumentNullException(nameof(actionTask));
            _cancellationTokenSource = cancellationTokenSource;
        }

        public Task RunPeriodic(TimeSpan? interval = null)
        {
            interval = interval ?? TimeSpan.FromSeconds(1);
            return Task.Run(async () =>
            {
                try
                {
                    while (!_cancellationTokenSource.IsCancellationRequested)
                    {
                        await Device.InvokeOnMainThreadAsync(async () =>
                        {
                            if (!_cancellationTokenSource.IsCancellationRequested)
                            {
                                try
                                {
                                    if (_action != null)
                                    {
                                        _action();
                                    }
                                    else if (_actionTask != null)
                                    {
                                        await _actionTask();
                                    }
                                }
                                catch (Exception ex)
                                {
                                    _cancellationTokenSource?.Cancel();
                                    _logger?.Exception(ex);
                                }
                            }
                        });
                        await Task.Delay(interval.Value, _cancellationTokenSource.Token);
                    }
                }
                catch (TaskCanceledException) { }
                catch (Exception ex)
                {
                    _logger?.Exception(ex);
                }
            }, _cancellationTokenSource.Token);
        }
    }
}
