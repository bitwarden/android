using System;

namespace Bit.App.Models
{
    public abstract class Cipher
    {
        public int Id { get; set; }
        public string ServerId { get; set; }
        public CipherString Name { get; set; }
    }
}
