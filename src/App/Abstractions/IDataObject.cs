using System;

namespace Bit.App.Abstractions
{
    public interface IDataObject<T> where T : IEquatable<T>
    {
        T Id { get; }
    }
}
