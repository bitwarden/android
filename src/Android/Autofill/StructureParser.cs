using static Android.App.Assist.AssistStructure;
using Android.App.Assist;

namespace Bit.Android.Autofill
{
    public class StructureParser
    {
        private readonly AssistStructure _structure;
        private FilledAutofillFieldCollection _filledAutofillFieldCollection;

        public StructureParser(AssistStructure structure)
        {
            _structure = structure;
        }

        public AutofillFieldMetadataCollection AutofillFields { get; private set; }
            = new AutofillFieldMetadataCollection();

        public void ParseForFill()
        {
            Parse(true);
        }

        public void ParseForSave()
        {
            Parse(false);
        }

        /**
         * Traverse AssistStructure and add ViewNode metadata to a flat list.
         */
        private void Parse(bool forFill)
        {
            _filledAutofillFieldCollection = new FilledAutofillFieldCollection();

            for(var i = 0; i < _structure.WindowNodeCount; i++)
            {
                var node = _structure.GetWindowNodeAt(i);
                var view = node.RootViewNode;
                ParseLocked(forFill, view);
            }
        }

        private void ParseLocked(bool forFill, ViewNode viewNode)
        {
            var autofillHints = viewNode.GetAutofillHints();
            var autofillType = viewNode.AutofillType;
            var inputType = viewNode.InputType;
            var isEditText = viewNode.ClassName == "android.widget.EditText";
            if(isEditText || (autofillHints?.Length ?? 0) > 0)
            {
                if(forFill)
                {
                    AutofillFields.Add(new AutofillFieldMetadata(viewNode));
                }
                else
                {
                    _filledAutofillFieldCollection.Add(new FilledAutofillField(viewNode));
                }
            }

            for(var i = 0; i < viewNode.ChildCount; i++)
            {
                ParseLocked(forFill, viewNode.GetChildAt(i));
            }
        }

        public FilledAutofillFieldCollection GetClientFormData()
        {
            return _filledAutofillFieldCollection;
        }
    }
}