using System.Collections.Generic;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using MessagePack;

namespace Bit.Core.Models
{
    [MessagePackObject]
    public class WatchDTO
    {
        public WatchDTO()
        {
        }

        public WatchDTO(WatchState state)
        {
            State = state;
        }

        [Key(0)]
        public WatchState State { get; private set; }

        [Key(1)]
        public List<SimpleCipherView> Ciphers { get; set; }

        [Key(2)]
        public UserDataDto UserData { get; set; }

        [Key(3)]
        public EnvironmentUrlDataDto EnvironmentData { get; set; }

        //public SettingsDataDto SettingsData { get; set; }

        [MessagePackObject]
        public class UserDataDto
        {
            [Key(0)]
            public string Id { get; set; }

            [Key(1)]
            public string Email { get; set; }

            [Key(2)]
            public string Name { get; set; }
        }

        [MessagePackObject]
        public class EnvironmentUrlDataDto
        {
            [Key(0)]
            public string Base { get; set; }

            [Key(1)]
            public string Icons { get; set; }
        }

        //public class SettingsDataDto
        //{
        //    public int? VaultTimeoutInMinutes { get; set; }

        //    public VaultTimeoutAction VaultTimeoutAction { get; set; }
        //}
    }
}
