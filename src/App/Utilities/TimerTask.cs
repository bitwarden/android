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
        private readonly Func<Task> _func;
        private readonly CancellationToken _cancellationToken;

        public TimerTask(ILogger logger, Action action, CancellationToken cancellationToken)
        {
            _logger = logger;
            _action = action ?? throw new ArgumentNullException();
            _cancellationToken = cancellationToken;
        }

        public TimerTask(ILogger logger, Func<Task> action, CancellationToken cancellationToken)
        {
            _logger = logger;
            _func = action ?? throw new ArgumentNullException();
            _cancellationToken = cancellationToken;
        }

        public Task RunPeriodic(TimeSpan? interval = null)
        {
            interval = interval ?? TimeSpan.FromSeconds(1);
            return Task.Run(async () =>
            {
                try
                {
                    while (!_cancellationToken.IsCancellationRequested)
                    {
                        await Device.InvokeOnMainThreadAsync(async () =>
                        {
                            if (!_cancellationToken.IsCancellationRequested)
                            {
                                try
                                {
                                    if (_action != null)
                                    {
                                        _action();
                                    }
                                    else if (_func != null)
                                    {
                                        await _func();
                                    }
                                }
                                catch (Exception ex)
                                {
                                    _logger?.Exception(ex);
                                }
                            }
                        });
                        await Task.Delay(interval.Value, _cancellationToken);
                    }
                }
                catch (TaskCanceledException) { }
                catch (Exception ex)
                {
                    _logger?.Exception(ex);
                }
            }, _cancellationToken);
        }
    }
}
