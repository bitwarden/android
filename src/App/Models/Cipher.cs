using System;

namespace Bit.App.Models
{
    public abstract class Cipher
    {
        public string Id { get; set; }
        public CipherString Name { get; set; }
    }
}
