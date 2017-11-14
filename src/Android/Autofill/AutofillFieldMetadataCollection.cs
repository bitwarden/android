using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views;
using Android.Views.Autofill;

namespace Bit.Android.Autofill
{
    public class AutofillFieldMetadataCollection
    {
        private int _size = 0;

        public List<int> Ids { get; private set; } = new List<int>();
        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType { get; private set; } = SaveDataType.Generic;
        public List<string> AutofillHints { get; private set; } = new List<string>();
        public List<string> FocusedAutofillHints { get; private set; } = new List<string>();
        public List<AutofillFieldMetadata> Feilds { get; private set; }
        public IDictionary<int, AutofillFieldMetadata> IdToFieldMap { get; private set; } =
            new Dictionary<int, AutofillFieldMetadata>();
        public IDictionary<string, List<AutofillFieldMetadata>> AutofillHintsToFieldsMap { get; private set; } =
            new Dictionary<string, List<AutofillFieldMetadata>>();

        public void Add(AutofillFieldMetadata data)
        {
            _size++;
            SaveType |= data.SaveType;
            Ids.Add(data.Id);
            AutofillIds.Add(data.AutofillId);
            IdToFieldMap.Add(data.Id, data);

            if((data.AutofillHints?.Count ?? 0) > 0)
            {
                AutofillHints.AddRange(data.AutofillHints);
                if(data.IsFocused)
                {
                    FocusedAutofillHints.AddRange(data.AutofillHints);
                }

                foreach(var hint in data.AutofillHints)
                {
                    if(!AutofillHintsToFieldsMap.ContainsKey(hint))
                    {
                        AutofillHintsToFieldsMap.Add(hint, new List<AutofillFieldMetadata>());
                    }

                    AutofillHintsToFieldsMap[hint].Add(data);
                }
            }
        }
    }
}