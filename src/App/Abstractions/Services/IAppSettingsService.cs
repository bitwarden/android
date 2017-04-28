using System;

namespace Bit.App.Abstractions
{
    public interface IAppSettingsService
    {
        bool Locked { get; set; }
        DateTime LastActivity { get; set; }
    }
}