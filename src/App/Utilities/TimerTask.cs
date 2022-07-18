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
        private readonly CancellationTokenSource _cancellationToken;

        public TimerTask(ILogger logger, Action action, CancellationTokenSource cancellationToken)
        {
            _logger = logger;
            _action = action ?? throw new ArgumentNullException();
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
                        await Device.InvokeOnMainThreadAsync(() =>
                        {
                            if (!_cancellationToken.IsCancellationRequested)
                            {
                                try
                                {
                                    _action();
                                }
                                catch (Exception ex)
                                {
                                    _logger?.Exception(ex);
                                }
                            }
                        });
                        await Task.Delay(interval.Value, _cancellationToken.Token);
                    }
                }
                catch (TaskCanceledException) { }
                catch (Exception ex)
                {
                    _logger?.Exception(ex);
                }
            }, _cancellationToken.Token);
        }
    }
}
