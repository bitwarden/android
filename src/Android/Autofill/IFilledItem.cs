using Android.Service.Autofill;
using System;

namespace Bit.Android.Autofill
{
    public interface IFilledItem
    {
        string Name { get; set; }
        string Subtitle { get; set; }
        bool ApplyToFields(FieldCollection fieldCollection, Dataset.Builder datasetBuilder);
    }
}