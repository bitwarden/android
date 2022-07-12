using System;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities
{
    public class TimerTask
    {
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");
        private readonly Action _action;
        private readonly CancellationTokenSource _cancellationToken;

        public TimerTask(Action action, CancellationTokenSource cancellationToken)
        {
            _action = action;
            _cancellationToken = cancellationToken;
        }

        public Task Run()
        {
            return Task.Run(async () =>
            {
                try
                {
                    while (!_cancellationToken.IsCancellationRequested)
                    {
                        await Task.Delay(TimeSpan.FromSeconds(1), _cancellationToken.Token);
                        await Device.InvokeOnMainThreadAsync(() =>
                        {
                            if (!_cancellationToken.IsCancellationRequested)
                            {
                                try
                                {
                                    _action?.Invoke();
                                }
                                catch (Exception ex)
                                {
                                    _logger?.Value?.Exception(ex);
                                }
                            }
                        });
                        Console.WriteLine("TESTES TASK RUNNING");
                    }
                }
                catch (TaskCanceledException) { }
                catch (Exception ex)
                {
                    _logger?.Value?.Exception(ex);
                }
            }, _cancellationToken.Token);
        }
    }
}
