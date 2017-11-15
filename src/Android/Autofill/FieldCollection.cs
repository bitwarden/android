using System.Collections.Generic;
using Android.Service.Autofill;
using Android.Views.Autofill;

namespace Bit.Android.Autofill
{
    public class FieldCollection
    {
        public HashSet<int> Ids { get; private set; } = new HashSet<int>();
        public List<AutofillId> AutofillIds { get; private set; } = new List<AutofillId>();
        public SaveDataType SaveType { get; private set; } = SaveDataType.Generic;
        public List<string> Hints { get; private set; } = new List<string>();
        public List<string> FocusedHints { get; private set; } = new List<string>();
        public List<Field> Fields { get; private set; } = new List<Field>();
        public IDictionary<int, Field> IdToFieldMap { get; private set; } =
            new Dictionary<int, Field>();
        public IDictionary<string, List<Field>> HintToFieldsMap { get; private set; } =
            new Dictionary<string, List<Field>>();

        public void Add(Field field)
        {
            if(Ids.Contains(field.Id))
            {
                return;
            }

            SaveType |= field.SaveType;
            Ids.Add(field.Id);
            Fields.Add(field);
            AutofillIds.Add(field.AutofillId);
            IdToFieldMap.Add(field.Id, field);

            if((field.Hints?.Count ?? 0) > 0)
            {
                Hints.AddRange(field.Hints);
                if(field.Focused)
                {
                    FocusedHints.AddRange(field.Hints);
                }

                foreach(var hint in field.Hints)
                {
                    if(!HintToFieldsMap.ContainsKey(hint))
                    {
                        HintToFieldsMap.Add(hint, new List<Field>());
                    }

                    HintToFieldsMap[hint].Add(field);
                }
            }
        }
    }
}