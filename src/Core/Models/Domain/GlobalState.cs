using System;

namespace Bit.Core.Models.Domain
{
    public class GlobalState : Domain
    {
        public int? StateVersion { get; set; }
        public bool EnableBiometrics { get; set; }
    }
}
