using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.Core.Models
{
    public class WatchDTO
    {
        public WatchDTO(WatchState state)
        {
            State = state;
        }

        public WatchState State { get; private set; }

        public List<SimpleCipherView> Ciphers { get; set; }

        public UserDataDto UserData { get; set; }

        public EnvironmentUrlDataDto EnvironmentData { get; set; }

        //public SettingsDataDto SettingsData { get; set; }

        public class UserDataDto
        {
            public string Id { get; set; }
            public string Email { get; set; }
            public string Name { get; set; }
        }

        public class EnvironmentUrlDataDto
        {
            public string Base { get; set; }
            public string Icons { get; set; }
        }

        //public class SettingsDataDto
        //{
        //    public int? VaultTimeoutInMinutes { get; set; }

        //    public VaultTimeoutAction VaultTimeoutAction { get; set; }
        //}
    }
}
